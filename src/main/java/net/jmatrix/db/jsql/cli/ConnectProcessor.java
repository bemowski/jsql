package net.jmatrix.db.jsql.cli;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jline.console.completer.Completer;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.drivers.DriverMap;
import net.jmatrix.db.jsql.JSQL;

public class ConnectProcessor implements LineModeProcessor {
   static final TextConsole console=SysConsole.getConsole();

   static final String DRIVER="driver";
   static final String URL="url";
   static final String USER="username";
   static final String PASS="password";
   
   JSQL jsql=null;
   
   static String prompts[]=new String[]{DRIVER, URL, USER, PASS};
   
   static String defaults[]=new String[]{"oracle.jdbc.driver.OracleDriver", 
         "", "", ""};
   
   int pointer=0;
   String prompt=null;
   String def=null;
   
   Map<String, String> values=new HashMap<String,String>();
   
   public ConnectProcessor(JSQL j, String line) {
      jsql=j;
      
      
      StringBuilder sb=new StringBuilder();
      sb.append("Known Drivers(not limited to these):\n");
      for (String driver:DriverMap.drivers) {
         sb.append("   "+driver+"\n");
      }
      console.println(sb.toString());
   }
   
   @Override
   public String prompt() {
      prompt=prompts[pointer];
      def=defaults[pointer];
      
      if (prompt.startsWith("password") && def != null) {
         // mask password.
         def=def.replaceAll(".", "\\*");
      }
      
      pointer++;
      
      return prompt+" ["+def+"]>";
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
         connect();
         return null;
      }
      return this;
   }
   
   void connect() {
      String user=values.get(USER);
      
      // check for oracle style "sys as sysdba" usernames.
      Map<String, String> p=null;
      String usercomponents[]=user.split("\\ ");
      if (usercomponents.length == 3 && usercomponents[1].equals("as")) {
         p=new HashMap<String, String>();
         p.put("internal_logon", usercomponents[2]) ;
         user=usercomponents[0];
         
         console.info("Connecting as user="+user+", proeprties: "+p);
      }
      
      String url=values.get(URL);
      try {
         jsql.connect(values.get(DRIVER), url, user, values.get(PASS), p);
      } catch (Exception ex) {
         console.error("Error connecting to "+user+" at "+url, ex);
      }
   }

   @Override
   public Collection<Completer> getCompleters() {
      return null;
   }
}
