package net.jmatrix.db.jsql.cli;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;

public abstract class AbstractLineProcessor implements LineModeProcessor {
   static TextConsole console=SysConsole.getConsole();
   
   public static boolean confirm(String message, String options[], String positive) throws IOException {
      String x=confirm(message, options);
      return x.equals(positive);
   }
   
   public static String confirm(String message, String options[]) throws IOException {
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
