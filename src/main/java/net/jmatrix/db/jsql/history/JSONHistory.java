package net.jmatrix.db.jsql.history;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.DotConfig;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.JSQL;
import net.jmatrix.db.jsql.model.SQLHistoryItem;

public class JSONHistory implements SQLHistory {
   static final TextConsole console=SysConsole.getConsole();

   static final String JSON_HISTORY="history.json";
   
   List<SQLHistoryItem> history=null;
   DotConfig dotConfig=new DotConfig(JSQL.JSQL);
   ObjectMapper om=null;
   
   
   public JSONHistory() {
      om=new ObjectMapper();
      om.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
      om.enable(SerializationFeature.INDENT_OUTPUT);
   }
   
   @Override
   public void add(ConnectionInfo ci, String sql, int rows, boolean success) {
      SQLHistoryItem h=new SQLHistoryItem();
      h.setConInfo(ci.toString());
      h.setSql(sql);
      h.setRows(rows);
      h.setSuccess(success);
      history.add(h);
      
      try {
         save();
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }

   @Override
   public List<SQLHistoryItem> search(String query) {
      query=query.toLowerCase();
      
      List<SQLHistoryItem> results=new ArrayList<SQLHistoryItem>();
      for (SQLHistoryItem row:history) {
         if (row.matches(query) && row.getSuccess()) {
            results.add(row);
         }
      }
      //Collections.sort(results);
      return results;
   }
   
   @Override
   public void clear() throws IOException {
      DateFormat df=new SimpleDateFormat("dd.MM.yyyy-HH.mm.ss");
      String backupfile=JSON_HISTORY+"-cleared-"+df.format(new Date());
      console.info("Backing up history to "+backupfile);
      save(backupfile);
      history.clear();
      save();
   }

   @Override
   public void load() throws IOException {
      InputStream is=dotConfig.getInputStream(JSON_HISTORY);
      if (is == null)
         history=new ArrayList<SQLHistoryItem>();
      else
         history=om.readValue(is, new TypeReference<List<SQLHistoryItem>>(){});
      
      Collections.sort(history);
   }

   @Override
   public void save() throws IOException {
      om.writeValue(dotConfig.getFile(JSON_HISTORY), history);
   }
   
   private void save(String filename) throws IOException {
      om.writeValue(dotConfig.getFile(filename), history);
   }

   @Override
   public List<SQLHistoryItem> recent(int count) {
      List<SQLHistoryItem> results=new ArrayList<SQLHistoryItem>();
      int size=history.size();
      
      int found=0;
      for (int i=size-1; i>=0; i--) {
         SQLHistoryItem item=history.get(i);
         if (item.getSuccess()) {
            results.add(history.get(i));
            found++;
            if (found == count) 
               break;
         }
      }
      Collections.sort(results);
      return results;
   }
}
