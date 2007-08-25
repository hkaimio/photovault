/*
  Copyright (c) 2007 Harri Kaimio
  
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

package org.photovault.imginfo;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.command.CommandException;
import org.photovault.command.DataAccessCommand;
import org.photovault.common.PhotovaultException;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.dcraw.RawSettingsFactory;
import org.photovault.folder.PhotoFolder;
import org.photovault.folder.PhotoFolderDAO;

/**
  Command for changing the properties of {@link PhotoInfo}. This command provides 
 methods for changing all "simple properties of the photo and for adding or deleting
 it from folders. It can also be used for creating a new photo.
 
 */
public class ChangePhotoInfoCommand extends DataAccessCommand {
    
    static Log log = LogFactory.getLog( ChangePhotoInfoCommand.class );
    
    /**
     Fields in PhotoInfo
     @todo this should be part of PhotoInfo
     */
    public enum PhotoInfoFields {
        CAMERA ("camera", String.class ),
        CROP_BOUNDS( "cropBounds", Rectangle2D.class ),
        DESCRIPTION( "description", String.class ),
        FSTOP( "FStop", Double.class ),
        FILM( "film", String.class ),
        FILM_SPEED( "filmSpeed", Integer.class ),
        FOCAL_LENGTH( "focalLength", Integer.class ),
        LENS( "lens", String.class ),
        ORIG_FNAME( "origFname", String.class ),
        PHOTOGRAPHER( "photographer", String.class ),
        PREF_ROTATION( "prefRotation", Double.class ),
        QUALITY( "quality", Integer.class ),
        RAW_SETTINGS( "rawSettings", RawConversionSettings.class ),
        SHOOT_TIME( "shootTime", Date.class ),
        SHOOTING_PLACE( "shootingPlace", String.class ),
        SHUTTER_SPEED( "shutterSpeed", Double.class ),
        TECH_NOTES( "techNotes", String.class ),
        TIME_ACCURACY( "timeAccuracy", Double.class ),
        UUID( "UUID", UUID.class ),
        RAW_BLACK_LEVEL( "", Integer.class ),
        RAW_WHITE_LEVEL( "", Integer.class ),
        RAW_EV_CORR( "", Double.class ),
        RAW_HLIGHT_COMP( "", Double.class ),
        RAW_CTEMP( "", Double.class ),
        RAW_GREEN( "", Double.class ),
        RAW_COLOR_PROFILE( "", Double.class );

        PhotoInfoFields( String name, Class type ) {
            this.name = name;
            this.type = type;
        }
        
        private final String name;
        private final Class type;
        
        public Class getType() {
            return type;
        }
        
        public String getName() {
            return name;
        }
        
    }
    
    /**
     Construct a new command that creates a new PhotoInfo object.
     */
    public ChangePhotoInfoCommand() {
        
    }
    
    /** 
     Creates a new instance of ChangePhotoInfoCommand 
     @photoId Id of th ePhotoInfo to change 
     */
    public ChangePhotoInfoCommand( Integer photoId ) {
        if ( photoId != null ) {
            photoIds.add( photoId );
        }
    }
    
    /** 
     Creates a new instance of ChangePhotoInfoCommand 
     @photoId Array of IDs of all PhotoInfo objects that will be changed.
     */
    public ChangePhotoInfoCommand( Integer[] photoIds ) {
        for ( Integer id : photoIds ) {
            this.photoIds.add( id );
        }
    }

    /** 
     Creates a new instance of ChangePhotoInfoCommand 
     @photoId Collection of IDs of all PhotoInfo objects that will be changed.
     */
    public ChangePhotoInfoCommand( Collection<Integer> photoIds ) {
        this.photoIds.addAll( photoIds );
    }
    
    /**
     Fields that have been changed by this command
     */
    Map<PhotoInfoFields, Object> changedFields = new HashMap<PhotoInfoFields, Object>();
    
    /**
     Folders the photos should be added to
     */
    Set<PhotoFolder> addedToFolders = new HashSet<PhotoFolder>();

    /**
     Folders the photos should be removed from
     */    
    Set<PhotoFolder> removedFromFolders = new HashSet<PhotoFolder>();
    
    /**
     IDs of all photos that will be changed by this command.
     */
    Set<Integer> photoIds = new HashSet<Integer>();

    /**
     Photo instance with the changes applied (in command handler's persistence 
     context or later detached)
     */
    Set<PhotoInfo> changedPhotos = null;
    
    /**
     Get photo instance with the changes applied (in command handler's persistence 
     context or later detached)
     */
    public Set<PhotoInfo> getChangedPhotos() {
        return changedPhotos;
    }
    
    /**
     Set a field to be changed to new value
     @param field Foeld code for the field to change
     @param newValue New value for the field.
     */
    public void setField( PhotoInfoFields field, Object newValue ) {
        log.debug( "setField " + field + ": " + newValue );
        changedFields.put( field, newValue );
    }
    
    /**
     Get the ne wvalue for a field
     @return The value that will be changed to photos when this command is 
     executed or <code>null</code> if the field will be left unchanged.
     */
    public Object getField( PhotoInfoFields field ) {
        log.debug( "getField " + field  );
        return changedFields.get( field );
    }
    
    // Utility methods for setting fields
    
    public void setCamera( String newValue ) {
        setField( PhotoInfoFields.CAMERA, newValue );
    }
    
    public void setCropBounds( Rectangle2D newValue ) {
        setField( PhotoInfoFields.CROP_BOUNDS, newValue );
    }

    public void setDescription( String newValue ) {
        setField( PhotoInfoFields.DESCRIPTION, newValue );
    }

    public void setFStop( double newValue ) {
        setField( PhotoInfoFields.FSTOP, Double.valueOf( newValue ) );
    }

