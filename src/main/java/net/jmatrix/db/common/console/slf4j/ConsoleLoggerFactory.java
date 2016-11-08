package net.jmatrix.db.common.console.slf4j;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;

public class ConsoleLoggerFactory implements ILoggerFactory {
   TextConsole console=SysConsole.getConsole();
   
   static Map<String, SLF4JConsoleLoggerAdapter> map=new HashMap<String, SLF4JConsoleLoggerAdapter>();
   
   @Override
   public Logger getLogger(String name) {
      SLF4JConsoleLoggerAdapter log=map.get(name);
      if (log == null) {
         synchronized(ConsoleLoggerFactory.class) {
            log=map.get(name);
            if (log == null) {
               log=new SLF4JConsoleLoggerAdapter(name, console);
               map.put(name, log);
            }
         }
      }
      return log;
   }
}
