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

package org.photovault.common;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import javax.sql.DataSource;
import org.apache.commons.beanutils.DynaBean;
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
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.imginfo.ImageInstance;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.persistence.HibernateUtil;

/**
 SchemaUpdateAction updates the database schema created by an earlier version of 
 Photovault to match the current databae schema.<P>
 
 The actual schema update is done using DdlUtils but depending on exact situation
 other modificationto datamay be needed in addition just altering DB tables.
 */
public class SchemaUpdateAction {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( SchemaUpdateAction.class.getName() );
    
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
        Session session = HibernateUtil.getSessionFactory().openSession();
        Platform platform = null;
        if ( db.getInstanceType() == PVDatabase.TYPE_EMBEDDED ) {
            platform = PlatformFactory.createNewPlatformInstance( "derby" );            
        } else if ( db.getInstanceType() == PVDatabase.TYPE_SERVER ) {
            platform = PlatformFactory.createNewPlatformInstance( "mysql" );
        }
        platform.getPlatformInfo().setDelimiterToken( "" );
        
        // Get the database schema XML file
        InputStream schemaIS = getClass().getClassLoader().getResourceAsStream( "photovault_schema.xml" );
        DatabaseIO dbio = new DatabaseIO();
        dbio.setValidateXml( false );
        Database dbModel = dbio.read( new InputStreamReader( schemaIS ) );
        
        // Alter tables to match corrent schema
        Transaction tx = session.beginTransaction();
        Connection con = session.connection();
        try {
            /*
             TODO:
             Derby alter table statements created by DdlUtils have wrong syntax. 
             Luckily we do not need to do such modifications for now. There is 
             error report for DdlUtils (http://issues.apache.org/jira/browse/DDLUTILS-53),
             after it has been corrected the alterColumns flag should be set to
             true.
             */
             log.info( platform.getAlterTablesSql( con, dbModel, false, true, true ) );
             platform.alterTables( con, dbModel, false, true, true );
        } catch (DynaSqlException ex) {
            ex.printStackTrace();
        } 
        
        if ( oldVersion < 4 ) {
            // In older version hashcolumn was not included in schema so we must fill it.
            createHashes();
        }
        
        if ( oldVersion < 10 ) {
            // Initialize Hibernate sequence generators
            Query q = session.createQuery( "select max( rs.rawSettingId ) from RawConversionSettings rs" );
            int maxRawSettingId = (Integer) q.uniqueResult();
            q = session.createQuery( "select max( photo.id ) from PhotoInfo photo" );
            int maxPhotoId = (Integer) q.uniqueResult();
            q = session.createQuery( "select max( folder.folderId ) from PhotoFolder folder" );
            int maxFolderId = (Integer) q.uniqueResult();
            DynaBean dbInfo = dbModel.createDynaBeanFor( "unique_keys", false );
            dbInfo.set( "id_name", "hibernate_seq" );
            dbInfo.set( "next_val", new Integer( maxPhotoId+1 ) );
            platform.insert( con, dbModel, dbInfo );
            dbInfo.set( "id_name", "rawconv_id_gen" );
            dbInfo.set( "next_val", new Integer( maxRawSettingId+1 ) );
            platform.insert( con, dbModel, dbInfo );
            dbInfo.set( "id_name", "folder_id_gen" );
            dbInfo.set( "next_val", new Integer( maxFolderId+1 ) );
            platform.insert( con, dbModel, dbInfo );
            
            try {
                Statement stmt = con.createStatement();
                stmt.executeUpdate( "insert into unique_keys(hibernate_seq, values ( \"hibernate_seq\", " + (maxPhotoId+1) + " )" );
                stmt.close();
            } catch (SQLException sqlex) {
                sqlex.printStackTrace();
            }
        }
        DbInfo info = DbInfo.getDbInfo();
        info = (DbInfo) session.merge( info );
        info.setVersion( db.CURRENT_SCHEMA_VERSION );
        session.flush();
        tx.commit();
        session.close();
        
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
            for ( ImageInstance inst : photo.getInstances() ) {
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