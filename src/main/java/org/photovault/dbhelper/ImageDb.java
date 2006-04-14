/*
  Copyright (c) 2006 Harri Kaimio
  
  This file is part of Photovault.

  Photovault is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  Photovault is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Foobar; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.dbhelper;

import java.sql.*;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.accesslayer.LookupException;
import org.apache.ojb.broker.metadata.ConnectionRepository;
import org.apache.ojb.broker.metadata.MetadataManager;
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
        
        ConnectionRepository cr = MetadataManager.getInstance().connectionRepository();
        PBKey connKey = cr.getStandardPBKeyForJcdAlias( "pv" );
        PersistenceBroker broker = PersistenceBrokerFactory.createPersistenceBroker( connKey );
        broker.beginTransaction();
        try {
            conn = broker.serviceConnectionManager().getConnection();
        } catch (LookupException ex) {
            ex.printStackTrace();
        }
//	try {
//	    Class.forName( "com.mysql.jdbc.Driver" ).newInstance();
//	} catch ( Exception e ) {
//	    System.err.println( "DB driver not found" );
//	}
//
//	try {
//	    conn = DriverManager.getConnection( "jdbc:mysql:///pv_junit", "", "" );
//	} catch ( SQLException e ) {
//	    System.err.println( "ERROR: Could not create DB connection: "
//				+ e.getMessage() );
//	}
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
    
