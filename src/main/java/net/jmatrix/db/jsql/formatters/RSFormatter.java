package net.jmatrix.db.jsql.formatters;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;

import net.jmatrix.db.common.ConnectionInfo;

/**
 * Used to format data from a result set to a String.
 * 
 * Implementations may include: 
 *   shell - string formatted for shell output
 *   csv - output as csv
 *   sql - format output data as insert statements.
 */
public interface RSFormatter {
   public static final String CONSOLE_WIDTH="width";
   public static final String CONSOLE_LINES="lines";
   
   public String format(ResultSet rs) throws SQLException, IOException;
   public String format(ResultSet rs, int rows) throws SQLException, IOException;
   

   public int format(ResultSet rs, Writer writer) throws SQLException, IOException;
   public int format(ResultSet rs, Writer writer, int rows, String sql, String tablename)
      throws SQLException, IOException;
   
   
   // All formatters must implement this, the above 2 can be implemented by 
   // AbstractFormatter
   public int format(ResultSet rs, Writer writer, int rows, String sql, String tablename,
                     String[] columns)
      throws SQLException, IOException;
   
   public String header(String sql, ResultSet rs) throws SQLException;
   
   public String header(String sql, ResultSet rs, String[] columns) throws SQLException;
   
   public void setConnectionInfo(ConnectionInfo ci);
   
   /**
    * Sets the max number of rows to be processed, if 
    */
   public void setMaxRows(int r);
   
   public void set(String key, Object value);
}
