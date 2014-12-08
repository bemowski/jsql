package net.jmatrix.db.schema.data.v1;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import net.jmatrix.db.common.DBUtils;
import net.jmatrix.db.common.Version;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.schema.DBM;
import net.jmatrix.db.schema.DBVersion;


/** 
 * Manages all classes in the DBM Schema itself.  Tables include:
 * 
 * DBM_LOG
 *   id
 *   
 * 
 * DBM_VERSIONS
 *   id
 *   date
 *   version
 *   hash
 *   
 * 
 */
public class DBMData {
   static TextConsole console=SysConsole.getConsole();

   static String DBM_LOG="DBM_LOG";
   static String DBM_LOG_DDL=
         "CREATE TABLE DBM_LOG( \n"
         + "  ID VARCHAR(50) NOT NULL,\n"
         + "  TSTAMP TIMESTAMP NOT NULL,\n"
         + "  FILEPATH VARCHAR(500),\n"
         + "  HOSTNAME VARCHAR(500),\n"
         + "  USERNAME VARCHAR(500),\n"
         + "  STATUS VARCHAR(10),\n" // success/fail
         + "  NUM_ROWS INTEGER,\n"  // number of rows affected.
         + "  SQL VARCHAR(4000),\n"
         + "  ERROR VARCHAR(4000),\n"
         + "PRIMARY KEY (ID))";
   
   static String DROP_LOG="DROP TABLE DBM_LOG";
   
   static String DBM_VERSIONS="DBM_VERSIONS";
   static String DBM_VERSIONS_DDL=
         "CREATE TABLE DBM_VERSIONS(\n"
         + "  ID VARCHAR(50) NOT NULL,\n"
         + "  TSTAMP TIMESTAMP NOT NULL,\n"
         + "  FILEPATH VARCHAR(500),\n"
         +"   ACTION VARCHAR(20), \n" // apply/rollback
         + "  HOSTNAME VARCHAR(500),\n"
         +"   USERNAME VARCHAR(500),\n"
         + "  VERSION VARCHAR(100) NOT NULL,\n"
         + "  HASH VARCHAR(200),\n"
         + "PRIMARY KEY(ID))";
   
   static String DROP_VERSIONS="DROP TABLE DBM_VERSIONS";
   
   Connection con=null;
   
   public DBMData(Connection c) throws SQLException {
      con=c;
   }
   
   public void drop() throws SQLException {
      console.warn("Dropping "+DBM_LOG);
      try {
         DBUtils.executeUpdate(con, DROP_LOG);
      } catch (Exception ex) {}
      
      console.warn("Dropping "+DBM_VERSIONS);
      try {
         DBUtils.executeUpdate(con, DROP_VERSIONS);
      } catch (Exception ex) {}
   }
   
   public void init() throws SQLException {
      if (!tableExists(DBM_LOG)) {
         console.warn("Creating table "+DBM_LOG+"\n"+DBM_LOG_DDL);
         DBUtils.executeUpdate(con, DBM_LOG_DDL);
      }
      
      if (!tableExists(DBM_VERSIONS)) {
         console.warn("Creating table "+DBM_VERSIONS+"\n"+DBM_VERSIONS_DDL);
         DBUtils.executeUpdate(con, DBM_VERSIONS_DDL);
         setVersion("0", "INITIAL", null);
      }
   }
   
   boolean tableExists(String tablename) throws SQLException {
      DatabaseMetaData dbmd=null;
      ResultSet rs=null;
      console.debug("Looking for table "+tablename);
      try {
         dbmd=con.getMetaData();
         rs=dbmd.getTables(null, null, tablename, new String[] {"TABLE"});
         
         if (rs.next()) {
            String table=rs.getString("TABLE_NAME");
            console.debug("Found table: "+table);
            return true;
         }
      } finally {
         DBUtils.close(rs);
      }
      return false;
   }
   
