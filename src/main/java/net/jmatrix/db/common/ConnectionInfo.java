package net.jmatrix.db.common;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import net.jmatrix.db.drivers.DriverMap;
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
   implements Comparable<ConnectionInfo>{
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
   
   public enum Flavor {ORACLE, SQL_SERVER, MYSQL, HSQL, GENERIC};
   
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
      }
   }
   
   @JsonIgnore
   public boolean isConnected() {
      return connection != null;
   }
   
   public Connection initDefaultConnection() throws SQLException {
      Connection con=connect();
      setDefaultConnection(con);
      return con;
   }
   
   public Connection connect() throws SQLException {
      try {
         Class.forName(driverClass);
      } catch (Exception ex) {
         throw new SQLException("Cannot load driver '"+driverClass+"': "+ex);
      }
      
      try {
         driver=DriverManager.getDriver(url);
      } catch (Exception ex) {
         throw new SQLException("Error getting driver for url '"+url+"' "+ex);
      }
      
      if (driver == null) {
         throw new SQLException("No driver accepts url '"+url+"'");
      } else {
         //log("  Driver Version: "+driver.getMajorVersion()+"."+driver.getMinorVersion());
      }
      
      try {
         Connection con=DriverManager.getConnection(url, username, getPassword());
         
         //setConnection(con);
         
         return con;
      } catch (Exception ex) {
         throw new SQLException("Cannot connect: "+ex);
      }
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
}
