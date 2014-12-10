package net.jmatrix.db.jsql.cli.commands;

import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.common.console.TextConsole.Level;
import net.jmatrix.db.jsql.JSQL;

/** *
 * Shorthand command: 
 * 
 *   "debug" = set log level debug
 *   "trace" = set log level trace
 */
public class LogLevelCommand extends AbstractCommand {
   static final TextConsole console=SysConsole.getConsole();

   static final String DEBUG="debug";
   static final String TRACE="trace";
   
   public LogLevelCommand(JSQL j) {
      super(j);
   }

   @Override
   public boolean accepts(String command) {
      if (command.equals("debug") ||
          command.equals("trace")) {
         return true;
      }
      return false;
   }

   @Override
   public void process(String line) throws Exception {
      
      if (line.trim().equals(DEBUG)) {
         console.setLevel(Level.DEBUG);
         console.warn("Log level set to "+DEBUG);
      } else if (line.trim().equals(TRACE)) {
         console.setLevel(Level.ALL);
         console.warn("Log level set to all");
      }
   }
}
