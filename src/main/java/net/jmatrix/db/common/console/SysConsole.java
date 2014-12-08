package net.jmatrix.db.common.console;

public class SysConsole {
   static TextConsole console=null;
   
   
   
   public synchronized static final TextConsole getConsole() {
      if (console != null)
         return console;
      
      String os=System.getProperty("os.name");
      if (os != null) {
         if (os.toLowerCase().contains("linux")) {
            
            // See if JLine is available.
            if (isJLineAvailable()) {
               try {
                  console=new JLineConsole();
               } catch (Exception ex) {
                  throw new RuntimeException("Error getting console.", ex);
               }
            } else {
               System.out.println("JLine not available, returning LinuxConsole");
               console=new LinuxConsole();
            }
         }
      } 
      
      if (console == null)
         console=new DefaultConsole();
      
      return console;
   }
   
   private static boolean isJLineAvailable() {
      // See if JLine is available.
      
      try {
         Class.forName("jline.Terminal");
         return true;
      } catch (Exception ex) {
      }
      return false;
   }
}
