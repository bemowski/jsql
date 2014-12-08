package net.jmatrix.db.jsql.cli;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jline.console.completer.Completer;
import net.jmatrix.db.common.DBUtils;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.JSQL;
import net.jmatrix.db.jsql.formatters.RSFormatter;

public class PreparedStatementProcessor implements LineModeProcessor {
   static final TextConsole console=SysConsole.getConsole();

   JSQL jsql=null;

   String sp=null;
   
   List<String> arguments=new ArrayList<String>();
   
   int argumentCount=0;
   
   int pointer=0;
   
   public PreparedStatementProcessor(JSQL j) {
      jsql=j;
      
      console.info("Syntax: ");
      console.info("   {call proc_name(?, ?)}");
   }
   
   @Override
   public String prompt() {
      if (pointer == 0) {
         return "JSQL.PS>";
      }
      else {
         return "JSQL.PS{"+pointer+"}>";
      }
   }
   
   private static int count(String s, char c) {
      int count=0;
      
      char ch[]=s.toCharArray();
      for (char x:ch) {
         if (x == c)
            count++;
      }
      
      return count;
   }

   @Override
   public LineModeProcessor processLine(String line) {
      if (pointer == 0) {
         
         sp=line;
         
         argumentCount=count(sp, '?');
      } else {
         arguments.add(line);
      }
      
      pointer++;
      
      if (pointer > argumentCount) {
         // execute and return.
         execute();
         return null;
      } else {
         return this;
      }
   }

   void execute() {
      if (!jsql.isConnected()) {
         console.warn("Not Connected.");
         return;
      }
      
      console.debug("SQL \n"+sp);
      
      if (sp.trim().length() == 0) {
         console.info("No Procecure Specified.");
         return;
      }
      
      PreparedStatement state=null;
      ResultSet rs=null;

      long start=System.currentTimeMillis();
      try {
         state=jsql.getConnection().prepareStatement(sp);
         
         for (int i=0; i<arguments.size(); i++) {
            state.setObject(i+1, arguments.get(i));
         }
         
         boolean results=state.execute();
         
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
