package net.jmatrix.db.schema.action;

import java.io.File;
import java.util.List;

import net.jmatrix.db.common.ClassLogFactory;
import net.jmatrix.db.schema.DBM;
import net.jmatrix.db.schema.DBMException;
import net.jmatrix.db.schema.DBVersion;
import net.jmatrix.db.schema.DiskVersion;
import net.jmatrix.db.schema.SQLStatement;

import org.slf4j.Logger;

public class RollbackDiskAction extends AbstractAction {
   private static Logger log=ClassLogFactory.getLog();
   
   DiskVersion diskVer=null;

   public RollbackDiskAction(DBM d, DiskVersion dv) {
      super(d);
      diskVer=dv;
   }

   @Override
   public String toString() {
      return "RollbackDisk("+diskVer.getVersion()+")";
   }
   
   @Override
   public String summary() {
      StringBuilder sb=new StringBuilder();
      sb.append("Rollback: DiskVersion "+diskVer.getVersion()+" at "+diskVer.getPath()+"\n");
      
      List<File> files=diskVer.getRollbackFiles();
      for (File f:files) {
         sb.append("          File: "+f.getName()+"\n");
      }
      return sb.toString();
   }
   
   @Override
   public boolean execute() throws DBMException {
      try {
         log.info(">>>>>>>>>>>>> Disk Rollback "+diskVer);

         DBVersion currentVersion=dbm.getDBMData().getCurrentVersion();
          
         if (diskVer.getRollbackStatements() == null) {
            log.info("Version "+diskVer.getVersion()+" has no rollback statements.");
            return false;
         }
         
         int count=0;
         boolean versionSuccess=true;
         for (SQLStatement statement:diskVer.getRollbackStatements()) {
            count++;
            boolean success=dbm.executeStatement(statement);
            
            if (!success) {
               log.warn("Failed to execute rollback statement "+count+" from Version "+diskVer.getVersion());
               log.warn("   Path: "+statement.getFile());
               versionSuccess=false;
               //break;
            }
            //DebugUtils.sleep(100);
         }
         
         String previousVersion=dbm.getDBMData().getPreviousVersion();
         log.info("DBM Reports previous version as "+previousVersion);
         log.info("Current Version: "+currentVersion+", rollback requested "+diskVer.getApplyCount());
         
         if (versionSuccess) {
            if (currentVersion != null && 
                (currentVersion.getVersion().equals(diskVer.getVersion()))) {
               
               if (previousVersion != null) {
                  // we only set the version back 1 version if 
                  // we are rolling back the most recent version.  otherwise
                  // leave it unchanged.
                  log.info("Setting DBM Version to "+previousVersion);
                  dbm.getDBMData().setVersion(previousVersion, DBM.ROLLBACK, 
                        diskVer.getPath().getAbsolutePath(), false, null, "rollback "+diskVer, null);
               } else {
                  log.warn("Previous schema version is NULL.  Set version manually if appropriate.");
               }
            } else {
               log.warn("Rolling back something other than most recent version.");
               log.warn("Not updating DBM_VERSION. Set manually if appropriate.");
            }
         }
         else {
            log.error("Some errors in rollback. FIX MANUALLY. Not updating DBM_VERSION");
         }
         return versionSuccess;
         
      } catch (Exception ex) {
         throw new DBMException("Error Rolling back "+diskVer.getVersion()+".", ex);
      }
   }
}
