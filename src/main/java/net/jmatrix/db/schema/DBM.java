package net.jmatrix.db.schema;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.jmatrix.db.common.ClassLogFactory;
import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.DBUtils;
import net.jmatrix.db.common.DebugUtils;
import net.jmatrix.db.common.Version;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.formatters.PrettyFormatter;
import net.jmatrix.db.schema.action.Action;
import net.jmatrix.db.schema.action.ApplyAction;
import net.jmatrix.db.schema.action.InitAction;
import net.jmatrix.db.schema.action.ManualAction;
import net.jmatrix.db.schema.action.ReapplyAction;
import net.jmatrix.db.schema.action.RollbackDBAction;
import net.jmatrix.db.schema.action.RollbackDiskAction;
import net.jmatrix.db.schema.data.v2.DBMData;
import net.jmatrix.db.schema.data.v2.DBMLock;

import org.slf4j.Logger;

/**
 * Command line entry point for database management.
 * 
 * Inputs include: 
 *    - connection properties
 *    - directory of Versions
 * 
 * 
 * 
 * Recommended Updates: 
 *    Case 1: checksum of latest version mismatch.  
 *            Latest version must have been APPLY
 *       Action: reapply version 
 *       
 *    Case 2: (After case 1) DB version less than DiskVersion
 *       Action: UpdateAll - applies all versions up to and including current version.
 *       
 *    Case 3:  DB Version is ahead of disk version (likely a rollback of software)
 *             - all rollback versions must have been APPLYed. If MANUAL - cannot be rolled back.
 *       Action: Rollback to specified version - applying rollback scripts in 
 *               reverse order back to the version specified on disk.  Confirm checksum.
 *    
 *    Other cases: 
 *      - Mid version checksum mismatch - log schema error and continue.
 *        - File checksum does not match file checksum in DB. File was changed.
 *      - Last DB Version checksum mismatch
 *         - Schema was altered out of band.
 */
public class DBM {
   static TextConsole console=SysConsole.getConsole();
   private static Logger log=ClassLogFactory.getLog();
   
   public static final String DBM="DBM";
   public static final String APPLY="APPLY";
   public static final String ROLLBACK="ROLLBACK";
   public static final String MANUAL="MANUAL";
   
   // the path to the directory containing versions of the schema, and patches
   File path=null;
   
   // This is essendially a small DAO managing the SQL for the schema manager.
   DBMData dbmdata=null;
   
   ConnectionInfo conInfo=null;
   
   List<DiskVersion> diskVersions=null;
   List<DBVersion> dbVersions=null;
   
   int LOCK_TIMEOUT=60000;
   
   public DBM(String driver, String url, String user, 
         String pass, File p) throws SQLException {
      this(new ConnectionInfo(driver, url, user, pass), p);
   }
   
   /**
    * @throws SQLException  */
   public DBM(ConnectionInfo ci, File p) throws SQLException {
      path=p;
      conInfo=ci;
      
      dbmdata=new DBMData(conInfo);
   }
   
   public DBMData getDBMData() {
      return dbmdata;
   }
   
   /** */
   public void init() throws IOException, SQLException {
      log.debug("DBM.init()");
      
      conInfo.initDefaultConnection();
      
      //initDB();
      reloadDiskVersions();
      if (!conInfo.isConnected()) {
         conInfo.connect();
      }
      reloadDBVersions();
   }
   
   public void destroy() {
      log.debug("DBM.destroy()");
      if (conInfo != null) 
         conInfo.close();
   }
   
   /**
    * @throws SQLException  
    * @throws IOException */
   public void initDB() throws SQLException, IOException {
      dbmdata.init();
   }
   
   /** */
   public void showDBHistory() throws Exception {
      List<DBVersion> dbversions=dbmdata.getDBVersions();
      PrettyFormatter pf=new PrettyFormatter();
      
      String[] fields=new String[] {
            "version", "action", "hostname", "hostuser", "dbuser", "rollback", "dbChecksum", "fileChecksum", "id" 
      };
      
      String s=pf.format(dbversions, fields, console.getColumns());
      
      log.info(s);
      
      log.info("");
      log.info("Current DB Version: "+getCurrentDBVersion());
   }
   
