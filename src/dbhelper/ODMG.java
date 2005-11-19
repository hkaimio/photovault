// $Id: ODMG.java,v 1.1 2003/02/25 20:57:11 kaimio Exp $
package dbhelper;

import photovault.common.PVDatabase;
import photovault.common.PhotovaultSettings;
import org.odmg.*;
import org.apache.ojb.odmg.*;
import photovault.folder.PhotoFolder;
import org.apache.ojb.broker.metadata.ConnectionRepository;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.PBKey;


public class ODMG {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( ODMG.class.getName() );
    
    // Functions to get the ODMG persistence layer handles. This should really be moved into
    // its own helper class

    static Implementation odmg = null;
    public static Implementation getODMGImplementation() {
	if ( odmg == null ) {
            // System.setProperty("OJB.properties","conf/OJB.properties");
	    odmg = OJB.getInstance();
	}
	return odmg;
    }

    static Database db = null;
    public static Database getODMGDatabase() {
	return db;
    }

    public static boolean initODMG( String user, String passwd, PVDatabase dbDesc ) {

	getODMGImplementation();

        // Find the connection repository info
        ConnectionRepository cr = MetadataManager.getInstance().connectionRepository();
        PBKey connKey = cr.getStandardPBKeyForJcdAlias( "pv" );
        JdbcConnectionDescriptor connDesc = cr.getDescriptor( connKey );
        
        // Set up the OJB connection with parameters from photovault.properties
        String dbhost = dbDesc.getDbHost();
        String dbname = dbDesc.getDbName();
        connDesc.setDbAlias( "//" + dbhost + "/" + dbname );
	
        // Open the database connection
        db = odmg.newDatabase();        
	boolean success = false;
	try {
	    log.debug( "Opening database" );
	    db.open( "pv#" + user + "#" + passwd, Database.OPEN_READ_WRITE );
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
