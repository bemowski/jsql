package net.jmatrix.db.common;

import java.io.*;

/**
 *
 */
public final class StreamUtil 
{
   static final int EOS=-1;

   /** 
    * Pumps one stream to another, with a buffer - both streams are
    * closed when the pump is complete.
    */
   public static final int pump(InputStream is, OutputStream os)
      throws IOException {
      String perf="pump("+is+" -> "+os+")";
      try {
         PerfTrack.start(perf);
         byte buffer[]=new byte[8192];
         int bytes=is.read(buffer);
         int totalBytes=0;
         while (bytes > 0) {
            totalBytes+=bytes;
            os.write(buffer, 0, bytes);
            // System.out.println ("last read: "+bytes+" reading...");
            // SOLVED! -- See jet_net ChunkedInputStream, and 
            // Transfer-Encoding: chunked.  !!
            // pab, 24/7/2003
            bytes=is.read(buffer);
         }
         os.flush(); os.close();
         is.close();
         return totalBytes;
      } finally {
         PerfTrack.stop(perf);
      }

   }
   
   /** 
    * Pumps one stream to another, with a buffer - both streams are
    * closed when the pump is complete.
    */
   public static final int pump(Reader is, Writer os)
      throws IOException {
      String perf="pump("+is+" -> "+os+")";
      
      try {
         PerfTrack.start(perf);
         char buffer[]=new char[8192];
         int bytes=is.read(buffer);
         int totalBytes=0;
         while (bytes > 0) {
            totalBytes+=bytes;
            os.write(buffer, 0, bytes);
            // System.out.println ("last read: "+bytes+" reading...");
            // SOLVED! -- See jet_net ChunkedInputStream, and 
            // Transfer-Encoding: chunked.  !!
            // pab, 24/7/2003
            bytes=is.read(buffer);
         }
         os.flush(); os.close();
         is.close();
         return totalBytes;
      } finally {
         PerfTrack.stop(perf);
      }
   }
   
   public static final void unbufferedPump(InputStream is, OutputStream os) 
      throws IOException {
      int b=is.read();
      while (b != EOS) {
         os.write(b);
         b=is.read();
      }
      os.flush(); os.close();
      is.close();
   }

  /** */
  public static final void pumpExactly(InputStream is, OutputStream os, 
                                       int bytes) 
    throws IOException {
    for (int i=0; i<bytes; i++) {
      os.write(is.read());
    }
    os.flush();
    os.close();
    is.close();
  }

   /** 
    * Reads all remaining bytes from a stream and returns it 
    * as a string. 
    */
   public static String readToString(InputStream is) 
      throws IOException {
      String perf="readToString()";
      try {
         PerfTrack.start(perf);
         ByteArrayOutputStream baos=new ByteArrayOutputStream();
         pump(is, baos);
         
         return baos.toString();
      } finally {
         PerfTrack.stop(perf);
      }
   }
   
   /** 
    * Reads all remaining bytes from a stream and returns it 
    * as a string. 
    */
   public static String readToString(InputStream is, String encoding) 
      throws IOException {
      String perf="readToString()";
      try {
         PerfTrack.start(perf);
         ByteArrayOutputStream baos=new ByteArrayOutputStream();
         pump(is, baos);
         
         return baos.toString(encoding);
      } finally {
         PerfTrack.stop(perf);
      }
   }
   
   public static String readToString(File f) 
   throws IOException {
      return new String(readFully(new FileInputStream(f)));
   }

   public static byte[] readFully(InputStream is) 
      throws IOException {
      ByteArrayOutputStream baos=new ByteArrayOutputStream();
      int b=is.read();
      while (b != EOS) {
         baos.write(b);
         b=is.read();
      }
      is.close();
      return baos.toByteArray();
   }
   
   public static byte[] readFully(File f) 
   throws IOException {
      return readFully(new FileInputStream(f));
   }
   
   
   
   public static final void write(String s, Writer w) throws IOException {
      w.write(s);
      w.flush();
   }
   
   public static final void write(String s, File f) throws IOException {
      FileWriter fw=new FileWriter(f);
      write(s, fw);
      fw.close();
   }
   
   public static final void write(byte b[], File f) throws IOException {
      FileOutputStream fos=new FileOutputStream(f);
      fos.write(b);
      fos.flush();
      fos.close();
   }
   
   public static final void write(InputStream is, File f) throws IOException {
      pump(is, new FileOutputStream(f));
   }
   
   public static final void close(InputStream is) {
      if (is != null) {
         try {
            is.close();
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      }
   }
   
   public static final void close(Reader is) {
      if (is != null) {
         try {
            is.close();
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      }
   }
   public static final void close(Writer w) {
      if (w != null) {
         try {
            w.close();
         } catch (Exception ex) {
            ex.printStackTrace();
         }
      }
   }
}
