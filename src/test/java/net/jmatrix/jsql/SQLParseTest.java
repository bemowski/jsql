package net.jmatrix.jsql;

import net.jmatrix.db.common.ClassLogFactory;
import net.jmatrix.db.common.SQLUtil;
import net.jmatrix.db.common.StreamUtil;
import net.jmatrix.db.common.console.SysConsole;
import net.jmatrix.db.common.console.TextConsole;
import net.jmatrix.db.jsql.SQLRunner;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;

import java.util.List;

public class SQLParseTest {
  static final Logger log=ClassLogFactory.getLog();

  static {
    SysConsole.getConsole().setLevel(TextConsole.Level.DEBUG);
  }

  @Test
  public void parseSQLWithQuotedSemicolon() {
    String sql=
            "select * from foo;\n"+
                    "select * from bar;\n"+
                    "insert into foobar values('this; that; the other');\n"+
                    "select count(*) from foobar\n";

    log.info("Test SQL: \n\n"+sql);

    List<String> statements=SQLUtil.splitSQL(sql);

    for(int i=0; i<statements.size(); i++) {
      log.info(i+":"+statements.get(i));
    }
    int expected=4;
    Assert.assertTrue("statements.size() == "+statements.size()+" but should be "+expected,
            statements.size()==expected);  }

  @Test
  public void parseSQLMultiline() {
    String sql=
            "select * \n from foo;\n"+
                    "select * from bar;\n"+
                    "insert into foobar values('this; \nthat; the other');\n"+
                    "select count(*) from foobar\n";

    log.info("Test SQL: \n\n"+sql);

    List<String> statements=SQLUtil.splitSQL(sql);

    for(int i=0; i<statements.size(); i++) {
      log.info(i+":"+statements.get(i));
    }
    int expected=4;
    Assert.assertTrue("statements.size() == "+statements.size()+" but should be "+expected,
            statements.size()==expected);
  }

  @Test
  public void parseTest1SQL() throws Exception {
    String sql=StreamUtil.readToString(this.getClass().getResourceAsStream("/test-1.sql"));

    log.info("Test SQL: \n\n"+sql);

    int counttick=0;
    int countsemicolon=0;
    for (char c:sql.toCharArray()) {
      if (c == '\'')
        counttick++;
      if (c == ';')
        countsemicolon++;
    }
    log.debug("single quotes: "+counttick);
    log.debug("semicolons: "+countsemicolon);

    List<String> statements=SQLUtil.splitSQL(sql);

    for(int i=0; i<statements.size(); i++) {
      log.info(i+":"+statements.get(i));
    }
    int expected=2;
    Assert.assertTrue("statements.size() == "+statements.size()+" but should be "+expected,
            statements.size()==expected);
  }
} 
