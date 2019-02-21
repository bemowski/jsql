package net.jmatrix.db.common;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import net.jmatrix.db.drivers.DriverMap;

import org.slf4j.Logger;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * JSON serializable data containser for JDBC connection details.
 * 
 * Password is obfuscated by base64 - but this is certainly not secure. 
 * 
 */
public class ConnectionInfo 
   implements Comparable<ConnectionInfo>, DataSource {
   private static Logger log=ClassLogFactory.getLog();

   String url;
   String driverClass;
   String username;
   String passwordB64;
   
   long lastConnectTime=-1;
   
   // some property names
   static final String SCHEMA="jsql.schema";
   static final String CATALOG="jsql.catalog";

   Map<String, String> properties;
   
   Connection connection=null;
   Driver driver=null;
   
   public enum Flavor {ORACLE, SQL_SERVER, MYSQL, HSQL, POSTGRES, GENERIC};
   
   Flavor flavor=Flavor.GENERIC;
   
   BASE64Encoder b64encoder=new BASE64Encoder();
   BASE64Decoder b64decoder=new BASE64Decoder();
   
   public ConnectionInfo() {}
   
   public ConnectionInfo(String d, String u, String un, String pw) {
      driverClass=d;
      url=u;
      username=un;
      setPassword(pw);
      flavor();
   }
   
   static String cformat="user/pass@jdbc.url";
   /*
    * Connect string is: 
    * 
    * username/password@jdbcurl
    * 
    * Driver is inferred from connectionURL.
    */
   public ConnectionInfo(String s) throws SQLException {
      int at=s.indexOf("@");
      if (at == -1) {
         throw new SQLException("Invalid Connect format. missing @. use: "+cformat);
      }
      String upw=s.substring(0, at);
      url=s.substring(at+1);
      
      driverClass=DriverMap.findDriver(url);
      if (driverClass == null) {
         throw new SQLException("Cannot find driver class for url '"+url+"'.  Connect with explicit driver.");
      }
      
      String split[]=upw.split("\\/");
      if (split.length != 2) {
         throw new SQLException("Invalid Connect format.  username password has no '/'. use: "+cformat);
      }
      username=split[0];
      String pass=split[1];
      setPassword(pass);
      
      flavor();
   }

   public void close() {
      if (connection != null) {
         DBUtils.close(connection);
      }
   }
   
   @JsonIgnore
   public void setSchema(String schema) {
      setProperty(SCHEMA, schema);
      boolean error=false;

      try {
         connection.setSchema(schema); // only supported from Java 1.7
      } catch (Error er) {
         error=true;
      } catch (SQLException ex) {
         ex.printStackTrace();
      }
   }
   
   @JsonIgnore
   public String getSchema() {
      String schema=null;
      boolean error=false;
      try {
         schema=connection.getSchema();
      } catch (Error er) {
         error=true;
      } catch (SQLException ex) {
         ex.printStackTrace();
      }
      
      if (! error)
         return schema;
      
      schema = getProperty(SCHEMA);
      if (schema != null)
         return schema;
      
      if (flavor == Flavor.ORACLE)
         return username;
      return null;
   }
   
   @JsonIgnore
   public void setCatalog(String catalog) {
      setProperty(CATALOG, catalog);
   }
   
   @JsonIgnore
   public String getCatalog() {
      return getProperty(CATALOG);
   }

   @Override
   public int compareTo(ConnectionInfo o) {

      ConnectionInfo ci=(ConnectionInfo)o;
         
      return Long.compare(ci.lastConnectTime, lastConnectTime);
   }
   
   @Override
   public int hashCode() {
      return (driver+url+username).hashCode();
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof ConnectionInfo) {
         ConnectionInfo ci=(ConnectionInfo)o;
         
         //System.out.println ("Equals: "+ci+"\n"+
         //                    "        "+this);
         
         return equals(ci.driverClass, driverClass) &&
                equals(ci.url, url) &&
                equals(ci.username, username);
      }
      return false;
   }
   
   @Override
   public String toString() {
      return username+" on "+url;
   }
   
   private static final boolean equals(String s1, String s2) {
      if (s1 == null  && s2 == null)
         return true;
      if (s1 != null && s2 != null)
         return s1.equals(s2);
      return false;
   }
   
   /** Guesses DB flavor based on connection URL heuristics. */
   private void flavor() {
      if (url != null) {
         String lurl=url.toLowerCase();
         if (lurl.contains("oracle"))
            flavor=Flavor.ORACLE;
         else if (lurl.contains("sqlserver"))
            flavor=Flavor.SQL_SERVER;
         else if (lurl.contains("mysql")) 
            flavor=Flavor.MYSQL;
         else if (lurl.contains("postgres")) 
            flavor=Flavor.POSTGRES;
      }
   }
   
   @JsonIgnore
   public boolean isConnected() {
      if (connection == null)
         return false;
      

      try {
         if (connection.isClosed()) {
            log.debug("Connection is closed.");
            connection=null;
            return false;
         }
         
         return connection.isValid(10000);
      } catch (Exception ex) {
         log.warn("Connection invalid: "+ex);
         connection=null;
         return false;
      }
   }
   
   public Connection initDefaultConnection() throws SQLException {
      if (connection == null) {
         Connection con=connect();
         setDefaultConnection(con);
      } else {
         boolean valid=false;
         try {
            valid=connection.isValid(10000);
         } catch (Exception ex) {
            log.warn("Connection "+this.toString()+
                  " is not valid. Attempting reconnect...");
            valid=false;
         }
         if (!valid)
            setDefaultConnection(connect());
      }
      return connection;
   }
   
   public Connection connect() throws SQLException {
      try {
         Class.forName(driverClass);
         log.debug("Driver class loaded.");
      } catch (Exception ex) {
         throw new SQLException("Cannot load driver '"+driverClass+"': "+ex);
      }
      
      try {
         driver=DriverManager.getDriver(url);
         log.debug("Driver for URL is "+driver);
      } catch (Exception ex) {
         throw new SQLException("Error getting driver for url '"+url+"' "+ex);
      }
      
      if (driver == null) {
         throw new SQLException("No driver accepts url '"+url+"'");
      } else {
         //log("  Driver Version: "+driver.getMajorVersion()+"."+driver.getMinorVersion());
      }
      
      Connection con=null;
      try {
         if (properties == null) 
            con=DriverManager.getConnection(url, username, getPassword());
         else {
            con=DriverManager.getConnection(url, props());
         }
         log.debug("Connection: "+con);
      } catch (Exception ex) {
         throw new SQLException("Cannot connect: "+ex, ex);
      }
      return con;
   }
   
   private Properties props() {
      Properties p= new Properties();
      
      p.put("user", getUsername());
      p.put("password", getPassword());
      
      if (properties != null) {
         // don't log this here, doesn't turn out good.
         //log.info("Adding properties: "+properties);
         for (String key:properties.keySet()) {
            p.put(key, properties.get(key));
         }
      }
      
      return p;
   }
   
   public void setProperty(String key, String value) {
      if (properties == null)
         properties=new HashMap<String,String>();
      properties.put(key, value);
   }
   
   public String getProperty(String key) {
      if (properties == null)
         return null;
      return properties.get(key);
   }
   ///////////////////////////////////////////////////////////////////////////
   public String getUrl() {
      return url;
   }

   public void setUrl(String url) {
      this.url = url;
      flavor();
   }


   public String getUsername() {
      return username;
   }

   public void setUsername(String username) {
      this.username = username;
   }
   
   @JsonIgnore
   public String getPassword() {
      if (passwordB64 == null)
         return null;
      try {
         return new String(b64decoder.decodeBuffer(passwordB64), "UTF-8");
      } catch (Exception ex) {
         throw new RuntimeException("Unable to decode password.");
      }
   }
   
   @JsonIgnore
   public void setPassword(String password) {
      if (password == null)
         passwordB64=null;
      this.passwordB64=b64encoder.encode(password.getBytes());
   }
   
   public String getDriverClass() {
      return driverClass;
   }
   
   public void setDriverClass(String driverClass) {
      this.driverClass = driverClass;
   }
   
   @JsonIgnore
   public Connection getDefaultConnection() {
      return connection;
   }
   
   @JsonIgnore
   public void setDefaultConnection(Connection connection) {
      lastConnectTime=System.currentTimeMillis();
      this.connection = connection;
   }
   
   @JsonIgnore
   public Driver getDriver() {
      return driver;
   }
   
   @JsonIgnore
   public void setDriver(Driver driver) {
      this.driver = driver;
   }
   
   @JsonIgnore
   public Flavor getFlavor() {
      return flavor;
   }
   
   @JsonIgnore
   public void setFlavor(Flavor flavor) {
      this.flavor = flavor;
   }

   public String getPasswordB64() {
      return passwordB64;
   }

   public void setPasswordB64(String passwordB64) {
      this.passwordB64 = passwordB64;
   }

   public long getLastConnectTime() {
      return lastConnectTime;
   }

   public void setLastConnectTime(long lastConnectTime) {
      this.lastConnectTime = lastConnectTime;
   }

   public Map<String, String> getProperties() {
      return properties;
   }

   public void setProperties(Map<String, String> properties) {
      this.properties = properties;
   }

   
   ///////////////////////////////////////////////////////////////////////
   // Implementing DataSource
   @JsonIgnore
   @Override
   public PrintWriter getLogWriter() throws SQLException {
      return null;
   }

   @JsonIgnore
   @Override
   public void setLogWriter(PrintWriter out) throws SQLException {  }

   @JsonIgnore
   @Override
   public void setLoginTimeout(int seconds) throws SQLException {  }

   @JsonIgnore
   @Override
   public int getLoginTimeout() throws SQLException {
      return 0;
   }

   @JsonIgnore
   @Override
   public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
      throw new SQLFeatureNotSupportedException();
   }

   @JsonIgnore
   @Override
   public <T> T unwrap(Class<T> iface) throws SQLException {
      return null;
   }

   @JsonIgnore
   @Override
   public boolean isWrapperFor(Class<?> iface) throws SQLException {
      return false;
   }

   @JsonIgnore
   @Override
   public Connection getConnection() throws SQLException {
      return connect();
   }

   @JsonIgnore
   @Override
   public Connection getConnection(String username, String password)
         throws SQLException {
      return connect();
   }
}
