package net.jmatrix.db.jsql.cli.commands;

import net.jmatrix.db.jsql.JSQL;

public abstract class AbstractCommand implements Command {
   protected JSQL jsql=null;
   
   protected AbstractCommand(JSQL j) {
      jsql=j;
   }
}
