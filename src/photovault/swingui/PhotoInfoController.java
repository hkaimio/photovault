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
    }

    protected PhotoInfo photo = null;
    
    /**
       Sets the PhotoInfo record that will be edited
       @param photo The photoInfo object that is to be edited. If null the a new PhotoInfo record will be created     
    */
    public void setPhoto( PhotoInfo photo ) {
 	this.photo = photo;
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
    }

    /**
       Discards modifications done after last save
    */
    public void discard() {
    }

    /**
       Adds a new listener that will be notified of events related to this object
    */
    public void addListener( PhotoInfoListener l ) {
    }

    // Fields in PhotoInfo
    public final  String PHOTOGRAPHER = "Photographer";
    public final String SHOOTING_DATE = "Shooting date";
    public final String SHOOTING_PLACE = "Shooting place";
    public final String DESCRIPTION = "Description";

    protected HashMap modelFields = null;
    
    public void setField( String field, Object value ) {
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
