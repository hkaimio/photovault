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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.command.CommandException;
import org.photovault.command.DataAccessCommand;
import org.photovault.common.PhotovaultException;
import org.photovault.dcraw.ColorProfileDesc;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.dcraw.RawSettingsFactory;
import org.photovault.folder.FolderPhotoAssocDAO;
import org.photovault.folder.FolderPhotoAssociation;
import org.photovault.folder.PhotoFolder;
import org.photovault.folder.PhotoFolderDAO;
import org.photovault.image.ChannelMapOperationFactory;
import org.photovault.image.ColorCurve;
import org.photovault.imginfo.xml.ChangeDesc;
import org.photovault.replication.DTOResolverFactory;
import org.photovault.replication.VersionedObjectEditor;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

/**
  Command for changing the properties of {@link PhotoInfo}. This command provides 
 methods for changing all "simple properties of the photo and for adding or deleting
 it from folders. It can also be used for creating a new photo.
 
 */
public class ChangePhotoInfoCommand extends DataAccessCommand {
    
    static Log log = LogFactory.getLog( ChangePhotoInfoCommand.class );
    

    /**
     Construct a new command that creates a new PhotoInfo object.
     */
    public ChangePhotoInfoCommand() {
        
    }

    /** 
     Creates a new instance of ChangePhotoInfoCommand 
     @photoId UUID of the PhotoInfo to change 
     */
    public ChangePhotoInfoCommand( UUID photoUuid ) {
        if ( photoUuid != null ) {
            photoUuids.add( photoUuid );
        }
    }
    
    /** 
     Creates a new instance of ChangePhotoInfoCommand 
     @photoId Array of UUIDs of all PhotoInfo objects that will be changed.
     */
    public ChangePhotoInfoCommand( UUID[] photoUuids ) {
        for ( UUID id : photoUuids ) {
            this.photoUuids.add( id );
        }
    }

    /** 
     Creates a new instance of ChangePhotoInfoCommand 
     @photoId Collection of UUIDs of all PhotoInfo objects that will be changed.
     */
    public ChangePhotoInfoCommand( Collection<UUID> photoIds ) {
        this.photoUuids.addAll( photoIds );
    }
        /** 
     Creates a new instance of ChangePhotoInfoCommand 
     @photoId Collection of IDs of all PhotoInfo objects that will be changed.
     */
    
    /**
     Fields that have been changed by this command
     */
    Map<PhotoInfoFields, Object> changedFields = new HashMap<PhotoInfoFields, Object>();
    
    /**
     Folders the photos should be added to
     */
    Set<UUID> addedToFolders = new HashSet<UUID>();

    /**
     Folders the photos should be removed from
     */    
    Set<UUID> removedFromFolders = new HashSet<UUID>();
    
    /**
     UUIDs of all photos that will be changed by this command.
     */
    Set<UUID> photoUuids = new HashSet<UUID>();
    
    /**
     Photo instance with the changes applied (in command handler's persistence 
     context or later detached)
     */
    Set<PhotoInfo> changedPhotos = null;
    
    Set<ChangeDesc> changes = new HashSet<ChangeDesc>();
    
    /**
     Get photo instance with the changes applied (in command handler's persistence 
     context or later detached)
     */
    public Set<PhotoInfo> getChangedPhotos() {
        return changedPhotos;
    }

