package net.jmatrix.db.schema.action;

import net.jmatrix.db.common.ClassLogFactory;
import net.jmatrix.db.schema.DBM;
import net.jmatrix.db.schema.DBMException;

import org.slf4j.Logger;

/**
 * Represents a Manual action - where an operator is required to 
 * execute some action on the database manually. 
 */
public class ManualAction extends AbstractAction {
   private static Logger log=ClassLogFactory.getLog();
   
   String message=null;
   
   public ManualAction(DBM d, String m) {
      super(d);
      message=m;
   }
   
   @Override
   public String toString() {
      return "Manual{"+message+"}";
   }
   
   @Override 
   public boolean equals(Object o) {
      if (o instanceof ManualAction) {
         ManualAction ma=(ManualAction)o;
         return ma.message != null && message != null && ma.message.equals(message);
      }
      return false;
   }
   
   @Override
   public int hashCode() {
      if (message != null)
         return message.hashCode();
      return 0;
   }
   
   @Override
   public String summary() {
      StringBuilder sb=new StringBuilder();
      sb.append("ManualAction: \n");
      sb.append("   "+message);
      
      return sb.toString();
   }

   @Override
   public boolean execute() throws DBMException {
      log.warn("ManualAction: "+message);
      return false;
   }
}
