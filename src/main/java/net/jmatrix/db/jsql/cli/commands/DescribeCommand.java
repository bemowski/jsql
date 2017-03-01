package net.jmatrix.db.jsql.cli.commands;

import java.io.IOException;
import java.io.StringWriter;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.DBUtils;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.JSQL;
import net.jmatrix.db.jsql.formatters.PrettyFormatter;

/**
 * describe
 *    table
 */
public class DescribeCommand extends AbstractCommand {
   static final TextConsole console=SysConsole.getConsole();
   
   static final String TABLE="table";
   static final String PROCEDURE="procedure";
   static final String VIEW="view";

   public DescribeCommand(JSQL j) {
      super(j);
   }
   
   @Override
   public boolean accepts(String command) {
      return command != null && 
            (command.equals("desc") || command.equals("describe"));
   }
   
   
   /** 
    * Format of this command: 
    * 
    *   describe {tablename} - shorthand
    *   describe procedure {procedurename}
    *   describe table {tablename}
    *   
    *   
    */
   @Override
   public void process(String line) throws Exception {
      String split[]=line.split(" ");
      
      
      if (!jsql.isConnected()) {
         console.warn("Not connected.");
         return;
      }
      
      String type=null;
      String object=null;
      
      if (split.length < 2) {
         console.warn("Malformed 'describe' command.  Describe what?");
         return;
      } else if (split.length == 2) {
         // shorthand for table
         type=TABLE;
         object=split[1];
      } else if (split.length == 3) {
         // 2 argument is type
         String stype=split[1].toLowerCase();
         object=split[2];
         switch (stype) {
            case "proc":
            case "procedure": 
               type=PROCEDURE;
               break;
            case "table":
               type=TABLE;
               break;
            case "view":
               type=VIEW;
               break;
            default:
               console.warn("Can't understand type '"+stype+"'");
               return;
         }
      } else {
         console.warn("Malformed 'describe' command.  Describe what?");
         // fixme - help
         return;
      }

      
      Connection con=jsql.getConnection();
      switch (type) {
         case VIEW:
         case TABLE:
            describeTable(con, object);
            if (type.equals(TABLE))
               describeTableIndexes(con, object);
            break;
         case PROCEDURE:
            describeProc(con, object);
            break;
      }
   }
   
   /* 
    *  RS Columns.
    *   
TABLE_CAT String => table catalog (may be null)
TABLE_SCHEM String => table schema (may be null)
TABLE_NAME String => table name
COLUMN_NAME String => column name
DATA_TYPE int => SQL type from java.sql.Types
TYPE_NAME String => Data source dependent type name, for a UDT the type name is fully qualified
COLUMN_SIZE int => column size.
BUFFER_LENGTH is not used.
DECIMAL_DIGITS int => the number of fractional digits. Null is returned for data types where DECIMAL_DIGITS is not applicable.
NUM_PREC_RADIX int => Radix (typically either 10 or 2)
NULLABLE int => is NULL allowed.
   columnNoNulls - might not allow NULL values
   columnNullable - definitely allows NULL values
   columnNullableUnknown - nullability unknown
REMARKS String => comment describing column (may be null)
COLUMN_DEF String => default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be null)
SQL_DATA_TYPE int => unused
SQL_DATETIME_SUB int => unused
CHAR_OCTET_LENGTH int => for char types the maximum number of bytes in the column
ORDINAL_POSITION int => index of column in table (starting at 1)
IS_NULLABLE String => ISO rules are used to determine the nullability for a column.
   YES --- if the column can include NULLs
   NO --- if the column cannot include NULLs
   empty string --- if the nullability for the column is unknown
SCOPE_CATALOG String => catalog of table that is the scope of a reference attribute (null if DATA_TYPE isn't REF)
SCOPE_SCHEMA String => schema of table that is the scope of a reference attribute (null if the DATA_TYPE isn't REF)
SCOPE_TABLE String => table name that this the scope of a reference attribute (null if the DATA_TYPE isn't REF)
SOURCE_DATA_TYPE short => source type of a distinct type or user-generated Ref type, SQL type from java.sql.Types (null if DATA_TYPE isn't DISTINCT or user-generated REF)
IS_AUTOINCREMENT String => Indicates whether this column is auto incremented
   YES --- if the column is auto incremented
   NO --- if the column is not auto incremented
   empty string --- if it cannot be determined whether the column is auto incremented
IS_GENERATEDCOLUMN String => Indicates whether this is a generated column
   YES --- if this a generated column
   NO --- if this not a generated column
   empty string --- if it cannot be determined whether this is a generated column
    * 
    */
   
   
   void describeTable(Connection con, String table) throws SQLException, IOException  {
      DatabaseMetaData dbmd=con.getMetaData();
      
      ResultSet rs=null;
      
      try {
         console.debug("Getting columns for "+table);
         
         // catalog, schema, table name pattern, column name pattern
         
         ConnectionInfo ci=jsql.getConnectionInfo();
         
         console.debug("catalog="+ci.getCatalog()+", schema="+ci.getSchema()+", table="+table.toUpperCase());
         
         rs=dbmd.getColumns(ci.getCatalog(), ci.getSchema(), table, null);

         PrettyFormatter pf=new PrettyFormatter(jsql.getConnectionInfo(), jsql.getConsole());
         StringWriter sw=new StringWriter();
         pf.format(rs, sw, 500, null, null, 
               new String[] {"COLUMN_NAME", "TYPE_NAME", "COLUMN_SIZE", "TABLE_SCHEM", "TABLE_CAT", 
               "IS_NULLABLE"});
         
         console.println(sw.toString());
      } finally {
         DBUtils.close(rs);
      }
   }
   
   
/*
TABLE_CAT String => table catalog (may be null)
TABLE_SCHEM String => table schema (may be null)
TABLE_NAME String => table name
NON_UNIQUE boolean => Can index values be non-unique. false when TYPE is tableIndexStatistic
INDEX_QUALIFIER String => index catalog (may be null); null when TYPE is tableIndexStatistic
INDEX_NAME String => index name; null when TYPE is tableIndexStatistic
TYPE short => index type:
tableIndexStatistic - this identifies table statistics that are returned in conjuction with a table's index descriptions
tableIndexClustered - this is a clustered index
tableIndexHashed - this is a hashed index
tableIndexOther - this is some other style of index
ORDINAL_POSITION short => column sequence number within index; zero when TYPE is tableIndexStatistic
COLUMN_NAME String => column name; null when TYPE is tableIndexStatistic
ASC_OR_DESC String => column sort sequence, "A" => ascending, "D" => descending, may be null if sort sequence is not supported; null when TYPE is tableIndexStatistic
CARDINALITY int => When TYPE is tableIndexStatistic, then this is the number of rows in the table; otherwise, it is the number of unique values in the index.
PAGES int => When TYPE is tableIndexStatisic then this is the number of pages used for the table, otherwise it is the number of pages used for the current index.
 */
   void describeTableIndexes(Connection con, String tablename) throws SQLException, IOException {
      DatabaseMetaData dbmd=con.getMetaData();
      
      ResultSet rs=null;
      
      
      
      try {
         console.debug("Getting indexes for "+tablename);
         
         // catalog, schema, table name pattern, column name pattern
         
         ConnectionInfo ci=jsql.getConnectionInfo();
         
         rs=dbmd.getIndexInfo(null, null, tablename, false, false);
         
         PrettyFormatter pf=new PrettyFormatter(jsql.getConnectionInfo(), jsql.getConsole());
         StringWriter sw=new StringWriter();
         int rows=pf.format(rs, sw, 500, null, null, 
               new String[] {"COLUMN_NAME", "INDEX_NAME", "CARDINALITY", "NON_UNIQUE"});
         
         if (rows > 0) {
            console.println("indexes:");
            console.println(sw.toString());
         } else {
            console.println("No indexes found on "+tablename);
         }
      } finally {
         DBUtils.close(rs);
      }
   }
   
