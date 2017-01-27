package net.jmatrix.db.jsql.model;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * History model.
 * 
 * @author bemo
 */
public class SQLHistoryItem implements Comparable<SQLHistoryItem> {
   String conInfo;
   String sql;
   int rows=-1;
   Boolean success=false;
   long execTime;
   
   public SQLHistoryItem() {
      execTime=System.currentTimeMillis();
   }

   public SQLHistoryItem(String conInfo, String sql, int rows, boolean success) {
      this();
      this.conInfo=conInfo;
      this.sql=sql;
      this.rows=rows;
      this.success=success;
   }
   
   public boolean matches(String s) {
      return sql.toLowerCase().contains(s);
   }
   
   @JsonIgnore
   public Date getExecDate() {
      return new Date(execTime);
   }

   @Override
   public int compareTo(SQLHistoryItem h) {
      if (h.execTime > execTime) return -1;
      if (h.execTime < execTime) return 1;
      return 0;
   }

   /////////////////////////////////////////////////////////////////////////
   public String getConInfo() {
      return conInfo;
   }

   public void setConInfo(String conInfo) {
      this.conInfo = conInfo;
   }

   public String getSql() {
      return sql;
   }

   public void setSql(String sql) {
      this.sql = sql;
   }

   public int getRows() {
      return rows;
   }

   public void setRows(int rows) {
      this.rows = rows;
   }

   public Boolean getSuccess() {
      return success;
   }

   public void setSuccess(Boolean success) {
      this.success = success;
   }

   public long getExecTime() {
      return execTime;
   }

   public void setExecTime(long execTime) {
      this.execTime = execTime;
   }
}
