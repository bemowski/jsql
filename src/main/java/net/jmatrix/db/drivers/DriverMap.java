package net.jmatrix.db.drivers;

import java.util.HashMap;
import java.util.Map;

public class DriverMap {
   public static String ORACLE="oracle.jdbc.driver.OracleDriver";
   public static String MS_SQL_SERVER="com.microsoft.sqlserver.jdbc.SQLServerDriver";
   public static String MYSQL="com.mysql.jdbc.Driver";
   public static String JTDS="net.sourceforge.jtds.jdbc.Driver";
   public static String POSTGRES="org.postgresql.Driver";
   
   public static String[] drivers=
         new String[] {ORACLE, MS_SQL_SERVER, MYSQL, JTDS, POSTGRES};
   
   static Map<String, String> urlTemplate=new HashMap<>();
   
   static {
      urlTemplate.put(POSTGRES, "jdbc:postgresql://[<host|localhost>[:<port|5432>]]/<database>");
      urlTemplate.put(ORACLE, "jdbc:oracle:<drivertype>:[<user>/<password>@]<database - host:port:sid>");
   }
   
   public static String findDriver(String url) {
      if (url == null)
         return null;
      url=url.toLowerCase();
      if (url.contains("jdbc:oracle"))
         return ORACLE;
      if (url.contains("jdbc:sqlserver"))
         return MS_SQL_SERVER;
      if (url.contains("jdbc:postgresql"))
         return POSTGRES;
      
      return null;
   }
   
   public static String getUrlTemplate(String driver) {
      return urlTemplate.get(driver);
   }
}
