// $Id: ODMG.java,v 1.1 2003/02/25 20:57:11 kaimio Exp $
package dbhelper;

import org.odmg.*;
import org.apache.ojb.odmg.*;


public class ODMG {

    
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
	if ( db == null ) {
	    db = odmg.newDatabase();
	    try {
		db.open( "repository.xml", Database.OPEN_READ_WRITE );
	    } catch ( ODMGException e ) {
// 		log.warn( "Could not open database: " + e.getMessage() );
		db = null;
	    }
	}
	return db;
    }

    // Init ODMG fields at creation time
    {
	
	getODMGImplementation();
	getODMGDatabase();
    }
    
}
