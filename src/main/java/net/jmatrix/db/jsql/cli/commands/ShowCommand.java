package net.jmatrix.db.jsql.cli.commands;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.DBUtils;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.JSQL;
import net.jmatrix.db.jsql.formatters.PrettyFormatter;
import net.jmatrix.db.jsql.formatters.RSFormatter;

/**
 * show
 *    db
 *    tables
 *    views
 *    ...
 */
public class ShowCommand extends AbstractCommand {
   static final TextConsole console=SysConsole.getConsole();

   public ShowCommand(JSQL j) {
      super(j);
   }
   
   @Override
   public boolean accepts(String command) {
      return command != null && command.equals("show");
   }
   
   static final String usage="Usage: show {db|tables|views|procedures|connection|driver|catalogs} [namePattern]";

   @Override
   public void process(String line) throws Exception {
      String split[]=line.split(" ");
      
//      if (split.length < 2) {
//         System.out.println(usage);
//         return;
//      }
      
      if (!jsql.isConnected()) {
         System.out.println("Not connected.");
         return;
      }
      
      Connection con=jsql.getConnection();
      
      switch (split[1]) {
         case "db":
            showDB(con); break;
         case "tables":
            showTables(con, split, "TABLE");
            break;
         case "views":
            showTables(con, split, "VIEW");
            break;
         case "connection":
            showConnection(con);
            break;
         case "catalogs":
            showCatalogs(con);
            break;
         case "driver":
            showDriver();
            break;
         case "procedures":
            showProcedures(con, split);
            break;
         default:
            console.warn(usage);
      }
   }
   
   private void showCatalogs(Connection con) throws Exception {
      console.info("Getting catalogs...");
      try {
         DatabaseMetaData dbmd=con.getMetaData();
         
         ResultSet rs=dbmd.getCatalogs();
         
         RSFormatter formatter=jsql.getFormatter();
      formatter.format(rs);
      } catch (Throwable t) {
         console.error("Cannot get Catalogs", t);
      }
   }

   void showDB(Connection con) throws SQLException {
      // print some metadata
      DatabaseMetaData dbmd=con.getMetaData();
      ConnectionInfo ci=jsql.getConnectionInfo();
      
      StringBuilder info=new StringBuilder();
      info.append("Connected to "+dbmd.getDatabaseProductName()+"\n");
      info.append("  url: "+ci.getUrl()+"\n");
      info.append("   as: "+ci.getUsername()+"\n");

      info.append("  Version: "+dbmd.getDatabaseProductVersion()+"\n");
      
      info.append("    catalog term: "+dbmd.getCatalogTerm()+"\n");
      info.append("     schema term: "+dbmd.getSchemaTerm()+"\n");
      info.append("  procedure term: "+dbmd.getProcedureTerm()+"\n");

      
      console.println(info.toString());
   }
   
