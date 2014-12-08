package org.slf4j.impl;

import net.jmatrix.db.common.console.slf4j.ConsoleLoggerFactory;

import org.slf4j.ILoggerFactory;
import org.slf4j.spi.LoggerFactoryBinder;

public class StaticLoggerBinder implements LoggerFactoryBinder {
   static StaticLoggerBinder SINGLEDON=null;
   
   static ConsoleLoggerFactory clf=null;
   static {
      clf=new ConsoleLoggerFactory();
      SINGLETON = new StaticLoggerBinder();

   }  
   
   private static StaticLoggerBinder SINGLETON = new StaticLoggerBinder();
   
   @Override
   public ILoggerFactory getLoggerFactory() {
      return clf;
   }

   @Override
   public String getLoggerFactoryClassStr() {
      return ConsoleLoggerFactory.class.getName();
   }
   
   public static StaticLoggerBinder getSingleton() {
      return SINGLETON;
    }
}
