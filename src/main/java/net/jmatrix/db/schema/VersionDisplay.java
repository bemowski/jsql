package net.jmatrix.db.schema;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

import net.jmatrix.db.common.Version;

public class VersionDisplay {
   Version version=null;
   DiskVersion diskVersion;
   DBVersion dbVersion=null;
   
   DateFormat df=new SimpleDateFormat("ddMMMyyyy HH:mm");

   public VersionDisplay() {}
   public VersionDisplay(Version v, DiskVersion dv, DBVersion dbv) {
      version=v;
      diskVersion=dv;
      dbVersion=dbv;
   }
   
   public Version getVersion() {
      return version;
   }
   public DiskVersion getDiskVersion() {
      return diskVersion;
   }
   public DBVersion getDbVersion() {
      return dbVersion;
   }
   
   public String getApplyCount() {
      if (diskVersion != null)
         return ""+diskVersion.getApplyCount();
      return "";
   }
   
   public String getRollbackCount() {
      if (diskVersion != null)
         return ""+diskVersion.getRollbackCount();
      return "";
   }
   
   public String getDiskDate() {
      if (diskVersion != null) {
         return df.format(diskVersion.getDate());
      }
      return "";
   }
   
   public String getDbDate() {
      if (dbVersion != null) {
         return df.format(dbVersion.getApplyDate());
      }
      return "";
   }
   
   public String getAction() {
      if (dbVersion != null) {
         return dbVersion.getAction();
      }
      return "";
   }
}