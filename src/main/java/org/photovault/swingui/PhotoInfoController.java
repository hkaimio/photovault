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

package org.photovault.swingui;

import java.lang.reflect.InvocationTargetException;
import org.apache.commons.beanutils.PropertyUtils;
import org.odmg.LockNotGrantedException;
import org.photovault.common.PhotovaultException;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.dcraw.ColorProfileDesc;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.dcraw.RawSettingsFactory;
import org.photovault.image.ChannelMapOperation;
import org.photovault.image.ChannelMapOperationFactory;
import org.photovault.image.ColorCurve;
import org.photovault.image.PhotovaultImage;
import org.photovault.imginfo.FuzzyDate;
import java.util.*;
import java.io.*;
import org.photovault.imginfo.*;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoNotFoundException;
import org.photovault.swingui.folderpane.FolderController;
import org.apache.log4j.Logger;


/**
 PhotoInfoController contains the application logic for creating and editing 
 PhotoInfo records in database, i.e. it implements the controller role in MVC 
 pattern.
 */

public class PhotoInfoController {
    
    static Logger log = Logger.getLogger( PhotoInfoController.class.getName() );
    
    /**
     Default constructor
     */
    public PhotoInfoController() {
//        modelFields = new HashMap();
        views = new ArrayList<PhotoInfoView>();
        cmd = new ChangePhotoInfoCommand();
//        initModelFields();
    }
    
    static class PhotoInfoFieldCtrl extends FieldController<PhotoInfo,ChangePhotoInfoCommand> {
        PhotoInfoFieldCtrl( String field, PhotoInfo m ) {
            super( field, m );
        }

        PhotoInfoFieldCtrl( String field, PhotoInfo[] m ) {
            super( field, m );
        }
        
    }
    
    /**
     Special field controller for handling UI fields that are stored as part of
     RawConversionSettings.
     */
    abstract class RawSettingFieldCtrl extends FieldController {
        /**
         * Constructs a new RawSettingsFieldCtrl
         * @param model PhotoInfo objects that form the model
         * @param field Field that is controlled bu this object
         */
        public RawSettingFieldCtrl( Object model, String field ) {
            super(  field, model );
        }
                
        /**
         * This must be overridden by derived classes to set the raw settings field in the
         * RawSettingsFactory that will be used for creating new settings for controlled 
         * object.
         * @param f The factory in which set field must be set
         * @param newValue New value for the field. Note that this can be <code>null</code> even for 
         * primitive type fields.
         */
        protected abstract void doSetModelValue( RawSettingsFactory f, Object newValue );
        /**
         * Get the value of the controlled field in RawConversionSettings.
         * @param r The raw conversion settings object whose field value we are interested in.
         * @return Value of field in r
         */
        protected abstract Object doGetModelValue( RawConversionSettings r );
        /**
         * Set a view to reflect current model value
         * @param view The view that should be set up.
         */
        protected abstract void doSetViewValue( RawPhotoView view );
        /**
         * Get the value of this field in a given view
         * @param view The view whose field value must be retrieved
         * @return Value of controlled field in given view.
         */
        protected abstract Object doGetViewValue( RawPhotoView view );
        /**
         * Set the multivalued state in a given view
         * @param view The view that will be set up.
         */
        protected abstract void doSetViewMultivaluedState( RawPhotoView view );
        
        
        protected void setModelValue( Object model ) {
            PhotoInfo obj = (PhotoInfo) model;
            RawSettingsFactory f = getRawSettingsFactory( obj );
            if ( f != null ) {
                doSetModelValue( f, value );
                rawSettingsChanged();
            }
        }
        protected Object getModelValue( Object model ) {
            PhotoInfo obj = (PhotoInfo) model;
            RawConversionSettings r = obj.getRawSettings();
            Object ret = null;
            if ( r != null ) {
                ret = doGetModelValue( r );
            }
            return ret;
        }
        protected void updateView( Object view ) {
            if ( view instanceof RawPhotoView ) {
                RawPhotoView obj = (RawPhotoView) view;
                doSetViewValue( (RawPhotoView) view );
            }
        }
        protected void updateViewMultivalueState( Object view ) {
            if ( view instanceof RawPhotoView ) {
                RawPhotoView obj = (RawPhotoView) view;
                doSetViewMultivaluedState( obj );
            }
        }
        protected void updateValue( Object view ) {
            if ( view instanceof RawPhotoView ) {
                RawPhotoView obj = (RawPhotoView) view;
                value = doGetViewValue( obj );
            }
        }
    };

