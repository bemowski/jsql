package net.jmatrix.db.schema.data.v2;

import java.io.IOException;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import net.jmatrix.db.common.ClassLogFactory;
import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.DBUtils;
import net.jmatrix.db.common.DebugUtils;
import net.jmatrix.db.common.SQLUtil;
import net.jmatrix.db.common.StreamUtil;
import net.jmatrix.db.common.Version;
import net.jmatrix.db.schema.DBMException;
import net.jmatrix.db.schema.DBVersion;
import net.jmatrix.db.schema.SQLStatement;

import org.slf4j.Logger;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;


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
   private static Logger log=ClassLogFactory.getLog();

   static String DBM_LOG="DBM_LOG";
   
   static String DBM_VERSIONS="DBM_VERSIONS";
   
   //static String DBM_LOCK="DBM_LOCK";
   
   enum DBMVersion {NONE,V1,V2, V2_1};
   
   Connection con=null;
   ConnectionInfo conInfo=null;
   
   public static int LOCK_CHECK_INTERVAL=2500;
   
   public DBMData(ConnectionInfo ci) throws SQLException {
      conInfo=ci;
      if (!ci.isConnected())
         ci.initDefaultConnection();
      
      con=ci.getDefaultConnection();
   }
   
   public void drop() throws SQLException {
//      console.warn("Dropping "+DBM_LOG);
//      try {
//         DBUtils.executeUpdate(con, DROP_LOG);
//      } catch (Exception ex) {}
//      
//      console.warn("Dropping "+DBM_VERSIONS);
//      try {
//         DBUtils.executeUpdate(con, DROP_VERSIONS);
//      } catch (Exception ex) {}
   }
   
   public void init() throws SQLException, IOException {
      if (!tableExists(DBM_VERSIONS)) {
         // no dbm.  init clean.
         createV2();
      } else {
         if (isV1Schema()) {
            log.info("DBM Schema is V1.  Migrating.");
            migrate();
         } else {
            
            
         }
      }
      
      if (!columnExists(DBM_VERSIONS, "ROLLBACK_SQL")) {
         log.info("DBMSchema updating to 2.1");
         migrate21();
      }
      log.info("DBM Schema is available.");
   }
   
   /** */
   void createV2()  throws IOException, SQLException {
      String v2clean=StreamUtil.readToString(this.getClass().getResourceAsStream("v2_clean.sql"));
      v2clean=SQLUtil.stripSQLComments(v2clean);
      List<String> sqls=SQLUtil.splitSQL(v2clean, ";");
      
      for (String sql:sqls) {
         log.info("Executing \n"+DebugUtils.indent(sql, 3));
         
         int rows=DBUtils.executeUpdate(con, sql);
         log.info(rows+" rows.");
         log.info("============================");
      }
      SimpleDateFormat df=new SimpleDateFormat("dd.MMM.yyyy HH:mm:ss");
      setVersion("0", "INITIAL", null, false, null, "Initialiation at "+df.format(new Date()), null);
   }
   
   void migrate() throws IOException, SQLException {
      // To migrate schmea: 
      //  1) backup old schema.
      //  2) install a clean v2 schema
      //  3) restore the data from v1
      
      String v1back=StreamUtil.readToString(this.getClass().getResourceAsStream("v1_back.sql"));
      v1back=SQLUtil.stripSQLComments(v1back);
      
      String v2clean=StreamUtil.readToString(this.getClass().getResourceAsStream("v2_clean.sql"));
      v2clean=SQLUtil.stripSQLComments(v2clean);

      
      List<String> sqls=SQLUtil.splitSQL(v1back, ";");
      sqls.addAll(SQLUtil.splitSQL(v2clean, ";"));
      
      for (String sql:sqls) {
         log.info("Executing \n"+DebugUtils.indent(sql, 3));
         
         int rows=DBUtils.executeUpdate(con, sql);
         log.info(rows+" rows.");
         log.info("============================");
      }
      
      restoreV1Data();
      
      String v1drop=StreamUtil.readToString(this.getClass().getResourceAsStream("v1_drop.sql"));
      v1drop=SQLUtil.stripSQLComments(v1drop);
      sqls=SQLUtil.splitSQL(v1drop, ";");
      for (String sql:sqls) {
         log.info("Executing \n"+DebugUtils.indent(sql, 3));
         
         int rows=DBUtils.executeUpdate(con, sql);
         log.info(rows+" rows.");
         log.info("============================");
      }
   }
   
   void migrate21() throws IOException, SQLException {
      String migrate=StreamUtil.readToString(this.getClass().getResourceAsStream("v2.1_alter.sql"));
      migrate=SQLUtil.stripSQLComments(migrate);
      List<String> sqls=SQLUtil.splitSQL(migrate, ";");
      
      for (String sql:sqls) {
         log.info("Executing \n"+DebugUtils.indent(sql, 3));
         
         int rows=DBUtils.executeUpdate(con, sql);
         log.info(rows+" rows.");
         log.info("============================");
      }
   }
   
   /** damn you oracle.  danm you to hell. 
    * @throws SQLException */
   /* 
insert into dbm_log (id, tstamp, filepath, success, num_rows, sql, error)
  select id, tstamp, filepath, status, num_rows, sql, error from dbm_log_v1;
  
insert into dbm_versions(id, tstamp, filepath, action, hostname, hostuser, version)
  select id, tstamp, filepath, action, hostname, username, version from dbm_versions_v1; 
  */
   private void restoreV1Data() throws SQLException {
      log.info("Restoring v1 data");
      PreparedStatement read=null;
      PreparedStatement write=null;
      ResultSet rs=null;
      try {
         read=con.prepareStatement("select id, tstamp, filepath, status, num_rows, sql, error from dbm_log_v1");
         write=con.prepareStatement("insert into dbm_log (id, tstamp, filepath, success, num_rows, sql, error)\n"+
         "values (?, ?, ?, ?, ?, ?, ?)");
         
         rs=read.executeQuery();
         int count=0;
         while (rs.next()) {
            count++;
            write.clearParameters();
            write.setString(1, rs.getString(1));
            Timestamp ts=rs.getTimestamp(2);
            write.setLong(2, ts == null?0:ts.getTime());
            write.setString(3, rs.getString(3));
            write.setString(4, rs.getString(4));
            write.setInt(5, rs.getInt(5));
            write.setString(6, rs.getString(6));
            write.setString(7, rs.getString(7));
            write.execute();
         }
         log.info("Restored "+count+" rows to dbm_log");
      } finally {
         DBUtils.close(rs);
         DBUtils.close(read);
         DBUtils.close(write);
      }
      
      try {
         read=con.prepareStatement("select id, tstamp, filepath, action, hostname, username, version from dbm_versions_v1");
         write=con.prepareStatement("insert into dbm_versions(id, tstamp, filepath, action, hostname, hostuser, version)\n"+
         "values (?, ?, ?, ?, ?, ?, ?)");
         
         rs=read.executeQuery();
         int count=0;
         while (rs.next()) {
            count++;
            write.clearParameters();
            write.setString(1, rs.getString(1));
            Timestamp ts=rs.getTimestamp(2);
            write.setLong(2, ts == null?0:ts.getTime());
            write.setString(3, rs.getString(3));
            write.setString(4, rs.getString(4));
            write.setString(5, rs.getString(5));
            write.setString(6, rs.getString(6));
            write.setString(7, rs.getString(7));
            write.execute();
         }
         log.info("Restored "+count+" rows to dbm_versions");

      } finally {
         DBUtils.close(rs);
         DBUtils.close(read);
         DBUtils.close(write);
      }
   }
   
   boolean isV1Schema() throws SQLException {
      Statement state=null;
      ResultSet rs=null;
      try {
         state=con.createStatement();
         rs=state.executeQuery("select * from "+DBM_VERSIONS);
         ResultSetMetaData rsmd=rs.getMetaData();
         int cols=rsmd.getColumnCount();
         return cols==8;
      } finally {
         DBUtils.close(null, state, rs);
      }
   }
   
   boolean isV2Schema() throws SQLException {
      Statement state=null;
      ResultSet rs=null;
      try {
         state=con.createStatement();
         try {
            rs=state.executeQuery("select * from "+DBM_VERSIONS);
            ResultSetMetaData rsmd=rs.getMetaData();
            int cols=rsmd.getColumnCount();
            return cols==12;
         } catch (SQLException ex) {
            log.warn("Got error checking schema version: "+ex);
            return false;
         }

      } finally {
         DBUtils.close(null, state, rs);
      }
   }
   
   boolean isV21Schema() throws SQLException {
      Statement state=null;
      ResultSet rs=null;
      try {
         state=con.createStatement();
         try {
            rs=state.executeQuery("select * from "+DBM_VERSIONS);
            ResultSetMetaData rsmd=rs.getMetaData();
            int cols=rsmd.getColumnCount();
            return cols==13;
         } catch (SQLException ex) {
            log.warn("Got error checking schema version: "+ex);
            return false;
         }
      } finally {
         DBUtils.close(null, state, rs);
      }
   }
   
   public boolean isDBMSchemaCurrent() throws SQLException {
      return isV21Schema();
   }
   
   boolean tableExists(String tablename) throws SQLException {
      DatabaseMetaData dbmd=null;
      ResultSet rs=null;
      log.debug("Looking for table "+tablename);
      try {
         dbmd=con.getMetaData();
         rs=dbmd.getTables(null, null, tablename, new String[] {"TABLE"});
         
         if (rs.next()) {
            String table=rs.getString("TABLE_NAME");
            log.debug("Found table: "+table);
            return true;
         }
      } finally {
         DBUtils.close(rs);
      }
      return false;
   }
   
   boolean columnExists(String tablename, String colname) throws SQLException {
      DatabaseMetaData dbmd=null;
      ResultSet rs=null;
      log.debug("Looking for column "+colname+" in table "+tablename);
      try {
         dbmd=con.getMetaData();
         rs=dbmd.getColumns(null, null, tablename, null);
         
         while (rs.next()) {
            String table=rs.getString("TABLE_NAME");
            String column=rs.getString("COLUMN_NAME");
            
            if (column.equalsIgnoreCase(colname)) {
               log.debug("Found column: "+table+"."+column);
               return true;
            }
         }
      } finally {
         DBUtils.close(rs);
      }
      return false;
   }
   
   public void logStatement(SQLStatement sqlState, 
         boolean success, int rows, String err) throws SQLException {
      String sql=
            "insert into "+DBM_LOG+" values(?, ?, ?, ?, ?, ?, ?, ?)";
      
      log.debug("Log sql: "+sql);
      
      PreparedStatement state=null;
      try {
         state=con.prepareStatement(sql);
         
         state.setString(1, UUID.randomUUID().toString());
         state.setString(2, sqlState.getVersion().toString());
         state.setLong(3, System.currentTimeMillis());
         state.setString(4, sqlState.getFile());
         state.setString(5, success?"T":"F");
         state.setInt(6, rows);
         state.setString(7, DebugUtils.truncate(sqlState.getSql(), 4000));
         state.setString(8, err);
         
         state.execute();
         
      } finally {
         DBUtils.close(state);
      }
   }
   
   public void setVersion(String version, String action, String path, boolean rollback,
         String checksum, String notes, String rollbackSql) throws SQLException {
      String sql=
            "insert into "+DBM_VERSIONS+" values(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
      PreparedStatement state=null;
      try {
         state=con.prepareStatement(sql);
         
         state.setString(1, UUID.randomUUID().toString());
         state.setLong(2, System.currentTimeMillis());
         state.setString(3, path);
         state.setString(4, action);
         state.setString(5, rollback?"T":"F"); // can rollback this version?
         state.setString(6, getHost());
         state.setString(7, getUser());
         state.setString(8, conInfo.getUsername());
         state.setString(9, version);
         state.setString(10, checksum);
         state.setString(11, schemaChecksum());
         state.setString(12, notes);
         state.setString(13, rollbackSql);
         
         state.execute();
      } finally {
         DBUtils.close(state);
      }
   }
   
   public DBVersion getCurrentVersion() throws SQLException {
      String sql="select * from "+DBM_VERSIONS+" where action <> 'LOCK' order by tstamp desc";
      log.debug("Getting current version with \n   "+sql);
      
      Statement state=null;
      ResultSet rs=null;
      try {
         state=con.createStatement();
         
         rs=state.executeQuery(sql);
         
         if (rs.next()) {
            return buildDBVersion(rs);
         } else {
            return null;
         }
      } finally {
         DBUtils.close(null, state, rs);
      }
   }
   
   public String getPreviousVersion() throws SQLException {
      String sql="select version from "+DBM_VERSIONS+" where action <> 'LOCK' order by tstamp desc";
      log.debug("Getting current version with \n   "+sql);
      
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
      String sql="select * from "+DBM_VERSIONS+" where action <> 'LOCK' order by tstamp";
      log.debug("Getting db versions with \n   "+sql);
      
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
   
   /**
    * @throws IOException 
    * @throws JsonMappingException 
    * @throws JsonParseException  */
   private static final DBVersion buildDBVersion(ResultSet rs) throws SQLException {
      DBVersion dbv=new DBVersion();
      dbv.setId(rs.getString("id"));
      
      dbv.setVersion(new Version(rs.getString("version")));
      dbv.setAction(rs.getString("action"));
      
      long l=rs.getLong("tstamp");
      
      dbv.setApplyDate(new Date(l));
      dbv.setHostname(rs.getString("hostname"));
      dbv.setHostuser(rs.getString("hostuser"));
      dbv.setDbuser(rs.getString("dbuser"));
      
      dbv.setDbChecksum(rs.getString("db_checksum"));
      dbv.setFileChecksum(rs.getString("file_checksum"));
      
      // pull out rollback sql
      String rbs=rs.getString("rollback_sql");
      if (rbs != null) {
         // parse as json list of strings.
         ObjectMapper om=new ObjectMapper();
         try {
            List<String> sql=om.readValue(rbs, new TypeReference<List<String>>(){});
            dbv.setRollbackSql(sql);
         } catch (IOException ex) {
            log.warn("Unable to parse sql from version '"+rbs+"'", ex);
         }
      }
      
      String rollback=rs.getString("rollback");
      if (rollback != null) {
         dbv.setRollback(rollback.equals("T"));
      }

      
      return dbv;
   }
   
   /** 
    * Algorithm TBD.  Basically shouild looks at all tables and columns 
    * data types, view specs, stored procs, etc, and generate an MD5
    * that represents the schema.
    */
   String schemaChecksum() {
      GenericSchemaChecksum hasher=new GenericSchemaChecksum(conInfo);
      try {
         long checksum=hasher.calculateSchemaChecksum();
         
         return ""+checksum;
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
   
   ///////////////////////////////////////////////////////////////////////////
   // Locking
   
   public DBMLock selectLock(String id) throws SQLException {
      Connection con=null;
      PreparedStatement state=null;
      ResultSet rs=null;
      
      String sql="select * from "+DBM_VERSIONS+" where action='LOCK' and id=?";
      log.debug("selectLock("+sql+", id="+id+")");
      try {
         con=conInfo.connect(); // new clean connection.
         con.setAutoCommit(true);
         state=con.prepareStatement(sql);
         state.setString(1, id);
         rs=state.executeQuery();
         
         if (rs.next()) {
            return buildLock(rs);
         } return null;
      } finally {
         DBUtils.close(con, state, rs);
      }
   }
   
   private DBMLock createLock(String id, String notes) throws SQLException {
      Connection con=null;
      PreparedStatement state=null;
      ResultSet rs=null;
      
      String sql="insert into "+DBM_VERSIONS+" (id, action, version, hostname, hostuser, tstamp, notes)\n"
            + "values (?, 'LOCK', 'LOCK', ?, ?, ?, ?)";
      
      log.debug("CreateLock("+sql+", id="+id+")");
      
      try {
         con=conInfo.connect(); // new clean connection.
         con.setAutoCommit(true);
         state=con.prepareStatement(sql);
         state.setString(1, id);
         state.setString(2, getHost());
         state.setString(3, System.getProperty("user.name"));
         state.setLong(4, System.currentTimeMillis());
         state.setString(5, notes);
         int rows=state.executeUpdate(); // may throw.
      } finally {
         DBUtils.close(con, state, rs);
      }
      return selectLock(id);
   }
   
   private void deleteLock(String id) throws SQLException {
      Connection con=null;
      PreparedStatement state=null;
      ResultSet rs=null;
      
      String sql="delete from "+DBM_VERSIONS+" where action='LOCK' and id=?";
      log.debug("deleteLock("+sql+", id="+id+")");
      try {
         con=conInfo.connect(); // new clean connection.
         con.setAutoCommit(true);
         state=con.prepareStatement(sql);
         state.setString(1, id);
         int rows=state.executeUpdate(); // may throw.
      } finally {
         DBUtils.close(null, state, rs);
      }
   }
   
   private static final DBMLock buildLock(ResultSet rs) throws SQLException {
      DBMLock lock=new DBMLock();
      lock.setId(rs.getString("id"));
      lock.setHost(rs.getString("hostname"));
      lock.setUser(rs.getString("hostuser"));
      lock.setTimestamp(rs.getLong("tstamp"));
      lock.setNotes(rs.getString("notes"));
      return lock;
   }
   
   public DBMLock acquireLock(String id, long millis) throws InterruptedException, SQLException {
      long start=System.currentTimeMillis();
      log.debug("Attempting to acquire lock '"+id+"'");
      
      DBMLock existing=selectLock(id);
      
      if (existing != null) {
         log.warn("Lock in use: "+existing+", waiting "+millis+" to acquire lock.");
         Thread.sleep(LOCK_CHECK_INTERVAL);
      } else {
         log.debug("No lock appears at present.");
      }
      int count=0;
      // poll every 5 s in this thread.
      long et=System.currentTimeMillis()-start;
      DBMLock lock=null;
      while (lock == null && et < millis) {
         try {
            count++;
            log.debug("Attempt "+count+" to obtain Lock '"+id+"', current wait="+et);
            lock=createLock(id, null);
         } catch (Exception ex) {
            lock=null;
            log.debug("Cannot acquire lock: "+ex);
         }
         
         if (lock != null)
            return lock;
         else {
            Thread.sleep(LOCK_CHECK_INTERVAL);
         }
         et=System.currentTimeMillis()-start;
      }
      et=System.currentTimeMillis()-start;
      DBMLock recent=selectLock(id);
      throw new DBMException("Unable to obtain lock id '"+id+"' in "+millis+
            "ms,  et="+et+". in use: "+
         (recent == null?(existing == null?"unknown":existing.toString()):recent.toString()));
   }
   
   
   public void releaseLock(DBMLock lock) throws SQLException {
      if (lock == null) {
         log.info("releaseLock called w/ null lock.");
         return;
      }
      releaseLock(lock.getId());
   }
   
   public void releaseLock(String ID) throws SQLException {
      log.debug("Releasing lock "+ID);
      deleteLock(ID);
   }
}
