package net.jmatrix.db.schema;

import java.io.File;

import net.jmatrix.db.common.ArgParser;
import net.jmatrix.db.common.ConnectionInfo;

public class DBMCLI {
   
   static final String usage=
         "DBM -schema <path.to.schema.dir> [options] <database.connect.string>\n"+
         "\n"+
         "  Options: \n"+
         "    -update: update the schema. indempotent.  "+
         "             Will not apply same version if already applied. \n"+
         "    -rollback <version>: Rollback the version indicated. \n"+
         "    -reapply <version>: rollback, then apply the same version.\n"+
         "\n"+
         "  database.connnect.string: format: \n"+
         "      user/password@jdbc.url";
   
   /**
    * Commands: 
    *   updateall - updates schema to latest version on disk.
    *   rollback version - execute rollback files to previous version
    *   rollback all - execute rollbacks from current version to the beginning
    *   reapply version - execute rollback, then apply of version
    */
   public static void main(String args[]) throws Exception {
      ArgParser ap=new ArgParser(args);
      
      if (args.length < 1) {
         System.out.println(usage);
         System.exit(1);
      }
      
      String last=ap.getLastArg();
      
      ConnectionInfo ci=new ConnectionInfo(last);
      ci.connect();
      
      String schemaPath=ap.getStringArg("-schema");
      if (schemaPath == null) {
         System.out.println(usage);
         System.exit(1);
      }
      
      DBM dbm=new DBM(ci, new File(schemaPath));
      dbm.init();
      
      //console.log("Versions\n"+dbm.listVersions());
      
      if (ap.getBooleanArg("-reapply")) {
        
         String version=ap.getStringArg("-reapply");
         
         dbm.reapply(version);
      } else if (ap.getBooleanArg("-update")) {

         
         dbm.updateAll();
      } else if (ap.getBooleanArg("-rollback")) {

         String version=ap.getStringArg("-rollback");
         dbm.rollback(version);
      } else {
         
         dbm.showDBHistory();
         
//         System.out.println(usage);
//         System.exit(1);
      }
   }
}
