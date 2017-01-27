package net.jmatrix.db.jsql.formatters;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;


import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.DebugUtils;

public abstract class AbstractFormatter implements RSFormatter {
   protected ConnectionInfo conInfo=null;
   
   protected int maxrows=Integer.MAX_VALUE;
   
   protected int lastRowCount=-1;
   
   Map<String, Object> parameters=new HashMap<String, Object>();
   
   protected AbstractFormatter(ConnectionInfo ci){
      conInfo=ci;
   }
   
   protected AbstractFormatter() {}
   
   public int getLastRowCount() {
      int lrc=lastRowCount;
      lastRowCount=-1;
      return lrc;
   }
   
   @Override
   public String toString() {
      return DebugUtils.shortClassname(this)+": "+parameters;
   }
   
   @Override
   public String format(ResultSet rs)  throws SQLException, IOException {
      return format(rs, maxrows);
   }

   @Override
   public String format(ResultSet rs, int rows) throws SQLException, IOException {
      StringWriter sw=new StringWriter();
      format(rs, sw, rows, null, null);
      
      return sw.toString();
   }
   
   @Override
   public int format(ResultSet rs, Writer writer, int rows, String sql, String tablename)
         throws SQLException, IOException {
      return format(rs, writer, rows, sql, tablename, null);
   }
   
   @Override
   public String header(String sql, ResultSet rs) throws SQLException {
      return header(sql, rs, null);
   }

   
   @Override
   public int format(ResultSet rs, Writer writer) throws SQLException, IOException {
      return format(rs, writer, maxrows, null, null);
   }
   
   @Override
   public void setConnectionInfo(ConnectionInfo conInfo) {
      this.conInfo = conInfo;
   }
   
   @Override
   public void setMaxRows(int m) {
      maxrows=m;
   }
   
   @Override
   public void set(String k, Object v) {
      parameters.put(k, v);
   }
}
