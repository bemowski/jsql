package net.jmatrix.db.jsql;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.List;
import java.util.Properties;

import net.jmatrix.db.common.ArgParser;
import net.jmatrix.db.common.ClassLogFactory;
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
import net.jmatrix.db.schema.DBM;
import net.jmatrix.db.schema.action.Action;

import org.slf4j.Logger;

/**
 * 
 */
public class JSQL {
   static final TextConsole console=SysConsole.getConsole();
   private static Logger log=ClassLogFactory.getLog();

   
   public static String JSQL="jsql";
   
   ConnectionInfo conInfo=null;
   
   RSFormatter formatter=null;
   
   //TextConsole console=null;
   
   RecentConnections recentConnections=null;
   
   SystemInProcessor sysInProc=null;
   CommandProcessor commandProcessor=null;
   
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
   
   static final String usage=
         "\njsql [-h] [-r | -c <url>] [-dbm.path <path> [-dbm.action <action>]]\n"+
         "where\n"+
         "   -h: displays this help message\n\n"+
         "   -v: verbose output mode.\n"+
         "   -vv: really verbose output. \n\n"+
         "   -r: reconnect to the most recently connected database.  \n"+
         "       useful in interactive mode.\n\n"+
         "   -c <url> connects to the specified database\n"+
         "      url: user/password@jdbc-connection-url\n"+
         "      driver is determined from the connection url \n"+
         "\n"+
         "   DBM Mode: \n"+
         "   -dbm.path <path>: triggers dbm schema managemet mode.\n"+
         "      path: filesystem path which contains database versions in \n"+
         "            dbm format.  dbm format specified in documentation.\n\n"+
         "   -dbm.action <action>: specifies dbm action.  If not specified, update is exeduted\n"+
         "      action: one of: \n"+
         "              update: runs all recommended updates\n"+
         "              recommend: outputs recommendations, without executing any action.\n"
         
         ;
   
   
   /** */
   public static void main(String args[]) throws Exception {
      
      ArgParser ap=new ArgParser(args);
      
      if (ap.getBooleanArg("-h")) {
         System.out.println(usage); 
         System.exit(0);
      }
      
      TextConsole console=SysConsole.getConsole();
      
      
      if (ap.getBooleanArg("-vv")) 
         console.setLevel(Level.ALL);
      else if (ap.getBooleanArg("-v"))
         console.setLevel(Level.DEBUG);
      else 
         console.setLevel(Level.LOG);
      
      System.out.println(console);
      
      console.println(splash());
      
      // DBM mode?
      if (ap.getBooleanArg("-dbm.path")) {
         //console.setLevel(Level.DEBUG);

         // We NEED path and connection info.  Validate both.
         String url=ap.getStringArg("-c");
         String path=ap.getStringArg("-dbm.path");
         
         boolean error=false;
         
         if (url == null) {
            log.warn("Required connection info not specified with -c command line argument.");
            error=true;
         }
         if (path == null) {
            log.warn("Required dbm path not specified with -dbm.path command line argument.");
            error=true;
         }
         
         File fpath=new File(path);
         if (!fpath.exists() || !fpath.isDirectory() || !fpath.canRead()) {
            log.warn("DBM path '"+fpath+"' does not exist, or is not readable, or is not a dir.");
         }
         
         if (error) {
            log.error("DBM Command has errors, see above.  Exit 1.");
            System.exit(1);
            return;
         }
         
         
         log.info("DBM Path: "+path);
         ConnectionInfo ci=null;
         try {
            ci=new ConnectionInfo(url);
            
            // we do this just so it gets stored in recent connections for 
            // the user in question.
            try {
               RecentConnections rc=RecentConnections.load(JSQL);
               if (rc != null)
                  rc.update(ci);
               rc.save(JSQL);
            } catch (Exception ex) {
               log.warn("Cannot update recent connections.");
            }
            
            log.info("DBM Connection: "+ci);
            
            DBM dbm=new DBM(ci, fpath);
            
            log.info("  DBM - Disk Version: "+dbm.getMaxDiskVersion());
            log.info("  DBM - DB Version:   "+dbm.getCurrentDBVersion());
            
            // get action
            String dbmAction="update";
            
            dbmAction=ap.getStringArg("-dbm.action", dbmAction);
            
            log.info("DBM Action: "+dbmAction);
            
            switch (dbmAction) {
               case "recommend":
                  List <Action> recommend=dbm.recommendUpdateActions();
                  log.info("Found "+recommend.size()+" recommendations.");
                  if (recommend.size() > 0) {
                     for (Action a:recommend) {
                        log.info("   "+a);
                     }
                  }
               break;
               case "update":
                  dbm.updateAll();
               break;
               default: 
                  log.warn("Unknown DBM Action '"+dbmAction+"'.  Exit 3.");
                  System.exit(3);
            }
            
            log.info(dbmAction+" success.  Exit 0.");
            System.exit(0);
         } catch (Exception ex) {
            log.error("Error in DBM", ex);
            System.exit(2);
            return;
         } finally {
            if (ci != null)
               ci.close();
         }
      } else {
         // JSQL Interactive mode
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
               console.warn("-r ignored.  No recent connections.");
            }
         } else if (ap.getBooleanArg("-c")) {
            String connect=ap.getStringArg("-c");
            jsql.connect(connect);
         }
         
         // this is interactive.
         jsql.start();
      }
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
      sysInProc=new SystemInProcessor();
      Thread t=new Thread(sysInProc);
      t.setName("sys.in.proc");
      t.start();
   }
   
   /**
    * This is the run loop for the LineModeProcessors.  
    */
   class SystemInProcessor implements Runnable {
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
      
      if (commandProcessor != null)
         commandProcessor.connect(conInfo);
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
      
      if (conInfo != null)
         formatter.setConnectionInfo(conInfo);
      
      if (console != null) {
         formatter.set(RSFormatter.CONSOLE_WIDTH, console.getColumns());
         formatter.set(RSFormatter.CONSOLE_LINES, console.getRows());
      }
   }
   
   public RecentConnections getRecentConnections() {
      return recentConnections;
   }
}
