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

package org.photovault.common;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.sql.DataSource;
import org.apache.ddlutils.DynaSqlException;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.PlatformFactory;
import org.apache.ddlutils.io.DatabaseIO;
import org.apache.ddlutils.model.Database;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.metadata.ConnectionRepository;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.apache.ojb.broker.accesslayer.LookupException;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.imginfo.ImageInstance;
import org.photovault.imginfo.PhotoInfo;

/**
 SchemaUpdateAction updates the database schema created by an earlier version of 
 Photovault to match the current databae schema.<P>
 
 The actual schema update is done using DdlUtils but depending on exact situation
 other modificationto datamay be needed in addition just altering DB tables.
 */
public class SchemaUpdateAction {
    
    PVDatabase db = null;
    
    /** Creates a new instance of SchemaUpdateAction 
     @param db The dabatbase whose schema is going to be updated
     */
    public SchemaUpdateAction( PVDatabase db ) {
        this.db = db;
    }
    
    public static final int PHASE_ALTERING_SCHEMA = 1;
    public static final int PHASE_CREATING_HASHES = 2;
    public static final int PHASE_COMPLETE = 3;
    
    
    /**
     Upgrades database schema and content to be compatible with the current 
     version of Photovault
     */
    public void upgradeDatabase() {
        fireStatusChangeEvent( new SchemaUpdateEvent( PHASE_ALTERING_SCHEMA, 0 ) );
        
        int oldVersion = db.getSchemaVersion();
        
        // Find needed information fr DdlUtils
        ConnectionRepository cr = MetadataManager.getInstance().connectionRepository();
        PBKey connKey = cr.getStandardPBKeyForJcdAlias( "pv" );
        JdbcConnectionDescriptor connDesc = cr.getDescriptor( connKey );
        DataSource src = connDesc.getDataSource();
        String jdbcDriver = connDesc.getDriver();
        Platform platform = null;
        if ( jdbcDriver.equals( "org.apache.derby.jdbc.EmbeddedDriver" ) ) {
            platform = PlatformFactory.createNewPlatformInstance( "derby" );            
        } else if ( jdbcDriver.equals( "com.mysql.jdbc.Driver" ) ){
            platform = PlatformFactory.createNewPlatformInstance( "mysql" );
        }
        platform.getPlatformInfo().setDelimiterToken( "" );
        
        // Get the database schema XML file
        InputStream schemaIS = getClass().getClassLoader().getResourceAsStream( "photovault_schema.xml" );
        Database dbModel = new DatabaseIO().read( new InputStreamReader( schemaIS ) );
        
        // Alter tables to match corrent schema
        PersistenceBroker broker = PersistenceBrokerFactory.createPersistenceBroker( connKey );
        broker.beginTransaction();
        try {
            Connection con = broker.serviceConnectionManager().getConnection();
            /*
             TODO:
             Derby alter table statements created by DdlUtils have wrong syntax. 
             Luckily we do not need to do such modifications for now. There is 
             error report for DdlUtils (http://issues.apache.org/jira/browse/DDLUTILS-53),
             after it has been corrected the alterColumns flag should be set to
             true.
             */
             System.out.println( platform.getAlterTablesSql( con, dbModel, false, true, true ) );
             platform.alterTables( con, dbModel, false, true, true );
        } catch (DynaSqlException ex) {
            ex.printStackTrace();
        } catch ( LookupException ex ) {
            ex.printStackTrace();
        }
        broker.commitTransaction();
        broker.close();
        
        if ( oldVersion < 4 ) {
            // In older version hashcolumn was not included in schema so we must fill it.
            createHashes();
        }

        DbInfo info = DbInfo.getDbInfo();
        info.setVersion( db.CURRENT_SCHEMA_VERSION );
        
        fireStatusChangeEvent( new SchemaUpdateEvent( PHASE_COMPLETE, 100 ) ); 
    
    }

    /**
     Create hashes for all instances that do not have those.
     */
    private void createHashes() {
        fireStatusChangeEvent( new SchemaUpdateEvent( PHASE_CREATING_HASHES, 0 ) );
        String oql = "select photos from " + PhotoInfo.class.getName();
        List photos = null;
        
        // Get transaction context
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Implementation odmg = ODMG.getODMGImplementation();
        
        try {
            OQLQuery query = odmg.newOQLQuery();
            query.create( oql );
            photos = (List) query.execute();
            txw.commit();
        } catch (Exception e ) {
            txw.abort();
            return;
        }
        
        Iterator iter =photos.iterator();
        int photoCount = photos.size();
        int processedPhotos = 0;
        while ( iter.hasNext() ) {
            PhotoInfo photo = (PhotoInfo) iter.next();
            for ( int n = 0; n < photo.getNumInstances(); n++ ) {
                ImageInstance inst = photo.getInstance( n );
                /*
                 Hashes are generated on demand, so this call calculates the hash
                 if it has not been calculated previously.
                 */
                byte[] hash = inst.getHash();
            }
            // Check tha also the photo info object contains original contains hash.
            byte[] origHash = photo.getOrigInstanceHash();
            processedPhotos++;
            fireStatusChangeEvent( new SchemaUpdateEvent( PHASE_CREATING_HASHES,
                    (processedPhotos*100)/photoCount ) );
        }
    }
    
    /**
     List of object implementing SchemaUpdateListener that need to be notified 
     about status changes.
     */
    Vector listeners = new Vector();
    
    /**
     Adds a new listener for this update action.
     @param l The listener that will be added.
     */
    public void addSchemaUpdateListener( SchemaUpdateListener l ) {
        listeners.add( l );
    }
    
    /**
     Removes a listener from this action.
     
     @param l The listener that will be removed.
     */
    public void removeSchemaUpdateListener( SchemaUpdateListener l ) {
        listeners.remove( l );
    }

    /**
     Notifies all listeners about a status change.
     
     @param e The event that will be sent to all listeners.
     */
    protected void fireStatusChangeEvent( SchemaUpdateEvent e ) {
        Iterator iter = listeners.iterator();
        while ( iter.hasNext() ) {
            SchemaUpdateListener l = (SchemaUpdateListener) iter.next();
            l.schemaUpdateStatusChanged( e );
        }
    }
}