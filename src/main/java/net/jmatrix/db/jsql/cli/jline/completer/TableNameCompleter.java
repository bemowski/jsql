package net.jmatrix.db.jsql.cli.jline.completer;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import jline.internal.Log;
import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.DBUtils;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;

/** */
public class TableNameCompleter implements Completer {
   static final TextConsole console=SysConsole.getConsole();

   StringsCompleter sc=null;
   
   static List<String> tableCompletionTriggers=
         Arrays.asList(new String[]{"from", "describe"});
   
   /** 
    * Returns this index in the buffer where the completion applies.  If there
    * are no candidates from this completer - return -1.
    */
   @Override
   public int complete(String buffer, int pos, List<CharSequence> candidates) {
      if (sc == null) 
         return -1;
      
      if (buffer == null)
         return -1;
      
      String lc=buffer.toLowerCase();
      
      String split[]=lc.split(" ");
      
      // Case 1: the final word is either "from" or "describe" 
      if (tableCompletionTriggers.contains(split[split.length-1]) &&
          lc.endsWith(" ")) {
         
      }
      // Case 2: the final word is a partially completed table name.
      
      
      //int index=sc.complete(buffer.substring(tableIndex), pos, candidates);
      
      return 0;
   }
   
   public void connect(ConnectionInfo conInfo) {
      (new TableNameThread(conInfo)).start();
   }
   
   public void disconnect() {
      sc=null;
   }
   
   /** */
   private class TableNameThread extends Thread {
      ConnectionInfo conInfo=null;
      
      public TableNameThread(ConnectionInfo ci) {
         this.conInfo=ci;
      }
      
      @Override
      public void run() {
         console.debug("Getting table names for completion.");
         ResultSet rs=null;
         Connection con=null;
         try {
            con=conInfo.connect();
            console.debug("Got new connection");
            DatabaseMetaData dbmd=con.getMetaData();
            String schema=null;
            
            try {
               schema=conInfo.getSchema();
            } catch (Error er) {
               // drivers that don't support Java 7 API don't have this method.
            }
            
            // catalog, schema, table name pattern, types
            rs=dbmd.getTables(null, schema, null, null);
            
            List<String> tnames=new ArrayList<String>();
            while(rs.next()) 
               tnames.add(rs.getString("TABLE_NAME"));
            
            console.debug("Adding "+tnames.size()+" table names to completer.");
            
            sc=new StringsCompleter(tnames);
         } catch (Throwable ex) {
            Log.debug("", ex);
         } finally {
            DBUtils.close(con, null, rs);
         }
      }
   }
}
