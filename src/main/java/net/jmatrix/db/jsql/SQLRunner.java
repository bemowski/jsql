package net.jmatrix.db.jsql;

import java.io.File;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;

import net.jmatrix.db.common.DBUtils;
import net.jmatrix.db.common.DebugUtils;
import net.jmatrix.db.common.SQLUtil;
import net.jmatrix.db.common.StreamUtil;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.formatters.RSFormatter;

public class SQLRunner implements Runnable {
   static TextConsole console=SysConsole.getConsole();
   
   JSQL jsql=null;
   File file=null;
   
   boolean failOnError=false;
   boolean printResults=true;
   
   public SQLRunner(JSQL js, File f) {
      file=f;
      jsql=js;
   }
   
   @Override
   public void run(){
      
      if (!jsql.isConnected()) {
         console.warn("Not connected.");
         return;
      }
      
      console.info("SQLRunner: "+file.getAbsolutePath());
      
      if (!file.exists()) {
         console.error("File '"+file.getAbsolutePath()+"' does not exist.");
         return;
      }
      if (!file.canRead()) {
         console.error("File '"+file.getAbsolutePath()+"' cannot be read.");
         return;
      }
      try {
         String sql=StreamUtil.readToString(file);
         
         sql=SQLUtil.stripSQLComments(sql);
         List<String> statements=SQLUtil.splitSQL(sql, ";");
         
         console.info("About to Execute "+statements.size()+" SQL statements.");
         
         int count=0;
         
         for (String statement:statements) {
            count++;
            try {
               execute(statement);
            } catch (Exception ex) {
               console.warn("Error executing statement "+count+": \n"+
                DebugUtils.indent(statement, 3), ex);
               if (failOnError)
                  break;
            }
         }
      } catch (Exception ex) {
         console.error("Error in SQLRunner", ex);
      }
   }
   
   private final void execute(String sql) throws Exception {
      console.info("Executing: ");
      console.info(DebugUtils.indent(sql, 3));
      
      Statement state=null;
      ResultSet rs=null;

      long start=System.currentTimeMillis();
      try {
         state=jsql.getConnection().createStatement();
         
         boolean results=state.execute(sql);
         
         if (results) {
            console.debug("execute returns "+results);
            
            //state.getMoreResults();
            
            rs=state.getResultSet();
            
            try {
               RSFormatter formatter=jsql.getFormatter();
               console.println(formatter.format(rs));
            } catch (Exception exx) {
               console.error("Error formatting results", exx);
            }
         } else {
            int rows=state.getUpdateCount();
            console.info("updated "+rows+" rows.");
         }
      } finally {
         DBUtils.close(null, state, rs);
         long et=System.currentTimeMillis()-start;
         console.info("sql took "+et+"ms");
      }
   }
}
