package net.jmatrix.db.schema;

import java.io.File;

import net.jmatrix.db.common.Version;

/**
 * Represents a single executable sql statement.
 */
public class SQLStatement {
   //DiskVersion diskVersion=null;
   Version version=null;
   String sql;
   
   // the file from whence this statement came, if any
   String file=null;
   
   public SQLStatement(DiskVersion dv, String s, File f) {
      sql=s;
      if (f != null)
         file=f.getAbsolutePath();
      
      version=dv.getVersion();
   }
   
   public SQLStatement(Version version, String sql, String file)  {
      this.version=version;
      this.sql=sql;
      this.file=file;
   }
   
   public SQLStatement(DBVersion dbv, String sql) {
      version=dbv.getVersion();
      file=dbv.getRollbackPath();
      this.sql=sql;
   }
   
   public String getFile() {
      return file;
   }

   public String getSql() {
      return sql;
   }
   
   public Version getVersion() {
      return version;
   }
}
