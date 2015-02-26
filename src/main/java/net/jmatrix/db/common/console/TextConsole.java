package net.jmatrix.db.common.console;

import java.io.IOException;
import java.util.Collection;

import jline.console.completer.Completer;

/* */
public interface TextConsole {
   public int getRows();
   public int getColumns();
   
   public void clear();
   
   //public String readLine() throws IOException;
   public String readLine(String prompt) throws IOException;
   
   public void setCompleters(Collection<Completer> completers);
   
   // console log levels: 
   //   ALL, DEBUG, LOG, WARN, ERROR
   
   public static enum Level {
      ALL(100), DEBUG(10), LOG(8), WARN(6), ERROR(4);

      int ilevel=0;
      
      Level(int i) {
         ilevel=i;
      }
      
      public int getILevel() {return ilevel;}
   };
   
   public void setLevel(Level l);
   public Level getLevel();

   /////////////////////////////////////////////////////////////////////////
   public void info(String s);
   public void info(String s, Throwable t);
   public void debug(String s);
   public void debug(String s, Throwable t);
   public void warn(String s);
   public void warn(String s, Throwable t);
   public void error(String s);
   public void error(String s, Throwable t);
   public void trace(String s);
   public void trace(String s, Throwable t);
   public void print(String s);
   public void println(String s);
}
