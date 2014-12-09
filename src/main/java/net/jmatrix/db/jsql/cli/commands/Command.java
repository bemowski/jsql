package net.jmatrix.db.jsql.cli.commands;


/** 
 * Represents a single line command processor.
 */
public interface Command {
   public boolean accepts(String command);
   public void process(String line) throws Exception;
}
