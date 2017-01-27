package net.jmatrix.db.jsql.history;

import java.io.IOException;
import java.util.List;

import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.jsql.model.SQLHistoryItem;

public interface SQLHistory {
   public void load() throws IOException;
   public void save() throws IOException;
   public void clear() throws IOException;
   
   public void add(ConnectionInfo ci, String sql, int rows, boolean success);
   
   public List<SQLHistoryItem> search(String query);
   public List<SQLHistoryItem> recent(int count);
}
