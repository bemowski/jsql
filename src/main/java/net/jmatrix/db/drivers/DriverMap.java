package net.jmatrix.db.drivers;

public class DriverMap {
   public static String ORACLE="oracle.jdbc.driver.OracleDriver";
   public static String MS_SQL_SERVER="com.microsoft.sqlserver.jdbc.SQLServerDriver";
   public static String MYSQL="com.mysql.jdbc.Driver";
   public static String JTDS="net.sourceforge.jtds.jdbc.Driver";
   
   public static String[] drivers=
         new String[] {ORACLE, MS_SQL_SERVER, MYSQL, JTDS};
   
   public static String findDriver(String url) {
      if (url == null)
         return null;
      url=url.toLowerCase();
      if (url.contains("jdbc:oracle"))
         return ORACLE;
      if (url.contains("jdbc:sqlserver"))
         return MS_SQL_SERVER;
      
      return null;
   }
}
