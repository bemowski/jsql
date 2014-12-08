package net.jmatrix.db.schema.action;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import net.jmatrix.db.common.ClassLogFactory;
import net.jmatrix.db.schema.DBM;
import net.jmatrix.db.schema.DBMException;
import net.jmatrix.db.schema.DiskVersion;
import net.jmatrix.db.schema.SQLStatement;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/** 
 * 
 */
public class ApplyAction extends AbstractAction {
   private static Logger log=ClassLogFactory.getLog();
   
   DiskVersion version=null;
   
   public ApplyAction(DBM d, DiskVersion dv) {
      super(d);
      version=dv;
   }
   
   public String toString() {
      return "Apply("+version.getVersion()+")";
   }
   
   @Override
   public String summary() {
      StringBuilder sb=new StringBuilder();
      sb.append("Apply: DiskVersion "+version.getVersion()+" at "+version.getPath()+"\n");
      
      List<File> files=version.getApplyFiles();
      for (File f:files) {
         sb.append("          File: "+f.getName()+"\n");
      }
      return sb.toString();
   }
   
   
   @Override
   public boolean execute() throws DBMException {
      try {
         log.info(">>>>>>>>>>>>> Applying "+version);
         
         int count=0;
         boolean versionSuccess=true;
         for (SQLStatement statement:version.getApplyStatements()) {
            count++;
            boolean success=dbm.executeStatement(statement);
            
            if (!success) {
               log.warn("Failed to execute statement "+count+" from Version "+version.getVersion());
               log.warn("   Path: "+statement.getFile());
               versionSuccess=false;
               break;
            }
            //DebugUtils.sleep(100);
         }
         
         if (versionSuccess) {
            // get rollback statements for later if possible.
            String rollback=rollbackJson();
            boolean canrollback=false;
            if (rollback != null && rollback.length() < 2000) {
               canrollback=true;
            } else {
               rollback=null;
            }
            
            dbm.getDBMData().setVersion(version.getVersion().toString(), DBM.APPLY, 
                  version.getPath().getAbsolutePath(), canrollback, 
                  version.getChecksum(), "apply "+version, rollback);
         }
         else {
            log.warn("Not updating DBM Version, fix above errors manually.");
         }
         return versionSuccess;
      } catch (Exception ex) {
         throw new DBMException("Error Applying "+version.getVersion()+".", ex);
      }
   }
   
   String rollbackJson() throws JsonProcessingException {
      List<SQLStatement> rollbackStatements=version.getRollbackStatements();
      List<String> sqls=new ArrayList<String>();
      if (rollbackStatements != null) {
         for (SQLStatement state:rollbackStatements) {
            sqls.add(state.getSql());
         }
      } else {
         return null;
      }
      
      ObjectMapper om=new ObjectMapper();
      String json=om.writeValueAsString(sqls);
      return json;
   }
}
