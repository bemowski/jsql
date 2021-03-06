package net.jmatrix.db.common;

import java.lang.reflect.Field;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;

/**
 * Small utilities related to SQL itself.
 */
public class SQLUtil {
   //static TextConsole console=SysConsole.getConsole();
   private static Logger log=ClassLogFactory.getLog();

   static Map<Integer,String> typeNameMap=new HashMap<Integer, String>();

   static {
      Class c=Types.class;
      Field fields[]=c.getFields();
      for (Field field:fields) {
         try {
            Object val=field.get(null);
            if (val instanceof Number)
               typeNameMap.put(((Number)val).intValue(), field.getName());
         } catch (Exception ex) {}
      }
   }
   
   
   /** */
   public static String stripSQLComments(String sql) {
      log.trace("SQL before comment removal, length is "+sql.length());
      
      String multiLineRegex="/\\*(.)*?\\*/";
      // This regex used to be: "/\\*(.|[\\r\\n])*?\\*/";
      // I changed it due to a stack overflow caused by a bug in the java 
      // regular expressions.  The bug reports here:
      // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=5050507
      // and here
      // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6337993
      // They suggest not using the | construct.  The . normally matches
      // any character except line terminators. But by adding the Pattern.DOTALL
      // flag, the dot match includes line terminators, and we can remove the |
      // Bemo, 25 Nov 2009.  
      Pattern multiLinePattern=Pattern.compile(multiLineRegex, Pattern.DOTALL);
      Matcher matcher = multiLinePattern.matcher(sql);
      
      sql=matcher.replaceAll("");
      
      log.trace("SQL after multiline comment removal, length is "+sql.length());

      String singleLineRegex="^(\\s)*\\-\\-.*?$";
      Pattern singleLinePattern=Pattern.compile(singleLineRegex, Pattern.MULTILINE);
      matcher=singleLinePattern.matcher(sql);
      
      sql=matcher.replaceAll("");
      log.trace("SQL after single line comment removal, length is "+sql.length());
      
      return sql;
   }

   public static List<String> splitSQL(String sql) {
    return splitSQLParser(sql);
   }

   public static List<String> splitSQLRegex(String sql) {
     String regex="\\;(?=(?:[^\\\']*\\\'[^\\\']*\\\')*[^\\\']*$)";
     log.debug("Split regex: "+regex);

     //List<String> statements= Arrays.asList(sql.split(regex));
     List<String> statements=new ArrayList<>();
     for (String s:sql.split(regex)) {
       s=s.trim();
       if (s.length() > 0)
        statements.add(s.trim());
     }
     return statements;
   }

   /** */
   public static List<String> splitSQLOLD(String sql, String delimiter) {
      List<String> statements=new ArrayList<String>();
      
      StringTokenizer st=new StringTokenizer(sql, delimiter, false);
      while (st.hasMoreTokens()) {
         String statement=st.nextToken().trim();
         
         // for '/' delimited triggers/procedures, it is common to have
         // a delimiter at the end.  This results in an empty sql statement
         // at the end.  So, here we require that the trimmed sql 
         // statement has 2 or more characters.
         
         if (statement.length() > 1) 
            statements.add(statement); 
      }
      return statements;
   }

   static List<String> splitSQLParser(String sql) {
     List<String> tokensList = new ArrayList<String>();
     boolean inQuotes = false;
     StringBuilder b = new StringBuilder();
     for (char c : sql.toCharArray()) {
       switch (c) {
         case ';':
           if (inQuotes) {
             b.append(c);
           } else {
             String s=b.toString().trim();
             if (s.length() > 0)
              tokensList.add(s);
             b = new StringBuilder();
           }
           break;
         case '\'':
           inQuotes = !inQuotes;
         default:
           b.append(c);
           break;
       }
     }
     String s=b.toString().trim();
     if (s.length() > 0)
       tokensList.add(s);
     return tokensList;
   }
 
   public static String jdbcTypeString (int i) {
      String name=typeNameMap.get(i);
      if (name == null)
         return "UNKNOWN";
      return name;
   }

   public static String escape(String string) {
      
      return string.replace("'", "''");
   }
}
