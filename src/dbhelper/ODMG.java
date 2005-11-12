// $Id: ODMG.java,v 1.1 2003/02/25 20:57:11 kaimio Exp $
package dbhelper;

import org.odmg.*;
import org.apache.ojb.odmg.*;
import photovault.folder.PhotoFolder;

public class ODMG {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( ODMG.class.getName() );
    
    // Functions to get the ODMG persistence layer handles. This should really be moved into
    // its own helper class

    static Implementation odmg = null;
    public static Implementation getODMGImplementation() {
	if ( odmg == null ) {
	    odmg = OJB.getInstance();
	}
	return odmg;
    }

    static Database db = null;
    public static Database getODMGDatabase() {
	String user = "harri";
	String passwd = "r1t1rat1";
	if ( db == null ) {
	    db = odmg.newDatabase();
	    try {
		db.open( "pv_test#" + user + "#" + passwd, Database.OPEN_READ_WRITE );
	    } catch ( ODMGException e ) {
 		log.error( "Could not open database: " + e.getMessage() );
		db = null;
	    }
	}
	return db;
    }

    public static boolean initODMG( String user, String passwd, String dbName ) {

        // TESTING!!!!!
//        if (db != null ) {
//            try {
//                db.close();
//            } catch ( org.odmg.ODMGException e ) {}
//        }
	getODMGImplementation();
	db = odmg.newDatabase();
	boolean success = false;
	try {
	    log.debug( "Opening database" );
	    db.open( dbName + "#" + user + "#" + passwd, Database.OPEN_READ_WRITE );
	    log.debug( "Success!!!" );
	} catch ( Exception e ) {
	    log.error( "Failed to get connection: " + e.getMessage() );
            e.printStackTrace();
	}

	// Test the connection by fetching something
	try {
	    PhotoFolder folder = PhotoFolder.getRoot();
	    if ( folder != null ) {
		success = true;
	    } else {
		log.error( "Could not open database connection" );
		try {
		    db.close();
		} catch (ODMGException e ) {
		    log.error( "Error closing database" );
		}
	    }
	} catch ( Throwable t ) {
	    log.error( "Could not open database connection" );
	    log.error( t.getMessage() );
            t.printStackTrace();
            try {
		db.close();
	    } catch (ODMGException e ) {
		log.error( "Error closing database" );
	    }
	}

	return ( success );
    }
    // Init ODMG fields at creation time
//     {
	
// 	getODMGImplementation();
    
// 	getODMGDatabase();
//     }
    
}
