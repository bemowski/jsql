package net.jmatrix.db.jsql;

import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import net.jmatrix.db.common.ArgParser;
import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.DebugUtils;
import net.jmatrix.db.common.StringUtils;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.common.console.TextConsole.Level;
import net.jmatrix.db.jsql.cli.CommandProcessor;
import net.jmatrix.db.jsql.cli.LineModeProcessor;
import net.jmatrix.db.jsql.formatters.PrettyFormatter;
import net.jmatrix.db.jsql.formatters.RSFormatter;
import net.jmatrix.db.jsql.model.RecentConnections;

/**
 * 
 */
public class JSQL {
   static final TextConsole console=SysConsole.getConsole();
   
   public static String JSQL="jsql";
   
   ConnectionInfo conInfo=null;
   
   RSFormatter formatter=null;
   
   //TextConsole console=null;
   
   RecentConnections recentConnections=null;
   
   // syntax similar to sqlplus
   // jsql user/pass@<jdbcurl>
   //
   
   JSQL(TextConsole c) {
      //formatter=new PlainFormatter(null);
      formatter=new PrettyFormatter(console);
      
      if (c != null) {
         formatter.set(RSFormatter.CONSOLE_WIDTH, c.getColumns());
         formatter.set(RSFormatter.CONSOLE_LINES, c.getRows());
      }
      
      try {
         recentConnections=RecentConnections.load(JSQL);
      } catch (Exception ex) {
         console.error("Error loading recent connections", ex);
      }
   }
   
   public static void main(String args[]) throws Exception {
      
      ArgParser ap=new ArgParser(args);
      
      TextConsole console=SysConsole.getConsole();
      
      if (ap.getBooleanArg("-v")) {
         console.setLevel(Level.DEBUG);
      } else {
         console.setLevel(Level.LOG);
      }
      
      System.out.println(console);

      JSQL jsql=new JSQL(console);
      
      
      if (ap.getBooleanArg("-r")) {
         // reconnect to the most recently connected database.
         
         ConnectionInfo ci=jsql.getRecentConnections().getMostRecent();
         
         if (ci != null) {
            System.out.println("Reconnecting to "+ci.toString());
            DebugUtils.sleep(10);
            try {
               jsql.connect(ci);
            } catch (SQLException ex) {
               console.warn("Cannot connect: "+ci, ex);
            }
         } else {
            System.out.println("-r ignored.  No recent connections.");
         }
      } else if (ap.getBooleanArg("-c")) {
         String connect=ap.getStringArg("-c");
         jsql.connect(connect);
      }
      
      console.println(splash());
      
      // this is interactive.
      jsql.start();
      
//      Thread.sleep (10);
//      Logger l=LoggerFactory.getLogger("");
//      
//      l.debug("debug");
//      l.info("log");
//      l.warn("warn");
//      l.error("error");
   }
   
   private static String splash() {
      StringBuilder sb=new StringBuilder();
      
      String jsqlart=
            "   .-. .--.  .--. .-.   \n" + 
            "   : :: .--': ,. :: :   \n" + 
            " _ : :`. `. : :: :: :   \n" + 
            ": :; : _`, :: :;_:: :__ \n" + 
            "`.__.'`.__.'`._:_;:___.'";
      
      sb.append("\n"+jsqlart+"\n\n");
      
      Properties p=new Properties();
      
      try {
         URL url=ClassLoader.getSystemResource("build.properties");
         console.debug("Build Properties: "+url);
         p.load(url.openStream());
      } catch (Exception ex) {
         console.debug("Error loading build properties.", ex);
      }
      
      // Tightly coupled to build.xml
      sb.append("  Version: "+p.getProperty("version")+"\n");
      sb.append("    Build: "+p.getProperty("build.time")+"\n");
      
      return sb.toString();
   }
   
   public void start() {
      Thread t=new Thread(new SystemInProcessor());
      t.setName("sys.in.proc");
      t.start();
   }
   
