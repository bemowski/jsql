package net.jmatrix.db.schema.action;

import java.io.File;
import java.util.List;

import net.jmatrix.db.common.ClassLogFactory;
import net.jmatrix.db.schema.DBM;
import net.jmatrix.db.schema.DBMException;
import net.jmatrix.db.schema.DiskVersion;

import org.slf4j.Logger;

public class ReapplyAction extends AbstractAction {
   private static Logger log=ClassLogFactory.getLog();
   
   DiskVersion version=null;
   
   RollbackDiskAction rollback=null;
   ApplyAction apply=null;
   
   public ReapplyAction(DBM d, DiskVersion dv) {
      super(d);
      version=dv;
      
      rollback=new RollbackDiskAction(d, dv);
      apply=new ApplyAction(d, dv);
   }
   
   @Override
   public String toString() {
      return "Reapply("+version.getVersion()+")";
   }
   
   @Override
   public String summary() {
      StringBuilder sb=new StringBuilder();
      sb.append("Reapply: DiskVersion "+version.getVersion()+" at "+version.getPath()+"\n");
      
      List<File> files=null; 
      
      
      files=version.getRollbackFiles();
      if (files != null) {
         sb.append("        Rollback:\n");
         files=version.getRollbackFiles();
         for (File f:files) {
            sb.append("          File: "+f.getName()+"\n");
         }
      }

      files=version.getApplyFiles();
      if (files != null) {
         sb.append("        Apply:\n");
         for (File f:files) {
            sb.append("          File: "+f.getName()+"\n");
         }
      }

      return sb.toString();
   }
   
   @Override
   public boolean execute() throws DBMException {
      log.info(">>>>>>>>>>>>> Reapply "+version);

      if (rollback.execute()) {
         return apply.execute();
      }
      return false;
   }
}
