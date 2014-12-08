package net.jmatrix.db.common;

import java.util.*;
import java.lang.reflect.Method;

//import org.apache.commons.logging.Log;

/**
 * PerfTrack tracks performance through multiple operations within
 * a Thread.  Functionally it works with static method calls, that can 
 * be called from anywhere within the code.  Primary methods are start/stop - 
 * which adds a tracking of the performance (or wall clock time) it takes 
 * to execute the operation in question.  
 * 
 * The PerfTrack builds a tree-like structure showing the amounts of time taken
 * for each sub-operation.  
 * 
 * This will be useful in tracking performance issues in the web application, 
 * and the tree based performance output allows us to focus in rapidly on 
 * problem areas of the code and its interactions with external system.
 * 
 * This code was Modelled on the Log4J MDC (Mapped Diagnostic Context).
 * 
 * @author Paul Bemowski
 */
public final class PerfTrack {
   //static Log log=ClassLogFactory.getLog();
   
   // FIXME: 
   //   - Change to InheritableThreadLocal??
   //   - Ensure thread safety - I think its threadsafe now.
   static ThreadLocal<Item> threadLocalCurrent=new ThreadLocal<Item>();
   
   /** */
   public static void start(Method m) {
      String s=getMethodString(m);
      start(s);
   }
   
   /** */
   public static void start(String x) {
      if (x == null)
         throw new NullPointerException("Null PerfTrack name.  Cannot perftrack null.");
      
      Item current=threadLocalCurrent.get();
      
      // fixme: remove - verbose PerfTrack Debug.
      //log.debug("PerfTrack.start("+x+"), current: "+current);
      
      if (current == null) {
         current=new Item(x, null);
         current.start();
         threadLocalCurrent.set(current);
      } else {
         Item child=new Item(x, current);  // new sub-item
         child.start();
         threadLocalCurrent.set(child);
      }
   }
   
   /** */
   public static void stop(Method m) {
      String s=getMethodString(m);
      stop(s);
   }
   
   /** Returns a string representing a method. */
   private static final String getMethodString(Method m) {
      String classname=m.getDeclaringClass().getName();
      classname=classname.substring(classname.lastIndexOf(".")+1);
      return classname+"."+m.getName();
   }
   
   /** */
   public static void stop(String x) {
      if (x == null)
         throw new NullPointerException("Null PerfTrack name.  Cannot perftrack null.");
      
      Item current=threadLocalCurrent.get();
      
      // fixme: remove - verbose PerfTrack Debug.
      //log.debug("PerfTrack.stop("+x+"), current: "+current);
      
      if (current==null) {
         System.out.println("Stopping, but current is null??");
      } else {
         if (current.getName() != null && current.getName().equals(x)) {
            current.stop();
            
            Item parent=current.getParent();
            if (parent != null) {
               threadLocalCurrent.set(parent);
            } else {
               // leave current as root...
               //threadLocalCurrent.set(null);
            }
         } else {
            // stopping item that is not current.  likely because 
            // someone forgot to stop a PerfTrack with try/finally
            // or other programming error/typo.
            System.out.println("Stopping '"+x+"' but current is '"+current.getName()+"'");
            
         }
      }
   }
   
   /** */
   public static String reportAndClear() {
      String s=toString(0);
      clear();
      return s;
   }
   
   public static final boolean hasData() {
      return threadLocalCurrent.get() != null;
   }
   
   /** */
   public static void clear() {
      //log.debug("PerfTrack.clear()");
      threadLocalCurrent.remove();
   }
   
   /** */
   public static boolean isCurrentRootAndComplete() {
      Item current=threadLocalCurrent.get();
      if (current != null ) {
         if (current.getParent() == null) {
            if (current.isDone()) {
               return true;
            }
         }
      } 
//      else {
//         return true;
//      }
      return false;
   }

   public static String toString(int depth) {
      Item current=threadLocalCurrent.get();
      if (current != null ) {
         if (current.getParent() == null) {
            // this is the root.
            return current.toString(depth);
         } else {
            // this is more common now.  adding perftrack to syslog.  
            // we just append UF (unfinished) to current time.
            //log.warn("toString() called, but current is not root.  Unfinished tree items.");
            Item root=current.findRoot();
            return root.toString(depth);
         }
      }
      else 
         return "PerfTrack: no data?";
   }
   
   /**  */
   static final class Item {
      String name;
      int callCount=0;
      Item parent=null;
      long start=0;
      long stop=0;
      long et=0;
      List<Item> children=new ArrayList<Item>();
      
      /** */
      public Item(String n, Item p) {
         name=n;
         parent=p;
         callCount++;
         if (parent != null) 
            parent.addChild(this);
      }
      
      public void start() {start=System.currentTimeMillis();}
      public void stop() {stop=System.currentTimeMillis(); et=stop-start;}
      public List<Item> getChildren() {return children;}
      public String getName() {return name;}
      public Item getParent() {return parent;}
      
      public String toString() {
         return "PTItem("+name+", start="+start+", stop="+stop+", parent="+parent+")";
      }
      
      public boolean hasChildren() {
         if (children.size() > 0)
            return true;
         return false;
      }
      
      public final boolean isDone() {
         if (stop == 0)
            return false;
         return true;
      }
      
      public final long getEt() {
         if (stop == 0) {
            return System.currentTimeMillis()-start;
         }
         return et;
      }
      
      public void addChild(Item i) {
         if (children.size() > 0) {
            Item lastChild=children.get(children.size()-1);
            if (i.getName().equals(lastChild.getName())) {
               // just add count and time.
               lastChild.callCount++;
               lastChild.et=i.et;
            } else {
               children.add(i);
            }
         } else {
            children.add(i);
         }
      }
      
      public Item findRoot() {
         if (parent == null)
            return this;
         else
            return parent.findRoot();
      }
      
      public String toString(int depth) {
         StringBuilder sb=new StringBuilder();
         for (int i=0; i<depth; i++) {
            sb.append("  ");
         }
         
         long localstop=stop;
         String timesuffix=null;
         if (localstop == 0) {
            localstop=System.currentTimeMillis();
            timesuffix="UF";
         }
         
         // if the local item is not finished (stop time = 0) then create
         // a 'local' (local to this method) stop time, and suffix the time
         // with UF - unfinished.
         sb.append(name+" "+(callCount > 1?"("+callCount+") ":"")+ (localstop-start)+
               (timesuffix == null?"":"(UF)")+"ms\n");
         
         if (children.size() > 0) {
            long unaccounted=et;
            
            for (Item child:children) {
               unaccounted=unaccounted-child.getEt();
               
               sb.append(child.toString(depth+1));
            }
            
            // unaccounted
            for (int i=0; i<depth+1; i++) {
               sb.append("  ");
            }
            sb.append("Other "+unaccounted+"ms\n");
         }
         return sb.toString();
      }
   }
}
