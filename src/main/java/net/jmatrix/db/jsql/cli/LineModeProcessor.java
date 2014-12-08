package net.jmatrix.db.jsql.cli;

import java.util.Collection;

import jline.console.completer.Completer;

public interface LineModeProcessor {
   
   
   
   public String prompt();
   
   /**
    * Processes the current line.  returns true if line processing is complete,
    * else returns false - indidating that it is still in control. 
    */
   public LineModeProcessor processLine(String line);
   
   public Collection<Completer> getCompleters();
}