    public Set<ChangeDesc> getChanges() {
        return changes;
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
    
    public enum FolderStates {
        UNMODIFIED,
        ADDED,
        REMOVED
    };
    
    /**
     Instruct command to add all photos to given folder
     @param folder The folder into which the photos will be added
     */
    public void addToFolder( PhotoFolder folder ) {
        removedFromFolders.remove( folder.getUuid() );
        addedToFolders.add( folder.getUuid() );
    }
    
    /**
     Instruct command to add all photos to given folder
     @param folder The folder into which the photos will be added
     */
    public void removeFromFolder( PhotoFolder folder ) {
        addedToFolders.remove( folder.getUuid() );
        removedFromFolders.add( folder.getUuid() );
    }
    
    
    public FolderStates getFolderState( PhotoFolder folder ) {
        if ( addedToFolders.contains( folder.getUuid() ) ) {
            return FolderStates.ADDED;        
        } else if ( removedFromFolders.contains( folder.getUuid() ) ) {
            return FolderStates.REMOVED;
        }
        return FolderStates.UNMODIFIED;
    }    
        
    private void setRawField( RawSettingsFactory settings, PhotoInfoFields field, Object newValue ) {
        switch ( field ) {
            case RAW_BLACK_LEVEL:
                settings.setBlack( (Integer)newValue );
                break;
            case RAW_WHITE_LEVEL:
                settings.setWhite( (Integer) newValue );
                break;
            case RAW_CTEMP:
                settings.setColorTemp( (Double) newValue );
                break;
            case RAW_EV_CORR:
                settings.setEvCorr( (Double) newValue );
                break;
            case RAW_GREEN:
                settings.setGreenGain( (Double) newValue );
                break;
            case RAW_HLIGHT_COMP:
                settings.setHlightComp( (Double) newValue );
                break;
            case RAW_COLOR_PROFILE:
                settings.setColorProfile( (ColorProfileDesc) newValue);
                break;
        }
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
            for ( UUID id : photoUuids ) { 
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
        if ( photoUuids.size() == 0 ) {
            PhotoInfo photo = photoDAO.create();
            photos.add( photo );
        } else {
            for ( UUID id : photoUuids ) {
                PhotoInfo photo = photoDAO.findByUUID( id );
                photos.add( photo );
            }
        }
        changedPhotos = new HashSet<PhotoInfo>();
        Set<PhotoInfoFields> rawSettingsFields = 
                EnumSet.range( PhotoInfoFields.RAW_BLACK_LEVEL, PhotoInfoFields.RAW_COLOR_PROFILE);
        Set<PhotoInfoFields> colorCurveFields = 
                EnumSet.range( PhotoInfoFields.COLOR_CURVE_VALUE, PhotoInfoFields.COLOR_CURVE_SATURATION );
        
        DTOResolverFactory resolverFactory = daoFactory.getDTOResolverFactory();
        for ( PhotoInfo photo : photos ) {
            /*
            Ensure that this photo is persistence & the instance belongs to 
            current persistence context
             */
            changedPhotos.add( photo );
            VersionedObjectEditor<PhotoInfo> pe = new VersionedObjectEditor(
                    photo, resolverFactory );
            RawSettingsFactory rawSettingsFactory = null;
            ChannelMapOperationFactory channelMapFactory = null;
            for ( Map.Entry<PhotoInfoFields, Object> e : changedFields.entrySet() ) {
                PhotoInfoFields field = e.getKey();
                Object value = e.getValue();
                if ( rawSettingsFields.contains( field ) ) {
                    // This is a raw setting field, we must use factory for changing it
                    if ( rawSettingsFactory == null ) {
                        rawSettingsFactory = new RawSettingsFactory(
                                photo.getRawSettings() );
                    }
                    this.setRawField( rawSettingsFactory, field, value );
                } else if ( colorCurveFields.contains( field ) ) {
                    if ( channelMapFactory == null ) {
                        channelMapFactory = new ChannelMapOperationFactory(
                                photo.getColorChannelMapping() );
                    }
                    switch ( field ) {
                        case COLOR_CURVE_VALUE:
                            channelMapFactory.setChannelCurve( "value",
                                    (ColorCurve) value );
                            break;
                        case COLOR_CURVE_RED:
                            channelMapFactory.setChannelCurve( "red",
                                    (ColorCurve) value );
                            break;
                        case COLOR_CURVE_BLUE:
                            channelMapFactory.setChannelCurve( "blue",
                                    (ColorCurve) value );
                            break;
                        case COLOR_CURVE_GREEN:
                            channelMapFactory.setChannelCurve( "green",
                                    (ColorCurve) value );
                            break;
                        case COLOR_CURVE_SATURATION:
                            channelMapFactory.setChannelCurve( "saturation",
                                    (ColorCurve) value );
                            break;
                    }
                } else {
                    pe.setField( e.getKey().getName(), e.getValue() );
                }
            }
            if ( rawSettingsFactory != null ) {
                try {
                    pe.setField( "rawSettings", rawSettingsFactory.create() );
                } catch ( PhotovaultException ex ) {
                    throw new CommandException( "Cannot set raw settings:" + ex.getMessage() );
                }
            }
            if ( channelMapFactory != null ) {
                pe.setField( "colorChannelMapping", channelMapFactory.create() );
            }
            
            pe.apply();
//            RawSettingsFactory rawSettingsFactory = null;
//            ChannelMapOperationFactory channelMapFactory = null;
//            for ( Map.Entry<PhotoInfoFields, Object> e: changedFields.entrySet() ) {
//                PhotoInfoFields field = e.getKey();
//                Object value = e.getValue();
//                if ( rawSettingsFields.contains( field ) ) {
//                    // This is a raw setting field, we must use factory for changing it
//                    if ( rawSettingsFactory == null ) {
//                        rawSettingsFactory = new RawSettingsFactory( photo.getRawSettings() );
//                    }
//                    this.setRawField( rawSettingsFactory, field, value );
//                } else if ( colorCurveFields.contains( field ) ) {
//                    if ( channelMapFactory == null ) {
//                        channelMapFactory = new ChannelMapOperationFactory( photo.getColorChannelMapping() );
//                    }
//                    switch ( field ) {
//                        case COLOR_CURVE_VALUE:
//                            channelMapFactory.setChannelCurve( "value", (ColorCurve) value);
//                            break;
//                        case COLOR_CURVE_RED:
//                            channelMapFactory.setChannelCurve( "red", (ColorCurve) value);
//                            break;
//                        case COLOR_CURVE_BLUE:
//                            channelMapFactory.setChannelCurve( "blue", (ColorCurve) value);
//                            break;
//                        case COLOR_CURVE_GREEN:
//                            channelMapFactory.setChannelCurve( "green", (ColorCurve) value);
//                            break;
//                        case COLOR_CURVE_SATURATION:
//                            channelMapFactory.setChannelCurve( "saturation", (ColorCurve) value);
//                            break;
//                    }
//                } else {
//                    try {
//                        PropertyUtils.setProperty( photo, field.getName(), value );
//                    } catch ( Exception ex) {
//                        log.error( "Exception while executing command", ex );
//                        throw new CommandException( 
//                                "Error while executing command: " 
//                                + ex.getMessage() );
//                    }
//                }                
//            }
//            if ( rawSettingsFactory != null ) {
//                try {
//                    photo.setRawSettings( rawSettingsFactory.create() );
//                } catch (PhotovaultException ex) {
//                    log.error( "Exception while executing command", ex );
//                    ex.printStackTrace();
//                }
//            }
//            if ( channelMapFactory != null ) {
//                photo.setColorChannelMapping( channelMapFactory.create() );
//                
//            }
            
            PhotoFolderDAO folderDAO = daoFactory.getPhotoFolderDAO();
            FolderPhotoAssocDAO assocDAO = daoFactory.getFolderPhotoAssocDAO();
            Set<PhotoFolder> af = new HashSet<PhotoFolder>();
            for ( UUID folderId : addedToFolders ) {
                log.debug( "Adding photo " + photo.getUuid() + " to folder " + folderId );
                PhotoFolder folder = folderDAO.findById( folderId, false );
                FolderPhotoAssociation a = assocDAO.getAssociation( folder, photo );
                photo.addFolderAssociation( a );
                folder.addPhotoAssociation( a );
                af.add(folder);
            }
            Set<PhotoFolder> rf = new HashSet<PhotoFolder>();
            for ( UUID folderId : removedFromFolders ) {
                log.debug( "Removing photo " + photo.getUuid() + " from folder " + folderId );
                PhotoFolder folder = folderDAO.findById( folderId, false );
                FolderPhotoAssociation a = assocDAO.getAssociation( folder, photo );
                folder.removePhotoAssociation( a );
                photo.removeFolderAssociation( a );
                assocDAO.makeTransient( a );
            }
            
//            PhotoInfoChangeDesc change = new PhotoInfoChangeDesc( 
//                    photo, changedFields, af, rf );
//            changes.add( change );
//            photo.setVersion( change );
        }
    }
    String xml = null;

    
    public String getAsXml() {
        if ( xml == null ) {
            DOMImplementationRegistry registry = null;
            try {
                 registry = DOMImplementationRegistry.newInstance();
            } catch ( Exception e ) {
                log.error( "Error instantiating DOM implementation");
            }

            DOMImplementation domImpl =
                    (DOMImplementation) registry.getDOMImplementation( "LS" );


            Document doc = domImpl.createDocument( null, "object-history", null );
            Element root = doc.getDocumentElement();

            Element changeEnvelope = doc.createElement( "change" );
            root.appendChild( changeEnvelope );
            Element changeDesc = doc.createElement( "change-desc" );
            changeEnvelope.appendChild( changeDesc );
            changeDesc.setAttribute( "target", "uuid-placehoder" );
            Element chClass = doc.createElement( "change-class" );
            chClass.setAttribute( "class", this.getClass().getName() );
            changeDesc.appendChild( chClass );

            Element predecessors = doc.createElement( "predecessors" );
            changeDesc.appendChild( predecessors );

            Element pred = doc.createElement( "change-ref" );
            predecessors.appendChild( pred );
            pred.setAttribute( "uuid", "uuild-placehoder" );

            Element fields = doc.createElement( "fields" );
            changeDesc.appendChild( fields );

            for ( Map.Entry<PhotoInfoFields, Object> e : changedFields.entrySet() ) {
                Element field = doc.createElement( "field" );
                fields.appendChild( field );
                field.setAttribute( "name", e.getKey().getName() );
                field.appendChild( doc.createTextNode( e.getValue().toString() ) );
            }

            if ( addedToFolders.size() > 0 ) {
                Element addedFolders = doc.createElement( "folders-added" );
                changeDesc.appendChild( addedFolders );
                for ( UUID id : addedToFolders ) {
                    Element f = doc.createElement( "folder" );
                    f.setAttribute( "uuid", id.toString() );
                    addedFolders.appendChild( f );
                }
            }
            if ( removedFromFolders.size() > 0 ) {
                Element removedFolders = doc.createElement( "folders-removed" );
                changeDesc.appendChild( removedFolders );
                for ( UUID id : removedFromFolders ) {
                    Element f = doc.createElement( "folder" );
                    f.setAttribute( "uuid", id.toString() );
                    removedFolders.appendChild( f );
                }
            }
            LSSerializer writer = ((DOMImplementationLS)domImpl).createLSSerializer();
            xml = writer.writeToString(doc);            
        }
        
        return xml;
    }
}
