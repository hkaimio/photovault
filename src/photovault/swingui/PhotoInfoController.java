// PhotoInfoController.java

package photovault.swingui;

import java.util.*;
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

	// TODO: Add other fields
    }    
	

    protected PhotoInfo photo = null;
    
    /**
       Sets the PhotoInfo record that will be edited
       @param photo The photoInfo object that is to be edited. If null the a new PhotoInfo record will be created     
    */
    public void setPhoto( PhotoInfo photo ) {
	boolean wasCreatingNewPhoto = ( this.photo == null );
 	this.photo = photo;

	// Inform all fields that the model has changed
	Iterator fieldIter = modelFields.values().iterator();
	while ( fieldIter.hasNext() ) {
	    FieldController fieldCtrl = (FieldController) fieldIter.next();
	    fieldCtrl.setModel( photo );
	}
    }

    /**
       Returns the hotoInfo record that is currently edited.
    */
    public PhotoInfo getPhoto() {
 	return photo;
    }

    /**
       Save the modifications made to the PhotoInfo record
    */
    public void save() {
	// Inform all fields that the modifications should be saved to model
	Iterator fieldIter = modelFields.values().iterator();
	while ( fieldIter.hasNext() ) {
	    FieldController fieldCtrl = (FieldController) fieldIter.next();
	    fieldCtrl.save();
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

    protected HashMap modelFields = null;
    
    public void setField( String field, Object value ) {
	System.err.println( "Set field " + field + ": " + value.toString() );
	FieldController fieldCtrl = (FieldController) modelFields.get( field );
	if ( fieldCtrl != null ) {
	    fieldCtrl.setValue( value );
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
