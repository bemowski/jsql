package net.jmatrix.db.jsql.cli;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import jline.console.completer.Completer;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.JSQL;
import net.jmatrix.db.jsql.export.Export;
import net.jmatrix.db.jsql.export.ExportCSV;
import net.jmatrix.db.jsql.export.ExportSQL;

public class ExportProcessor implements LineModeProcessor {
   static final TextConsole console=SysConsole.getConsole();

   static final String TABLE="table";
   static final String FILE="file";
   static final String WHERE="where";
   static final String FORMAT="format [csv|sql]";
   
   JSQL jsql=null;
   
   static String prompts[]=new String[]{FORMAT, FILE, TABLE, WHERE};
   
   String defaults[]=new String[]{"sql", "", "", ""};
   
   int pointer=0;
   String prompt=null;
   String def=null;
   
   Map<String, String> values=new HashMap<String,String>();
   
   public ExportProcessor(JSQL j, String line) {
      jsql=j;
      
      String split[]=line.split("\\ ");
      if (split.length > 1) {
         defaults[2]=split[1];
         defaults[1]=split[1]+".sql";
      }
   }
   
   @Override
   public String prompt() {
      prompt=prompts[pointer];
      def=defaults[pointer];
      
      pointer++;
      return "Export-"+prompt+" ["+def+"]>";
   }

   @Override
   public LineModeProcessor processLine(String line) {
      
      line=line.trim();
      if (line.length() == 0) {
         values.put(prompt, def);
      } else {
         values.put(prompt, line);
         defaults[pointer-1]=line;
      }
      
      
      if (pointer==prompts.length) {
         
         export();
         return null;
      }
      return this;
   }
   
   void export() {
      
      String format=values.get(FORMAT);
      String filename=values.get(FILE);
      String table=values.get(TABLE);
      String where=values.get(WHERE);
      
      Export export=null;
      
      if (format.equalsIgnoreCase("csv")) {
         export=new ExportCSV(jsql.getConnectionInfo());
      } else if (format.equalsIgnoreCase("sql")) {
         export=new ExportSQL(jsql.getConnectionInfo());
      } else {
         throw new RuntimeException("Invalid value for 'format' [csv|sql]: '"+format+"'");
      }
      
      try {
         File file=new File(filename);
         
         export.export(file, table, where);
      } catch (Exception ex) {
         console.error("Error exporting from "+table+" where "+where+" to "+filename, ex);
      }
   }

   @Override
   public Collection<Completer> getCompleters() {
      // TODO Auto-generated method stub
      return null;
   }
}
