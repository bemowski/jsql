package net.jmatrix.db.schema.action;

import net.jmatrix.db.schema.DBM;


/**
 * Represents an action that can be taken by the DBM schema manager.
 * 
 * Actions can include - Init, Rollback, Apply, and Reapply(rollback + apply)
 */
public abstract class AbstractAction implements Action {
   protected DBM dbm=null;
   
   public AbstractAction(DBM dbm) {
      this.dbm=dbm;
   }
}