    /**
     Special field controller for handling Color curves.
     */
    class ColorCurveCtrl extends FieldController {
        /**
         * Constructs a new ColorCurveCtrl
         * @param model PhotoInfo objects that form the model
         * @param curveName Name of the generated curve
         */
        public ColorCurveCtrl( Object model, String curveName ) {
            super( "", model );
            this.name = curveName;
        }
        
        final String name;        
        
        protected void setModelValue( Object model ) {
            PhotoInfo obj = (PhotoInfo) model;
            ChannelMapOperationFactory f = getColorMappingFactory( obj );
            if ( f != null ) {
                f.setChannelCurve( name, (ColorCurve) value );
            }
            colorMappingChanged();
        }
        protected Object getModelValue( Object model ) {
            PhotoInfo obj = (PhotoInfo) model;
            ChannelMapOperation cm = obj.getColorChannelMapping();
            ColorCurve ret = null;
            if ( cm != null ) {
                ret = cm.getChannelCurve( name );
            }
            return ret;
        }
        
        protected void updateView( Object view ) {
            if ( view instanceof PhotoInfoView ) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setColorChannelCurve( name, (ColorCurve) value );
            }
        }
        protected void updateViewMultivalueState( Object view ) {
            if ( view instanceof PhotoInfoView ) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setColorChannelMultivalued( name, isMultiValued, 
                        ( valueSet != null ) ? 
                            (ColorCurve[]) valueSet.toArray( new ColorCurve[0] ) : null );
            }
        }
        protected void updateValue( Object view ) {
            if ( view instanceof PhotoInfoView ) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getColorChannelCurve( name );
            }
        }
    };

    
    /**
     initModelFields() initializes the modelFields structure to match the model object.
     It will contain one FieldController object for each fields in the model.
     */
    protected void initModelFields() {
        modelFields.put( PHOTOGRAPHER, new PhotoInfoFieldCtrl( "photographer", photos ) );
        
        modelFields.put( FUZZY_DATE, new PhotoInfoFieldCtrl( "", photos ) {
            protected void setModelValue( ChangePhotoInfoCommand obj ) {
                log.debug( "FUZZY_DATE - setModeValue ({}) " );
                FuzzyDate fd = (FuzzyDate) value;
                if ( fd != null ) {
                    obj.setShootTime( fd.getDate() );
                    obj.setTimeAccuracy( fd.getAccuracy() );
                } else {
                    obj.setShootTime( null );
                    obj.setTimeAccuracy( 0.0 );
                }
            }
            
            protected Object getModelValue( PhotoInfo obj ) {
                log.debug( "FUZZY_DATE - getModeValue ({}) " );
                Date date = obj.getShootTime();
                double accuracy = obj.getTimeAccuracy();
                return new FuzzyDate( date, accuracy );
            }
            
            protected void updateView( Object o ) {
                PhotoInfoView view = (PhotoInfoView) o;
                view.setFuzzyDate( (FuzzyDate) value);                
            }

            protected void updateViewMultivalueState( Object o ) {
                PhotoInfoView view = (PhotoInfoView) o;
                view.setFuzzyDateMultivalued( isMultiValued );
            }

        });
        
        modelFields.put( QUALITY, new PhotoInfoFieldCtrl( "quality", photos ) {
            protected void setModelValue( ChangePhotoInfoCommand obj ) {
                if ( value != null ) {
                    obj.setQuality( ((Number)value).intValue() );
                } else {
                    obj.setQuality( 0 );
                }
            }
        });
        
        
        modelFields.put( SHOOTING_PLACE, new PhotoInfoFieldCtrl( "shootingPlace", photos ) );
        
        modelFields.put( CAMERA_MODEL, new PhotoInfoFieldCtrl( "camera", photos ) );
        modelFields.put( FILM_TYPE, new PhotoInfoFieldCtrl( "film", photos ) );
        
        modelFields.put( LENS_TYPE, new PhotoInfoFieldCtrl( "lens", photos ) );
        
        modelFields.put( DESCRIPTION, new PhotoInfoFieldCtrl( "description", photos ) );
        
        modelFields.put( TECHNOTE, new PhotoInfoFieldCtrl( "techNotes", photos ) );
        
        modelFields.put( F_STOP, new PhotoInfoFieldCtrl( "FStop", photos ) );
        
        modelFields.put( SHUTTER_SPEED, new PhotoInfoFieldCtrl( "shutterSpeed", photos ) );
        
        modelFields.put( FOCAL_LENGTH, new PhotoInfoFieldCtrl( "focalLength", photos ) );
        
        modelFields.put( FILM_SPEED, new PhotoInfoFieldCtrl( "filmSpeed", photos ) );
/*        
 
        modelFields.put( RAW_SETTINGS, new FieldController<PhotoInfo,ChangePhotoInfoCommand>( "rawSettings", photos ) {
            protected void setModelValue( ChangePhotoInfoCommand obj ) {
                obj.setRawSettings( (RawConversionSettings) value );
            }
            protected Object getModelValue( PhotoInfo model ) {
                PhotoInfo obj = (PhotoInfo) model;
                return obj.getRawSettings();
            }
            protected void updateView( Object view ) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setRawSettings( (RawConversionSettings) value );
            }
            protected void updateViewMultivalueState( Object view ) {
                PhotoInfoView obj = (PhotoInfoView) view;
                obj.setRawSettingsMultivalued( isMultiValued );
                
            }
            protected void updateValue( Object view ) {
                PhotoInfoView obj = (PhotoInfoView) view;
                value = obj.getRawSettings();
            }
        });
        
        // Raw setting fields
        
        modelFields.put( RAW_BLACK_LEVEL, new RawSettingFieldCtrl( photos, RAW_BLACK_LEVEL ) {
            protected void doSetModelValue( RawSettingsFactory f, Object newValue ) {
                if ( newValue != null ) {
                    f.setBlack( ((Number)value).intValue() );
                } else {
                    f.setBlack( 0 );
                }
            }
            protected Object doGetModelValue( RawConversionSettings r ) {
                return new Integer( r.getBlack() );
            }
            protected void doSetViewValue( RawPhotoView view ) {
                if ( value != null ) {
                    view.setRawBlack( ((Number)value).intValue() );
                } else {
                    view.setRawBlack( 0 );
                }
            }
            protected Object doGetViewValue( RawPhotoView view ) {
                return new Integer( view.getRawBlack() );
            }
            protected void doSetViewMultivaluedState( RawPhotoView view ) {
                view.setRawBlackMultivalued( isMultiValued, valueSet.toArray() );
            }
        });
        
        modelFields.put( RAW_EV_CORR, new RawSettingFieldCtrl( photos, RAW_EV_CORR ) {
            protected void doSetModelValue( RawSettingsFactory f, Object newValue ) {
                if ( newValue != null ) {
                    f.setEvCorr( ((Number)value).doubleValue() );
                } else {
                    f.setEvCorr( 0 );
                }
            }
            protected Object doGetModelValue( RawConversionSettings r ) {
                return new Double( r.getEvCorr() );
            }
            protected void doSetViewValue( RawPhotoView view ) {
                if ( value != null ) {
                    view.setRawEvCorr( ((Number)value).doubleValue() );
                } else {
                    view.setRawEvCorr( 0.0 );
                }
            }
            protected Object doGetViewValue( RawPhotoView view ) {
                return new Double( view.getRawEvCorr() );
            }
            protected void doSetViewMultivaluedState( RawPhotoView view ) {
                view.setRawEvCorrMultivalued( this.isMultiValued, valueSet.toArray() );
            }
        });
        

        modelFields.put( RAW_HLIGHT_COMP, new RawSettingFieldCtrl( photos, RAW_HLIGHT_COMP ) {
            protected void doSetModelValue( RawSettingsFactory f, Object newValue ) {
                if ( newValue != null ) {
                    f.setHlightComp( ((Number)value).doubleValue() );
                } else {
                    f.setHlightComp( 0 );
                }
            }
            protected Object doGetModelValue( RawConversionSettings r ) {
                return new Double( r.getHighlightCompression() );
            }
            protected void doSetViewValue( RawPhotoView view ) {
                if ( value != null ) {
                    view.setRawHlightComp( ((Number)value).doubleValue() );
                } else {
                    view.setRawHlightComp( 0.0 );
                }
            }
            protected Object doGetViewValue( RawPhotoView view ) {
                return new Double( view.getRawHlightComp() );
            }
            protected void doSetViewMultivaluedState( RawPhotoView view ) {
                view.setRawHlightCompMultivalued( this.isMultiValued, valueSet.toArray() );
            }
        });

        modelFields.put( RAW_CTEMP, new RawSettingFieldCtrl( photos, RAW_CTEMP ) {
            protected void doSetModelValue( RawSettingsFactory f, Object newValue ) {
                if ( newValue != null ) {
                    f.setColorTemp( ((Number)value).doubleValue() );
                } else {
                    f.setColorTemp( 0 );
                }
            }
            protected Object doGetModelValue( RawConversionSettings r ) {
                return new Double( r.getColorTemp() );
            }
            protected void doSetViewValue( RawPhotoView view ) {
                if ( value != null ) {
                    view.setRawColorTemp( ((Number)value).doubleValue() );
                } else {
                    view.setRawColorTemp( 0.0 );
                }
            }
            protected Object doGetViewValue( RawPhotoView view ) {
                return new Double( view.getRawColorTemp() );
            }
            protected void doSetViewMultivaluedState( RawPhotoView view ) {
                view.setRawColorTempMultivalued( this.isMultiValued, valueSet.toArray() );
            }
        });        
                        
        modelFields.put( RAW_GREEN, new RawSettingFieldCtrl( photos, RAW_GREEN ) {
            protected void doSetModelValue( RawSettingsFactory f, Object newValue ) {
                if ( newValue != null ) {
                    f.setGreenGain( ((Number)value).doubleValue() );
                } else {
                    f.setGreenGain( 0 );
                }
            }
            protected Object doGetModelValue( RawConversionSettings r ) {
                return new Double( r.getGreenGain() );
            }
            protected void doSetViewValue( RawPhotoView view ) {
                if ( value != null ) {
                    view.setRawGreenGain( ((Number)value).doubleValue() );
                } else {
                    view.setRawGreenGain( 0.0 );
                }
            }
            protected Object doGetViewValue( RawPhotoView view ) {
                return new Double( view.getRawGreenGain() );
            }
            protected void doSetViewMultivaluedState( RawPhotoView view ) {
                view.setRawGreenGainMultivalued( this.isMultiValued, valueSet.toArray() );
            }
        });        
        
        modelFields.put( RAW_COLOR_PROFILE, new RawSettingFieldCtrl( photos, RAW_COLOR_PROFILE ) {
            protected void doSetModelValue( RawSettingsFactory f, Object newValue ) {
                f.setColorProfile( (ColorProfileDesc) newValue );
            }
            
            protected Object doGetModelValue( RawConversionSettings r ) {
                return r.getColorProfile();
            }
            
            protected void doSetViewValue( RawPhotoView view ) {
                view.setRawProfile( (ColorProfileDesc) value );
            }
            protected Object doGetViewValue( RawPhotoView view ) {
                return view.getRawProfile();
            }
            protected void doSetViewMultivaluedState( RawPhotoView view ) {
                view.setRawProfileMultivalued( this.isMultiValued, valueSet.toArray() );
            }
        });
        
        modelFields.put( COLOR_MAPPING, new FieldController<PhotoInfo,ChangePhotoInfoCommand>( "colorChannelMapping", photos ));
 */       
        /*
         TODO: Yes, this is a hack since preview image is not saved to model
         at any point. Perhaps PhotoInofController should offer a generic
         communication mechanism between views?
         */
        modelFields.put( PREVIEW_IMAGE, new PhotoInfoFieldCtrl( "preview", photos ) {
           protected void setModelValue( ChangePhotoInfoCommand model ) {
               // This is intended only as an information for other views
           } 
            protected Object getModelValue( Object model ) {
                return null;
            }
            protected void updateView( Object view ) {
                if ( view instanceof PreviewImageView ) {
                    PreviewImageView obj = (PreviewImageView) view;
                    obj.modelPreviewImageChanged( (PhotovaultImage) value );
                }
            }
            protected void updateViewMultivalueState( Object view ) {
                // no implementation
            }
            protected void updateValue( Object view ) {
                if ( view instanceof PreviewImageView ) {
                    PreviewImageView obj = (PreviewImageView) view;
                    value = obj.getPreviewImage();
                } else {
                    value = null;
                }
            }
        });
        
/*        
        modelFields.put( COLOR_CURVE_VALUE, new ColorCurveCtrl( photos, "value" ) );
        modelFields.put( COLOR_CURVE_RED, new ColorCurveCtrl( photos, "red" ) );
        modelFields.put( COLOR_CURVE_GREEN, new ColorCurveCtrl( photos, "green" ) );
        modelFields.put( COLOR_CURVE_BLUE, new ColorCurveCtrl( photos, "blue" ) );
        modelFields.put( COLOR_CURVE_SATURATION, new ColorCurveCtrl( photos, "saturation" ) );
*/        
/*
        folderCtrl = new FolderController( photos );
        modelFields.put( PHOTO_FOLDERS, folderCtrl );
*/        
        // TODO: Add other fields
        
        // Init the views in the fields
        Iterator iter = modelFields.values().iterator();
        while( iter.hasNext() ) {
            PhotoInfoFieldCtrl fieldCtrl = (PhotoInfoFieldCtrl) iter.next();
            fieldCtrl.setViews( views );
        }
    }
    
    
    protected PhotoInfo[] photos = null;
    protected boolean isCreatingNew = true;
    
    protected Collection<PhotoInfoView> views = null;
    
    /**
     Sets the PhotoInfo record that will be edited
     @param photo The photoInfo object that is to be edited. If null the a new PhotoInfo record will be created
     */
    public void setPhoto( PhotoInfo photo ) {
        if ( photo != null ) {
            isCreatingNew = false;
        } else {
            isCreatingNew = true;
        }
        this.photos = new PhotoInfo[1];
        photos[0] = photo;
        cmd = new ChangePhotoInfoCommand( photo.getId() );
        for ( ChangePhotoInfoCommand.PhotoInfoFields f : EnumSet.allOf( ChangePhotoInfoCommand.PhotoInfoFields.class ) ) {
            updateViews( null, f );
        }
        // changeModelInFields( isCreatingNew );
    }
    
    /**
     Sets a group of PhotoInfo records that will be edited. If all of the records will have same value for a
     certain field the views will display this value. Otherwise, <code>null</code> is displayed and if the
     value is changed in a view, the new value is updated to all controlled objects.
     */
    public void setPhotos( PhotoInfo[] photos ) {
        this.photos = photos;
        // If we are editing several photos simultaneously we certainly are not creating a new photo...
        isCreatingNew = false;
        List<Integer> photoIds = new ArrayList<Integer>();
        if ( photos != null ) {
            for ( PhotoInfo p : photos ) {
                photoIds.add( p.getId() );
            }
        }
        this.cmd = new ChangePhotoInfoCommand( photoIds );
        for ( ChangePhotoInfoCommand.PhotoInfoFields f : EnumSet.allOf( ChangePhotoInfoCommand.PhotoInfoFields.class ) ) {
            updateViews( null, f );
        }
        // changeModelInFields( isCreatingNew );
    }
    
    /**
     Sets the view that is contorlled by this object
     @param view The controlled view
     */
    public void setView( PhotoInfoView view ) {
        views.clear();
        addView( view );
    }
    
    /**
     Add a new view to those that are controlled by this object.
     TODO: Only the new view should be set to match the model.
     @param view The view to add.
     */
    public void addView( PhotoInfoView view ) {
        views.add( view );
        for ( ChangePhotoInfoCommand.PhotoInfoFields f : EnumSet.allOf( ChangePhotoInfoCommand.PhotoInfoFields.class ) ) {
            updateViews( null, f );
        }
    }
    
    /**
     Sets up the controller to create a new PhotoInfo
     @param imageFile the original image that is to be added to database
     */
    public void createNewPhoto( File imageFile ) {
        setPhoto( null );
        originalFile = imageFile;
        isCreatingNew = true;
    }
    
    /**
     Returns the hotoInfo record that is currently edited.
     */
    public PhotoInfo getPhoto() {
        PhotoInfo photo = null;
        if ( photos != null ) {
            photo = photos[0];
        }
        return photo;
    }
    
    /**
     Get the photos in current model.
     */
    public PhotoInfo[] getPhotos() {
        if ( photos != null ) {
            return photos.clone();
        }
        return null;
    }
    
    public FolderController getFolderController() {
        return folderCtrl;
    }
    
    FolderController folderCtrl;
    
    public ChangePhotoInfoCommand getChangeCommand() {
        return cmd;
    }
    
    /**
     Save the modifications made to the PhotoInfo record
     @throws PhotovaultException if an error occurs during save (most likely 
     the photo was locked by another operation. In this case the transaction 
     is canceled.
     @throws PhotoNotFoundException if the original image cound not be located
     *@deprecated TODO: delete this method
     */
    public void save() throws PhotoNotFoundException, PhotovaultException {
        // Get a transaction context (whole saving operation should be done in a single transaction)
        try {
            // Check if we already have a PhotoInfo object to control
            ChangePhotoInfoCommand changeCmd = null;
            if ( isCreatingNew ) {
                changeCmd = new ChangePhotoInfoCommand();
                isCreatingNew = false;
            } else {
                Integer photoIds[] = new Integer[photos.length];
                for ( int n = 0 ; n < photos.length; n++ ) {
                    photoIds[n] = photos[n].getId();
                }
                changeCmd = new ChangePhotoInfoCommand( photoIds );
            }
            
            // Inform all fields that the modifications should be saved to model
            Iterator fieldIter = modelFields.values().iterator();
            while ( fieldIter.hasNext() ) {
                PhotoInfoFieldCtrl fieldCtrl = (PhotoInfoFieldCtrl) fieldIter.next();
                fieldCtrl.save( changeCmd );
            }
            
            // Update the raw settings if any field affecting them has been changed
            if ( isRawSettingsChanged ) {
                Iterator rawIter = rawFactories.entrySet().iterator();
                while ( rawIter.hasNext() ) {
                    Map.Entry e = (Map.Entry) rawIter.next();
                    PhotoInfo p = (PhotoInfo) e.getKey();
                    RawSettingsFactory f = (RawSettingsFactory) e.getValue();
                    RawConversionSettings r = null;
                    try {
                        r = f.create();
                    } catch (PhotovaultException ex) {
                        ex.printStackTrace();
                    }
                    p.setRawSettings( r );
                }
            }

            // Update the color mapping if any field affecting them has been changed
            if ( isColorMappingChanged ) {
                Iterator colorIter = colorMappingFactories.entrySet().iterator();
                while ( colorIter.hasNext() ) {
                    Map.Entry e = (Map.Entry) colorIter.next();
                    PhotoInfo p = (PhotoInfo) e.getKey();
                    ChannelMapOperationFactory f = (ChannelMapOperationFactory) e.getValue();
                    ChannelMapOperation o = null;
                    o = f.create();
                    p.setColorChannelMapping( o );
                }
            }
        } catch ( LockNotGrantedException e ) {
            throw new PhotovaultException( "Photo locked for other use", e );
        }
    }
    
    /**
     Discards modifications done after last save
     */
    public void discard() {
        setPhotos( photos );
    }
    
    /**
     Adds a new listener that will be notified of events related to this object
     */
    public void addListener( PhotoInfoListener l ) {
    }

    // Fields in PhotoInfo
    public final static String PHOTOGRAPHER = "Photographer";
    public final static String FUZZY_DATE = "Fuzzy date";
    public final static String QUALITY = "Quality";
