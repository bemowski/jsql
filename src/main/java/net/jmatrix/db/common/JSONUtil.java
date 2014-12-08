package net.jmatrix.db.common;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class JSONUtil {
   
   
   public static <U> U read(String s, Class<U> u) throws IOException {
      ObjectMapper om=new ObjectMapper();
      om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
      om.enable(SerializationFeature.INDENT_OUTPUT);
      return om.readValue(s, u);
   }
   
   public static String write(Object o)  throws IOException {
      ObjectMapper om=new ObjectMapper();
      om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
      om.enable(SerializationFeature.INDENT_OUTPUT);
      return om.writeValueAsString(o);
   }
}
