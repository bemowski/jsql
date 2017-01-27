package net.jmatrix.db.jsql.cli.commands;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import net.jmatrix.db.common.StringUtils;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.JSQL;
import net.jmatrix.db.jsql.cli.AbstractLineProcessor;
import net.jmatrix.db.jsql.history.SQLHistory;
import net.jmatrix.db.jsql.model.SQLHistoryItem;

/**
 * history [int] [search <query>]
 * 
 * history - lists most recent X queries, where x is some default
 * history 20 - lists 20 most recent queries
 * history search <query> - searches sql history for recent commands containing 
 *                          search string
 * 
 * @author bemo
 */

public class HistoryCommand extends AbstractCommand {
   static final TextConsole console=SysConsole.getConsole();

   SQLHistory history=null;
   
   static String HELP=
         "history [<int>] [clear] [search <query>]";
   
   public HistoryCommand(JSQL j) {
      super(j);
      history=j.getSQLHistory();
   }
   
   @Override
   public boolean accepts(String command) {
      return command != null && command.equals("history");
   }

   @Override
   public void process(String line) throws Exception {
      
      
      String comp[]=line.split(" ");
      
      List<SQLHistoryItem> results=null;
      
      if (comp.length == 1) {
         results=history.recent(10);
      }
      else if (comp.length == 2) {
         String sub=comp[1];
         
         if (sub.equalsIgnoreCase("clear")) {
            boolean confirm=AbstractLineProcessor.confirm("Confirm clear all SQL history?", 
                  new String[] {"yes", "no"}, "yes");
            if (confirm) {
               history.clear();
            }
         } else {
            int max=0;
            try {
               max=Integer.parseInt(comp[1]);
            } catch (NumberFormatException ex) {
               console.warn(HELP, ex);
               return;
            }
            results=history.recent(max);
         }
      } else if (comp.length == 3) {
         String sub=comp[1];
         if (sub.equalsIgnoreCase("clear")) {
            
            boolean confirm=AbstractLineProcessor.confirm("Confirm clear all SQL history?", 
                  new String[] {"yes", "no"}, "yes");
            if (confirm) {
               history.clear();
            }
         } else if (sub.equalsIgnoreCase("search")) {
            results=history.search(comp[2]);
         } else {
            console.warn(HELP);
            return;
         }
      } else {
         console.warn(HELP);
         return;
      }
      display(results);
   }
   
   void display(List<SQLHistoryItem> results) {
      if (results == null || results.size() == 0) {
        console.warn("No History results.");
        return;
      }
      String delim=StringUtils.pad(console.getColumns(), '-');
      
      console.print(delim);
      for (SQLHistoryItem item:results) {
         console.println(format(item));
         console.println(delim);
      }
   }
   
   DateFormat df=new SimpleDateFormat("dd.MMM.yyyy HH:mm:ss");
   
   String format(SQLHistoryItem item) {
      StringBuilder sb=new StringBuilder();
      sb.append("   "+df.format(item.getExecDate())+
            ":  Success: "+item.getSuccess()+" - "+item.getRows()+" rows\n");
      sb.append("   "+item.getConInfo()+"\n\n");
      sb.append(item.getSql());
      
      return sb.toString();
   }
}
