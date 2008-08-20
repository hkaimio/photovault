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

import java.awt.geom.Rectangle2D;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.Integer;
import java.lang.Integer;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.UUID;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.DynaBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ddlutils.DatabaseOperationException;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.PlatformFactory;
import org.apache.ddlutils.io.DatabaseIO;
import org.apache.ddlutils.model.Database;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.folder.PhotoFolder;
import org.photovault.folder.PhotoFolderDAO;
import org.photovault.image.ChannelMapOperation;
import org.photovault.image.ChannelMapOperationFactory;
import org.photovault.imginfo.CopyImageDescriptor;
import org.photovault.imginfo.FuzzyDate;
import org.photovault.imginfo.ImageDescriptorDAO;
import org.photovault.imginfo.ImageFile;
import org.photovault.imginfo.ImageFileDAO;
import org.photovault.imginfo.OriginalImageDescriptor;
import org.photovault.imginfo.PhotoEditor;
import org.photovault.imginfo.PhotoEditorInvocationHandler;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;
import org.photovault.replication.DTOResolverFactory;
import org.photovault.replication.VersionedObjectEditor;

/**
 SchemaUpdateAction updates the database schema created by an earlier version of 
 Photovault to match the current databae schema.<P>
 
 The actual schema update is done using DdlUtils but depending on exact situation
 other modificationto datamay be needed in addition just altering DB tables.
 */
public class SchemaUpdateAction {
    static Log log = LogFactory.getLog( SchemaUpdateAction.class.getName() );
    
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
        
        /*
         TODO: Schema changes should be done using Hibernate tools. But how to 
         handle the oledd schemas?
         */
//        try {
//            /*
//            TODO:
//            Derby alter table statements created by DdlUtils have wrong syntax.
//            Luckily we do not need to do such modifications for now. There is
//            error report for DdlUtils (http://issues.apache.org/jira/browse/DDLUTILS-53),
//            after it has been corrected the alterColumns flag should be set to
//            true.
//             */
//            log.info( platform.getAlterTablesSql( con, dbModel ) );
//            platform.alterTables( con, dbModel, false );
//        } catch ( DatabaseOperationException ex ) {
//            log.error( ex.getMessage(), ex );
//        }


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
        
        if ( oldVersion < 11 ) {
            upgrade11( session );
        }
        
