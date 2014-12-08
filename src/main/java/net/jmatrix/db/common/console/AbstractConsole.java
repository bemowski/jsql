package net.jmatrix.db.common.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;

import net.jmatrix.db.common.DebugUtils;
import jline.console.completer.Completer;

public abstract class AbstractConsole implements TextConsole {
   Level level=Level.LOG;
   BufferedReader br=null;
   
   protected AbstractConsole() {
      br=new BufferedReader(new InputStreamReader(System.in));
   }
   
   @Override
   public void setCompleters(Collection<Completer> completers) {}
   
   @Override
   public void setLevel(Level l) {
      level=l;
   }
   
   @Override 
   public Level getLevel() {
      return level;
   }
   
   @Override
   public String readLine(String prompt) throws IOException {
      print(prompt);
      return br.readLine();
   }

   @Override
   public void info(String s) {
      info(s, null);
   }

   @Override
   public void info(String s, Throwable t) {
      println (s);

      if (t != null) {
         //t.printStackTrace();
         println(DebugUtils.stackString(t));
      }
   }

   @Override
   public void debug(String s) {
      debug(s, null);
   }

   @Override
   public void debug(String s, Throwable t) {
      if (level.getILevel() >= Level.DEBUG.getILevel()) {
         info(s, t);
      }
   }

   @Override
   public void warn(String s) {
      warn(s, null);
   }

   @Override
   public void warn(String s, Throwable t) {
      info(s, t);
   }

   @Override
   public void error(String s) {
      error(s, null);
   }

   @Override
   public void error(String s, Throwable t) {
      info(s, t);
   }

   @Override
   public void print(String s) {
      System.out.print(s);
   }

   @Override
   public void println(String s) {
      System.out.println(s);
   }
}
