package net.jmatrix.jsql;

import net.jmatrix.db.common.ClassLogFactory;
import net.jmatrix.db.common.SQLUtil;
import net.jmatrix.db.jsql.SQLRunner;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.List;

public class SQLParseTest {
  static final Logger log=ClassLogFactory.getLog();

  @Test
  public void parseSQLWithQuotedSemicolon() {
    String sql=
            "select * from foo;\n"+
                    "select * from bar;\n"+
                    "insert into foobar values('this; that; the other');\n"+
                    "select count(*) from foobar\n";

    log.info("Test SQL: \n\n"+sql);

    List<String> statements=SQLUtil.splitSQL(sql, ";");

    for(int i=0; i<statements.size(); i++) {
      log.info(i+":"+statements.get(i));
    }
    Assert.assertTrue("sql splitter not working", statements.size()==4);
  }
} 
