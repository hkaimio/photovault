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

import java.beans.PropertyDescriptor;
import java.lang.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import org.apache.commons.beanutils.PropertyUtils;

/**
   FieldController is an abstract helper class for implementing the controller logic and mapping between
   controller and model classes. It describes a certain of a model object to controller and keeps
   track of whether the field has been modified and how to set and get the values for the field
*/

public class FieldController<T, CHANGE_CMD> {

    /** Constructor
	@param model The object whose field is controlled
    */
    public FieldController( String field, T model ) {
	this.model = new ArrayList();
        this.field = field;
	this.model.add( model );
	if ( model != null ) {
	    value = getModelValue( model );
	} 
    }

    public FieldController( String field, T[] model ) {
	this.field = field;
        List<T> m = null;
        if ( model != null ) {
            m = new ArrayList<T>( model.length );
            for ( T o : model ) {
                m.add( o );
            }
        }
        setModel( m, false );
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
    public void setModel( T model, boolean preserveState ) {
	List<T> modelArr = new ArrayList<T>();
	modelArr.add( model );
	setModel( modelArr, preserveState );
    }

    /**
       Sets a new model for the object and discard any changes made after the field was last saved
       @param model The new model
    */
    public void setModel( T model ) {
	setModel( model, false );
    }

    /**
       Sets a model that consists of several obejcts. If all the objects have an equal value
       the model value will be the same. However, if the value differs in some of the objects
       the controller value will be null until the controller is modified.
    */
    public void setModel( List<T> model, boolean preserveState ) {
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
		if ( model.get( 0 ) != null ) {
		    valueCandidate = getModelValue( model.get( 0 ) );
                    valueSet.add( valueCandidate );
		}
		for ( T o : model ) {
		    Object modelObjectValue = getModelValue( o );
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
      @param cmd The {@link Command} object used for changing the models
    */
    public void save( CHANGE_CMD cmd ) {
        if ( model != null ) {
            if ( modified ) {
                setModelValue( cmd );
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
       the command used for changing model
     *@param cmd The {@link Command} object used for setting new value for model
    */
    protected void setModelValue( CHANGE_CMD cmd ) {
        if ( cmd == null ) {
            return;
        }
        try {
            PropertyUtils.setProperty( cmd, field, value );
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
    }

    /** This abstract method must be overridden in derived classes to return current value of the model's
	field that is controlled by this object.
    */
    protected Object getModelValue( Object modelObject ) {
        if ( modelObject == null ) {
            return null;
        }
        try {
            return PropertyUtils.getProperty( modelObject, field );
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    /** This abstract method must be overridden to update the associated view.
     */
    protected void updateView( Object view ) {
        try {
            PropertyUtils.setProperty( view, field, value );        
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }        
    }

    /**a
       This abstract method must be overridden to update the contorller value from the view
    */
    protected void updateValue( Object view ) {
        try {
            value = PropertyUtils.getProperty( view, field );        
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }        
    };

    /** This abstract method must be overridden to update view when isMultiValued changes */
    protected void updateViewMultivalueState( Object view ) {
        try {
            PropertyUtils.setProperty( view, field + "Multivalued", isMultiValued );                
        } catch (InvocationTargetException ex) {
            ex.printStackTrace();
        } catch (NoSuchMethodException ex) {
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            ex.printStackTrace();
        }                
    };

    /**
       Array of th eobjects that comprise the model
    */
    protected List<T> model = null;
    protected String field = null;
    protected Object value;
    protected Set valueSet = new HashSet();
    protected Collection views = null;
    protected boolean modified = false;       
    protected boolean isMultiValued = false;
}
       
	
