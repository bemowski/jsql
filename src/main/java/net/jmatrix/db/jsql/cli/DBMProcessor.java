package net.jmatrix.db.jsql.cli;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import jline.internal.Log;
import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.JSQL;
import net.jmatrix.db.schema.DBM;
import net.jmatrix.db.schema.action.Action;
import net.jmatrix.db.schema.data.v2.DBMLock;

public class DBMProcessor implements LineModeProcessor {
   static TextConsole console=SysConsole.getConsole();
   
   DBM dbm=null;
   JSQL jsql=null;
   ConnectionInfo conInfo=null;
   
   String def=null;
   
   static final String DBM_PATH="dbm.path";
   
   List<Completer> completers=new ArrayList<Completer>();
   
   DBMLock lock=null;
   
   static final String usage=
         "show \n"+
         "   status: shows an overall status comparison of disk and db versions.\n"+
         "   db.versions: shows the DBM history\n"+
         "   disk.versions: shows versions from Disk\n"+
         "   recommendations: shows schema update recommendations\n\n"+
         
         "init:      Initialize the DBM meta-data tables in the database.\n"+
         "update:    Update DB to match disk version, applying any versions < current DB version.\n"+
         "apply    <version>: apply only verison requested\n"+
         "rollback <version>: rollback only version requested\n"+
         "reapply  <version>: rollback, then apply version requessted\n"+
         "\n"+
         "set\n"+
         "   version <version>: sets DB version to value manually.\n"+
         "\n"+
         "exit: exit DBM mode\n"+
         "quit: quit the application\n";
   
   public DBMProcessor(JSQL j) {
      jsql=j;
      conInfo=jsql.getConnectionInfo();
      
      if (conInfo.getProperties() != null) {
         def=conInfo.getProperties().get(DBM_PATH);
      }
      
      List<String> commands=Arrays.asList(
            new String[] {"update", "apply", "rollback", "reapply", 
                  "exit", "quit", "clear", "init",
                  "lock","unlock","force-unlock",
                  "show db.versions", "show disk.versions", "show status",
                  "show recommendations", "show lock"});
      Completer cc=new StringsCompleter(commands);
      completers.add(cc);
   }

   @Override
   public Collection<Completer> getCompleters() {
      return completers;
   }
   
   @Override
   public String prompt() {
      
      if (dbm == null) {
         return "JSQL.DBM - schema ["+def+"]>";
      }
      
      return "JSQL.DBM>";
   }

