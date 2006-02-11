// PhotoInfoController.java

package photovault.swingui;

import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.imginfo.FuzzyDate;
import java.util.*;
import java.io.*;
import org.photovault.imginfo.*;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoNotFoundException;
import photovault.swingui.folderpane.FolderController;
import org.apache.log4j.Logger;


/**
   PhotoInfoController contains the application logic for creating and editing PhotoInfo records in database,
   i.e. it implements the controller role in MVC pattern.
*/

public class PhotoInfoController {

    static Logger log = Logger.getLogger( PhotoInfoController.class.getName() );

    /**
       Default constructor
    */
    public PhotoInfoController() {
	modelFields = new HashMap();
	views = new Vector();
	initModelFields();
    }

    /**
       initModelFields() initializes the modelFields structure to match the model object.
       It will contain one FieldController object for each fields in the model.
    */
    protected void initModelFields() {
	modelFields.put( PHOTOGRAPHER, new FieldController( photos ) {
		protected void setModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    obj.setPhotographer( (String) value );
		}
		protected Object getModelValue( Object model ) {
		    if ( model == null ) {
			return null;
		    }
		    PhotoInfo obj = (PhotoInfo) model;
		    return obj.getPhotographer();
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setPhotographer( (String) value );
		}

		protected void updateViewMultivalueState( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setPhotographerMultivalued( isMultiValued );

		}

		protected void updateValue( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    value = obj.getPhotographer();
		}
	    });

	modelFields.put( FUZZY_DATE, new FieldController( photos ) {
		protected void setModelValue( Object model ) {
		    log.debug( "FUZZY_DATE - setModeValue ({}) " );
		    PhotoInfo obj = (PhotoInfo) model;
		    FuzzyDate fd = (FuzzyDate) value;
		    if ( fd != null ) {
			obj.setShootTime( fd.getDate() );
			obj.setTimeAccuracy( fd.getAccuracy() );
		    } else {
			obj.setShootTime( null );
			obj.setTimeAccuracy( 0 );
		    }
		}
		protected Object getModelValue( Object model ) {
		    log.debug( "FUZZY_DATE - getModeValue ({}) " );
		    PhotoInfo obj = (PhotoInfo) model;
		    Date date = obj.getShootTime();
		    double accuracy = obj.getTimeAccuracy();
		    return new FuzzyDate( date, accuracy );
		}
		protected void updateView( Object view ) {
		    log.debug( "FUZZY_DATE - updateView ({}) " );
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setFuzzyDate( (FuzzyDate) value );
		}
		protected void updateViewMultivalueState( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setFuzzyDateMultivalued( isMultiValued );

		}
		protected void updateValue( Object view ) {
		    log.debug( "FUZZY_DATE - updateValue ({}) " );
		    PhotoInfoView obj = (PhotoInfoView) view;
		    value = obj.getFuzzyDate();
		}
	    });