   public void showDiskVersions() throws Exception {
      if (diskVersions == null) {
         log.warn("No Schema Versions on Disk at "+path);
         return;
      }
      
      PrettyFormatter pf=new PrettyFormatter();
      
      String[] fields=new String[] {
            "version", "applyCount", "rollbackCount", "checksum"
      };
      
      String s=pf.format(diskVersions, fields, console.getColumns());
      
      log.info(s);
      
      log.info("");
      log.info("Current DB Version: "+getCurrentDBVersion());
   }
   
   public void showVersionStatus() throws NoSuchMethodException, 
     SecurityException, IllegalAccessException, IllegalArgumentException, 
     InvocationTargetException, IOException, SQLException {
      reloadDBVersions();
       if (diskVersions == null) {
          log.warn("No Schema Versions on Disk at "+path);
          return;
       }
       if (dbVersions == null) {
          log.warn("no Schema Versions in DB at "+conInfo);
          return;
       }
       
       Set<Version> allVersions=new TreeSet<Version>();
       
       for (DiskVersion version:diskVersions) {
          allVersions.add(version.getVersion());
       }
       for (DBVersion version:dbVersions) {
          allVersions.add(version.getVersion());
       }
       
       List<Version> avlist=new ArrayList<Version>();
       avlist.addAll(allVersions);
       Collections.sort(avlist);
       
       //dbmdata.getCurrentVersion();
       
       Version currentDBVer=getCurrentDBVersion();
       
       List<VersionDisplay> vdlist=new ArrayList<VersionDisplay>();
       for (Version version:avlist) {
          String sver=version.toString();
          DBVersion dbv=findLatestDBVersion(sver);
          
          if (dbv != null && dbv.getVersion().compareTo(currentDBVer) > 0) {
             dbv=null;
          }
          
          DiskVersion dv=findDiskVersion(sver);
          
          VersionDisplay vd=new VersionDisplay(version, dv, dbv);
          vdlist.add(vd);
       }
       
       PrettyFormatter pf=new PrettyFormatter();
       
       String report=pf.format(vdlist, 
             new String[] {"version", "applyCount", "rollbackCount", 
             "dbDate", "action"}, console.getColumns());
       
       log.info(report);
       
       log.info("");
       log.info("Current DB Version: "+getCurrentDBVersion());
   }
   
   public boolean reloadDBVersions() throws SQLException {
      log.debug("Reloading DBM DB Versions from "+conInfo.getUrl());

      if (dbmdata.isDBMSchemaCurrent()) {
         dbVersions=dbmdata.getDBVersions();
         return true;
      } else {
         log.warn("DBM Schema not available or out date.  Update w/ init.");
      }
      return false;
   }
   
   /**
    * @throws IOException  */
   public List<DiskVersion> reloadDiskVersions() throws IOException {
      log.debug("Reloading DBM Disk Versions from "+path);
      if (path == null)
         return null;
      
      File vdirs[]=path.listFiles(new FileFilter() {
         @Override
         public boolean accept(File path) {
            if (path.isDirectory()) 
               return true;
            return false;
         }
      });
      
      diskVersions=new ArrayList<DiskVersion>();
      
      for (File vdir:vdirs) {
         DiskVersion dver=new DiskVersion(vdir);
         
         // In some cases - a directory exists with no valid files.
         // in theses cases, don't add the version, it is not valid.
         if (dver.getApplyCount() <=0 && 
             dver.getRollbackCount() <=0) {
            log.warn("Disk version at path "+vdir+" does not have any SQL. ignoring.");
         } else {
            diskVersions.add(dver);
         }
      }
      
      Collections.sort(diskVersions);
      
      return diskVersions;
   }
   
   public Version getMaxDiskVersion() throws IOException {
      List<DiskVersion> versions=reloadDiskVersions();
      if (versions != null) {
         return versions.get(versions.size()-1).getVersion();
      }
      return null;
   }
   
