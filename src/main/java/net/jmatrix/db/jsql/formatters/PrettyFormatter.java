package net.jmatrix.db.jsql.formatters;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import net.jmatrix.db.common.ClassLogFactory;
import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.StringUtils;
import net.jmatrix.db.common.console.TextConsole;

import org.slf4j.Logger;



public class PrettyFormatter extends AbstractFormatter {
   private static Logger log=ClassLogFactory.getLog();

   static final boolean debug=false;
   
   TextConsole console=null;
   int consoleWidth=80;

//   public PrettyFormatter(ConnectionInfo ci) {
//      super(ci);
//   }
   
   public PrettyFormatter() {}
   
   public PrettyFormatter(TextConsole c) {
      console=c;
   }
   
   public PrettyFormatter(ConnectionInfo ci, TextConsole c) {
      super(ci);
      console=c;
   }
   
   @Override
   public int format(ResultSet rs, Writer writer, int maxrows, String sql,
         String tablename, String[] columns) throws SQLException, IOException {
      
      
      //int consoleWidth=80;
      
      if (console != null) {
         consoleWidth=console.getColumns();
      } else {
         Integer cw=(Integer)parameters.get(CONSOLE_WIDTH);
         if (cw != null)
            consoleWidth=cw;
         else 
            log.warn("ConsoleWidth parameter is null.");
            
         if (consoleWidth == -1)
            consoleWidth=80;
      }
      
      Data data=getData(rs, columns, maxrows, consoleWidth);
      
      return format(data, writer, consoleWidth);
   }

   @Override
   public String header(String sql, ResultSet rs, String[] columns) throws SQLException {
      return null;
   }
   
   private int format(Data data, Writer writer, int width) throws IOException {
      log.trace("Formatting "+data.rowcount()+" x "+data.cols+", width="+width);
      
      // now calculate column widths based on avg column width
      int w[]=new int[data.cols];
      int rowwidth=0;
      
      int availableWidth=width-data.cols-1;
      
      for (int i=0; i<data.cols; i++) {
         Column c=data.columns.get(i);
         
         if (c != null)
            log.trace(c.toString());
         
         w[i]=(int)((double)availableWidth*(c.avewidth/data.avgTotal));
         
         if (w[i] > c.maxwidth) 
            w[i]=c.maxwidth;
         if (w[i] < 2) {
            w[i]=2;
         }
         
         rowwidth=rowwidth+w[i]+1;
      }
      
      rowwidth=rowwidth+1;
      
      if (debug) {
         log.debug("Avail = "+availableWidth+", rowwidth="+rowwidth);

         
         log.debug("   ");
         for (int i=0; i<w.length; i++) {
            System.out.print(w[i]+" ");
         }
         log.debug("");
      }
      
      writer.append(StringUtils.pad(rowwidth, '-')+"\n");

      StringBuilder line=new StringBuilder();
      line.append("|");
      for (int i=0; i<data.cols; i++) {
         
         Column c=data.columns.get(i);
         line.append(exact(c.name, w[i])+"|");

      }
      writer.append(line+"\n");
      
      writer.append(StringUtils.pad(rowwidth, '-')+"\n");
      
      int rows=data.rowcount();
      for (int i=0; i<rows; i++) {
         line=new StringBuilder();
         line.append("|");
         List<String> row=data.rows.get(i);
         
         for (int j=0; j<data.cols; j++) {
            
            line.append(exact(row.get(j), w[j])+"|");
         }
         
         
         writer.append(line.toString()+"\n");
      }
      writer.append(StringUtils.pad(rowwidth, '-')+"\n");

      return data.rowcount();
   }
   
   
   public String format(List items, String[] fields, int width) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, IOException {
      StringWriter sw=new StringWriter();
      
      Data data=getData(items, fields);
      
      format(data, sw, width);
      
      return sw.toString();
   }
   
   Data getData(List items, String[] fields) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
      
      Method method[]=new Method[fields.length];
      
      Class type=items.get(0).getClass();
      
