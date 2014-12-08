package net.jmatrix.db.schema;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.zip.CRC32;

import net.jmatrix.db.common.ClassLogFactory;
import net.jmatrix.db.common.SQLUtil;
import net.jmatrix.db.common.StreamUtil;
import net.jmatrix.db.common.Version;

import org.slf4j.Logger;

/**
 * Encapsulates a directory with a version of the database, or a delta
 * to the database.
 * 
 * Each directory should contain "apply" and "rollback" directories.
 */
public class DiskVersion implements Comparable<DiskVersion> {
   //static TextConsole console=SysConsole.getConsole();
   static Logger log=ClassLogFactory.getLog();
   
   Version version=null;
   File path=null;
   
   /** Date in this context is the timestamp of the latest modification time
    * of any of the apply or rollback files.  tar preserves timestamps
    * so this should preserve latest modification time of the SQL. 
    * 
    * This is actually less useful - git does not preserve file mod time.*/
   Date date=null;
   
   List<File> applyFiles=null;
   List<File> rollbackFiles=null;
   
   List<SQLStatement> applyStatements;
   List<SQLStatement> rollbackStatements;
   
   long checksum=-1;
   
   /** */
   public DiskVersion(File p) throws IOException {
      path=p;

      log.debug("Path name: "+path.getName());
      
      String sver=path.getName();
      
      version=new Version(sver);

      applyFiles=getSQLFiles(new File(path, "apply"));
      rollbackFiles=getSQLFiles(new File(path, "rollback"));
      
      if (applyFiles != null) {
         applyStatements=getStatmentsFromFiles(applyFiles);
      }
      if (rollbackFiles != null) {
         rollbackStatements=getStatmentsFromFiles(rollbackFiles);
      }
      
      updateDiskDate();
   }
   
   private void updateDiskDate() {
      long mtime=0;
      if (applyFiles != null) {
         for (File f:applyFiles) {
            if (f.lastModified() > mtime)
               mtime=f.lastModified();
         }
      }
      if (rollbackFiles != null) {
         for (File f:rollbackFiles) {
            if (f.lastModified() > mtime)
               mtime=f.lastModified();
         }
      }
      date=new Date(mtime);
   }
   
   @Override
   public int compareTo(DiskVersion o) {
      return version.compareTo(o.version);
   }
   
   public String toString() {
      StringBuilder sb=new StringBuilder();
      
      sb.append(version+": ");
      if (applyFiles != null) {
         sb.append("Apply ["+applyStatements.size()+" statements in "+applyFiles.size()+" files]");
      } else {
         sb.append("No Apply");
      }
      
      if (rollbackFiles != null) {
         sb.append(" Rollback ["+rollbackStatements.size()+" statements in "+rollbackFiles.size()+" files]");
      } else { 
         sb.append(" No Rollback");
      }
      return sb.toString();
   }
   
   private List<SQLStatement> getStatmentsFromFiles(List<File> files) throws IOException {
      List<SQLStatement> statements=new ArrayList<SQLStatement>();
      for (File file:files) {
         log.debug("Parsing SQL file "+file);
         
         String sql=StreamUtil.readToString(file);
         
         sql=SQLUtil.stripSQLComments(sql);
         
         List<String> sqls=SQLUtil.splitSQL(sql, ";");
         
         for (String st:sqls) {
            statements.add(new SQLStatement(this, st, file));
         }
      }
      return statements;
   }
   
   /** */
   private static List<File> getSQLFiles(File path) {
      
      if (path.exists() && path.canRead()) {
         
         File files[]=path.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
               if (pathname.getName().toLowerCase().endsWith(".sql") ||
                     pathname.getName().toLowerCase().endsWith(".ddl")) {
                  return true;
               }
               return false;
            }
         });
         List<File> fileList=Arrays.asList(files);
         Collections.sort(fileList);
         return fileList;
      }
      return null;
   }
   
   /**
    * Returns a checksum of the applied changes only.
    */
   long calculateApplyChecksum() {
      if (applyStatements == null || applyStatements.size() == 0) {
         return 0;
      }
      
      CRC32 crc=new CRC32();
      for (SQLStatement applystate:applyStatements) {
         crc.update(applystate.getSql().getBytes());
      }
      return crc.getValue();
   }
   
   ///////////////////////////////////////////////////////////////////////////
   
   public String getChecksum() {
      if (checksum == -1) {
         checksum=calculateApplyChecksum();
      }
      return ""+checksum;
   }
   
   public Version getVersion() {
      return version;
   }

   public void setVersion(Version version) {
      this.version = version;
   }

   public File getPath() {
      return path;
   }

   public void setPath(File path) {
      this.path = path;
   }

   public List<File> getApplyFiles() {
      return applyFiles;
   }

   public void setApplyFiles(List<File> applyFiles) {
      this.applyFiles = applyFiles;
   }

   public List<File> getRollbackFiles() {
      return rollbackFiles;
   }

   public void setRollbackFiles(List<File> rollbackFiles) {
      this.rollbackFiles = rollbackFiles;
   }

   public List<SQLStatement> getApplyStatements() {
      return applyStatements;
   }

   public void setApplyStatements(List<SQLStatement> applyStatements) {
      this.applyStatements = applyStatements;
   }

   public List<SQLStatement> getRollbackStatements() {
      return rollbackStatements;
   }

   public void setRollbackStatements(List<SQLStatement> rollbackStatements) {
      this.rollbackStatements = rollbackStatements;
   }
   
   public int getApplyCount() {
      if (applyStatements == null)
         return 0;
      return applyStatements.size();
   }
   public int getRollbackCount() {
      if (rollbackStatements == null)
         return 0;
      return rollbackStatements.size();
   }

   public Date getDate() {
      return date;
   }
}
