// FieldController

package photovault.swingui;

import java.lang.*;

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
	value = getModelValue();
    }

    /**
       Returns true if the field has been modified after last save
    */
    public boolean isModified() {
	return modified;
    }

    /**
       Sets a new model for the object and discard any changes made after the field was last saved
    */
    public void setModel( Object model ) {
	this.model = model;
	modified = false;
    }

    /**
       Sets a new value for the field controlled by this object
    */
    public void setValue( Object newValue ) {
	value = newValue;
	modified = true;
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
	if ( modified ) {
	    setModelValue();
	}
	modified = false;
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

    protected Object model;
    protected Object value;
    protected boolean modified = false;       
}
       
	