    public void setFilm( String newValue ) {
        setField( PhotoInfoFields.FILM, newValue );
    }

    public void setFilmSpeed( Integer newValue ) {
        setField( PhotoInfoFields.FILM_SPEED, newValue );
    }

    public void setFocalLength( Integer newValue ) {
        setField( PhotoInfoFields.FOCAL_LENGTH, newValue );
    }

    public void setLens( String newValue ) {
        setField( PhotoInfoFields.LENS, newValue );
    }

    public void setOrigFname( String newValue ) {
        setField( PhotoInfoFields.ORIG_FNAME, newValue );
    }

    public void setPhotographer( String newValue ) {
        setField( PhotoInfoFields.PHOTOGRAPHER, newValue );
    }

    public void setPrefRotation( double newValue ) {
        setField( PhotoInfoFields.PREF_ROTATION, Double.valueOf( newValue ) );
    }

    public void setQuality( Integer newValue ) {
        setField( PhotoInfoFields.QUALITY, newValue );
    }

    public void setRawSettings( RawConversionSettings newValue ) {
        setField( PhotoInfoFields.RAW_SETTINGS, newValue );
    }

    public void setShootTime( Date newValue ) {
        setField( PhotoInfoFields.SHOOT_TIME, newValue );
    }

    public void setShootingPlace( String newValue ) {
        setField( PhotoInfoFields.SHOOTING_PLACE, newValue );
    }

    public void setShutterSpeed( Double newValue ) {
        setField( PhotoInfoFields.SHUTTER_SPEED, newValue );
    }

    public void setTechNotes( String newValue ) {
        setField( PhotoInfoFields.TECH_NOTES, newValue );
    }

    public void setTimeAccuracy( Double newValue ) {
        setField( PhotoInfoFields.TIME_ACCURACY, newValue );
    }

    public void setUUID( UUID newValue ) {
        setField( PhotoInfoFields.UUID, newValue );
    }
    
    public void addToFolder( PhotoFolder folder ) {
        removedFromFolders.remove( folder );
        addedToFolders.add( folder );
    }
    
    public void removeFromFolder( PhotoFolder folder ) {
        addedToFolders.remove( folder );
        removedFromFolders.add( folder );
    }
    
    /**
     Execute the command.
     */
    public void execute() throws CommandException {
        StringBuffer debugMsg = null;
        if ( log.isDebugEnabled() ) {
            debugMsg = new StringBuffer();
            debugMsg.append( "execute()" );
            boolean isFirst = true;
            for ( Integer id : photoIds ) {
                debugMsg.append( isFirst ? "Photo ids: " : ", " );
                debugMsg.append( id );
            }
            debugMsg.append( "\n" );
            debugMsg.append( "Changed values:\n" );
            for ( Map.Entry<PhotoInfoFields, Object> e: changedFields.entrySet() ) {
                PhotoInfoFields field = e.getKey();
                Object value = e.getValue();
                debugMsg.append( field ).append( ": " ).append( value ).append( "\n" );
            }
            log.debug( debugMsg );
        }
        PhotoInfoDAO photoDAO = daoFactory.getPhotoInfoDAO();
        Set<PhotoInfo> photos = new HashSet<PhotoInfo>();
        if ( photoIds.size() == 0 ) {
            PhotoInfo photo = PhotoInfo.create();
            photos.add( photo );
        } else {
            for ( Integer id : photoIds ) {
                PhotoInfo photo = photoDAO.findById( id, false );
                photos.add( photo );
            }
        }
        changedPhotos = new HashSet<PhotoInfo>();
        Set<PhotoInfoFields> rawSettingsFields = 
                EnumSet.range( PhotoInfoFields.RAW_BLACK_LEVEL, PhotoInfoFields.RAW_COLOR_PROFILE);
        for ( PhotoInfo photo : photos ) {
            /*
             Ensure that this photo is persistence & the instance belongs to 
             current persistence context
             */
            changedPhotos.add( photo );
            RawSettingsFactory rawSettingsFactory = null;
            for ( Map.Entry<PhotoInfoFields, Object> e: changedFields.entrySet() ) {
                PhotoInfoFields field = e.getKey();
                Object value = e.getValue();
                if ( !rawSettingsFields.contains( field ) ) {
                    try {
                        PropertyUtils.setProperty( photo, field.getName(), value );
                    } catch ( Exception ex) {
                        log.error( "Exception while executing command", ex );
                        throw new CommandException( "Error while executing command: " + ex.getMessage() );
                    }
                } else {
                    // This is a raw setting field, we must use factory for changing it
                    if ( rawSettingsFactory == null ) {
                        rawSettingsFactory = new RawSettingsFactory( photo.getRawSettings() );
                    }
                    try {
                        PropertyUtils.setProperty( rawSettingsFactory, field.getName(), value );
                    } catch ( Exception ex) {
                        log.error( "Exception while executing command", ex );
                        throw new CommandException( "Error while executing command: " + ex.getMessage() );
                    }
                    
                }
                
            }
            if ( rawSettingsFactory != null ) {
                try {
                    photo.setRawSettings( rawSettingsFactory.create() );
                } catch (PhotovaultException ex) {
                    log.error( "Exception while executing command", ex );
                    ex.printStackTrace();
                }
            }
            PhotoFolderDAO folderDAO = daoFactory.getPhotoFolderDAO();
            for ( PhotoFolder folder : addedToFolders ) {
                folder = folderDAO.findById( folder.getFolderId(), false );
                folder.addPhoto ( photo );
            }
            for ( PhotoFolder folder : removedFromFolders ) {
                folder = folderDAO.findById( folder.getFolderId(), false );
                folder.removePhoto( photo );
            }
        }
    }
}
