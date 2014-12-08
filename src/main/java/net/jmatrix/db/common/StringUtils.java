package net.jmatrix.db.common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import sun.misc.BASE64Encoder;

/**
 * 
 */
public class StringUtils {
   static Map<String, String> padmapper=new HashMap<String, String>();
   
   static BASE64Encoder b64=new BASE64Encoder();
   
   public static final String pad(int w, char c) {
      String s=padmapper.get(w+"-"+c);
      
      if (s != null)
         return s;
      StringBuilder sb=new StringBuilder();
      for (int i=0; i<w; i++)
         sb.append(c);
      s=sb.toString();
      padmapper.put(w+"-"+c, s);
      return s;
   }
   
   public static String shortUUID() {
      UUID uuid=UUID.randomUUID();
      
      String uuidString=UUID.randomUUID().toString();
      
      long longOne = uuid.getMostSignificantBits();
      long longTwo = uuid.getLeastSignificantBits();

      byte b[]=new byte[] {
           (byte)(longOne >>> 56),
           (byte)(longOne >>> 48),
           (byte)(longOne >>> 40),
           (byte)(longOne >>> 32),   
           (byte)(longOne >>> 24),
           (byte)(longOne >>> 16),
           (byte)(longOne >>> 8),
           (byte) longOne,
           (byte)(longTwo >>> 56),
           (byte)(longTwo >>> 48),
           (byte)(longTwo >>> 40),
           (byte)(longTwo >>> 32),   
           (byte)(longTwo >>> 24),
           (byte)(longTwo >>> 16),
           (byte)(longTwo >>> 8),
           (byte) longTwo
            };
      
      String s=b64.encode(b).replaceAll("=", "");
      
      System.out.println ("    '"+uuid+"'");
      System.out.println ("    '"+s+"'");
      
      return s;
   }
   
   public static void main(String args[]) {
      shortUUID();
   }
}
