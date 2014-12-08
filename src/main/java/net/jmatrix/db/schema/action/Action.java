package net.jmatrix.db.schema.action;

import net.jmatrix.db.schema.DBMException;

public interface Action {
   
   /**
    * Returns a (possibly) multi-line summary of the details of this action.
    * Example: 

Apply: DiskVersion 1.28.1 at /home/bemo/gitroot/server/db/sql/1.X/1.28.1
          File: 1.survey_number_inserts.sql
          File: 2.survey_number_updates.sql
    */
   public String summary();
   /**
    * Executes said action, and returns true if successful.
    */
   public abstract boolean execute() throws DBMException;
}
