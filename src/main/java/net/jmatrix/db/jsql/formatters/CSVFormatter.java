package net.jmatrix.db.jsql.formatters;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

import net.jmatrix.db.common.ConnectionInfo;

public class CSVFormatter extends AbstractFormatter {
   
   public CSVFormatter(ConnectionInfo ci) {
      super(ci);
   }
   
   public CSVFormatter() {
      
   }

   @Override
   public int format(ResultSet rs, Writer writer, int rows, String sql, String table,
         String[] columns)
         throws SQLException, IOException {
      writer.append(header(sql, rs)+"\n");
      
      // fixme: columns ignored.
      
      int rowcount=0;
      
      ResultSetMetaData rsmd=rs.getMetaData();
      
      int cols=rsmd.getColumnCount();
      
      while (rs.next()) {
         rowcount++;
         for (int i=1; i<=cols; i++) {
            Object o=rs.getString(i);
            writer.append(asString(o));
            if (i < cols)
               writer.append(",");
         }
         writer.append("\n");
         
         if (rowcount > rows)
            break;
      }
      return rowcount;
   }
   
   /** A little CSV escapting... */
   static final String asString(Object o) {
      String val=null;
      
      if (o == null)
         val="NULL";
      val=o.toString();
      
      val.replace("\"", "\"\"");
      return "\""+val+"\"";
   }

   @Override
   public String header(String sql, ResultSet rs, String[] columns) throws SQLException {
      StringBuilder sb=new StringBuilder();
      
      ResultSetMetaData rsmd=rs.getMetaData();
      
      if (columns != null) {
         // fixme.
      }
      
      int cols=rsmd.getColumnCount();
      
      for (int i=1; i<=cols; i++) {
         String cname=rsmd.getColumnName(i);
         sb.append(cname); 
         
         if (i < cols) {
            sb.append(",");
         }
      }
      
      return sb.toString();
   }
}
