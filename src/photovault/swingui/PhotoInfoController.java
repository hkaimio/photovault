// PhotoInfoController.java

package photovault.swingui;

import java.util.*;
import java.io.*;
import imginfo.*;

/**
   PhotoInfoController contains the application logic for creating and editing PhotoInfo records in database,
   i.e. it implements the controller role in MVC pattern.
*/

public class PhotoInfoController {

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
	modelFields.put( PHOTOGRAPHER, new FieldController( photo ) {
		protected void setModelValue() {
		    PhotoInfo obj = (PhotoInfo) model;
		    obj.setPhotographer( (String) value );
		}
		protected Object getModelValue() {
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
	    });

	modelFields.put( SHOOTING_DATE, new FieldController( photo ) {
		protected void setModelValue() {
		    PhotoInfo obj = (PhotoInfo) model;
		    obj.setShootTime( (Date) value );
		}
		protected Object getModelValue() {
		    PhotoInfo obj = (PhotoInfo) model;
		    return obj.getShootTime();
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setShootTime( (Date) value );
		}
	    });

	modelFields.put( SHOOTING_PLACE, new FieldController( photo ) {
		protected void setModelValue() {
		    PhotoInfo obj = (PhotoInfo) model;
		    obj.setShootingPlace( (String) value );
		}
		protected Object getModelValue() {
		    PhotoInfo obj = (PhotoInfo) model;
		    return obj.getShootingPlace();
		}
		protected void updateView( Object view ) {
		    PhotoInfoView obj = (PhotoInfoView) view;
		    obj.setShootPlace( (String) value );
		}
	    });

	// TODO: Add other fields

	// Init the views in the fields
	Iterator iter = modelFields.values().iterator();
	while( iter.hasNext() ) {
	    FieldController fieldCtrl = (FieldController) iter.next();
	    fieldCtrl.setViews( views );
	}
    }    
	

    protected PhotoInfo photo = null;
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
 	this.photo = photo;
	
	changeModelInFields( isCreatingNew );
    }

    /**
       Sets the view that is contorlled by this object
       @param view The controlled view
    */
    public void setView( PhotoInfoView view ) {
	views.clear();
	views.add( view );
    }

    protected void changeModelInFields( boolean preserveFieldState ) {
	// Inform all fields that the model has changed
	Iterator fieldIter = modelFields.values().iterator();
	while ( fieldIter.hasNext() ) {
	    FieldController fieldCtrl = (FieldController) fieldIter.next();
	    // If we are creating a new photo we will copy the current field values to it.
	    fieldCtrl.setModel( photo, preserveFieldState );
	}
    }

    /**
       Sets up the controller to create a new PhotoInfo
       @imageFile the original image that is to be added to database
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
 	return photo;
    }

    /**
       Save the modifications made to the PhotoInfo record
       @throws PhotoNotFoundException if the original image cound not be located
    */
    public void save() throws PhotoNotFoundException {
	// Check if we already have a PhotoInfo object to control
	if ( isCreatingNew ) {
	    if ( originalFile != null ) {
		photo = PhotoInfo.addToDB( originalFile );
	    } else {
		photo = PhotoInfo.create();
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
	if ( photo != null ) {
	    photo.updateDB();
	}
    }

    /**
       Discards modifications done after last save
    */
    public void discard() {
	// Setting the model again causes all fields to reread field values from model
	Iterator fieldIter = modelFields.values().iterator();
	while ( fieldIter.hasNext() ) {
	    FieldController fieldCtrl = (FieldController) fieldIter.next();
	    fieldCtrl.setModel( photo );
	}
    }

    /**
       Adds a new listener that will be notified of events related to this object
    */
    public void addListener( PhotoInfoListener l ) {
    }

    // Fields in PhotoInfo
    public final static String PHOTOGRAPHER = "Photographer";
    public final static String SHOOTING_DATE = "Shooting date";
    public final static String SHOOTING_PLACE = "Shooting place";
    public final static String DESCRIPTION = "Description";
    public final static String F_STOP = "F-stop";
    public final static String FOCAL_LENGTH = "Focal length";

    protected HashMap modelFields = null;

    // The original file that is to be added to database (if we are creating a new PhotoInfo object)
    // If we are editing an existing PhotoInfo record this is null 
    File originalFile = null;

    public void setField( String field, Object value ) {
	System.err.println( "Set field " + field + ": " + value.toString() );
	FieldController fieldCtrl = (FieldController) modelFields.get( field );
	if ( fieldCtrl != null ) {
	    fieldCtrl.setValue( value );
	} else {
	    System.err.println( "No field " + field );
	}
    }

    /**
       This method must be called by a view when it has been changed
       @param view The changed view
       @param field The field that has been changed
       @param newValue New value for the field       
    */
    public void viewChanged( PhotoInfoView view, String field, Object newValue ) {
	FieldController fieldCtrl = (FieldController) modelFields.get( field );
	if ( fieldCtrl != null ) {
	    fieldCtrl.viewChanged( view, newValue );
	} else {
	    System.err.println( "No field " + field );
	}
    }
	
    public Object getField( String field ) {
	Object value = null;
	FieldController fieldCtrl = (FieldController) modelFields.get( field );
	if ( fieldCtrl != null ) {
	    value = fieldCtrl.getValue();
	}
	return value;
    }
    
}