   /** Manually sets the version to some value.  This can be helpful
    * when starting to manage a schema 'in flight' 
    * @throws IOException 
    * @throws InvocationTargetException 
    * @throws IllegalArgumentException 
    * @throws IllegalAccessException 
    * @throws SecurityException 
    * @throws NoSuchMethodException */
   public void setVersion(String ver) throws SQLException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
      log.info("Attempting to set DB version to '"+ver+"'");
      // make suere the version is valid.
      Version version=new Version(ver);
      
      DBMLock lock=null;
      try {
         lock=dbmdata.acquireLock(DBM, LOCK_TIMEOUT);
         dbmdata.setVersion(version.toString(), MANUAL, null, false, null, null, null);
      } catch (Exception ex) {
         log.error("Error setting DB Version", ex);
      } finally {
         dbmdata.releaseLock(lock);
      }
      
      showVersionStatus();
   }
   
   /** */
   public void reapply(String version) throws SQLException, InterruptedException {
      DiskVersion diskVer=findDiskVersion(version);
      Action action=new ReapplyAction(this, diskVer);
      executeActionWithLock(action);
   }
   
   /** */
   public void rollback(String version) throws SQLException, IOException, InterruptedException {
      DiskVersion diskVer=findDiskVersion(version);
      DBVersion dbVer=findLatestDBApply(version);
      
      // Choose - db or disk rollback?
      
      if (diskVer == null && dbVer == null) {
         throw new DBMException("Cannot rollback version '"+version+
               "' - cannot find disk or db version.");
      } else {
         Action action=null;
         if (dbVer != null) {
            log.info("Rolling back '"+version+"' using database version "+
                     "APPLYed on "+dbVer.getApplyDate());
            action=new RollbackDBAction(this, dbVer);
         } else if (diskVer != null) {
            log.info("DB Version not available for rollback, rolling back using "+
                     "diskVersion "+diskVer);
            action=new RollbackDiskAction(this, diskVer);
         } else {} // not possible.
         
         executeActionWithLock(action);
      }
   }
   
   /** Executes rollback using rollback SQL found on the current
    * version on disk.  */
   public void rollbackDisk(String version) throws SQLException, IOException, InterruptedException {
      DiskVersion diskVer=findDiskVersion(version);
      if (diskVer == null) {
         throw new DBMException("Cannot rollback disk version '"+version+
               "' - cannot find version on disk.");
      } else {
         Action action=new RollbackDiskAction(this, diskVer);
         executeActionWithLock(action);
      }
   }
   
   /** Executes rollback using rollback SQL found on the current
    * version on disk.  */
   public void rollbackDb(String version) throws SQLException, IOException, InterruptedException {
      DBVersion dbVer=findLatestDBApply(version);
      if (dbVer == null) {
         throw new DBMException("Cannot rollback db version '"+version+
               "' - cannot find APPLY in db.");
      } else {
         Action action=new RollbackDBAction(this, dbVer);
         executeActionWithLock(action);
      }
   }
   
   /** */
   public void apply(String version) throws SQLException, IOException, InterruptedException {
      DiskVersion diskVer=findDiskVersion(version);
      Action action=new ApplyAction(this, diskVer);
      executeActionWithLock(action);
   }
   
   private DiskVersion findDiskVersion(String ver) {
      DiskVersion diskVer=null;
      for (DiskVersion diskVersion:diskVersions) {
         if (diskVersion.getVersion().toString().equals(ver)) {
            diskVer=diskVersion;
            break;
         }
      }
      return diskVer;
   }
   
   /**
    * In the case of reapplication of specific versions, it is possible
    * for the DB to have the same version number state a various times
    * in history.
    * 
    * This returns the most recent time that the DB had the queried version.
    */
   private DBVersion findLatestDBVersion(String ver) {
      DBVersion dbv=null;
      for (DBVersion v:dbVersions) {
         if (v.getVersion().toString().equals(ver))
            dbv=v;
      }
      return dbv;
   }
   
   DBVersion findLatestDBApply(String ver) {
      DBVersion dbv=null;
      for (DBVersion v:dbVersions) {
         if (v.getVersion().toString().equals(ver) &&
             v.getAction().equals(APPLY))
            dbv=v;
      }
      return dbv;
   }
   
   public DBVersion findPreviousDBApply(Version version) {
      DBVersion dbv=null;
      for (int i=dbVersions.size()-1; i>=0; i--) {
         DBVersion v=dbVersions.get(i);
         
         if (v.getVersion().compareTo(version) < 0 &&
             v.getAction().equals(APPLY))
            return v;
      }
      return null;
   }
   
   /**
    * This method makes logical comparisons between disk and db versions
    * and recommends actions to bring the two in sync.
    * 
    * Common scenarios: 
    *   1) Disk version greater than db version - recommend apply disk versions
    *   2) DB Version greater than disk version - recommend rollback if possible
    *   3) Current disk revision checksum differnet than that in db.  
    *      recommend: reapply current version.
    */
   public List<Action> recommendUpdateActions() throws IOException, SQLException {
      // First, get current version on disk, and current version in DB.
      
      List<Action> actions=new ArrayList<Action>();

      Version dbVersionNum=getCurrentDBVersion();    // could be -1 if no versions in db, which should not happen
      Version diskVersionNum=getMaxDiskVersion(); // could be null if no path specified.
      
      if (diskVersionNum == null) {
         log.info("DiskVersions are null. Cannot recommend update.");
         return null;
      }
      
      if (dbVersionNum == null) {
         log.warn("DBM DB Version is null - schema not initialized or out of date.  Recommend: init.");
         
         dbVersionNum=new Version("-1");
         
         actions.add(new InitAction(this));
      }
      
      DiskVersion diskVersion=findDiskVersion(diskVersionNum.toString());
      
      DBVersion dbVersion=null; 
      
      if (dbVersionNum.equals(new Version("-1"))) {
        
      } else {
         dbVersion=findLatestDBVersion(dbVersionNum.toString());
      }
      
      if (diskVersionNum.equals(dbVersionNum)) {
         // Compare Checksums, recommend reapply if necessary
         
         // if current revision is the result of a rollback - find 
         // the most recent APPLY
         DBVersion latestApply=findLatestDBApply(dbVersion.getVersion().toString());
         if (latestApply != null) {
            if (!diskVersion.getChecksum().equals(latestApply.getFileChecksum())) {
               log.warn("Disk Version "+diskVersion.getVersion()+" checksum("+
                     diskVersion.getChecksum()+") does not match DB Version "+latestApply.getVersion()+
                     " checksum("+latestApply.getFileChecksum()+")");
               actions.add(new ReapplyAction(this, diskVersion));
            } else {
               // nothing - system is up to date.
               log.info("Disk and Database versions are in sync, checkums match");
            }
         } else {
            log.warn("Cannot find latest APPLY of "+dbVersion.getVersion());
            log.info("Disk and database versions are the same, though cannot compare checksums.");
         }

      }
      else if (diskVersionNum.compareTo(dbVersionNum) > 0) {
         // disk version larger than db version.  apply deltas 
         for (int i=0; i<diskVersions.size(); i++) {
            DiskVersion dv=diskVersions.get(i);
            
            if (dv.getVersion().compareTo(dbVersionNum) > 0) {
               actions.add(new ApplyAction(this, dv));
            }
         }
      } else { //diskVersionNumb.compareTo(dbVersionNum) < 0
         // db version larger than disk version.
         // must recommend manual rollback
         Set<Version> rbv=new HashSet<>();

         // loop backward, and recommend manual actions.
         for (int i=dbVersions.size()-1; i>=0; i--) {
            DBVersion dv=dbVersions.get(i);
            
            if (dv.getVersion().compareTo(diskVersionNum) > 0) {
               DBVersion latestApply=findLatestDBApply(dv.getVersion().toString());
               
               // Latest Apply can be null - if we are rolling back to 
               // a manual set point.
               
               
               log.debug("Version "+dv.getVersion());
               log.debug("   v:"+dv.getApplyDate()+"  / "+dv.getId());
               
               if (latestApply != null) {
                  log.debug("   l:"+latestApply.getApplyDate()+"  / "+latestApply.getId()+" "+latestApply.getVersion());
               } else
                  log.debug("   l: cannot find APPLY in history, likely a manual or initial version.");
               
               Action action=null; 
               
               if (latestApply != null && latestApply.getRollback()) {
                  action=new RollbackDBAction(this, latestApply);
               } else {
                  action=new ManualAction(this, "RollbackManual("+latestApply.getVersion()+")");
               }
               log.debug("   "+action);
               
               if (!rbv.contains(dv.getVersion())) {
                  actions.add(action);
                  rbv.add(dv.getVersion());
               }
            }
         }
      }
      return actions;
   }
   
   public void executeActionWithLock(Action a) throws SQLException, InterruptedException {
      List<Action> actions=new ArrayList<Action>(1);
      actions.add(a);
      executeActionsWithLock(actions);
   }
   
   public void executeActionsWithLock(List<Action> actions) throws SQLException, InterruptedException {
      log.info("Executing "+actions.size()+" actions.");
      
      StringBuilder actionpath=new StringBuilder();
      
      Action first=actions.get(0);
      if (first instanceof InitAction) {
         log.info("First Action is InitDB, executing before lock");
         first.execute();
         actionpath.append(first.toString());
         actions.remove(0);
      }
      
      DBMLock lock=null;
      try {
         lock=dbmdata.acquireLock(DBM, LOCK_TIMEOUT);
         log.debug("Obtained "+lock);
         
         for (Action action:actions) {
            boolean success=action.execute();
            if (actionpath.length() > 0)
               actionpath.append("->");
            
            actionpath.append(action.toString()+": "+(success?"Success":"FAIL"));
         }
         
      } finally {
         if (lock != null) {
            log.debug("Releasing "+lock);
            dbmdata.releaseLock(lock);
         }
      }
      log.info(actionpath.toString());
      reloadDBVersions();
   }
   
   /**
    * Executes all recommended updates - which could include rollbacks.
    */
   public void updateAll() throws SQLException, IOException, InterruptedException {
      List<Action> actions=recommendUpdateActions();
      if (actions.size() > 0) {
         log.info("Executing "+actions.size()+" actions.");
         executeActionsWithLock(actions);
      } else {
         log.info("DBM Schema up to date.");
      }
   }
   
   public Version getCurrentDBVersion() throws SQLException {
      reloadDBVersions();
      
      Version currentVersion=null;
      log.debug("Getting Current DB Version");
      try {
         DBVersion current=dbmdata.getCurrentVersion();
         
         if (current != null) 
            currentVersion=current.getVersion();
         else 
            currentVersion=new Version("-1");
      } catch (Exception ex) {
         log.info("Error getting current Version of database.");
      }
      return currentVersion;
   }
   
   public DBMLock getExistingLock() throws SQLException {
      // Likely null.
      return dbmdata.selectLock(DBM);
   }

   /** */
   public boolean executeStatement(SQLStatement sql) throws SQLException {
      
      log.info("Executing "+sql.getSql());
      boolean success=false;

      try {
         int rows=-1;
         String err=null;
         Statement state=null;
         try {
            Connection con=conInfo.getDefaultConnection();
            state=con.createStatement();
            rows=state.executeUpdate(sql.getSql());
            
            success=true;
         } catch (Exception ex) {
            log.error("Error executing\n "+sql.getSql(), ex);
            String stack=DebugUtils.stackString(ex);
            if (stack.length() > 4000) {
               stack=stack.substring(0, 4000);
            }
            err=stack;
         } finally {
            DBUtils.close(state);
         }
         
         dbmdata.logStatement(sql, success, rows, err);
      } finally {
         
      }
      return success;
   }
   
   public DBMLock lock() throws InterruptedException, SQLException {
      return dbmdata.acquireLock(DBM, LOCK_TIMEOUT);
   }
   
   public void unlock(DBMLock lock) throws SQLException {
      log.info("Attempting to release "+lock);
      dbmdata.releaseLock(lock);
   }
   
   public void forceUnlock(String id) throws SQLException {
      log.info("Attempting to force release any '"+id+"' lock.");
      dbmdata.releaseLock(id);
   }
}
