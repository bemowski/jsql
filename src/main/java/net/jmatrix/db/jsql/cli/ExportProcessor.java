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

public class ExportProcessor implements LineModeProcessor {
   static final TextConsole console=SysConsole.getConsole();

   static final String TABLE="table";
   static final String FILE="file";
   static final String WHERE="where";
   
   JSQL jsql=null;
   
   static String prompts[]=new String[]{FILE, TABLE, WHERE};
   
   String defaults[]=new String[]{"", "", ""};
   
   int pointer=0;
   String prompt=null;
   String def=null;
   
   Map<String, String> values=new HashMap<String,String>();
   
   public ExportProcessor(JSQL j, String line) {
      jsql=j;
      
      String split[]=line.split("\\ ");
      if (split.length > 1) {
         defaults[1]=split[1];
         defaults[0]=split[1]+".sql";
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
      
      String filename=values.get(FILE);
      String table=values.get(TABLE);
      String where=values.get(WHERE);
      try {
         ExportSQL export=new ExportSQL(jsql.getConnectionInfo());

         
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