   /*
PROCEDURE_CAT String => procedure catalog (may be null)
PROCEDURE_SCHEM String => procedure schema (may be null)
PROCEDURE_NAME String => procedure name
COLUMN_NAME String => column/parameter name
COLUMN_TYPE Short => kind of column/parameter:
  procedureColumnUnknown - nobody knows
  procedureColumnIn - IN parameter
  procedureColumnInOut - INOUT parameter
  procedureColumnOut - OUT parameter
  procedureColumnReturn - procedure return value
  procedureColumnResult - result column in ResultSet
DATA_TYPE int => SQL type from java.sql.Types
TYPE_NAME String => SQL type name, for a UDT type the type name is fully qualified
PRECISION int => precision
LENGTH int => length in bytes of data
SCALE short => scale - null is returned for data types where SCALE is not applicable.
RADIX short => radix
NULLABLE short => can it contain NULL.
  procedureNoNulls - does not allow NULL values
  procedureNullable - allows NULL values
  procedureNullableUnknown - nullability unknown
REMARKS String => comment describing parameter/column
COLUMN_DEF String => default value for the column, which should be interpreted as a string when the value is enclosed in single quotes (may be null)
   The string NULL (not enclosed in quotes) - if NULL was specified as the default value
   TRUNCATE (not enclosed in quotes) - if the specified default value cannot be represented without truncation
   NULL - if a default value was not specified
SQL_DATA_TYPE int => reserved for future use
SQL_DATETIME_SUB int => reserved for future use
CHAR_OCTET_LENGTH int => the maximum length of binary and character based columns. For any other datatype the returned value is a NULL
ORDINAL_POSITION int => the ordinal position, starting from 1, for the input and output parameters for a procedure. A value of 0 is returned if this row describes the procedure's return value. For result set columns, it is the ordinal position of the column in the result set starting from 1. If there are multiple result sets, the column ordinal positions are implementation defined.
IS_NULLABLE String => ISO rules are used to determine the nullability for a column.
  YES --- if the column can include NULLs
  NO --- if the column cannot include NULLs
  empty string --- if the nullability for the column is unknown
SPECIFIC_NAME String => the name which uniquely identifies this procedure within its schema.
    */
   void describeProc(Connection con, String proc) throws SQLException, IOException  {
      DatabaseMetaData dbmd=con.getMetaData();
      
      ResultSet rs=null;
      
      try {
         console.debug("Getting columns for "+proc);
         
         // catalog, schema, table name pattern, column name pattern
         ConnectionInfo ci=jsql.getConnectionInfo();
         rs=dbmd.getProcedureColumns(ci.getCatalog(), ci.getSchema(), proc.toUpperCase(), null);

         PrettyFormatter pf=new PrettyFormatter(jsql.getConnectionInfo(), jsql.getConsole());
         StringWriter sw=new StringWriter();
         pf.format(rs, sw, 500, null, null, 
               new String[] {"COLUMN_NAME", "COLUMN_TYPE", "TYPE_NAME",
               "PROCEDURE_SCHEM", "PROCEDURE_CAT", "IS_NULLABLE"});
         
         console.println(sw.toString());
      } finally {
         DBUtils.close(rs);
      }
   }
}
