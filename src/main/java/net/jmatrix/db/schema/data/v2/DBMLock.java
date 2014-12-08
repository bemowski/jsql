package net.jmatrix.db.schema.data.v2;

public class DBMLock {
   String id;
   String host;
   String user;
   long timestamp;
   String notes=null;
   
   public DBMLock(){}
   
   public long age() {
      return System.currentTimeMillis()-timestamp;
   }
   
   public String toString() {
      return "DBMLock(id='"+id+"', held by "+user+" at "+host+", lock age="+age()+"ms"+
            (notes == null?")":" "+notes+")");
   }
   
   public String getId() {
      return id;
   }
   public void setId(String id) {
      this.id = id;
   }
   public String getHost() {
      return host;
   }
   public void setHost(String host) {
      this.host = host;
   }
   public String getUser() {
      return user;
   }
   public void setUser(String user) {
      this.user = user;
   }
   public long getTimestamp() {
      return timestamp;
   }
   public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
   }
   public String getNotes() {
      return notes;
   }
   public void setNotes(String notes) {
      this.notes = notes;
   }
}
