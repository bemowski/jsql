package net.jmatrix.db.common.console;

public class DefaultConsole extends AbstractConsole {

   @Override
   public int getRows() {
      return -1;
   }

   @Override
   public int getColumns() {
      return -1;
   }

   @Override
   public void clear() {
   }
}
