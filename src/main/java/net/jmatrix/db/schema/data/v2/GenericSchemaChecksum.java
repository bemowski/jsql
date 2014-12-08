package net.jmatrix.db.schema.data.v2;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.CRC32;

import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.DBUtils;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;

/**
 * Generic hasher based on datbase metadata from jdbc.
 * 
 * 
 */
public class GenericSchemaChecksum implements SchemaChecksum {
   static TextConsole console=SysConsole.getConsole();

   Connection con=null;
   ConnectionInfo conInfo=null;
   
   List<String> configTables=null;
   
   List<String> tables=null;
   
   public GenericSchemaChecksum(ConnectionInfo ci) {
      conInfo=ci;
      con=ci.getDefaultConnection();
      reset();
   }
   
   @Override
   public long calculateSchemaChecksum() throws Exception {
      long checksum=-1;
      
      long start=System.currentTimeMillis();
      try {
         CRC32 crc=new CRC32();
         
         add(con, crc, new String[]{"TABLE", "VIEW"});
         
         if (configTables != null) {
            for (String tablename:configTables) {
               addRows(con, crc, tablename);
            }
         }
         
         checksum=crc.getValue();
      } finally {
         long et=System.currentTimeMillis()-start;
         console.info("Calculated schema checksum '"+checksum+"' in "+et+"ms");
      }
      
      return checksum;
   }
   
   void reset() {
      tables=new ArrayList<String>();
   }
   
   boolean ignoreTable(String tablename) {
      if (tablename == null)
         return true;
      if (tablename.toUpperCase().matches("^DBM.*"))
         return true;
      return false;
   }
   
   private void add(Connection con, CRC32 crc, String types[]) throws SQLException {
      DatabaseMetaData dbmd=con.getMetaData();
      
      ResultSet rs=null;
      
      try {
         String schema=conInfo.getSchema();
         rs=dbmd.getTables(null, schema, null, types);
         
         while (rs.next()) {
            String tableName=rs.getString("table_name");
            
            if (!ignoreTable(tableName)) {
               tables.add(tableName.toUpperCase());
               
               ResultSet rs2=null;
               console.info("Checksum adding table: "+tableName);
               try {
                  rs2=dbmd.getColumns(null, con.getSchema(), tableName, null);
                  // column name, type, size
                  while(rs2.next()) {
                     String cname=rs2.getString("column_name");
                     String tname=rs2.getString("type_name");
                     String size=rs2.getString("column_size");
                     crc.update(cname.getBytes());
                     crc.update(tname.getBytes());
                     crc.update(size.getBytes());
                  }
               } finally {
                  DBUtils.close(rs2);
               }
            } else {
               console.info("Ignoring "+tableName);
            }
         }
      } finally {
         DBUtils.close(rs);
      }
   }
   
   private void addRows(Connection con, CRC32 crc, String tablename) throws SQLException {
      if (!tables.contains(tablename.toUpperCase())) {
         console.warn("Cannot find table '"+tablename+"' in known schema tables: "+tables);
         
         crc.update("TABLE_DOES_NOT_EXIST".getBytes());
         return;
      }
      String sql="select * from "+tablename;
      
      console.info("hashing table "+tablename);
      
      Statement state=null;
      ResultSet rs=null;
      try {
         state=con.createStatement();
         rs=state.executeQuery(sql);
         
         ResultSetMetaData rsmd=rs.getMetaData();
         int cols=rsmd.getColumnCount();
         
         crc.update((""+cols).getBytes());
         
         int count=0;
         while (rs.next()) {
            count++;
            for (int i=1; i<=cols; i++) {
               String val=rs.getString(i);
               
               if (val == null)
                  crc.update("NULLVALUE".getBytes());
               else
                  crc.update(val.getBytes());
            }
         }
         console.info("Updated hash with "+count+" rows x "+cols+" cols");
      } finally {
         
      }
   }
   


   public List<String> getConfigTables() {
      return configTables;
   }

   public void setConfigTables(List<String> configTables) {
      this.configTables = configTables;
   }
}
