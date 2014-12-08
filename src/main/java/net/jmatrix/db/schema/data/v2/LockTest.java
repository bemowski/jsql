package net.jmatrix.db.schema.data.v2;

import jline.internal.Log.Level;
import net.jmatrix.db.common.ArgParser;
import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;

public class LockTest {
   static TextConsole console=SysConsole.getConsole();
   
   public static void main(String args[]) throws Exception {
      console.println("Hello console");
      console.setLevel(TextConsole.Level.DEBUG);
      
      ArgParser ap=new ArgParser(args);
      
      
      long wait=ap.getIntArg("-w", 0);
      
      long hold=ap.getIntArg("-h", 0);
      
      String connect=ap.getLastArg();
      
      ConnectionInfo ci=new ConnectionInfo(connect);
      ci.connect();
      
      DBMData dbmdata=new DBMData(ci);
      
      DBMLock lock=null;
      
      try {
         lock=dbmdata.acquireLock("DBM", wait);
         System.out.println ("Lock: "+lock);
         
         System.out.println("Sleeping "+hold);
         Thread.sleep(hold);
         
      } catch (Exception ex) {
         ex.printStackTrace();
      } finally {
         if (lock != null)
            dbmdata.releaseLock(lock);
      }
   }
}