   void showTables(Connection con, String split[], String type) throws SQLException, IOException {
      DatabaseMetaData dbmd=con.getMetaData();
      
      ResultSet rs=null;
      
      List<String> tables=new ArrayList<String>();
      
      try {
         console.debug("Getting tables");
         
         // TABLE, VIEW, PROCEDURE, FUNCTION, TRIGGER
         
//         Each table description has the following columns:

//            TABLE_CAT String => table catalog (may be null)
//            TABLE_SCHEM String => table schema (may be null)
//            TABLE_NAME String => table name
//            TABLE_TYPE String => table type. Typical types are "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
//            REMARKS String => explanatory comment on the table
//            TYPE_CAT String => the types catalog (may be null)
//            TYPE_SCHEM String => the types schema (may be null)
//            TYPE_NAME String => type name (may be null)
//            SELF_REFERENCING_COL_NAME String => name of the designated "identifier" column of a typed table (may be null)
//            REF_GENERATION String => specifies how values in SELF_REFERENCING_COL_NAME are created. Values are "SYSTEM", "USER", "DERIVED". (may be null)
         
         String schema=null;
         
         try {
            schema=con.getSchema();
         } catch (Error er) {
            // drivers that don't support Java 7 API don't have this method.
         }
         
         String namePattern=null;
         if (split.length == 3) {
            // 3rd parameter is name pattern - example: 
            //  sp_%
            namePattern=split[2];
         }
         
         // catalog, schema, table name pattern, types
         rs=dbmd.getTables(null, schema, namePattern, new String[]{type});
         
         PrettyFormatter pf=new PrettyFormatter(jsql.getConnectionInfo(), jsql.getConsole());
         StringWriter sw=new StringWriter();
         int rows=pf.format(rs, sw, 500, null, null, 
               new String[] {"TABLE_NAME", "TABLE_TYPE", "TABLE_SCHEM", "TABLE_CAT"});
         
         if (rows >0 ) 
            console.println(sw.toString());
         else 
            console.print("No tables found.");
      } finally {
         DBUtils.close(rs);
      }
      Collections.sort(tables);
      
      StringBuilder sb=new StringBuilder();
      for (String t:tables) {
         sb.append("  "+t+"\n");
      }
      sb.append(tables.size()+" "+type+"s.");
      
      console.println(sb.toString());
   }
   
   void showProcedures(Connection con, String split[]) throws SQLException, IOException {
      DatabaseMetaData dbmd=con.getMetaData();
      
      ResultSet rs=null;
      
      try {
         console.debug("Getting tables");
         
         // TABLE, VIEW, PROCEDURE, FUNCTION, TRIGGER

         /* 
PROCEDURE_CAT String => procedure catalog (may be null)
PROCEDURE_SCHEM String => procedure schema (may be null)
PROCEDURE_NAME String => procedure name
reserved for future use
reserved for future use
reserved for future use
REMARKS String => explanatory comment on the procedure
PROCEDURE_TYPE short => kind of procedure:
  procedureResultUnknown - Cannot determine if a return value will be returned
  procedureNoResult - Does not return a return value
  procedureReturnsResult - Returns a return value
SPECIFIC_NAME String => The name which uniquely identifies this procedure within its schema.
          */
         String schema=null;
         
         try {
            schema=con.getSchema();
         } catch (Error er) {
            // drivers that don't support Java 7 API don't have this method.
         }
         
         String namePattern=null;
         if (split.length == 3) {
            // 3rd parameter is name pattern - example: 
            //  sp_%
            namePattern=split[2];
         }
         
         // catalog, schema, name pattern
         rs=dbmd.getProcedures(null, schema, namePattern);
         
         PrettyFormatter pf=new PrettyFormatter(jsql.getConnectionInfo(), jsql.getConsole());
         StringWriter sw=new StringWriter();
         pf.format(rs, sw, 500, null, null, 
               new String[] {"PROCEDURE_NAME", "PROCEDURE_TYPE", //"SPECIFIC_NAME", 
               "PROCEDURE_SCHEM", "PROCEDURE_CAT"});
         
         console.println(sw.toString());
      } finally {
         DBUtils.close(rs);
      }
   }
   
   void showConnection(Connection con) throws SQLException {
      
      String schema=null;
      try {
         schema=con.getSchema();
      } catch (Error ex) {
         schema="not supported.";
      }
      console.info("  con.getSchmea(): "+schema);
      console.info("  con.getCatalog(): "+con.getCatalog());
      console.info("  con.getAutocommt(): "+con.getAutoCommit());
      console.info("  con.isReadOnly(): "+con.isReadOnly());
   }
   
   void showDriver() throws SQLException {
      if (jsql.getConnectionInfo() == null) {
         console.info("Not connected.");
         return;
      }
      Driver driver=jsql.getConnectionInfo().getDriver();
      console.info("Driver:: "+driver.getClass().getName());
      console.info("  driver.getMajorVersion(): "+driver.getMajorVersion());
      console.info("  driver.getMinorVersion(): "+driver.getMinorVersion());
      
   }
}
