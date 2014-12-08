package net.jmatrix.db.common.console;

import net.jmatrix.db.common.DebugUtils;
import net.jmatrix.db.common.StreamUtil;
import net.jmatrix.db.common.StringUtils;

public class LinuxConsole extends AbstractConsole implements TextConsole {
   
   public static final String ANSI_RESET = "\u001B[0m";
   public static final String ANSI_BLACK = "\u001B[30m";
   public static final String ANSI_RED = "\u001B[31m";
   public static final String ANSI_GREEN = "\u001B[32m";
   public static final String ANSI_YELLOW = "\u001B[33m";
   public static final String ANSI_BLUE = "\u001B[34m";
   public static final String ANSI_PURPLE = "\u001B[35m";
   public static final String ANSI_CYAN = "\u001B[36m";
   public static final String ANSI_WHITE = "\u001B[37m";
   
   public String toString() {
      return "LinuxConsole ["+getRows()+"x"+getColumns()+"]";
   }

   @Override
   public int getRows() {
      try {
         ProcessBuilder pb=new ProcessBuilder("/bin/bash", "-c", "tput lines 2>/dev/tty");
         
         Process p=pb.start();
         
         int exit=p.waitFor();
         //System.out.println ("Exit: "+exit);
         
         String s=StreamUtil.readToString(p.getInputStream());
         //System.out.println ("  output '"+s+"'");
         return Integer.parseInt(s.trim());
      } catch (Exception ex) {
         ex.printStackTrace();
      }
      return -1;
   }

   @Override
   public int getColumns() {
      try {
         ProcessBuilder pb=new ProcessBuilder("/bin/bash", "-c", "tput cols 2>/dev/tty");
         
         Process p=pb.start();
         
         int exit=p.waitFor();
         //System.out.println ("Exit: "+exit);
         
         String s=StreamUtil.readToString(p.getInputStream());
         
         return Integer.parseInt(s.trim());
      } catch (Exception ex) {
         ex.printStackTrace();
      }
      return -1;
   }

   @Override
   public void clear() {
      int rows=getRows();
      if (rows <=0)
         rows=30;
      System.out.println(StringUtils.pad(rows, '\n'));
   }
   
   private static final String color(String key, String s) {
      return key+s+ANSI_RESET;
   }
   
   ///////////////////////////////////////////////////////////////////////
   @Override
   public void error(String s, Throwable t) {
      System.out.println(color(ANSI_RED, s));
      if (t != null)
         System.out.println(color(ANSI_RED, DebugUtils.stackString(t)));
   }
   @Override
   public void warn(String s, Throwable t) {
      System.out.println(color(ANSI_YELLOW,s));
      if (t != null)
         System.out.println(color(ANSI_YELLOW, DebugUtils.stackString(t)));
   }
}
