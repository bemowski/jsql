package net.jmatrix.db.common;


/**
 * A version object and comparator for ordering.
 * 
 * Versions are dot delimited positive integers.
 * 
 * 
 * 3
 * 2.1
 * 2
 * 2.0.3
 * 2.2.2.2.2
 * 1.12345.2
 * 
 * Versions are constructed as strings, and if they contain invalid characters, 
 * then the Version(String) constructor will throw a RuntimeException.
 * 
 */
public class Version implements Comparable {
   private String sver=null;
   
   public Version(String s) {
      sver=s;
      validate();
   }
   
   void validate() {
      if (sver == null)
         throw new NullPointerException("Version cannot be constructed with null String value.");
      sver=sver.trim();
      
      String s[]=sver.split("\\.");
      for (int i=0; i<s.length; i++) {
         try {
            Integer.parseInt(s[i]);
         } catch (Exception ex) {
            throw new RuntimeException("Version '"+sver+"' component "+i+" is invalid due to "+ex.toString());
         }
      }
   }
   
   public String toString() {
      return sver;
   }
   
   /**
    * {(x, y) such that x.compareTo(y) <= 0}
    * 
    * if we compare x.compareTo(y) this method should return: 
    *    -1 if x < y
    *    -0 if x = y
    *     1 if x > y
    */
   @Override
   public int compareTo(Object o) {
      int rval=0;
      if (o instanceof Version) {
         Version ver=(Version)o;
         
         String s1[]=ver.sver.split("\\.");
         String s2[]=sver.split("\\.");
         
         // pad the shorter of the 2 versions.  
         // for instance, when comparing: 
         // 2.0 to 2.0.1
         //
         // convert 2.0 to 2.0.-1, changing comparison to:
         // 2.0.-1 to 2.0.1
         
         if (s1.length > s2.length) {
            String s3[]=new String[s1.length];
            for (int i=0; i<s2.length; i++) {
               s3[i]=s2[i];
            }
            for (int i=s2.length; i<s3.length; i++) {
               s3[i]="-1";
            }
            s2=s3;
         } else if(s2.length > s1.length) {
            String s3[]=new String[s2.length];
            for (int i=0; i<s1.length; i++) {
               s3[i]=s1[i];
            }
            for (int i=s1.length; i<s3.length; i++) {
               s3[i]="-1";
            }
            s1=s3;
         } else {
            // they are the same length;
         }
         
         //System.out.println ("S1("+ver.sver+"): "+Arrays.asList(s1));
         //System.out.println ("S2("+sver+"): "+Arrays.asList(s2));
         
         for (int i=0; i<s1.length; i++) {
            int i1=Integer.parseInt(s1[i]);
            int i2=Integer.parseInt(s2[i]);
            
            if (i1<i2) {
               rval=1; break;
            } else if (i1>i2) {
               rval=-1; break;
            }
         }
      }
      
      //System.out.println ("S1 compareTo S2, ret: "+rval);
      return rval;
   }
   
   @Override
   public int hashCode() {
      return sver.hashCode();
   }
   
   @Override
   public boolean equals(Object o) {
      if (o instanceof Version) {
         Version v=(Version)o;
         return v.sver.equals(sver);
      }
      return false;
   }
}
