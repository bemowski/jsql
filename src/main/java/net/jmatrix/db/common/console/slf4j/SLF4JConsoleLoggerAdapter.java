package net.jmatrix.db.common.console.slf4j;

import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.common.console.TextConsole.Level;

import org.slf4j.helpers.MarkerIgnoringBase;

public class SLF4JConsoleLoggerAdapter extends MarkerIgnoringBase {
   String name=null;
   TextConsole console=null;
   
   public SLF4JConsoleLoggerAdapter(String n, TextConsole c) {
      name=n;
      console=c;
   }
   
   //////////////////////////// TRACE ////////////////////////////////
   @Override
   public boolean isTraceEnabled() {
      return console.getLevel().getILevel() >= Level.ALL.getILevel();
   }

   @Override
   public void trace(String msg) {
     trace(msg, (Throwable)null);
   }

   @Override
   public void trace(String format, Object arg) {
      trace(String.format(format, arg));
   }

   @Override
   public void trace(String format, Object arg1, Object arg2) {
      trace(String.format(format, arg1, arg2));
   }

   @Override
   public void trace(String format, Object... arguments) {
      trace(String.format(format, arguments));
   }

   @Override
   public void trace(String msg, Throwable t) {
      console.trace(msg, t);
   }

   //////////////////////////// DEBUG ////////////////////////////////
   @Override
   public boolean isDebugEnabled() {
      return console.getLevel().getILevel() >= Level.DEBUG.getILevel();
   }

   
   @Override
   public void debug(String msg) {
      debug(msg, (Throwable)null);
   }

   @Override
   public void debug(String format, Object arg) {
      debug(String.format(format, arg));
   }

   @Override
   public void debug(String format, Object arg1, Object arg2) {
      debug(String.format(format, arg1, arg2));
   }

   @Override
   public void debug(String format, Object... arguments) {
      debug(String.format(format, arguments));
   }

   @Override
   public void debug(String msg, Throwable t) {
      console.debug(msg, t);
   }

   @Override
   public boolean isInfoEnabled() {
      return console.getLevel().getILevel() >= Level.LOG.getILevel();
   }

   //////////////////////////// INFO ////////////////////////////////
   @Override
   public void info(String msg) {
       console.info(msg);
   }

   @Override
   public void info(String format, Object arg) {
      info(String.format(format, arg));
   }

   @Override
   public void info(String format, Object arg1, Object arg2) {
      info(String.format(format, arg1, arg2));
   }

   @Override
   public void info(String format, Object... arguments) {
      info(String.format(format, arguments));
   }

   @Override
   public void info(String msg, Throwable t) {
      console.info(msg, t);
   }

   //////////////////////////// WARN ////////////////////////////////
   @Override
   public boolean isWarnEnabled() {
      return console.getLevel().getILevel() >= Level.WARN.getILevel();
   }

   @Override
   public void warn(String msg) {
      console.warn(msg, (Throwable)null);
   }

   @Override
   public void warn(String format, Object arg) {
      warn(String.format(format, arg));
   }

   @Override
   public void warn(String format, Object... arguments) {
      warn(String.format(format, arguments));
   }

   @Override
   public void warn(String format, Object arg1, Object arg2) {
      warn(String.format(format, arg1, arg2));
   }

   @Override
   public void warn(String msg, Throwable t) {
      console.warn(msg, t);
   }

   //////////////////////////// ERROR ////////////////////////////////
   @Override
   public boolean isErrorEnabled() {
      return console.getLevel().getILevel() >= Level.ERROR.getILevel();
   }

   @Override
   public void error(String msg) {
      console.error(msg, (Throwable)null);
   }

   @Override
   public void error(String format, Object arg) {
      error(String.format(format, arg));
   }

   @Override
   public void error(String format, Object arg1, Object arg2) {
      error(String.format(format, arg1, arg2));
   }

   @Override
   public void error(String format, Object... arguments) {
      error(String.format(format, arguments));
   }

   @Override
   public void error(String msg, Throwable t) {
      console.error(msg, t);
   }
}
