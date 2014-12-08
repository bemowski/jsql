package net.jmatrix.db.jsql.cli;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collection;

import jline.console.completer.Completer;
import net.jmatrix.db.common.DBUtils;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.JSQL;
import net.jmatrix.db.jsql.formatters.RSFormatter;

public class SQLProcessor implements LineModeProcessor {
   static final TextConsole console=SysConsole.getConsole();
   
   JSQL jsql=null;
   StringBuilder sqlbuffer=new StringBuilder();

   public SQLProcessor(JSQL j) {
      jsql=j;
   }

   @Override
   public String prompt() {
      return "JSQL.$>";
   }

   @Override
   public LineModeProcessor processLine(String line) {
      // trim only trailing whitespae: 
      line = line.replaceAll("\\s+$", "");
      

      if (line.endsWith(";")) {
         sqlbuffer.append(line);
         execute(sqlbuffer.toString());
         sqlbuffer.delete(0, sqlbuffer.length());
         return null;
      } else {
         sqlbuffer.append(line+"\n");
      }
      return this;
   }
   
   void execute(String sql) {
      if (!jsql.isConnected()) {
         console.warn("Not Connected.");
         return;
      }
      
      if (sql.endsWith(";"))
         sql=sql.substring(0, sql.length()-1);
      
     console.debug("SQL \n"+sql);
      
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
      } catch (SQLException ex) {
         console.warn("Error executing sql",ex);
      } catch (Exception ex) {
         console.error("Error exeuging sql", ex);
      } finally {
         DBUtils.close(null, state, rs);
         long et=System.currentTimeMillis()-start;
         console.info("sql took "+et+"ms");
      }
   }

   @Override
   public Collection<Completer> getCompleters() {
      // TODO Auto-generated method stub
      return null;
   }
}
