package net.jmatrix.db.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/** */
public class ClassLogFactory {
   /** */
   public static final Logger getLog() {
      String callingClassname=DebugUtils.getCallingClassName(1);
      Logger log=LoggerFactory.getLogger(callingClassname);
      return log;
   }

   public static final Logger getLog(int i) {
      String callingClassname=DebugUtils.getCallingClassName(1+i);
      Logger log=LoggerFactory.getLogger(callingClassname);
      return log;
   }
   
   public static final Logger getLog(String calledClassName) {
      String callingClassname=DebugUtils.getCallingClassName(calledClassName);
      Logger log=LoggerFactory.getLogger(callingClassname);
      return log;
   }
}