      for (int i=0; i<fields.length; i++) {
         String field=fields[i];
         String getterName="get"+
               field.substring(0,1).toUpperCase()+
               field.substring(1);
         method[i]=type.getMethod(getterName, new Class[]{});
      }
      
      Data data=new Data(fields);
      
      for (Object item:items) {
         List<String> row=new ArrayList<String>();
         for (int i=0; i<fields.length; i++) {
            Object value=method[i].invoke(item, new Object[]{});
            String svalue=null;
            if (value == null)
               svalue="null";
            else
               svalue=value.toString();
            row.add(svalue);
         }
         data.add(row);
      }
      lastRowCount=data.rowcount();
      return data;
   }
   
   
   /** Get formatting Data object from a ResultSet */
   Data getData(ResultSet rs, String[] columns, int maxrows, int maxstring) throws SQLException {
      Data data=new Data(rs, columns);
      
      int count=0;
      while (rs.next() && count < maxrows) {
         List<String> row=new ArrayList<String>();
         
         List<Column> dc=data.columns;
         
         for (int i=1; i<=data.cols; i++) {
            //String s=rs.getString(i);
            
            // This does not work for some SQL server stored procs - returning 
            // a single value in a single column WITH NO NAME.  the getString(i)
            // above was previously commented in favor of this. I think possibly 
            // so we could re-order columns in the result set.
            
            String s=null;
            
            if (dc.get(i-1).name != null && !dc.get(i-1).name.equals("")) {
               s=rs.getString(dc.get(i-1).name);
            } else {
               s=rs.getString(i);
            }
            
            
            if (s == null)
               s="NULL";
            
            s=s.replaceAll("\n", "\\n");
            s=s.replaceAll("\t", "\\t");
            if (s.length() > maxstring) {
               s=s.substring(0, maxstring);
            }
            
            row.add(s);
         }
         data.add(row); count++;
      }
      data.finish();
      return data;
   }
   
   class Column {
      String name=null;
      int maxwidth=0;
      double avewidth=0;
      int count=0;
      
      public String toString() {
         return "Col('"+name+"', max="+maxwidth+", avg="+avewidth+", rowcount="+count+")";
      }
      
      void add(int x) {
         if (x > maxwidth)
            maxwidth=x;
         
         avewidth=((avewidth*count)+x)/(count+1);
         count++;
      }
   }
   
   class Data {
      int cols=0;
      List<Column> columns=new ArrayList<Column>();
      List<List<String>> rows=new ArrayList<List<String>>();
      
      double avgTotal=0;
      
      public Data(ResultSet rs, String[] columns) throws SQLException {
         ResultSetMetaData rsmd=rs.getMetaData();
         
         if (columns != null) {
            cols=columns.length;
            
            for (String column:columns) {
               addColumn(column);
            }
         } else {
            cols=rsmd.getColumnCount();
            
            for (int i=1; i<=cols; i++) {
               addColumn(rsmd.getColumnName(i));
            }
            
            log.debug("Columns: "+this.columns);
         }
      }
      
      public Data(String[] fields) {
         cols=fields.length;
         for (int i=0; i<fields.length; i++) {
            addColumn(fields[i]);
         }
      }
      
      void addColumn(String name) {
         Column c=new Column();
         c.name=name;
         columns.add(c);
         
         c.add(c.name.length());
      }
      
      void add(List<String> row) {
         for (int i=0; i<cols; i++) {
            Column c=columns.get(i);
            c.add(row.get(i).length());
         }
         rows.add(row);
      }
      
      int rowcount() {
         return rows.size();
      }
      
      void finish() {
         for (int i=0; i<cols; i++) {
            avgTotal=avgTotal+columns.get(i).avewidth;
         }
      }
   }
   
   // returns a string of the exact wtih requested, either padding or trimming.
   private static final String exact(String s, int w) {
      // assume s is non-null.
      
      if (s.length() == w) 
         return s;
      if (s.length() < w) {
         return s+StringUtils.pad(w-s.length(), ' ');
      } else { // s.length() > w
         return s.substring(0, w-1)+">";
      }
   }
}