   @Override
   public LineModeProcessor processLine(String line) {
      
      String split[]=line.split(" ");
      
      String command=split[0];
      
      try {
         
         // This code is executed upon the initial entry into 
         // DBM Processor mode.  It initializes the DBM.
         if (dbm == null) {
            // line should be path to schema.
            String pathstring=null;

            if (line.length() == 0) {
               pathstring=def;
            } else {
               pathstring=line;
            }
            
            //String pathstring=conInfo.getProperties().get("dbm.path");
            
            if (pathstring != null) {
               console.info("DBM Disk Path: "+pathstring);
               
               File path=new File(pathstring);
               if (!path.exists() || !path.isDirectory() || !path.canRead()) {
                  console.warn("Cannot find/read DBM Disk path at "+path.getAbsolutePath());
               } else {
                  conInfo.setProperty(DBM_PATH, pathstring);
                  
                  try {
                     jsql.getRecentConnections().save(JSQL.JSQL);
                  } catch (Exception ex) {
                     console.warn("Error saving recent connections: "+ex);
                  }
                  
                  dbm=new DBM(conInfo, path);
                  dbm.reloadDiskVersions();
               }
            }
            if (dbm == null) {
               dbm=new DBM(conInfo, null);
               console.info("Creating DB only SchemaManager");
            }
            
            
            if (dbm.reloadDBVersions()) {
               dbm.showVersionStatus();
               
               showRecommendationsShort();

               // we only want to look for a lock if the table exists, 
               // otherwise it throws up a nasty exception.
               DBMLock lock=dbm.getExistingLock();
               if (lock != null) {
                  console.warn("DBM System has existing lock.");
                  console.warn("   "+lock);
               } else {
                  console.info("DBM not currently locked.");
               }
            } else {
               console.warn("Recommend: init dbm schema with 'init' command.");
            }
             
            return this;
         }
         
         switch (command) {
            case "exit":
            return null;
            
//            case "history":
//               dbm.showHistory();
//            break;
            
            case "set":
               switch (split[1]) {
                  case "version":
                     dbm.setVersion(split[2]);
                  break;
                  default:
                     console.warn("Don't know how to set '"+split[1]+"'");
               }
               break;
               
            case "update":{
               List<Action> actions=dbm.recommendUpdateActions();
               
               if (actions == null || actions.size() == 0) {
                  console.info("No update actions.");
               } else {
                  
                  console.info("About to execute action(s):\n");
                  for (Action action:actions) {
                     console.info(action.summary());
                  }
                  console.info("");
                  
                  if (confirm("Do you want to proceeed with above actions:",
                        new String[] {"yes", "no"}, "yes")) {
                     dbm.executeActionsWithLock(actions);
                  } else {
                     console.info("Update cancelled.");
                  }
               }
               break;
            }
            case "apply":
               dbm.apply(split[1]);
               break;
               
            case "reapply":
               dbm.reapply(split[1]);
               break;
               
            case "rollback":
               dbm.rollback(split[1]);
               break;
               
            case "clear":
               console.clear();
               break;
               
            case "init":
               
               // FIXME: confirmation dialog
               dbm.initDB();
               break;
               
            case "quit":
               if (lock != null)
                  dbm.unlock(lock);
               System.exit(0);
               break;
               
            case "lock":
               lock=dbm.lock();
               console.info(""+lock);
               break;
               
            case "unlock":
               if (lock != null) {
                  dbm.unlock(lock);
               } else {
                  console.info("Don't hold any lock.");
               }
               break;
               
            case "force-unlock": {
               DBMLock lock=dbm.getExistingLock();
               if (lock != null) {
                  console.info("Releasing "+lock);
                  dbm.unlock(lock);
               } else {
                  console.info("Can't find any lock.");
               }
            } break;
               
            case "show":{
               switch (split[1]) {
                  case "disk.versions":
                     dbm.showDiskVersions();
                     break;
                  case "db.versions":
                     dbm.showDBHistory();
                  break;
                  case "status":
                     dbm.showVersionStatus();
                     break;
                  case "recommendations":
                     showRecommendationsShort();
                     break;
                  case "lock":
                     DBMLock lock=dbm.getExistingLock();
                     if (lock != null)
                        console.info(""+lock);
                     else
                        console.info("No Lock.");
                     break;
                  default:
                     console.warn("Don't know how to show '"+split[1]+"'");
               }
            } break;
            case "?":
            case "help":
               console.println(usage);
               break;
               
            case "":
               break;
            default:
               console.warn("Do not understand '"+split[0]+"'");
         }
      }  catch (Exception ex) {
         console.error("Error in DBM Processor", ex);
      } 
      
      return this;
   }
   
   void showRecommendationsShort() throws IOException, SQLException {
      List<Action> actions=dbm.recommendUpdateActions();
      if (actions == null || actions.size() == 0) {
         console.info("No Recommendations.");
      } else {
         console.info("Recommend: ");
         for (Action action:actions) {
            console.info("   "+action);
         }
      }
   }
   
   boolean confirm(String message, String options[], String positive) throws IOException {
      String x=confirm(message, options);
      return x.equals(positive);
   }
   
   String confirm(String message, String options[]) throws IOException {
      try {
         List<String> lopt=Arrays.asList(options);
         
         console.setCompleters(Arrays.asList(new Completer[] {new StringsCompleter(lopt)}));
         String line=console.readLine(message+" "+lopt+"?").trim();
         while (!lopt.contains(line)) {
            console.warn("Please choose from available options");
            line=console.readLine(message+" "+lopt+"?").trim();
         }
         return line;
         
      } catch (Exception ex) {
         console.warn("Error confirming selection.", ex); // should never happen
         return "";
      }
   }
   
}
