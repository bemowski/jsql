package net.jmatrix.db.common;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;



/**
 * A series of utility methods used for debugging and logging.
 */
public class DebugUtils {
   private static boolean debug = Boolean.valueOf(System.getProperty("debug", "false"));

   public static int MAX_LENGTH=65;
   
   public static final String debugString(Object o) {
      return debugString(o, 0);
   }
   
   /** 
    * The debugString() method uses introspection to write the
    * state of an object based on its public getters.
    */
   public static final String debugString(Object obj, int depth) {
      if (obj == null)
         return "null";
      StringBuilder sb=new StringBuilder();
      
      Method methods[]=obj.getClass().getMethods();
      //System.out.println ("There are "+fields.length+" fields in "+this.getClass());
      Method method=null;
      
      String pad=padString((depth*3)+3, " ");
      try {
         sb.append(pad+"getClass: "+obj.getClass().getName()+"\n");
         
         for (int i=0; i<methods.length; i++) {
            method=methods[i];
            if ((method.getName().startsWith("get") &&
                 method.getParameterTypes().length == 0 &&
                 !method.getName().equals("getClass")) 
                
                ||
                
                (method.getName().startsWith("is") &&
                 method.getParameterTypes().length == 0)) {
               
               Object val=method.invoke(obj, (Object[])null);
               
               String valueString=valueString(val, depth);
               if (!valueString.endsWith("\n")) {
                  valueString=valueString+"\n";
               }
               
               sb.append(pad+method.getName()+": "+valueString);
            }
         }
      } catch (Exception ex) {
         throw new RuntimeException("Error introspecting Method "+method.getName()+" to build debug string for "+obj.getClass().getName(), ex);
      }
      
      return sb.toString();
   }
   
   /** */
   private static final String valueString(Object obj, int depth) {
      if (obj == null)
         return "null";
      else if (obj.getClass().isEnum()) {
         return obj.toString();
      } else if (obj instanceof List) {
         List list=(List)obj;
//         StringBuilder sb=new StringBuilder();
//         sb.append("List["+list.size());
//         if (list.size() > 0) {
//            sb.append(", type="+list.get(0).getClass().getName());
//         }
//         sb.append("]");
//         
//         return truncate(sb.toString());
         return list.toString();
      } else if (obj instanceof String) {
         return truncate(obj.toString());
      } else if (obj.getClass().getName().toLowerCase().contains("domain") ||
            obj.getClass().getName().toLowerCase().contains("mongo.test")||
            obj.getClass().getName().toLowerCase().contains("etws")) {
         return "\n"+debugString(obj, depth+1);
      } else {
         return truncate(obj.toString());
      }
   }
   
   /** */
   public static final String truncate(String s) {
      return truncate(s, MAX_LENGTH);
   }
   
   /** */
   public static final String truncate(String s, int len) {
      if (s == null)
         return "null";
      
      if (s.length() > len) {
         return s.substring(0, len)+"...";
      }
      return s;
   }
   
   /** */
   public static final String splitString(String s, String prefix, 
         String suffix, int chunk) {
      
      if (s.length() < chunk) {
         return prefix+s+suffix;
      }
      
      StringBuilder split=new StringBuilder();
      int chunks=(int)Math.ceil((double)s.length()/(double)chunk);
      //System.out.println ("s.length() = "+s.length()+" chunk="+chunk);
      //System.out.println ("Chunks="+chunks);
      
      for (int i=0; i<chunks-1; i++) {
         split.append(prefix);
         int start=i*chunk;
         int end=(i+1)*chunk;
         //System.out.println ("  start="+start+" end="+end);
         split.append(s.substring(start, end));
         split.append(suffix);
      }
      split.append(prefix);
      split.append(pad(s.substring(chunk*(chunks-1)), chunk, " "));
      split.append(suffix);
      
      return split.toString();
   }
   
   /** */
   public static final String pad(String s, int len, String padchar) {
      if (s.length() >= len) 
         return s;
      int padchars=len - s.length();
      StringBuilder sb=new StringBuilder();
      sb.append(s);
      for (int i=0; i<padchars; i++) {
         sb.append(padchar);
      }
      return sb.toString();
   }
   
   public static final String fixedWidth(Object obj, int len) {
      String s=null;
      if (obj == null)
         s="null";
      else
         s=obj.toString();
      
      if (s.length() == len)
         return s;
      else if (s.length() < len) 
         return pad(s, len, " ");
      else 
         return s.substring(0, len);
   }
   
   /** */
   public static final String stackString(Throwable ex) {
      ByteArrayOutputStream baos=new ByteArrayOutputStream();
      PrintWriter pw=new PrintWriter(new OutputStreamWriter(baos));
      ex.printStackTrace(pw);
      pw.flush();
      return baos.toString();
   }
   
   public static final String shortClassname(Object o) {
      if (o == null) return "null";
      return shortClassname(o.getClass().getName());
   }
   
   public static final String shortClassname(Class c) {
      if (c == null) return "null";
      return shortClassname(c.getName());
   }
   
   public static final String shortClassname(String s) {
      return s.substring(s.lastIndexOf(".")+1);
   }
   
