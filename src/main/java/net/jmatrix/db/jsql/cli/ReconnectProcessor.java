package net.jmatrix.db.jsql.cli;

import java.util.Collection;
import java.util.List;

import jline.console.completer.Completer;
import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.JSQL;
import net.jmatrix.db.jsql.model.RecentConnections;


public class ReconnectProcessor implements LineModeProcessor {
   static final TextConsole console=SysConsole.getConsole();

   JSQL jsql=null;
   
   RecentConnections rc=null;
   
   public ReconnectProcessor(JSQL j) {
      jsql=j;
      rc=jsql.getRecentConnections();
   }
   
   String def="0";
   
   @Override
   public String prompt() {
      
      List<ConnectionInfo> connections=rc.getConnections();
      
      int size=connections.size();
      for (int i=size-1; i>=0; i--) {
         ConnectionInfo ci=connections.get(i);
         console.println("   "+i+": "+ci);
      }
      
      return "reconnect["+def+"]>";
   }

   @Override
   public LineModeProcessor processLine(String line) {
      // only input should be an integer.
      
      String sindex;
      if (line.trim().equals("")) {
         sindex=def;
      } else {
         sindex=line.trim();
      }
      
      int index=-1;
      try {
         index=Integer.parseInt(sindex);
      } catch (NumberFormatException ex) {
         console.warn("Cannot parse reconnect index from '"+sindex+"'");
         return null;
      }
      
      
      List<ConnectionInfo> connections=rc.getConnections();
      if(index < 0 || index > connections.size() || connections.size() == 0) {
         console.warn("Reconnect index out of range.");
         return null;
      }
      
      ConnectionInfo ci=connections.get(index);
      
      try {
         jsql.connect(ci);
      } catch (Exception ex) {
         console.error("Cannot reconnect to "+ci+": "+ex.toString());
      }
      return null;
   }

   @Override
   public Collection<Completer> getCompleters() {
      // TODO Auto-generated method stub
      return null;
   }
}