	modelFields.put( QUALITY, new FieldController( photos ) {
		protected void setModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    if ( value != null ) {
			obj.setQuality( ((Number)value).intValue() );
		    } else {
			obj.setQuality( 0 );
		    }
		}
		protected Object getModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    return new Double( obj.getQuality() );
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setQuality( (Number)value );
		}
		protected void updateViewMultivalueState( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setQualityMultivalued( isMultiValued );

		}
		protected void updateValue( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    value =  obj.getQuality();
		}
	    });

	
	modelFields.put( SHOOTING_PLACE, new FieldController( photos ) {
		protected void setModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    obj.setShootingPlace( (String) value );
		}
		protected Object getModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    return obj.getShootingPlace();
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setShootPlace( (String) value );
		}
		protected void updateViewMultivalueState( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setShootPlaceMultivalued( isMultiValued );

		}
		protected void updateValue( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    value = obj.getShootPlace();
		}
	    });

	modelFields.put( CAMERA_MODEL, new FieldController( photos ) {
		protected void setModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    obj.setCamera( (String) value );
		}
		protected Object getModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    return obj.getCamera();
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setCamera( (String) value );
		}
		protected void updateViewMultivalueState( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setCameraMultivalued( isMultiValued );

		}
		protected void updateValue( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    value = obj.getCamera();
		}
	    });

	modelFields.put( FILM_TYPE, new FieldController( photos ) {
		protected void setModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    obj.setFilm( (String) value );
		}
		protected Object getModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    return obj.getFilm();
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setFilm( (String) value );
		}
		protected void updateViewMultivalueState( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setFilmMultivalued( isMultiValued );

		}
		protected void updateValue( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    value = obj.getFilm();
		}
	    });

	modelFields.put( LENS_TYPE, new FieldController( photos ) {
		protected void setModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    obj.setLens( (String) value );
		}
		protected Object getModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    return obj.getLens();
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setLens( (String) value );
		}
		protected void updateViewMultivalueState( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setLensMultivalued( isMultiValued );

		}
		protected void updateValue( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    value = obj.getLens();
		}
	    });

	modelFields.put( DESCRIPTION, new FieldController( photos ) {
		protected void setModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    obj.setDescription( (String) value );
		}
		protected Object getModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    return obj.getDescription();
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setDescription( (String) value );
		}
		protected void updateViewMultivalueState( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setDescriptionMultivalued( isMultiValued );

		}
		protected void updateValue( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    value = obj.getDescription();
		}
	    });

	modelFields.put( TECHNOTE, new FieldController( photos ) {
		protected void setModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    obj.setTechNotes( (String) value );
		}
		protected Object getModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    return obj.getTechNotes();
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setTechNote( (String) value );
		}
		protected void updateViewMultivalueState( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setTechNoteMultivalued( isMultiValued );

		}
		protected void updateValue( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    value = obj.getTechNote();
		}
	    });



	
	modelFields.put( F_STOP, new FieldController( photos ) {
		protected void setModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    if ( value != null ) {
			obj.setFStop( ((Number)value).doubleValue() );
		    } else {
			obj.setFStop( 0 );
		    }
		}
		protected Object getModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    return new Double( obj.getFStop() );
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setFStop( (Number)value );
		}
		protected void updateViewMultivalueState( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setFStopMultivalued( isMultiValued );

		}
		protected void updateValue( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    value =  obj.getFStop();
		}
	    });

	modelFields.put( SHUTTER_SPEED, new FieldController( photos ) {
		protected void setModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    if ( value != null ) {
			obj.setShutterSpeed( ((Number)value).doubleValue() );
		    } else {
			obj.setShutterSpeed( 0 );
		    }
		}
		protected Object getModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    return new Double( obj.getShutterSpeed() );
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setShutterSpeed( (Number)value );
		}
		protected void updateViewMultivalueState( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setShutterSpeedMultivalued( isMultiValued );

		}
		protected void updateValue( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    value =  obj.getShutterSpeed();
		}
	    });

	modelFields.put( FOCAL_LENGTH, new FieldController( photos ) {
		protected void setModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    if ( value != null ) {
			obj.setFocalLength( ((Number)value).doubleValue() );
		    } else {
			obj.setFocalLength( 0 );
		    }
		}
		protected Object getModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    return new Double( obj.getFocalLength() );
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setFocalLength( (Number)value );
		}
		protected void updateViewMultivalueState( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setFocalLengthMultivalued( isMultiValued );

		}
		protected void updateValue( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    value =  obj.getFocalLength(); 
		}
	    });

	modelFields.put( FILM_SPEED, new FieldController( photos ) {
		protected void setModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    if ( value != null ) {
			obj.setFilmSpeed( ((Number)value).intValue() );
		    } else {
			obj.setFilmSpeed( 0 );
		    }
		}
		protected Object getModelValue( Object model ) {
		    PhotoInfo obj = (PhotoInfo) model;
		    return new Double( obj.getFilmSpeed() );
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setFilmSpeed( (Number)value );
		}
		protected void updateViewMultivalueState( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setFilmSpeedMultivalued( isMultiValued );

		}
		protected void updateValue( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    value =  obj.getFilmSpeed(); 
		}
	    });

	folderCtrl = new FolderController( photos );
	modelFields.put( PHOTO_FOLDERS, folderCtrl );
	
	// TODO: Add other fields

	// Init the views in the fields
	Iterator iter = modelFields.values().iterator();
	while( iter.hasNext() ) {
	    FieldController fieldCtrl = (FieldController) iter.next();
	    fieldCtrl.setViews( views );
	}
    }    
	

    protected PhotoInfo[] photos = null;
    protected boolean isCreatingNew = true;

    protected Collection views = null;
    
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
	
	changeModelInFields( isCreatingNew );
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
	changeModelInFields( isCreatingNew );
    }
    
    /**
       Sets the view that is contorlled by this object
       @param view The controlled view
    */
    public void setView( PhotoInfoView view ) {
	views.clear();
	views.add( view );
	// Inform all field controllers that the views collection has changed
	Iterator iter = modelFields.values().iterator();
	while( iter.hasNext() ) {
	    FieldController fieldCtrl = (FieldController) iter.next();
	    fieldCtrl.updateAllViews();
	}
	
    }

    protected void changeModelInFields( boolean preserveFieldState ) {
	// Inform all fields that the model has changed
	Iterator fieldIter = modelFields.values().iterator();
	while ( fieldIter.hasNext() ) {
	    FieldController fieldCtrl = (FieldController) fieldIter.next();
	    // If we are creating a new photo we will copy the current field values to it.
	    fieldCtrl.setModel( photos, preserveFieldState );
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

    public FolderController getFolderController() {
	return folderCtrl;
    }

    FolderController folderCtrl;
    
    /**
       Save the modifications made to the PhotoInfo record
       @throws PhotoNotFoundException if the original image cound not be located
    */
    public void save() throws PhotoNotFoundException {
	// Get a transaction context (whole saving operation should be done in a single transaction)
	ODMGXAWrapper txw = new ODMGXAWrapper();

	// Check if we already have a PhotoInfo object to control
	if ( isCreatingNew ) {
	    photos = new PhotoInfo[1];
	    if ( originalFile != null ) {
		photos[0] = PhotoInfo.addToDB( originalFile );
	    } else {
		photos[0] = PhotoInfo.create();
	    }
		
	    // Set the model to all fields but preserve field state so that it is changed to the photoInfo
	    // object
	    changeModelInFields( true );
	    isCreatingNew = false;
	}
	
	// Inform all fields that the modifications should be saved to model
	Iterator fieldIter = modelFields.values().iterator();
	while ( fieldIter.hasNext() ) {
	    FieldController fieldCtrl = (FieldController) fieldIter.next();
	    fieldCtrl.save();
	}
	txw.commit();
    }

    /**
       Discards modifications done after last save
    */
    public void discard() {
	// Setting the model again causes all fields to reread field values from model
	Iterator fieldIter = modelFields.values().iterator();
	while ( fieldIter.hasNext() ) {
	    FieldController fieldCtrl = (FieldController) fieldIter.next();
	    fieldCtrl.setModel( photos, false );
	}
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


    protected HashMap modelFields = null;

    // The original file that is to be added to database (if we are creating a new PhotoInfo object)
    // If we are editing an existing PhotoInfo record this is null 
    File originalFile = null;

    public void setField( String field, Object value ) {
	FieldController fieldCtrl = (FieldController) modelFields.get( field );
	if ( fieldCtrl != null ) {
	    fieldCtrl.setValue( value );
	} else {
	    log.warn( "No field " + field );
	}
    }

    /**
       This method must be called by a view when it has been changed
       @param view The changed view
       @param field The field that has been changed
       @param newValue New value for the field       
       @deprecated Use viewChanged( view, field ) instead.
    */
    public void viewChanged( PhotoInfoView view, String field, Object newValue ) {
	FieldController fieldCtrl = (FieldController) modelFields.get( field );
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
	FieldController fieldCtrl = (FieldController) modelFields.get( field );
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
	FieldController fieldCtrl = (FieldController) modelFields.get( field );
	if ( fieldCtrl != null ) {
	    value = fieldCtrl.getValue();
	}
	return value;
    }
    
}
