package net.jmatrix.db.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Reads and writes files to and from ~/.config/[app]. 
 * 
 */
public class DotConfig {
   String app=null;
   
   File dir=null;
   
   public DotConfig(String app) {
      this.app=app;
      init();
   }
   
   private void init() {
      dir=new File(System.getProperty("user.home")+File.separator+
            ".config"+File.separator+app);
      
      if (!dir.exists()) {
         dir.mkdirs();
      }
      
      if (dir.exists()) {
         if (!dir.canRead()) {
            throw new RuntimeException("Cannot read config dir at "+dir.getAbsolutePath());
         }
      } else {
         throw new RuntimeException("Config dir does not exist at "+dir.getAbsolutePath());
      }
   }
   
   public void write(String filename, String s) throws IOException {
      File f=new File(dir, filename);
      StreamUtil.write(s, f);
   }
   
   public String read(String filename) throws IOException {
      File f=new File(dir, filename);
      if (!f.exists()) {
         return null;
      }
      return StreamUtil.readToString(f);
   }
   
   public InputStream getInputStream(String filename) throws IOException {
      File f=new File(dir, filename);
      if (!f.exists()) {
         return null;
      }
      return new FileInputStream(f);
   }
   
   public File getFile(String filename) {
      return new File(dir, filename);
   }
}
