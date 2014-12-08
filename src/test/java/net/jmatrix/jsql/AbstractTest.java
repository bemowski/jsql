package net.jmatrix.jsql;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AbstractTest {
   
   static final DateFormat df=new SimpleDateFormat("HH:mm:ss.SSSS");
   
   protected static final void log(String s) {
      log(s, null);
   }
   
   protected static final void log(String s, Throwable t) {
      String tn=Thread.currentThread().toString();
      
      System.out.println(timestamp()+" ["+tn+"]: "+s);
   }
   
   private synchronized static final String timestamp() {
      return df.format(new Date());
   }
}
