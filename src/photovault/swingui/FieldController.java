// FieldController

package photovault.swingui;

import java.lang.*;
import java.util.*;

/**
   FieldController is an abstract helper class for implementing the controller logic and mapping between
   controller and model classes. It describes a certain of a model object to controller and keeps
   track of whether the field has been modified and how to set and get the values for the field
*/

public abstract class FieldController {

    /** Constructor
	@param model The object whose field is controlled
    */
    public FieldController( Object model ) {
	this.model = model;
	if ( model != null ) {
	    value = getModelValue();
	} 
    }

    
    /**
       Sets the collection of views that this controller updates. Note that the collection is not owned by
       FieldController.
       @param views Collection that contains the views that must be updated according to this field
    */
    public void setViews( Collection views ) {
	this.views = views;
    }
    
    /**
       Returns true if the field has been modified after last save
    */
    public boolean isModified() {
	return modified;
    }

    /**
       Sets a new model for the object. It can be controlled whether the current state of controller is preserved
       or whether the controller state is read from the new model
       @param model The new model
       @param preserveState if true, preserve controller state - if save() is called later, the state will
       be written to the new model. This is useful when starting with null model (if it has not yet been created).
       If false, the controller state is changed to match the new model.
    */
    public void setModel( Object model, boolean preserveState ) {
	this.model = model;

	if ( preserveState ) {
	    // We are copying the state of this controller to the new model. Set the modified flag to indicate
	    // that the state must be saved.
	    modified = true;
	} else {
	    // Update value from the model
	    if ( model != null ) {
		value = getModelValue();
	    } else {
		value = null;
	    }
	    // Controller mathces model set flag accordingly
	    modified = false;
	}
	
	// Update info in all views
	updateViews( null );
    }

    /**
       Sets a new model for the object and discard any changes made after the field was last saved
       @param model The new model
    */
    public void setModel( Object model ) {
	setModel( model, false );
    }
	

    /**
       Sets a new value for the field controlled by this object
    */
    public void setValue( Object newValue ) {
	value = newValue;
	modified = true;
	// Update all associated views
	updateViews( null );
    }

    /**
       Inform the Field controller that associated view has changed the field value. Acts similarly
       as the setValue but does not inform the view that changed the information.
       @param newValue The new value
       @param view View that initiated the value change
    */
    public void viewChanged( Object view, Object newValue ) {
	value = newValue;
	modified = true;
	updateViews( view );
    }
	
    
    /**
       Returns the current value that would be set by this object to the model if saved now
    */
    public Object getValue() {
	return value;
    }

    /**
       Save any changes to the model
    */
    public void save() {
	if ( model != null ) {
	    if ( modified ) {
		setModelValue();
	    }
	    modified = false;
	}
    }

    
    protected void updateViews( Object source ) {
	System.err.println( "Updating views" );
	if ( views == null ) {
	    System.err.println( " no views!" );
	    return;
	}
	Iterator iter = views.iterator();
	while ( iter.hasNext() ) {
	    Object view = iter.next();
	    System.err.println( " view found" );
	    if ( view != source ) {
		System.err.println( "  update view" );
		updateView( view );
	    }
	}
    }
	    
    
    /**
       This abstract method must be overridden in derived classes to set the value stored in value to
       the controlled field in model.
    */
    protected abstract void setModelValue();

    /** This abstract method must be overridden in derived classes to return current value of the model's
	field that is controlled by this object.
    */
    protected abstract Object getModelValue();

    /** This abstract method must be overridden to update the associated view.
     */
    protected abstract void updateView( Object view );

    protected Object model;
    protected Object value;
    protected Collection views = null;
    protected boolean modified = false;       
}
       
	
