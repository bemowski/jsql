package net.jmatrix.db.schema.action;

import net.jmatrix.db.common.ClassLogFactory;
import net.jmatrix.db.common.DebugUtils;
import net.jmatrix.db.schema.DBM;
import net.jmatrix.db.schema.DBMException;
import net.jmatrix.db.schema.DBVersion;
import net.jmatrix.db.schema.SQLStatement;

import org.slf4j.Logger;

/**
 * A Rollback Action based on rollback sql statements stored in the 
 * databasae in the DBM_VERSIONS table.
 * 
 */
public class RollbackDBAction extends AbstractAction {
   private static Logger log=ClassLogFactory.getLog();
   
   DBVersion dbVer=null;
   
   public RollbackDBAction(DBM d, DBVersion dv) {
      super(d);
      dbVer=dv;
   }
   
   @Override
   public String toString() {
      return "RollbackDB("+dbVer.getVersion()+"/"+dbVer.getId()+")";
   }
   
   @Override
   public String summary() {
      StringBuilder sb=new StringBuilder();
      sb.append("Rollback: DBVersion "+dbVer.getVersion()+" at "+dbVer.getId()+"\n");
      
      for (String sql:dbVer.getRollbackSql()) {
         sb.append(DebugUtils.indent(sql, 3)+";\n");
      }
      return sb.toString();
   }


   @Override
   public boolean execute() throws DBMException {
      try {
         log.info(">>>>>>>>>>>>> DB Rollback "+dbVer);

         DBVersion currentVersion=dbm.getDBMData().getCurrentVersion();
          
         if (dbVer.getRollbackSql() == null) {
            log.info("Version "+dbVer.getVersion()+" has no rollback statements.");
            return false;
         }
         
         int count=0;
         boolean versionSuccess=true;
         for (SQLStatement statement:dbVer.getRollbackStatements()) {
            count++;
            boolean success=dbm.executeStatement(statement);
            
            if (!success) {
               log.warn("Failed to execute rollback statement "+count+" from Version "+dbVer.getRollbackPath());
               log.warn("   Path: "+statement.getFile());
               versionSuccess=false;
               //break;
            }
            //DebugUtils.sleep(100);
         }
         
         DBVersion previousVersion=dbm.findPreviousDBApply(currentVersion.getVersion());
         log.info("DBM Reports previous version as "+previousVersion);
         log.info("Current Version: "+currentVersion+", rollback requested "+dbVer.getVersion());
         
         if (versionSuccess) {
            if (currentVersion != null && 
                (currentVersion.getVersion().equals(dbVer.getVersion()))) {
               
               if (previousVersion != null) {
                  // we only set the version back 1 version if 
                  // we are rolling back the most recent version.  otherwise
                  // leave it unchanged.
                  log.info("Setting DBM Version to "+previousVersion);
                  dbm.getDBMData().setVersion(previousVersion.getVersion().toString(), 
                        DBM.ROLLBACK, dbVer.getRollbackPath(), 
                        false, null, "rollback "+dbVer, null);
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
         throw new DBMException("Error Rolling back "+dbVer.getVersion()+".", ex);
      }
   }
}
