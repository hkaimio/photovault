// ImageDb.java
package dbhelper;

import java.sql.*;
/**
   This class contains static methods that ain in database usage etc.
*/
public class ImageDb {
    
    // Database routines
    private static Connection conn = null;

    /**
       Returns the database connection to current database. This probably should use some
       kind of pool to handle potentially multiple connections.
       @return Connection to the currently active database
    */
    public static Connection getConnection() {
	if ( conn == null ) {
	    initDB();
	}
	return conn;
    }
    
    private static void initDB() {
	try {
	    Class.forName( "com.mysql.jdbc.Driver" ).newInstance();
	} catch ( Exception e ) {
	    System.err.println( "DB driver not found" );
	}

	try {
	    conn = DriverManager.getConnection( "jdbc:mysql:///pv_test", "harri", "" );
	} catch ( SQLException e ) {
	    System.err.println( "ERROR: Could not create DB connection: "
				+ e.getMessage() );
	}
    }

    /**
       This fuction generates a unique integer uid for usage as a database key.
       @return unique integer
    */

    public static int newUid() {
	int uid = -1;
	Connection con  = getConnection();
	try {
	    Statement stmt = conn.createStatement();
	    stmt.executeUpdate( "UPDATE sequence SET id = LAST_INSERT_ID( id+1 )" );
	    ResultSet rs = stmt.executeQuery( "SELECT LAST_INSERT_ID()" );
	    if ( rs.next() ) {
		uid = rs.getInt( 1 );
	    }
	} catch ( SQLException e ) {
	    System.err.println( "Error generating uid: " + e.getMessage() );
	}
	return uid;
    }
    
}
    
