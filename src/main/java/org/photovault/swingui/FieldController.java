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
	this.model = new Object[1];
	this.model[0] = model;
	if ( model != null ) {
	    value = getModelValue( model );
	} 
    }

    public FieldController( Object[] model ) {
	setModel( model, false );
    }
    
    /**
       Sets the collection of views that this controller updates. Note that the collection is not owned by
       FieldController. Therefore, if the views collection is changed the changer must call updateAllViews
       to get the new views updated to match the state of controller!!!
       @param views Collection that contains the views that must be updated according to this field
    */
    public void setViews( Collection views ) {
	this.views = views;
	updateViews( null );
    }

    public void updateAllViews() {
	updateViews( null );
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
	Object[] modelArr = new Object[1];
	modelArr[0] = model;
	setModel( modelArr, preserveState );
    }

    /**
       Sets a new model for the object and discard any changes made after the field was last saved
       @param model The new model
    */
    public void setModel( Object model ) {
	setModel( model, false );
    }

    /**
       Sets a model that consists of several identical obejcts. If all the objects have an equal value
       the model value will be the same. However, if the value differs in some of the objects
       the controller value will be null until the controller is modified.
    */
    public void setModel( Object[] model, boolean preserveState ) {
	this.model = model;
        valueSet = new HashSet();
	if ( preserveState ) {
	    // We are copying the state of this controller to the new model. Set the modified flag to indicate
	    // that the state must be saved.
	    modified = true;
	} else {
	    // Check the value of all objects in the model. If these are all equal save it as the model value
	    isMultiValued = false;
	    Object valueCandidate = null;
	    if ( model != null ) {
		if ( model[0] != null ) {
		    valueCandidate = getModelValue( model[0] );
                    valueSet.add( valueCandidate );
		}
		for ( int n = 1; n < model.length; n++ ) {
		    Object modelObjectValue = getModelValue( model[n] );
                    if ( !valueSet.contains( modelObjectValue ) ) {
                        valueSet.add( modelObjectValue );
                    }
		    if ( modelObjectValue != null ) {
			if ( !modelObjectValue.equals( valueCandidate ) ) {
			    isMultiValued = true;
			}
		    } else if ( valueCandidate != null ) {
			isMultiValued = true;

		    }
		}
	    }
	    
	    if ( isMultiValued ) {
		value = null;
	    } else {
		value = valueCandidate;
	    }
	    
	    // Controller mathces model set flag accordingly
	    modified = false;
	    // Update info in all views
	    updateViews( null );
	}
    }

    
    /**
       Sets a new value for the field controlled by this object
    */
    public void setValue( Object newValue ) {
	value = newValue;
	isMultiValued = false;
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

    public void viewChanged( Object view ) {
	modified = true;
	updateValue( view );
	isMultiValued = false;
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
		for ( int n = 0; n < model.length; n++ ) {
		    if ( model[n] != null ) {
			setModelValue( model[n] );
		    }
		}
	    }
	    modified = false;
	}
    }

    
    protected void updateViews( Object source ) {
	if ( views == null ) {
	    return;
	}
	Iterator iter = views.iterator();
	while ( iter.hasNext() ) {
	    Object view = iter.next();
	    if ( view != source ) {
		updateView( view );
	    }
	    // The multivalueState is upated anyway since it cannot be determined
	    // the view
	    updateViewMultivalueState( view );
	}
    }
	    
    
    /**
       This abstract method must be overridden in derived classes to set the value stored in value to
       the controlled field in model.
    */
    protected abstract void setModelValue( Object modelObject );

    /** This abstract method must be overridden in derived classes to return current value of the model's
	field that is controlled by this object.
    */
    protected abstract Object getModelValue( Object modelObject );

    /** This abstract method must be overridden to update the associated view.
     */
    protected abstract void updateView( Object view );

    /**a
       This abstract method must be overridden to update the contorller value from the view
    */
    protected abstract void updateValue( Object view );

    /** This abstract method must be overridden to update view when isMultiValued changes */
    protected abstract void updateViewMultivalueState( Object view );

    /**
       Array of th eobjects that comprise the model
    */
    protected Object[] model = null;
    protected Object value;
    protected Set valueSet = new HashSet();
    protected Collection views = null;
    protected boolean modified = false;       
    protected boolean isMultiValued = false;
}
       
	