   /**
    * java.lang.NullPointerException -> j.l.NullPointerException
    */
   public static final String abbreviateClassname(Class c) {
      if (c == null)
         return "null";
      String exc=c.getName();
      String split[]=exc.split("\\.");
      StringBuilder sb=new StringBuilder();
      for (int i=0; i<split.length-1; i++) 
         sb.append(split[i].charAt(0)+".");
      sb.append(split[split.length-1]);
      return sb.toString();
   }
   
   /** */
   public static final String indent(String s, int depth) {
      StringBuilder sb=new StringBuilder();
      for (int i=0; i<depth; i++) {
         sb.append(" ");
      }
      String pad=sb.toString();
      
      s=pad+s;
      s=s.replace("\n", "\n"+pad);
      return s;
   }
   
   /** */
   public static final String padString(int depth, String pc) {
      StringBuilder sb=new StringBuilder();
      for (int i=0; i<depth; i++) {
         sb.append(pc);
      }
      return sb.toString();
   }
   
   /** */
   public static final String getStackContext(int depth) {
      StackTraceElement[] stack=Thread.currentThread().getStackTrace();
      
      StringBuilder sb=new StringBuilder();
      if (stack != null) {
         for (int i=0; i<stack.length && i<depth; i++) {
            sb.append("   "+shortClassname(stack[i].getClassName())+"."+
                      stack[i].getMethodName()+"(): "+
                      stack[i].getLineNumber()+"\n");
         }
      }
      return sb.toString();
   }
   public static final String getCallingClassName() {
      return getCallingClassName(0);
   }
   
   /** */
   public static final String getCallingClassName(int depth) {
      StackTraceElement stack[]=Thread.currentThread().getStackTrace();
      String thisClassname=DebugUtils.class.getName();
      
      int i=0;
      for (i=0; i<stack.length; i++) {
         String classname=stack[i].getClassName();
         //System.out.println (i+":"+classname);
         if (classname != null && classname.equals(thisClassname))
            break;
      }
      int callingEle=i+1+depth;
      if (callingEle<stack.length) 
         return stack[callingEle].getClassName();
      
      return null;
   }
   
   /** */
   public static final String getCallingClassAndMethod(int depth) {
      // FIXME: THIS Does not really work as of 6/7/11.
      StackTraceElement stack[]=Thread.currentThread().getStackTrace();
      String thisClassname=DebugUtils.class.getName();
      
      int i=0;
      for (i=0; i<stack.length; i++) {
         String classname=stack[i].getClassName();
         //System.out.println (i+":"+classname);
         if (classname != null && !classname.equals(thisClassname) &&
             // ignore JVM classes.
             !classname.startsWith("sun.") && !classname.startsWith("java."))
            break;
      }
      int callingEle=i+1+depth;
      
      if (callingEle<stack.length) { 
         String cl=stack[callingEle].getClassName();
         String meth=stack[callingEle].getMethodName();
         int line=stack[callingEle].getLineNumber();
         String file=stack[callingEle].getFileName();
         return callingEle+":"+cl+"."+meth+"("+file+": "+line+")";
      }
      
      return null;
   }
   
   /** */
   public static final String getCallingClassName(String calledClassName) {
      StackTraceElement stack[]=Thread.currentThread().getStackTrace();
      
      int i=0;
      boolean found = false;
      for (i=0; i<stack.length; i++) {
         String className=stack[i].getClassName();
         if (debug) System.out.println ("==> "+i+":"+className+"."+stack[i].getMethodName());
         if (className != null && className.equals(calledClassName))
            found=true;
         else if (found)
            break;
      }
      if (i<stack.length) 
      {
         String className = stack[i].getClassName();
         if (debug) System.out.println ("==> "+i+":"+className+"."+stack[i].getMethodName());
         return className;
      }
      return null;
   }
   
   /** */
   public static final String formatAsHtml(String s) {
      s=s.replace("<", "&lt;");
      s=s.replace(">", "&gt;");

      s=s.replace("\n", "<br/>\n");
      
      return s;
   }
   
   /** */
   public static final String formatAsHtml(Throwable t) {
      ByteArrayOutputStream baos=new ByteArrayOutputStream();
      PrintWriter pw=new PrintWriter(new OutputStreamWriter(baos));
      t.printStackTrace(pw);
      pw.flush();
      String stack=baos.toString();
      stack=stack.replace("<", "&lt;");
      stack=stack.replace(">", "&gt;");
      return "<pre>\n"+stack+"\n</pre>\n";
   }
   
   public static final String jsonDebug(Object o) {
      return jsonDebug(o, true);
   }
   
   public static final String jsonDebug(Object o, boolean indent) {
      ObjectMapper om=new ObjectMapper();
      om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
      if (indent)
         om.enable(SerializationFeature.INDENT_OUTPUT);
      try {
         if (o == null)
            return "null";
         String result = om.writeValueAsString(o);
         result.replaceAll("([pP]assword[ =\":]+)[^\",]*([,\"])", "$1******$2");
         return result;
      } catch (Exception ex) {
         throw new RuntimeException("Error in debug serialization.", ex);
      }
   }
   
   public static final void sleep(long millis) {
      try {
         Thread.sleep(millis);
      } catch (Exception ex) {}
   }
}
