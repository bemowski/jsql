package net.jmatrix.db.schema;

import java.sql.SQLException;

public class DBMException extends SQLException {
   public DBMException(String s) {super(s);}
   public DBMException(String s, Throwable t) {super(s, t);}
}
