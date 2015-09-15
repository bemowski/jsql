package net.jmatrix.db.jsql.cli;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jline.console.completer.Completer;
import jline.console.completer.FileNameCompleter;
import jline.console.completer.StringsCompleter;
import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.JSQL;
import net.jmatrix.db.jsql.SQLRunner;
import net.jmatrix.db.jsql.cli.commands.Command;
import net.jmatrix.db.jsql.cli.commands.DescribeCommand;
import net.jmatrix.db.jsql.cli.commands.LogLevelCommand;
import net.jmatrix.db.jsql.cli.commands.SetCommand;
import net.jmatrix.db.jsql.cli.commands.ShowCommand;
import net.jmatrix.db.jsql.cli.jline.completer.TableNameCompleter;

public class CommandProcessor implements LineModeProcessor {
   static final TextConsole console=SysConsole.getConsole();
   
   TableNameCompleter tableNameCompleter=null;
   
   JSQL jsql=null;
   
   List<Command> commands=null;
   
   List<Completer> completers=new ArrayList<Completer>();
   
   public CommandProcessor(JSQL j) {
      jsql=j;
      
      commands=new ArrayList<Command>();
      commands.add(new ShowCommand(jsql));
      commands.add(new DescribeCommand(jsql));
      commands.add(new SetCommand(jsql));
      commands.add(new LogLevelCommand(jsql));
      
      
      List<String> commands=Arrays.asList(
            new String[] {"connect", "reconnect", "disconnect", "exit", "quit", 
                  "describe", "export", "show", "exportsql",
                  
                  // fixme: these should go into the "ShowCommand"
                  "show db", "show tables", "show views", "show connection",
                  "show procedures",
                  
                  "sql",
                  
                  // Fixme: these should move to LogLevelCommand
                  "debug", "trace",
                  
                  "select", "insert", "update", "delete", "create", "drop",
                  "alter",
                  "dbm", "clear",
                  "help", "?"});
      StringsCompleter cc=new StringsCompleter(commands);
      
      CustomCommandCompleter customCompleter=new CustomCommandCompleter(cc);
      
      completers.add(customCompleter);
      
      tableNameCompleter=new TableNameCompleter();
      if (jsql.isConnected()) {
         tableNameCompleter.connect(jsql.getConnectionInfo());
      }
      completers.add(tableNameCompleter);
   }
   
   @Override
   public String prompt() {
      return "JSQL>";
   }

   @Override
   public Collection<Completer> getCompleters() {
      return completers;
   }
   
   String usage=
         "   connect - leads through prompts to connect to DB.\n"+
         "   reconnect - reconnect to recent databases.\n"+

         "   disconnect - disconnects\n"+
         "   exit: exits the VM.\n\n"+
         
         "   [select|insert|update|delete|create|drop] ... : enter single line sql directly. \n"+
         "   sql: enter multiline sql mode\n"+
         "   sp: enter prepared statement / stored proc processor.\n"+
         "   @<sqlfile>: execute a file.  Command line completion of files\n"+
         "\n"+
         "   dbm: enter DBM mode.  Schema Management.\n"+
         "   show \n"+
         "      db - information about the database\n"+
         "      tables [spec] - list tables\n"+
         "      views - list views\n"+
         "      connection - list views\n"+
         "   describe <table> - show columns for the table.\n"+
         "   export [table]: prompts to export data as inserts from a file.\n"+
         "\n"+
         "   clear: clear the screen\n";
         ;
   
   @Override
   public LineModeProcessor processLine(String line) {
      String split[]=line.split(" ");
      
      String command=split[0];
      
      String lccommand=command.toLowerCase();
      
      long start=System.currentTimeMillis();
      try {
         if (line.startsWith("@")) {
            String filename=line.substring(1).trim();
            File file=new File(filename);
            SQLRunner sqlrunner=new SQLRunner(jsql, file);
            
            sqlrunner.run();
            
            return this;
         }
         
         
         switch (lccommand) {
            case "sql":
               return new SQLProcessor(jsql);

            case "help":
            case "?":
               System.out.println(usage);
               break;
            case "exit":
            case "quit":
               System.exit(0);
               break;
            case "connect":
               return new ConnectProcessor(jsql, line);
            case "reconnect":
               if (jsql.getRecentConnections() != null &&
                   jsql.getRecentConnections().getConnections().size() > 0) 
                  return new ReconnectProcessor(jsql);
               else
                  console.warn("No recent connections available.");
               break;
               
            case "ps":
            case "sp":
               return new PreparedStatementProcessor(jsql);
            case "export":
               return new ExportProcessor(jsql, line);
            case "exportsql":
               return new ExportQueryProcessor(jsql, line);
            case "dbm":
               if (!jsql.isConnected()) {
                  console.warn("Not Connected.");
                  break;
               }
               return new DBMProcessor(jsql);
            case "disconnect":
               jsql.disconnect();
               break;
               

               
            case "select":
            case "insert":
            case "update":
            case "delete":
            case "drop":
            case "create":
            case "alter":
               SQLProcessor sp=new SQLProcessor(jsql);
               sp.execute(line);
               break;
            case "getenv":
               if (split.length == 2)
                  System.out.println (split[1]+":"+System.getenv(split[1]));
               else {
                  Map<String, String> env=System.getenv();
                  List<String> keys=new ArrayList<String>();
                  
                  keys.addAll(env.keySet());
                  Collections.sort(keys);
                  for (String key:keys) {
                     System.out.println (key+":"+env.get(key));
                  }
               }
               break;
            case "clear":
               console.clear();
               
               break;
               
            case "history":
               
               
            case "":
               break;
            default: 
               // Finally, check if we have a single line command processor
               // matching this command.
               Command c=getCommand(command);
               
               if (c != null) {
                  c.process(line);
               } else {
                  console.warn("Do not understand command '"+command+"'");
               }
               
               break;
         }
      } catch (Exception ex) {
         //ex.printStackTrace(System.err);
         console.error("Error in CommandProcessor.", ex);
      } finally {
         long et=System.currentTimeMillis()-start;
         if (et > 20)
            console.info(command+" took "+et+"ms");
      }
      return this;
   }
   
   Command getCommand(String c) {
      
      for(Command command:commands) {
         if (command.accepts(c))
            return command;
      }
      
      return null;
   }
   
   public void connect(ConnectionInfo coninfo) {
      tableNameCompleter.connect(coninfo);
   }
   
   public void disconnect() {
      tableNameCompleter.disconnect();
   }
   
   
   public static class CustomCommandCompleter implements Completer { 
      
      Completer stringCompleter=null;
      Completer fileCompleter=null;
      
      public CustomCommandCompleter(Completer c) {
         stringCompleter=c;
         
         fileCompleter=new FileNameCompleter();
      }
      
      @Override
      public int complete(String buffer, int pos, List<CharSequence> candidates) {
         if (buffer.startsWith("@")) {
            
            int index=fileCompleter.complete(buffer.substring(1), pos, candidates);
            
            
            
//            
//            List<CharSequence> fixed=new ArrayList<CharSequence>(candidates.size());
//            
//            for (CharSequence c:candidates) {
//                fixed.add("@"+c);
//            }
//            candidates.clear();
//            candidates.addAll(fixed);
            return index+1;
         }
         return stringCompleter.complete(buffer, pos, candidates);
      }
   }
}

