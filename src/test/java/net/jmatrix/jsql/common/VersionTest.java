package net.jmatrix.jsql.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;
import net.jmatrix.db.common.Version;
import net.jmatrix.jsql.AbstractTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;


public class VersionTest extends AbstractTest {
   
   @Before
   public void before(){}
   
   @After
   public void after() {}
   
   @Test
   public void testSort1() {
      String vs[]=new String[] {"2", "1.0", "132", "2.0.1", "1", "1.0.0.0"};
      
      List<Version> versions=new ArrayList<Version>();
      
      for (String v:vs) {
         Version version=new Version(v);
         versions.add(version);
      }
      
      log("Before Sort: "+versions);
      Collections.sort(versions);
      log("After sort: "+versions);
      
      assertTrue("Sort order is wrong.", versions.get(0).toString().equals("1"));
      assertTrue("Sort order is wrong.", versions.get(versions.size()-1).toString().equals("132"));
   }
}
