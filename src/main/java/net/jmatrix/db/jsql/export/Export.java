package net.jmatrix.db.jsql.export;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;

public interface Export {
   public void export(File file, String table) throws SQLException, IOException;
   public void export(File file, String table, String where) throws SQLException, IOException;
   public void exportSQL(File file, String table, String sql) throws SQLException, IOException;
}
