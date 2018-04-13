package net.jmatrix.db.jsql.export;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import net.jmatrix.db.common.ArgParser;
import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.DBUtils;
import net.jmatrix.db.common.StreamUtil;
import net.jmatrix.db.jsql.formatters.SQLFormatter;

public class ExportSQL implements Export {
   ConnectionInfo conInfo=null;

   static String usage=
         "ExportSQL -t <table> -o <file> <connection>";
   
   public ExportSQL(ConnectionInfo ci) {
      conInfo=ci;
   }
   
   public static void main(String args[]) throws Exception {
      ArgParser ap=new ArgParser(args);
      
      // -t <tablename>
      // -o <file>
      // last arg is connect string
      
      String table=ap.getStringArg("-t");
      String filepath=ap.getStringArg("-o");
      
      if (table == null || filepath == null) {
         System.out.println (usage);
         System.exit(1);;
      }
      
      ConnectionInfo ci=new ConnectionInfo(ap.getLastArg());
      ci.initDefaultConnection();
      
      ExportSQL export=new ExportSQL(ci);
      export.export(new File(filepath), table);
   }

   
   static String query(String table, String where) {
      String sql="select * from "+table;
      if (where != null && where.length() > 0) {
         if (where.toLowerCase().startsWith("where")) {
            sql=sql+" "+where;
         } else
            sql=sql+" where "+where;
      }
      return sql;
   }
   
   /** */
   public void export(File file, String table) throws SQLException, IOException {
      export(file, table, null);
   }
   
   /** */
   public void export(File file, String table, String where) throws SQLException, IOException {
      String sql=query(table, where);
      export(file, table, sql);
   }
   
   /** */
   public void exportSQL(File file, String table, String sql) throws SQLException, IOException {
      System.out.println ("SQL: "+sql);
      
      Connection con=conInfo.getDefaultConnection();
      Statement state=null;
      ResultSet rs=null;
      
      SQLFormatter sqlformatter=new SQLFormatter(conInfo);
      Writer writer=new FileWriter(file);
      try {
         //writer.write(header(ci, table, where));
         
         state=con.createStatement();
         rs=state.executeQuery(sql);
         
         int rows=sqlformatter.format(rs, writer, Integer.MAX_VALUE, sql, table);

         System.out.println ("Exported "+rows+ " rows");
      } finally {
         DBUtils.close(null, state, rs);
         StreamUtil.close(writer);
      }
   }
}