   /**
    * This is the run loop for the LineModeProcessors.  
    */
   class SystemInProcessor implements Runnable {
      CommandProcessor commandProcessor=null;
      LineModeProcessor currentProcessor=null;
      
      public SystemInProcessor() {
         commandProcessor=new CommandProcessor(JSQL.this);
         currentProcessor=commandProcessor;
      }
      
      @Override
      public void run() {
         
         try {
            console.setCompleters(currentProcessor.getCompleters());
            
            String line=console.readLine(currentProcessor.prompt());
            while (line != null) {
               
               currentProcessor=currentProcessor.processLine(line);
               
               if (currentProcessor == null)
                  currentProcessor=commandProcessor;
               
               
               console.setCompleters(currentProcessor.getCompleters());
               line=console.readLine(currentProcessor.prompt());
            }
         } catch (Exception ex) {
            console.error("Error in processor loop", ex);
         }
      }
   }
   
   public boolean isConnected() {
      return conInfo != null && conInfo.isConnected();
   }
   
   void connect(String s) throws SQLException {
      console.info("Connecting '"+s+"'");
      ConnectionInfo ci=new ConnectionInfo(s);
      connect(ci);
   }
   
   public void connect(String driver, String url, String user, String pass) throws SQLException {
      //conInfo=DBUtils.connect(driver, url, user, pass);
      ConnectionInfo ci=new ConnectionInfo(driver, url, user, pass);
      connect(ci);
   }
   
   public void connect(ConnectionInfo ci) throws SQLException {
      ci.initDefaultConnection();
      
      conInfo=ci;
      formatter.setConnectionInfo(conInfo);
      
      console.println("connected.");

      if (recentConnections != null) {
         recentConnections.update(conInfo);
         try {
            recentConnections.save(JSQL);
         } catch (Exception ex) {
            console.warn("Could not save recent Connections: "+ex.toString());
         }
      } else {
         console.println("RecentConnections not available.");
      }
      
      connectBanner();
   }
   
   public void disconnect() {
      try {
         conInfo.close();
      } catch (Exception ex) {
         console.error("Error disconnecting.", ex);
      }
      conInfo=null;
      console.println("disconnected.");
   }
   
   void connectBanner() {
      StringBuilder banner=new StringBuilder();
      Connection con=conInfo.getDefaultConnection();
      Driver driver=conInfo.getDriver();
      
      banner.append(StringUtils.pad(console.getColumns(), '-')+"\n");

      banner.append("    Driver  class: "+conInfo.getDriverClass()+"\n");
      banner.append("          version: "+driver.getMajorVersion()+"."+driver.getMinorVersion()+"\n");
      banner.append("\n");
      banner.append("    Connected as "+conInfo.getUsername()+" to "+conInfo.getUrl()+"\n");
      
      banner.append("    Flavor: "+conInfo.getFlavor()+"\n");
      
      banner.append("\n");
      
      try {
         DatabaseMetaData dbmd=con.getMetaData();
         banner.append("    Database Product: "+dbmd.getDatabaseProductName()+"\n");
         banner.append("             Version: "+dbmd.getDatabaseProductVersion()+"\n");

      } catch (SQLException ex) {
         banner.append("Error getting DB MetaData: "+ex);
      }
      
      banner.append(StringUtils.pad(console.getColumns(), '-')+"\n");
      
      console.println(banner.toString());
   }
   
   public TextConsole getConsole() {
      return console;
   }
   
   public Connection getConnection() {
      return conInfo.getDefaultConnection();
   }
   
   public ConnectionInfo getConnectionInfo() {
      return conInfo;
   }
   
   public RSFormatter getFormatter() {
      return formatter;
   }
   
   public void setFormatter(RSFormatter f) {
      formatter=f;
   }
   
   public RecentConnections getRecentConnections() {
      return recentConnections;
   }
}
