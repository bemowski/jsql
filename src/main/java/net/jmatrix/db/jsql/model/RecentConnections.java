package net.jmatrix.db.jsql.model;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.jmatrix.db.common.ConnectionInfo;
import net.jmatrix.db.common.DotConfig;
import net.jmatrix.db.common.JSONUtil;

import com.fasterxml.jackson.annotation.JsonIgnore;



/**
 * A list of recent connections. 
 */
public class RecentConnections {
   static final String RECENT_CONNECTIONS="recent.connections.json";
   
   int number=20;
   
   List<ConnectionInfo> connections=new ArrayList<ConnectionInfo>();
   
   public RecentConnections() { }
   
   
   public void update(ConnectionInfo ci) {
      int index=connections.indexOf(ci);
      if (index != -1) {
         connections.set(index, ci);
      } else {
         connections.add(ci);
      }
      Collections.sort(connections);
      
      if (connections.size() > number) {
         connections.remove(connections.size()-1);
      }
   }
   
   public ConnectionInfo get(int i) {
      return connections.get(i);
   }
   
   @JsonIgnore
   public ConnectionInfo getMostRecent() {
      if (connections != null && connections.size() > 0) {
         return connections.get(0);
      }
      return null;
   }
   
   
   public static RecentConnections load(String app) throws IOException {
      DotConfig dc=new DotConfig(app);
      
      String s=dc.read(RECENT_CONNECTIONS);
      if (s == null || s.length() == 0) {
         RecentConnections rc=new RecentConnections();
         rc.save(app);
         return rc;
      } else {
         return JSONUtil.read(s, RecentConnections.class);
      }
   }
   
   public void save(String app) throws IOException {
      DotConfig dc=new DotConfig(app);
      
      dc.write(RECENT_CONNECTIONS, JSONUtil.write(this));
   }
   
   ////////////////////////////////////////////////////////////////////////////
   public int getNumber() {
      return number;
   }

   public void setNumber(int number) {
      this.number = number;
   }

   public List<ConnectionInfo> getConnections() {
      return connections;
   }

   public void setConnections(List<ConnectionInfo> connections) {
      this.connections = connections;
   }
}

