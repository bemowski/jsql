package net.jmatrix.db.common;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;



/**
 * 
 */
public final class DBUtils {
   static Logger log = ClassLogFactory.getLog();
   
   public static final void close(Connection con, Statement state, ResultSet rs) {
      close(rs);
      close(state);
      close(con);
   }

   /** */
   public static final void close(Statement state) {
      if (state != null) {
         try {
            state.close();
         } catch (Exception ex) {
            log.error("Error closing statement: ", ex);
         }
      }
   }

   /** */
   public static final void close(ResultSet rs) {
      if (rs != null) {
         try {
            rs.close();
         } catch (Exception ex) {
            log.error("Error closing ResultSet: ", ex);
         }
      }
   }

   /** */
   public static final void close(Connection con) {
      if (con != null) {
         try {
            if (!con.isClosed())
               con.close();
         } catch (Exception ex) {
            log.error("Error closing connection: ", ex);
         }
      }
   }

   /** */
   public static int intFunction(Statement state, String sql)
         throws SQLException {
      int result = 0;

      ResultSet rs = null;
      try {
         log.trace("intFunction(sql): " + sql);
         rs = state.executeQuery(sql);
         if (rs.next()) {
            result = rs.getInt(1);
         }
      } finally {
         DBUtils.close(rs);
      }
      return result;
   }
   
   /** */
   public static Date dateFunction(Statement state, String sql)
         throws SQLException {
      Date result=null;

      ResultSet rs = null;
      try {
         log.trace("dateFunction(sql): " + sql);
         rs = state.executeQuery(sql);
         if (rs.next()) {
            result = rs.getTimestamp(1);
         }
      } finally {
         DBUtils.close(rs);
      }
      return result;
   }

//   public static double doubleFunction(String sql)
//   throws SQLException {
//      Connection con=null;
//      Statement state=null;
//      ResultSet rs=null;
//      try {
//         con=Config.getInstance().getDataSource().getConnection();
//         state=con.createStatement();
//         log.trace("doubleFunction(sql): " + sql);
//         rs = state.executeQuery(sql);
//         if (rs.next()) {
//            return rs.getDouble(1);
//         }
//      } finally {
//         DBUtils.close(con, state, rs);
//      }
//      return -1d;
//   }
//   
//   public static List<String> stringListFunction(String sql)
//   throws SQLException {
//      Connection con=null;
//      Statement state=null;
//      try {
//         con=Config.getInstance().getDataSource().getConnection();
//         state=con.createStatement();
//         return stringListFunction(state, sql);
//      } finally {
//         DBUtils.close(con, state, null);
//      }
//   }
   
   /** */
   public static List<String> stringListFunction(Statement state, String sql)
         throws SQLException {
      List<String> list = new ArrayList<String>();

      ResultSet rs = null;
      try {
         log.trace("stringListFunction(sql): " + sql);
         rs = state.executeQuery(sql);
         while (rs.next()) {
            list.add(rs.getString(1));
         }
      } finally {
         DBUtils.close(rs);
      }
      return list;
   }

   public static final Connection getConnection(String driver, String url, String user,
         String pass) throws SQLException {
      try {

         log.debug("Connecting to " + url + " as " + user);

         Class.forName(driver);
         Connection con = DriverManager.getConnection(url, user, pass);

         return con;
      } catch (Exception ex) {
         throw new SQLException("Error getting connection...", ex);
      }
   }
   
   public static int executeUpdate(Connection con, String sql) throws SQLException {
      Statement state=null;
      try {
         state=con.createStatement();
         state.execute(sql);
         
         return state.getUpdateCount();
      } finally {
         close(state);
      }
   }
   
   public static final void log(String s) {
      System.out.println (s);
   }
   
//   static String cformat="user/pass@jdbc.url";
//   /*
//    * Connect string is: 
//    * 
//    * username/password@jdbcurl
//    * 
//    * Driver is inferred from connectionURL.
//    */
//   public static ConnectionInfo connect(String s) throws SQLException {
//      log("connecting...");
//      int at=s.indexOf("@");
//      if (at == -1) {
//         throw new SQLException("Invalid Connect format. missing @. use: "+cformat);
//      }
//      String upw=s.substring(0, at);
//      String url=s.substring(at+1);
//      
//      String driver=DriverMap.findDriver(url);
//      
//      String split[]=upw.split("\\/");
//      if (split.length != 2) {
//         throw new SQLException("Invalid Connect format.  username password has no '/'. use: "+cformat);
//      }
//      String user=split[0];
//      String pass=split[1];
//      
//      return connect(driver, url, user, pass);
//   }
//   
//   public static ConnectionInfo connect(String dclass, String url, String user, String pass) 
//         throws SQLException {
//      
//      //log("Connecting to "+user+" / "+url);
//      
//      ConnectionInfo conInfo=new ConnectionInfo(dclass, url, user, pass);
//      
//      try {
//         Class.forName(dclass);
//      } catch (Exception ex) {
//         throw new SQLException("Cannot load driver '"+dclass+"': "+ex);
//      }
//      Driver driver=null;
//      try {
//         driver=DriverManager.getDriver(url);
//         conInfo.setDriver(driver);
//      } catch (Exception ex) {
//         throw new SQLException("Error getting driver for url '"+url+"' "+ex);
//      }
//      
//      if (driver == null) {
//         throw new SQLException("No driver accepts url '"+url+"'");
//      } else {
//         log("  Driver Version: "+driver.getMajorVersion()+"."+driver.getMinorVersion());
//      }
//      
//      try {
//         Connection con=DriverManager.getConnection(url, user, pass);
//         
//         conInfo.setConnection(con);
//         //log("connected.");
//         return conInfo;
//      } catch (Exception ex) {
//         throw new SQLException("Cannot connect: "+ex);
//      }
//   }
}
