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
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.dbhelper;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import org.apache.derby.impl.jdbc.EmbedSQLException;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.accesslayer.LookupException;
import org.photovault.common.PVDatabase;
import org.photovault.common.PhotovaultException;
import org.photovault.common.PhotovaultSettings;
import org.odmg.*;
import org.apache.ojb.odmg.*;
import org.photovault.folder.PhotoFolder;
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

    /**
     Initialize database connection & OJB ODMG framework
     @param user User name that is used to log into database
     @param passwd password of the user
     @dbDesc The database that is opened
     @throws @see PhotovaultException if database login is not succesfull     
     */
    public static void initODMG( String user, String passwd, PVDatabase dbDesc )
        throws PhotovaultException {

	getODMGImplementation();

        // Find the connection repository info
        ConnectionRepository cr = MetadataManager.getInstance().connectionRepository();
        PBKey connKey = cr.getStandardPBKeyForJcdAlias( "pv" );
        JdbcConnectionDescriptor connDesc = cr.getDescriptor( connKey );
        
        // Set up the OJB connection with parameters from photovault.properties
        if ( dbDesc.getInstanceType() == PVDatabase.TYPE_EMBEDDED ) {
            connDesc.setDriver( "org.apache.derby.jdbc.EmbeddedDriver" );
            connDesc.setDbms( "derby" );
            connDesc.setSubProtocol( "derby" );
            connDesc.setDbAlias( "photovault" );
            File derbyDir = new File( dbDesc.getEmbeddedDirectory(), "derby" );
            System.setProperty( "derby.system.home", derbyDir.getAbsolutePath()  );
        } else {
            // This is a MySQL database
            String dbhost = dbDesc.getDbHost();
            String dbname = dbDesc.getDbName();
            connDesc.setDbAlias( "//" + dbhost + "/" + dbname );
            connDesc.setUserName( user );
            connDesc.setPassWord( passwd );
            connDesc.setDriver( "com.mysql.jdbc.Driver" );
            connDesc.setDbms( "MySQL" );
            connDesc.setSubProtocol( "mysql" );
        }        
	
        
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
        
        // Check whether the database was opened correctly
        try {
            PersistenceBroker broker = PersistenceBrokerFactory.createPersistenceBroker(connKey);
            broker.beginTransaction();
            Connection con = broker.serviceConnectionManager().getConnection();
            broker.commitTransaction();
            broker.close();
        } catch (Exception ex) {
            /*
             Finding the real reason for the error needs a bit of guesswork: first 
             lets find the original exception
            */
            Throwable rootCause = ex;
            while ( rootCause.getCause() != null ) {
                rootCause = rootCause.getCause();
            }
            log.error( rootCause.getMessage() );
            if ( rootCause instanceof SQLException ) {
                if ( rootCause instanceof EmbedSQLException ) {
                    /*
                     We are using Derby, the problem is likely that another 
                     instance of the database has been started
                     */
                     throw new PhotovaultException( "Cannot start database.\n"
                             + "Do you have another instance of Photovault running?", rootCause );
                }
                if ( dbDesc.getInstanceType() == PVDatabase.TYPE_SERVER ) {
                    throw new PhotovaultException( "Cannot log in to MySQL database", rootCause );
                }
            }
            throw new PhotovaultException( "Unknown error while starting database:\n"
                    + rootCause.getMessage(), rootCause );
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
                throw new PhotovaultException( "Unknown error while starting database:\n" );
	    }
	} catch ( Exception t ) {
	    log.error( "Could not open database connection" );
	    log.error( t.getMessage() );
            t.printStackTrace();
            try {
		db.close();
	    } catch (ODMGException e ) {
                log.error( "Error closing database" );
            }
            throw new PhotovaultException( "Unknown error while starting database:\n"
                    + t.getMessage(), t );
            
	}
    }    
}