//     public final static String SHOOTING_DATE = "Shooting date";
//     public final static String TIME_ACCURACY = "Shooting time accuracy";
    public final static String SHOOTING_PLACE = "Shooting place";
    public final static String DESCRIPTION = "Description";
    public final static String TECHNOTE = "Tech note";
    public final static String F_STOP = "F-stop";
    public final static String SHUTTER_SPEED = "Shutter speed";
    public final static String FOCAL_LENGTH = "Focal length";
    public final static String CAMERA_MODEL = "Camera model";
    public final static String FILM_TYPE = "Film type";
    public final static String FILM_SPEED = "Film speed";
    public final static String LENS_TYPE = "Lens type";
    public final static String PHOTO_FOLDERS = "Photo folders";
    public final static String RAW_SETTINGS = "Raw conversion settings";
    public final static String RAW_BLACK_LEVEL = "Raw conversion black level";
    public final static String RAW_EV_CORR = "Raw conversion EV correction";
    public final static String RAW_HLIGHT_COMP = "Raw conversion highlight compression";
    public final static String RAW_CTEMP = "Raw conversion color temperature";
    public final static String RAW_GREEN = "Raw conversion green gain";
    public final static String RAW_COLOR_PROFILE = "Raw conversion ICC profile";
    
    public final static String COLOR_MAPPING = "Color channel mapping";
    public final static String COLOR_CURVE_VALUE = "Value color curve";
    public final static String COLOR_CURVE_RED = "Red color curve";
    public final static String COLOR_CURVE_GREEN = "Green color curve";
    public final static String COLOR_CURVE_BLUE = "Blue color curve";
    public final static String COLOR_CURVE_SATURATION = "Saturation curve";

    public final static String PREVIEW_IMAGE = "Preview image";
    
    protected HashMap modelFields = null;
    
    // The original file that is to be added to database (if we are creating a new PhotoInfo object)
    // If we are editing an existing PhotoInfo record this is null
    File originalFile = null;
    
    public void setField( String field, Object value ) {
        PhotoInfoFieldCtrl fieldCtrl = (PhotoInfoFieldCtrl) modelFields.get( field );
        if ( fieldCtrl != null ) {
            fieldCtrl.setValue( value );
        } else {
            log.warn( "No field " + field );
        }
    }
    
    ChangePhotoInfoCommand cmd;
    public void setField( ChangePhotoInfoCommand.PhotoInfoFields field, Object newValue ) {
        StringBuffer debugMsg = new StringBuffer();
        debugMsg.append( "setField " ).append( field ).append( ": ").append( newValue );
        Set fieldValues = getFieldValues( field ); 
        debugMsg.append( "\nOld values: [");
        boolean first = true;
        for ( Object oldValue : fieldValues ) {
            if ( !first ) debugMsg.append( ", " );
            debugMsg.append( oldValue );
            first = false;
        }
        debugMsg.append( "]" );
        log.debug( debugMsg.toString() );
        cmd.setField( field, newValue );
        for ( PhotoInfoView view : views ) {
            view.setField( field, newValue );
            view.setFieldMultivalued( field, false );
        }
    }
    
    public Set getFieldValues( ChangePhotoInfoCommand.PhotoInfoFields field ) {
        Set values = new HashSet();
        Object value = cmd.getField( field );
        if ( value != null ) {
            values.add( value );
        } else if ( photos != null) {            
            for ( PhotoInfo p : photos ) {
                try {
                    value = PropertyUtils.getProperty( p, field.getName() );
                } catch (Exception ex) {
                    log.error( ex.getMessage() );
                    ex.printStackTrace();
                } 
                values.add( value );
            }
        }
        return values;
    }
    
    private void updateViews( PhotoInfoView src, ChangePhotoInfoCommand.PhotoInfoFields field ) {
        Set values = getFieldValues( field );
        Object value = null;
        boolean isMultivalued = true;
        if ( values.size() == 1 ) {
            value = values.toArray()[0];
            isMultivalued = false;
        }
        for ( PhotoInfoView view : views ) {
            if ( view != src ) {
                view.setField( field, value );
                view.setFieldMultivalued( field, isMultivalued );
            }
        }
    }
    
    public void viewChanged( PhotoInfoView view, ChangePhotoInfoCommand.PhotoInfoFields field, Object newValue ) {
        Set fieldValues = getFieldValues( field );
        if ( fieldValues.size() != 1 || !fieldValues.contains( newValue ) ) {
            cmd.setField( field, newValue );
        }
        updateViews( view, field );
    }
    
    /**
     This method must be called by a view when it has been changed
     @param view The changed view
     @param field The field that has been changed
     @param newValue New value for the field
     @deprecated Use viewChanged( view, field ) instead.
     */
    public void viewChanged( PhotoInfoView view, String field, Object newValue ) {
        PhotoInfoFieldCtrl fieldCtrl = (PhotoInfoFieldCtrl) modelFields.get( field );
        if ( fieldCtrl != null ) {
            fieldCtrl.viewChanged( view, newValue );
        } else {
            log.warn( "No field " + field );
        }
    }
    
    
    
    /**
     This method must be called by a view when it has been changed
     @param view The changed view
     @param field The field that has been changed
     */
    
    public void viewChanged( PhotoInfoView view, String field ) {
        PhotoInfoFieldCtrl fieldCtrl = (PhotoInfoFieldCtrl) modelFields.get( field );
        if ( fieldCtrl != null ) {
            fieldCtrl.viewChanged( view );
        } else {
            log.warn( "No field " + field );
        }
    }
    
    /**
     Returns the current value for a specified field
     @param field The field whose value is to be retrieved
     @return Value of the field or null if fiels is invalid
     */
    
    public Object getField( String field ) {
        Object value = null;
        PhotoInfoFieldCtrl fieldCtrl = (PhotoInfoFieldCtrl) modelFields.get( field );
        if ( fieldCtrl != null ) {
            value = fieldCtrl.getValue();
        }
        return value;
    }
    
    HashMap rawFactories = new HashMap();
    
    private RawSettingsFactory getRawSettingsFactory( PhotoInfo p ) {
        RawSettingsFactory f = null;
        if ( rawFactories.containsKey( p ) ) {
            f = (RawSettingsFactory) rawFactories.get( p );
        } else {
            RawConversionSettings r = p.getRawSettings();
            f = new RawSettingsFactory( r );
            if ( r != null ) {
                rawFactories.put( p, f );
            }
        }
        return f;
    }
    
    HashMap colorMappingFactories = new HashMap();
    
    private ChannelMapOperationFactory getColorMappingFactory( PhotoInfo p ) {
        ChannelMapOperationFactory f = null;
        if ( colorMappingFactories.containsKey( p ) ) {
            f = (ChannelMapOperationFactory) colorMappingFactories.get( p );
        } else {
            ChannelMapOperation r = p.getColorChannelMapping();
            f = new ChannelMapOperationFactory( r );
            colorMappingFactories.put( p, f );
        }
        return f;        
    }
    
    boolean isRawSettingsChanged = false;
    
    private void rawSettingsChanged() {
        isRawSettingsChanged = true;
    }
    
    boolean isColorMappingChanged = false;
    
    private void colorMappingChanged() {
        isColorMappingChanged = true;
    }
}