        if ( oldVersion < 12 ) {
            migrateToVersionedSchema();
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
     TODO: update this to work without ImageInstance
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
//            PhotoInfo photo = (PhotoInfo) iter.next();
//            for ( ImageInstance inst : photo.getInstances() ) {
//                /*
//                 Hashes are generated on demand, so this call calculates the hash
//                 if it has not been calculated previously.
//                 */
//                byte[] hash = inst.getHash();
//            }
//            // Check tha also the photo info object contains original contains hash.
//            byte[] origHash = photo.getOrigInstanceHash();
//            processedPhotos++;
//            fireStatusChangeEvent( new SchemaUpdateEvent( PHASE_CREATING_HASHES,
//                    (processedPhotos*100)/photoCount ) );
        }
    }
    

    /**
     Convert 0.5.0 style database to the new versioned schema used by 0.6.0.
     */
    private void migrateToVersionedSchema() {
        try {
            convertFolders();
            convertPhotos();
            
        } catch ( SQLException e ) {
            log.error( "Error while migrating to new schema: " + e.getMessage(), e );
        }
    }
    
     /**
     Convert volumes to new schema
     */
    private void convertVolumes() {
    }   
    
    /**
     Convert folder hiearchy from old schema to the new one
     */
    private void convertFolders() throws SQLException {
        log.debug( "Starting to convert folders to new schema" );
        Session s = HibernateUtil.getSessionFactory().openSession();
        Connection conn = s.connection();

        Queue<Integer> waiting = new LinkedList<Integer>();
        Map<Integer, PhotoFolder> foldersById = new HashMap<Integer, PhotoFolder>();
        waiting.add(  1 );
        HibernateDAOFactory df = 
                (HibernateDAOFactory) DAOFactory.instance( HibernateDAOFactory.class );
        df.setSession( s );
        PhotoFolderDAO folderDao = df.getPhotoFolderDAO();
        foldersById.put(  1, folderDao.findRootFolder() );
        PreparedStatement stmt = conn.prepareStatement( 
                "select * from photo_collections where parent = ?" );
        while ( !waiting.isEmpty() ) {
            int parentId = waiting.remove();
            PhotoFolder parent = foldersById.get(  parentId );
            log.debug( "Querying for folders with parent " + parentId );
            stmt.setInt( 1, parentId );
            ResultSet rs = stmt.executeQuery();
            while ( rs.next() ) {
                // Create the folder
                
                /*
                 TODO: should the UUID be created algorithmically?
                 Or better, how to ensure that UUIDs for folders that are part
                 of external volume will always be the same?
                 */
                
                String uuidStr = rs.getString( "collection_uuid" );
                int id = rs.getInt( "collection_id" );
                log.debug( "Creating folder with old id " + id + ", uuid " + uuidStr );
                UUID uuid = (uuidStr != null) ?
                    UUID.fromString( rs.getString( "collection_uuid" ) ) :
                    UUID.randomUUID();
                if ( id == 1 ) {
                    uuid = PhotoFolder.ROOT_UUID;
                }
                PhotoFolder f = PhotoFolder.create( uuid, parent );
                f.setName( rs.getString( "collection_name" ) );
                f.setDescription( rs.getString( "collection_desc" ) );
                /*
                 TODO: how to set the create time & last modified time without 
                 exposing them to others?
                 */
                folderDao.makePersistent( f );
                log.debug(  "folder saved" );
                foldersById.put( id, f );
                waiting.add( id );
            }
            try {
                rs.close();
            } catch ( SQLException e ) {
                log.error( "Error closing result set", e );
            }
        }
        s.flush();
    }
    
    /**
     SQL query for fetching information about photos and image instances from 
     old database schema. Alll information is fetched, ordered first by photo ID
     and secondly so that original instance is returned before copies and 
     thumbnails.
     */
    static private String oldPhotoQuerySql = "select " +
            "    p.photo_id as p_photo_id, " +
            "    p.photo_uuid as p_photo_uuid, " +
            "    p.shoot_time as p_shoot_time, " +
            "    p.time_accuracy as p_time_accuracy, " +
            "    p.shooting_place as p_shooting_place, " +
            "    p.photographer as p_photographer, " +
            "    p.f_stop as p_f_stop, " +
            "    p.focal_length as p_focal_length, " +
            "    p.shutter_speed as p_shutter_speed, " +
            "    p.camera as p_camera, " +
            "    p.lens as p_lens, " +
            "    p.film as p_film, " +
            "    p.film_speed as p_film_speed, " +
            "    p.pref_rotation as p_pref_rotation, " +
            "    p.clip_xmin as p_clip_xmin, " +
            "    p.clip_xmax as p_clip_xmax, " +
            "    p.clip_ymin as p_clip_ymin, " +
            "    p.clip_ymax as p_clip_ymax, " +
            "    p.orig_fname as p_orig_fname, " +
            "    p.description as p_description, " +
            "    p.photo_quality as p_photo_quality, " +
            "    p.last_modified as p_last_modified, " +
            "    p.channel_map as p_channel_map, " +
            "    p.hash as p_hash, " +
            "    dr.whitepoint as p_whitepoint, " +
            "    dr.blackpoint as p_blackpoint, " +
            "    dr.ev_corr as p_ev_corr, " +
            "    dr.hlight_corr as p_hlight_corr, " +
            "    dr.embedded_profile as p_embedded_profile , " +
            "    dr.wb_type as p_wb_type, " +
            "    dr.r_g_ratio as p_r_g_ratio, " +
            "    dr.b_g_ratio as p_b_g_ratio, " +
            "    dr.dl_r_g_ratio as p_dl_r_g_ratio, " +
            "    dr.dl_b_g_ratio as p_dl_b_g_ratio, " +
            "    i.volume_id as i_volume_id, " +
            "    i.fname as i_fname, " +
            "    i.instance_uuid as i_instance_uuid, " +
            "    i.width as i_width, " +
            "    i.height as i_height, " +
            "    i.rotated as i_rotated, " +
            "    i.instance_type as i_instance_type, " +
            "    i.hash as i_hash, " +
            "    i.channel_map as i_channel_map, " +
            "    i.file_size as i_file_size, " +
            "    i.mtime as i_mtime, " +
            "    i.check_time as i_check_time, " +
            "    i.crop_xmin as i_crop_xmin, " +
            "    i.crop_xmax as i_crop_xmax, " +
            "    i.crop_ymin as i_crop_ymin, " +
            "    i.crop_ymax as i_crop_ymax, " +
            "    ir.whitepoint as i_whitepoint, " +
            "    ir.blackpoint as i_blackpoint, " +
            "    ir.ev_corr as i_ev_corr, " +
            "    ir.hlight_corr as i_hlight_corr, " +
            "    ir.embedded_profile as i_embedded_profile , " +
            "    ir.wb_type as i_wb_type, " +
            "    ir.r_g_ratio as i_r_g_ratio, " +
            "    ir.b_g_ratio as i_b_g_ratio, " +
            "    ir.dl_r_g_ratio as i_dl_r_g_ratio, " +
            "    ir.dl_b_g_ratio as i_dl_b_g_ratio, " +
            "    case " +
            "        when i.instance_type = 'original' then 1 " +
            "        else 0 " +
            "    end i_is_original " +
            "from " +
            "    photos p left outer join " +
            "    dcraw_settings dr  on p.rawconv_id = dr.rawconv_id, " +
            "    image_instances i left outer join " +
            "    dcraw_settings ir  on i.rawconv_id = ir.rawconv_id " +
            "where " +
            "    i.photo_id = p.photo_id " +
            "order by p.photo_id, i_is_original desc ";

    private void convertPhotos() throws SQLException {

        Session s = HibernateUtil.getSessionFactory().openSession();
        HibernateDAOFactory daoFactory = 
                (HibernateDAOFactory) DAOFactory.instance( HibernateDAOFactory.class );
        daoFactory.setSession( s );
        DTOResolverFactory rf = daoFactory.getDTOResolverFactory();
        PhotoInfoDAO photoDao = daoFactory.getPhotoInfoDAO();
        ImageDescriptorDAO imgDao = daoFactory.getImageDescriptorDAO();
        ImageFileDAO ifDao = daoFactory.getImageFileDAO();
        
        /*
         We need a second session for reading old data so that Hibernate can 
         commit its changes
         */
        Session sqlSess = HibernateUtil.getSessionFactory().openSession();
        Connection conn = sqlSess.connection();
        Statement stmt = conn.createStatement();
        ResultSet rs = stmt.executeQuery( oldPhotoQuerySql  );
        int currentPhotoId = -1;
        OriginalImageDescriptor currentOriginal = null;
        
        while ( rs.next() ) {
            // Create the image file corresponding to this row
            ImageFile imgf = new ImageFile();
            imgf.setFileSize( rs.getInt( "i_file_size" ) );
            imgf.setHash( rs.getBytes( "i_hash" ) );
            imgf.setId( UUID.fromString( rs.getString( "i_instance_uuid"  ) ) );
            ifDao.makePersistent( imgf );
            
            int photoId = rs.getInt( "p_photo_id" );
            if ( photoId != currentPhotoId ) {
                currentPhotoId = photoId;
                 // instances of new photo start here
                if ( rs.getInt( "i_is_original" ) != 1 ) {
                    log.error( "No original instance found for photo " + photoId );                    
                }
                currentOriginal = new OriginalImageDescriptor( imgf, "image#0" );
                currentOriginal.setHeight( rs.getInt( "i_height" ) );
                currentOriginal.setWidth( rs.getInt( "i_width" ) );
                imgDao.makePersistent( currentOriginal );
                convertPhotoInfo(rs, rf, currentOriginal, photoDao );
            } else {
                // This is a copy of the previous image
                CopyImageDescriptor cimg = 
                        new CopyImageDescriptor( imgf, "image#0", currentOriginal );
                ChannelMapOperation cm = 
                        ChannelMapOperationFactory.createFromXmlData( 
                        rs.getBytes( "i_channel_map" ) );
                cimg.setColorChannelMapping( cm );
                Rectangle2D crop = readCropArea( rs, "i_" );
                cimg.setCropArea( crop );
                cimg.setHeight( rs.getInt(  "i_height" ));
                cimg.setWidth( rs.getInt(  "i_width" ));
                cimg.setRotation( rs.getDouble( "i_rotated" ) ); 
                imgDao.makePersistent( cimg );
            }
        }

        s.flush();
        s.close();

        try {
            rs.close();
            stmt.close();
            sqlSess.close();
        } catch ( SQLException e ) {
            log.error( e );
            throw e;
        }

    }

   /**
     Creates a PhotoInfo object using the new (0.6.0) data model from old database 
     schema. Field values are read from result set.
     <p>
     The conversion maintains old uuid, and creates 2 change records 
     - one that sets just original and UUID and another that sets the other fields
     to values they have in the old database.
     
     @param rs ResultSet from which the field values are read.
     @param rf Resolver factory used when constructing change objects
     @param original Origianl image of the photo
     @param photoDao DAO used to perist photo
     @throws java.oldPhotoQuerySql.SQLException If an error occurs when reading old data.
     */
    private void convertPhotoInfo( ResultSet rs, 
            DTOResolverFactory rf, 
            OriginalImageDescriptor original,
            PhotoInfoDAO photoDao ) throws SQLException {

        // Create photo to match this image
        PhotoInfo photo = null;
        String photoUuid = rs.getString( "p_photo_uuid" );
        if ( photoUuid != null ) {
            log.debug( "Creating photo with uuid " + photoUuid );
            photo = PhotoInfo.create( UUID.fromString( photoUuid ) );
        } else {
            log.debug( "No UUID in database, creating photo with random uuid" );
            photo = PhotoInfo.create();
        }
        photoDao.makePersistent( photo );

        // First change should contain just information about the original
        VersionedObjectEditor<PhotoInfo> pe = 
                new VersionedObjectEditor<PhotoInfo>( photo.getHistory(), rf );
        pe.setField( "original", original );
        pe.apply();

        // Second change sets all other fields
        pe = new VersionedObjectEditor<PhotoInfo>( photo.getHistory(), rf );
        PhotoEditor e = (PhotoEditor) pe.getProxy();
        e.setCamera( rs.getString( "p_camera" ) );
        ChannelMapOperation cm = 
                ChannelMapOperationFactory.createFromXmlData( 
                rs.getBytes( "i_channel_map" ) );
        // e.setColorChannelMapping( cm );
        e.setCropBounds( readCropArea( rs, "p_" ) );
        e.setDescription( rs.getString( "p_description" ) );
        e.setFStop( rs.getDouble( "p_f_stop" ) );
        e.setFilm( rs.getString( "p_film" ) );
        e.setFilmSpeed( rs.getInt( "p_film_speed" ) );
        e.setFocalLength( rs.getDouble( "p_focal_length" ) );
        FuzzyDate ft = new FuzzyDate( rs.getTimestamp( "p_shoot_time" ), 
                rs.getDouble( "p_time_accuracy" ) );
        e.setFuzzyShootTime( ft );
        e.setLens( rs.getString( "p_lens" ) );
        e.setOrigFname( rs.getString( "p_orig_fname" ) );
        e.setPhotographer( rs.getString( "p_photographer" ) );
        e.setPrefRotation( rs.getDouble( "p_pref_rotation" ) );
        e.setQuality( rs.getInt( "p_photo_quality" ) );
        e.setShootingPlace( rs.getString( "p_shooting_place" ) );
        pe.apply();
    }    
 
    /**
     Creates a rectangle based on crop coordinates from result set
     @param rs The result set from which to read the coordinates
     @param prefix Prefix to determine field names. The field names are of form 
     prefix(x|y)(min|max)
     @return Rectangle based on the coordinates read
     @throws java.oldPhotoQuerySql.SQLException if an error occurs during reading.
     */
    private Rectangle2D readCropArea( ResultSet rs, String prefix ) 
            throws SQLException {
        log.debug(  "entry: readCropArea " + prefix );
        double xmin = rs.getDouble( "i_crop_xmin" );
        double xmax = rs.getDouble( "i_crop_xmax" );
        double ymin = rs.getDouble( "i_crop_ymin" );
        double ymax = rs.getDouble( "i_crop_ymax" );
        Rectangle2D crop = 
                new Rectangle2D.Double( xmin, ymin, xmax - xmin, ymax - ymin );
        log.debug(  "crop area " + crop );
        return crop;
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

    
    /**
     Upgrade to chema version 11 (move raw settings to photos & image_instances
     tables.
     */
    private void upgrade11(Session session) {
        Connection con = session.connection();
        try {
            Statement stmt = con.createStatement();
            String sql = "update photos p, ";
            stmt.executeUpdate( "update photos p, ");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     Add {@link ExternalDir} to folders that correspond to an external volume 
     directory
     @param session
     */
    private void upgrade12( Session session ) {
        throw new UnsupportedOperationException( "Not yet implemented" );
    }
}