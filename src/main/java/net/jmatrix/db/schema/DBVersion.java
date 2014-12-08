package net.jmatrix.db.schema;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import net.jmatrix.db.common.Version;

/**
 * Represents a version as it exists in the database.
 * 
 */
public class DBVersion {
   String id=null;
   Version version=null;
   
   Date applyDate=null;
   
   String action=null;
   String hostname=null;
   String hostuser=null;
   String dbuser=null;
   
   Boolean rollback=null;
   
   String filepath=null;
   
   String fileChecksum=null;
   String dbChecksum=null;
   
   List<String> rollbackSql=null;
   
   public DBVersion() {
      
   }
   
   public List<SQLStatement> getRollbackStatements() {
      if (rollbackSql != null) {
         List<SQLStatement> state=new ArrayList<SQLStatement>();
         for (String sql:rollbackSql) {
            state.add(new SQLStatement(this, sql));
         }
         return state;
      }
      return null;
   }
   
   public String getRollbackPath() {
      return "ROLLBACK/"+version+"/"+id;
   }
   
   public String toString() {
      return "DBVersion("+version+", "+action+")";
   }

   public Version getVersion() {
      return version;
   }

   public void setVersion(Version version) {
      this.version = version;
   }

   public Date getApplyDate() {
      return applyDate;
   }

   public void setApplyDate(Date applyDate) {
      this.applyDate = applyDate;
   }

   public String getAction() {
      return action;
   }

   public void setAction(String action) {
      this.action = action;
   }

   public String getFilepath() {
      return filepath;
   }

   public void setFilepath(String filepath) {
      this.filepath = filepath;
   }

   public String getHostname() {
      return hostname;
   }

   public void setHostname(String hostname) {
      this.hostname = hostname;
   }

   public String getHostuser() {
      return hostuser;
   }

   public void setHostuser(String hostuser) {
      this.hostuser = hostuser;
   }

   public String getDbuser() {
      return dbuser;
   }

   public void setDbuser(String dbuser) {
      this.dbuser = dbuser;
   }

   public Boolean getRollback() {
      return rollback;
   }

   public void setRollback(Boolean rollback) {
      this.rollback = rollback;
   }

   public String getFileChecksum() {
      return fileChecksum;
   }

   public void setFileChecksum(String fileChecksum) {
      this.fileChecksum = fileChecksum;
   }

   public String getDbChecksum() {
      return dbChecksum;
   }

   public void setDbChecksum(String dbChecksum) {
      this.dbChecksum = dbChecksum;
   }

   public List<String> getRollbackSql() {
      return rollbackSql;
   }

   public void setRollbackSql(List<String> rollbackSql) {
      this.rollbackSql = rollbackSql;
   }

   public String getId() {
      return id;
   }

   public void setId(String id) {
      this.id = id;
   }
}
