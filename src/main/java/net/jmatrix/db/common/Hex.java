package net.jmatrix.db.common;

public class Hex {
   private static final char[] HEX_CHARS = "0123456789abcdef".toCharArray();

   public static String asHex(byte[] buf) {
       char[] chars = new char[2 * buf.length];
       for (int i = 0; i < buf.length; ++i)
       {
           chars[2 * i] = HEX_CHARS[(buf[i] & 0xF0) >>> 4];
           chars[2 * i + 1] = HEX_CHARS[buf[i] & 0x0F];
       }
       return new String(chars);
   }
   
   public static String asHex(String s) {
      if (s == null) return null;
      return asHex(s.getBytes());
   }
}
