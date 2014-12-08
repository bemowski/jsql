package net.jmatrix.db.schema.action;

import net.jmatrix.db.common.ClassLogFactory;
import net.jmatrix.db.schema.DBM;
import net.jmatrix.db.schema.DBMException;

import org.slf4j.Logger;

public class InitAction extends AbstractAction {
   private static Logger log=ClassLogFactory.getLog();

   public InitAction(DBM d) {
      super(d);
   }
   
   @Override
   public String toString() {
      return "InitDBMAction";
   }
   
   @Override
   public String summary() {
      StringBuilder sb=new StringBuilder();
      
      sb.append("InitDBM:\n");
      sb.append("   Initializes the meta-data tables in the DB to support DBM Schema Management.\n");
      return sb.toString();
   }
   
   
   @Override
   public boolean execute() throws DBMException {
      try {
         dbm.initDB();
      } catch (Exception ex) {
         throw new DBMException("Error Initializing the DBM Schema.", ex);
      }
      return true;
   }
}
