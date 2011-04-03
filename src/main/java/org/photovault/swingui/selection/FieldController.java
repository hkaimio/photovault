/*
  Copyright (c) 2011 Harri Kaimio

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

package org.photovault.swingui.selection;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.imginfo.PhotoInfo;

/**
 * Java bean to represent a multivalued text field in selection model.
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class FieldController<T> {
    private static Log log = LogFactory.getLog(  FieldController.class );
    private String property;
    private T value;
    private boolean isMultivalued = false;
    private PhotoSelectionController parentCtrl;
    private List<T> values;
    private List<T> uniqueValues = new ArrayList<T>();
    private PropertyChangeSupport propertySupport;

    /**
     * Constructory
     * @param parentCtrl The parent controller
     * @param propName Name of the property represented
     */
    public FieldController( PhotoSelectionController parentCtrl, String propName ) {
        this.parentCtrl = parentCtrl;
        this.property = propName;
        propertySupport = new PropertyChangeSupport( this );
        values = new ArrayList();
    }

    /**
     * Returns the property name
     * @return
     */
    public String getPropertyName() {
        return property;
    }

    /**
     * Returns the current value of the property
     * @return
     */
    public T getValue() {
        return value;
    }

    /**
     * Set value of the property
     * @param newValue
     */
    public void setValue( T newValue ) {
        T oldValue = this.value;
        boolean wasMultivalued = isMultivalued;
        value =  newValue;
        isMultivalued = false;
        parentCtrl.setField( property, newValue );
        propertySupport.firePropertyChange( "value", oldValue, newValue);
        propertySupport.firePropertyChange( "isMultivalued", wasMultivalued, isMultivalued );
    }

    /**
     * Get the ith value from the unique values if the property has several values
     * among selected objects
     *
     * @param i
     * @return
     */
    public T getUniqueValue( int i ) {
        return uniqueValues.get( i );
    }

    public int getUniqueValueCount() {
        return uniqueValues.size();
    }

    /**
     * Is the property having multiple values?
     * @return
     */
    public boolean isMultivalued() {
        return isMultivalued;
    }

    /**
     * Set photos in the model
     * @param photos
     */
    public void setPhotos( PhotoInfo[] photos ) {
            values.clear();
        uniqueValues.clear();
        boolean haveNulls = false;

        SortedSet<T> uniques = new TreeSet();
        T oldValue = value;
        boolean wasMultivalued = isMultivalued;
        if ( photos != null ) {
            for ( PhotoInfo p : photos ) {
                T propVal = null;
                try {
                    propVal = (T) PropertyUtils.getProperty( p, property );
                } catch ( IllegalAccessException ex ) {
                    log.error( ex );
                } catch ( InvocationTargetException ex ) {
                    log.error( ex );
                } catch ( NoSuchMethodException ex ) {
                    log.error( ex );
                }
                values.add( propVal );
                if ( propVal == null ) {
                    haveNulls = true;
                } else {
                    uniques.add( propVal );
                }
                value = propVal;
            }
        }
        uniqueValues.addAll( uniques );
        int valueCount = uniqueValues.size();
        if ( haveNulls ) valueCount++;
        isMultivalued = ( valueCount > 1 );
        if ( uniqueValues.size() != 1 ) {
            value = null;
        }
        propertySupport.firePropertyChange( "value", oldValue, value );
        propertySupport.firePropertyChange( "multivalued",
                wasMultivalued, isMultivalued );
    }

    /**
     * Get all unique values
     * @return
     */
    public List<T> getUniqueValues() {
        return uniqueValues;
    }

    /**
     * Get the property value for all selected objetcs. Unlike the list returned
     * by {@link #getUniqueValues()} this list cal contain duplicates. Elements
     * of the list are guaranteed to be in same order as photos in selection
     * model.
     * @return
     */
    public List<T> getValues() {
        return values;
    }

    public void addPropertyChangeListener( PropertyChangeListener l ) {
        propertySupport.addPropertyChangeListener( l );
    }

    public void removePropertyChangeListener( PropertyChangeListener l ) {
        propertySupport.removePropertyChangeListener( l );
    }

    public void addPropertyChangeListener( String propertyName, PropertyChangeListener l ) {
        propertySupport.addPropertyChangeListener( propertyName, l );
    }

    public void removePropertyChangeListener( String propertyName, PropertyChangeListener l ) {
        propertySupport.removePropertyChangeListener( propertyName, l );
    }

}