   public void logStatement(String path, String statement, 
         boolean success, int rows, String err) throws SQLException {
      String sql=
            "insert into "+DBM_LOG+" values(?, ?, ?, ?, ?, ?, ?, ?, ?)";
      
      console.info("Log sql: "+sql);
      
      PreparedStatement state=null;
      try {
         state=con.prepareStatement(sql);
         
         state.setString(1, UUID.randomUUID().toString());
         state.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
         state.setString(3, path);
         state.setString(4, getHost());
         state.setString(5, getUser());
         state.setString(6, success?"T":"F");
         state.setInt(7, rows);
         state.setString(8, statement);
         state.setString(9, err);
         state.execute();
         
      } finally {
         DBUtils.close(state);
      }
   }
   
   public void setVersion(String version, String action, String path) throws SQLException {
      String sql=
            "insert into "+DBM_VERSIONS+" values(?, ?, ?, ?, ?, ?, ?, ?)";
      PreparedStatement state=null;
      try {
         state=con.prepareStatement(sql);
         
         state.setString(1, UUID.randomUUID().toString());
         state.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
         state.setString(3, path);
         state.setString(4, action);
         state.setString(5, getHost());
         state.setString(6, getUser());
         state.setString(7, version);
         state.setString(8, schemaHash());
         
         state.execute();
      } finally {
         DBUtils.close(state);
      }
   }
   
   public String getCurrentVersion() throws SQLException {
      String sql="select version from "+DBM_VERSIONS+" order by tstamp desc";
      console.debug("Getting current version with \n   "+sql);
      
      Statement state=null;
      ResultSet rs=null;
      try {
         state=con.createStatement();
         
         rs=state.executeQuery(sql);
         
         if (rs.next()) {
            return rs.getString(1);
         } else {
            return null;
         }
      } finally {
         DBUtils.close(null, state, rs);
      }
   }
   
   public String getPreviousVersion() throws SQLException {
      String sql="select version from "+DBM_VERSIONS+" order by tstamp desc";
      console.debug("Getting current version with \n   "+sql);
      
      Statement state=null;
      ResultSet rs=null;
      try {
         state=con.createStatement();
         
         rs=state.executeQuery(sql);
         
         rs.next(); // this is the current version
         if (rs.next()) {
            return rs.getString(1);
         } else {
            return null;
         }
      } finally {
         DBUtils.close(null, state, rs);
      }
   }
   
   public List<DBVersion> getDBVersions() throws SQLException {
      String sql="select * from "+DBM_VERSIONS+" order by tstamp desc";
      console.debug("Getting db versions with \n   "+sql);
      
      Statement state=null;
      ResultSet rs=null;
      try {
         state=con.createStatement();
         
         rs=state.executeQuery(sql);
         
         List<DBVersion> versions=new ArrayList<DBVersion>();
         while (rs.next()) {
            versions.add(buildDBVersion(rs));
         }
         return versions;
      } finally {
         DBUtils.close(null, state, rs);
      }
   }
   
   /** */
   private static final DBVersion buildDBVersion(ResultSet rs) throws SQLException {
      DBVersion dbv=new DBVersion();
      
      dbv.setVersion(new Version(rs.getString("version")));
      dbv.setAction(rs.getString("action"));
      dbv.setApplyDate(rs.getTimestamp("tstamp"));
      dbv.setHostname(rs.getString("hostname"));
//      dbv.setUsername(rs.getString("username"));
//      dbv.setHash(rs.getString("hash"));
      
      return dbv;
   }
   
   /** 
    * Algorithm TBD.  Basically shouild looks at all tables and columns 
    * data types, view specs, stored procs, etc, and generate an MD5
    * that represents the schema.
    */
   String schemaHash() {
      GenericSchemaHasher hasher=new GenericSchemaHasher(con);
      try {
         String hash=hasher.calculateSchemaHash();
         
         return hash;
      } catch (Exception ex) {
         ex.printStackTrace();
         return "error";
      } finally {
         
      }
   }
   
   static String getHost() {
      try {
         return InetAddress.getLocalHost().getCanonicalHostName();
      } catch (Exception ex) {
         return "unknown";
      }
   }
   
   static String getUser() {
      return System.getProperty("user.name");
   }
}
