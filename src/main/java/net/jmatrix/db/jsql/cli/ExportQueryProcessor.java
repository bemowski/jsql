package net.jmatrix.db.jsql.cli;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jline.console.completer.Completer;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.ExportSQL;
import net.jmatrix.db.jsql.JSQL;

/**
 * The generic ExportProcessor is used to export an entire table - always
 * using "select * from [tablename]".  The ExportQueryProcessor will
 * export a generic query as a table.  In essence - it behaves like
 * creating a view - and then exporting the view as if it were
 * a single table. 
 */
public class ExportQueryProcessor implements LineModeProcessor {
   static final TextConsole console=SysConsole.getConsole();

   static final String TABLE="virtual tablename";
   static final String FILE="file";
   
   JSQL jsql=null;
   
   static String prompts[]=new String[]{FILE, TABLE};
   
   String defaults[]=new String[]{"", ""};
   
   int pointer=0;
   String prompt=null;
   String def=null;
   
   Map<String, String> values=new HashMap<String,String>();
   
   StringBuilder sqlbuffer=new StringBuilder();
   
   boolean sqlmode=false;
   
   public ExportQueryProcessor(JSQL j, String line) {
      jsql=j;
      
      String split[]=line.split("\\ ");
      if (split.length > 1) {
         defaults[1]=split[1];
         defaults[0]=split[1]+".sql";
      }
   }
   
   @Override
   public String prompt() {
      if (pointer < prompts.length) {
         prompt=prompts[pointer];
         def=defaults[pointer];
         
         pointer++;
         return "Export-"+prompt+" ["+def+"]>";
      } else {
         sqlmode=true;
         return "Export SQL $>";
      }
   }

   @Override
   public LineModeProcessor processLine(String line) {
      
      if (sqlmode) {
         line = line.replaceAll("\\s+$", "");
         
         if (line.endsWith(";"))  {
            sqlbuffer.append(line);
            export();
            return null;
         } else {
            sqlbuffer.append(line+"\n");
            return this;
         }
      }
      
      line=line.trim();
      if (line.length() == 0) {
         values.put(prompt, def);
      } else {
         values.put(prompt, line);
         defaults[pointer-1]=line;
      }
      return this;
   }
   
   void export() {
      String filename=values.get(FILE);
      String table=values.get(TABLE);
      try {
         ExportSQL export=new ExportSQL(jsql.getConnectionInfo());

         
         File file=new File(filename);
         
         export.exportSql(file, table, sqlbuffer.toString());
      } catch (Exception ex) {
         console.error("Error exporting sql as "+table+" to "+filename, ex);
      }
   }

   @Override
   public Collection<Completer> getCompleters() {
      // TODO Auto-generated method stub
      return null;
   }
}
