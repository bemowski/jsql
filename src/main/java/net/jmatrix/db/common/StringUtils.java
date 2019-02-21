package net.jmatrix.db.common;

import java.util.HashMap;
import java.util.Map;


/**
 * 
 */
public class StringUtils {
   static Map<String, String> padmapper=new HashMap<String, String>();
   
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

   public static final boolean empty(String s) {
      return s==null||s.length()==0;
   }


   public static final String notNull(String s) {
      if (s == null)
         return "";
      return s;
   }
}
