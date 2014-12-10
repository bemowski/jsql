package net.jmatrix.db.common.console;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import jline.Terminal;
import jline.console.ConsoleReader;
import jline.console.UserInterruptException;
import jline.console.completer.Completer;
import net.jmatrix.db.common.DebugUtils;

public class JLineConsole extends AbstractConsole {
   
   public static final String ANSI_RESET = "\u001B[0m";
   public static final String ANSI_BLACK = "\u001B[30m";
   public static final String ANSI_RED = "\u001B[31m";
   public static final String ANSI_GREEN = "\u001B[32m";
   public static final String ANSI_YELLOW = "\u001B[33m";
   public static final String ANSI_BLUE = "\u001B[34m";
   public static final String ANSI_PURPLE = "\u001B[35m";
   public static final String ANSI_CYAN = "\u001B[36m";
   public static final String ANSI_WHITE = "\u001B[37m";
   
   public static final String ANSI_BOLD = "\u001B[1m";

   
   ConsoleReader reader=null;
   
   PrintWriter out=null;
   Terminal terminal=null;
   
   public JLineConsole() throws IOException {
      reader=new ConsoleReader();
      reader.setHandleUserInterrupt(true);
      out=new PrintWriter(reader.getOutput());
      terminal=reader.getTerminal();
   }
   
   @Override
   public String toString() {
      return "JLineConsole("+getRows()+"x"+getColumns()+", "+level+")";
   }
   
   @Override
   public void setCompleters(Collection<Completer> completers) {
      Collection<Completer> old=reader.getCompleters();

      if (old != null) {
         Collection<Completer> copy=new ArrayList<>();  // defensive copy
         copy.addAll(old);
         
         for (Completer c:copy) 
            reader.removeCompleter(c);
      }
      
      if (completers != null) {
         //debug("Adding "+completers.size()+" completers.");
         for (Completer c:completers) 
            reader.addCompleter(c);
      }
   }
   
   @Override
   public int getRows() {
      return terminal.getHeight();
   }

   @Override
   public int getColumns() {
      return terminal.getWidth();
   }

   @Override
   public void clear() {
      try {
         reader.clearScreen();
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }
   
   public ConsoleReader getReader() {
      return reader;
   }
   
   @Override
   public void print(String s) {
      out.print(s);
      out.flush();
   }
   
   @Override
   public void println(String s) {
      out.println(s);
      out.flush();
   }

   private static final String color(String colorCode, String s) {
      return colorCode+s+ANSI_RESET;
   }
   
   @Override
   public String readLine(String prompt) throws IOException {
      try {
         String value=null; 
         
         if (prompt != null && prompt.toLowerCase().startsWith("password"))
            value=reader.readLine(prompt, new Character('*'));
         else
            value=reader.readLine(prompt);
         
         return value;
      } catch (UserInterruptException ex) {
         String partial=ex.getPartialLine();
         if (partial.length() == 0) {
            println("System exit via CTRL-C");
            System.exit(1);
            return null;
         } else {
            return "";
         }
      }
   }
   
   @Override
   public void error(String s, Throwable t) {
      println(color(ANSI_RED, s));
      if (t != null)
         println(color(ANSI_RED, DebugUtils.stackString(t)));
   }
   
   @Override
   public void warn(String s, Throwable t) {
      println(color(ANSI_YELLOW,s));
      
      if (t != null) {
         if (level.ilevel >= Level.DEBUG.ilevel)
            println(color(ANSI_YELLOW, DebugUtils.stackString(t)));
         else {
            println(color(ANSI_YELLOW ,t.toString()));
            println("Stack trace omitted.  'set log debug' to see full stack traces.");
         }
      }
   }
}
