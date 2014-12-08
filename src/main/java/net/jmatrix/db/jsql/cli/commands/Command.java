package net.jmatrix.db.jsql.cli.commands;

public interface Command {
   public boolean accepts(String command);
   public void process(String line) throws Exception;
}
