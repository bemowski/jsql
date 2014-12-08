package net.jmatrix.db.schema.data.v1;

import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import net.jmatrix.db.common.DBUtils;
import net.jmatrix.db.common.Hex;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;

/**
 * Generic hasher based on datbase metadata from jdbc.
 * 
 * 
 */
public class GenericSchemaHasher implements SchemaHasher {
   static TextConsole console=SysConsole.getConsole();

   Connection con=null;
   
   List<String> configTables=null;
   
   List<String> tables=null;
   
   public GenericSchemaHasher(Connection c) {
      con=c;
      reset();
   }
   
   @Override
   public String calculateSchemaHash() throws Exception {
      String hash=null;
      
      long start=System.currentTimeMillis();
      try {
         MessageDigest digest=MessageDigest.getInstance("MD5");
         digest.reset();
         
         add(con, digest, new String[]{"TABLE", "VIEW"});
         
         if (configTables != null) {
            for (String tablename:configTables) {
               addRows(con, digest, tablename);
            }
         }
         
         hash=Hex.asHex(digest.digest());
      } finally {
         long et=System.currentTimeMillis()-start;
         console.info("Calculated schema hash '"+hash+"' in "+et+"ms");
      }
      
      return hash;
   }
   
   void reset() {
      tables=new ArrayList<String>();
   }
   
   boolean ignoreTable(String tablename) {
      if (tablename == null)
         return true;
      if (tablename.toUpperCase().matches("^DBM"))
         return true;
      return false;
   }
   
   private void add(Connection con, MessageDigest digest, String types[]) throws SQLException {
      DatabaseMetaData dbmd=con.getMetaData();
      
      ResultSet rs=null;
      
      try {
         con.getSchema();
         
         rs=dbmd.getTables(null, con.getSchema(), null, types);
         
         while (rs.next()) {
            String tableName=rs.getString("table_name");
            
            if (!ignoreTable(tableName)) {
               tables.add(tableName.toUpperCase());
               
               ResultSet rs2=null;
               console.info("Hashing Table Schema: "+tableName);
               try {
                  rs2=dbmd.getColumns(null, con.getSchema(), tableName, null);
                  // column name, type, size
                  while(rs2.next()) {
                     String cname=rs2.getString("column_name");
                     String tname=rs2.getString("type_name");
                     String size=rs2.getString("column_size");
                     digest.update(cname.getBytes());
                     digest.update(tname.getBytes());
                     digest.update(size.getBytes());
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
   
   private void addRows(Connection con, MessageDigest digest, String tablename) throws SQLException {
      if (!tables.contains(tablename.toUpperCase())) {
         console.warn("Cannot find table '"+tablename+"' in known schema tables: "+tables);
         
         digest.update("TABLE_DOES_NOT_EXIST".getBytes());
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
         
         digest.update((""+cols).getBytes());
         
         int count=0;
         while (rs.next()) {
            count++;
            for (int i=1; i<=cols; i++) {
               String val=rs.getString(i);
               
               if (val == null)
                  digest.update("NULLVALUE".getBytes());
               else
                  digest.update(val.getBytes());
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
