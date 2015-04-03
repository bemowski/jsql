package net.jmatrix.db.jsql.cli.commands;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.common.console.TextConsole.Level;
import net.jmatrix.db.jsql.JSQL;
import net.jmatrix.db.jsql.formatters.CSVFormatter;
import net.jmatrix.db.jsql.formatters.PlainFormatter;
import net.jmatrix.db.jsql.formatters.PrettyFormatter;
import net.jmatrix.db.jsql.formatters.RSFormatter;
import net.jmatrix.db.jsql.formatters.SQLFormatter;

public class SetCommand extends AbstractCommand {
   static final TextConsole console=SysConsole.getConsole();
   
   static List<String> formatters=Arrays.asList(new String[]{"plain", "sql", "pretty", "csv"});
   
   public SetCommand(JSQL j) {
      super(j);
   }
   
   @Override
   public boolean accepts(String command) {
      if (command.equalsIgnoreCase("set"))
         return true;
      
      return false;
   }
   
   
   /**
    * set 
    *    connection
    *       schema {schema} : set the connetion schema - if not avilable, set on connection info
    *       catalog {catalog} : set the connection catalog
    *       readonly {T/F}: 
    *    jsql
    *       formatter [pretty|plain|sql]
    *        
    *          
    */
   @Override
   public void process(String line) throws Exception {
      
      // set connection catalog foo
      
      String split[]=line.split("\\ ");
      if (split.length == 1) {
         display();
      } else if (split.length == 3) {
         set(split[1], split[2], null);
      } else if (split.length == 4) {
         set(split[1], split[2], split[3]);
      } else {
         console.warn("Malformed set command.");
         return;
      }

   }
   
   void display() throws SQLException {
      // show all known settable variables
      
      StringBuilder sb=new StringBuilder();
      sb.append("\n");
      sb.append("connection\n");
      if (jsql.isConnected()) {
         ConnectionInfo ci=jsql.getConnectionInfo();
         sb.append("  readonly "+ci.getDefaultConnection().isReadOnly()+"\n");
         sb.append("  autocommit "+ci.getDefaultConnection().getAutoCommit()+"\n");
         sb.append("  schema "+ci.getSchema()+"\n");
         sb.append("  catalog "+ci.getCatalog()+"\n");
      } else {
         sb.append("   not connected\n");
      }
      sb.append("\n");
      
      
      // log.
      sb.append("log\n");
      sb.append("  level "+console.getLevel().toString()+"\n");
      sb.append("\n");
      
      
      // formatter
      sb.append("formatter  (Available: "+formatters+")\n");
      sb.append("  current: "+jsql.getFormatter()+"\n");
      
      console.info(sb.toString());
   }
   
   void set(String subsystem, String key, String value) throws Exception {
      
      
      switch (subsystem) {
         case "connection":
         case "con":{
            if (!jsql.isConnected()) {
               console.warn("not connected.");
               return;
            }
            
            switch (key) {
               case "catalog":
                  jsql.getConnectionInfo().setCatalog(value);
                  break;
               case "schema":
                  jsql.getConnectionInfo().setSchema(value);
                  break;
               default:
                  console.warn("Don't know how to set '"+key+"' on connection.");
            }
         } break;
            
         case "jsql": {
            switch ("formatter") {
               case "pretty":
                  RSFormatter f=new PrettyFormatter(jsql.getConnectionInfo(), jsql.getConsole());
                  jsql.setFormatter(f);
                  break;
               default:
                  console.warn("Don't understand formatter value "+value);
                  break;
            }  break;
         } 
         
         case "log": {
            if (key.equals("level") || value != null) {
               console.setLevel(Level.valueOf(value.toUpperCase()));
            } else {
               console.warn("Malformed set command.");
            }
         }break;
         
         case "formatter": {
            if (key.equals("plain")) {
               jsql.setFormatter(new PlainFormatter());
            } else if (key.equals("pretty")) {
               jsql.setFormatter(new PrettyFormatter(jsql.getConsole()));
            } else if (key.equals("sql")) {
               jsql.setFormatter(new SQLFormatter());
            } else if (key.equals("csv")) {
               jsql.setFormatter(new CSVFormatter());
            } else {
               console.warn("Don't know formatter '"+key+"' - known values: "+formatters);
            }
            console.info("Current Formatter: "+jsql.getFormatter());
         }break;
         
         default: 
            console.warn("Don't understand set subsystem '"+subsystem+"'");
      }
   }
}
