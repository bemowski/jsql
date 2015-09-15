package net.jmatrix.db.jsql.formatters;

import java.io.IOException;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import net.jmatrix.db.common.ClassLogFactory;
import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.SQLUtil;

import org.slf4j.Logger;

/**
 * Formats results as INSERT statments.
 *
 * */
public class SQLFormatter extends AbstractFormatter {
   private static Logger log=ClassLogFactory.getLog();

   DateFormat DATE_FORMAT=new SimpleDateFormat("yyyy-MM-dd");
   DateFormat TIME_FORMAT=new SimpleDateFormat("HH:mm:ss");
   DateFormat TIMESTAMP_FORMAT=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
   DateFormat DATETIME_FORMAT=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
   
   public SQLFormatter(ConnectionInfo ci) {
      super(ci);
   }
   
   public SQLFormatter() {}
   
   @Override
   public int format(ResultSet rs, Writer writer, final int rows, String sql, 
         String table, String[] columnns)
         throws SQLException, IOException {
      
      // 'columns' is ignored here - we assume we are exporting an entire table.
      
      writer.write(header(sql, rs)+"\n");
      
      ResultSetMetaData rsmd=rs.getMetaData();
      
      
      if (table == null) {
         try {
            // this returns the empty string on Oracle, but may work on other databses.
            table=rsmd.getTableName(1);
         } catch (Exception ex) {
            log.warn("Can't infer table name from ResultSetMetaData", ex);
         }
      }
      
      int rowcount=0;
      while (rs.next()) {
         rowcount++;
         writer.write(insertSQL(table, rs, rsmd)+"\n");
      }
      
      writer.flush();
      
      return rowcount;
   }
   
   
   /** */
   private final String insertSQL(String table, ResultSet rs, ResultSetMetaData rsmd) throws SQLException {
      StringBuilder sb=new StringBuilder();
      
      sb.append("INSERT INTO "+table+" (");
      
      int cols=rsmd.getColumnCount();
      for (int i=1; i<=cols; i++) {
         sb.append(rsmd.getColumnName(i));
         if (i < cols) 
            sb.append(",");
      }
      sb.append(")\n  VALUES(");
      
      for (int i=1; i<=cols; i++) {
         sb.append(asSQL(rs.getObject(i), rsmd.getColumnType(i)));
         if (i < cols) 
            sb.append(",");
      }
      sb.append(");");
      
      return sb.toString();
   }
   
   private final String asSQL(Object o, int colType) {
      if (o == null) {
         return "NULL";
      }
      if (o instanceof Number) {
         return ((Number)o).toString();
      } else if (o instanceof String) {
         
         String value=SQLUtil.escape(o.toString());
         return "'"+value+"'";
      } else if (o instanceof Boolean) {
         return ((Boolean)o)?"1":"0";
      } else if (o instanceof java.util.Date) {
         if (colType == Types.DATE) {
            return "DATE '"+DATE_FORMAT.format((java.util.Date)o)+"'";
         } else if (colType == Types.TIMESTAMP) {
            Timestamp timestamp=(Timestamp)o;
            
            return "TIMESTAMP '"+TIMESTAMP_FORMAT.format((java.util.Date)o)+"."+timestamp.getNanos()+"'";
         } else if (colType == Types.TIME) {
            return "TIME '"+TIME_FORMAT.format((java.util.Date)o)+"'";
         } else {
            System.out.println ("Unknown date type "+colType+": "+SQLUtil.jdbcTypeString(colType));
            return "'"+DATETIME_FORMAT.format((java.util.Date)o)+"'";
         }
      }
      
      else {
         System.out.println ("Unknown type "+o.getClass().getName()+", jdbc type "+colType+": "+SQLUtil.jdbcTypeString(colType));

         return "'"+o.toString()+"'";
      }
   }

   @Override
   public String header(String sql, ResultSet rs, String[] columns)
         throws SQLException {
      // 'columns' is ignored here - we assume we are exporting an entire table.

      StringBuilder sb=new StringBuilder();
      
      DateFormat df=new SimpleDateFormat("MM/dd/yyyy HH:mm:ss ZZZZ");
      
      sb.append("/*\n");
      sb.append(" * Export from "+conInfo.getUsername()+" on "+conInfo.getUrl()+"\n");
      sb.append(" * SQL: \n"+sql+"\n");
      sb.append(" * Export on "+df.format(new java.util.Date())+"\n");
      sb.append(" */\n\n");
      
      return sb.toString();
   }
}
