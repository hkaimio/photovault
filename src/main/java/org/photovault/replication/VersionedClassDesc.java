/*
  Copyright (c) 2008 Harri Kaimio
 
  This file is part of Photovault.
 
  Photovault is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
 
  Photovault is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even therrore implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.photovault.replication;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 VersionedClassDesc is used to analyze and store description of methods that
 the replication framework uses to manipulate versioned classes. It parses the
 annotations in given class.
 
 @author Harri Kaimio
 @since 0.6.0
 */
public class VersionedClassDesc {
    
    Log log = LogFactory.getLog( VersionedClassDesc.class.getName() );
    /**
     Class described by this instance
     */
    private Class clazz;
    
    /**
     Interface used for editing the class
     */    
    private Class editorIntf;
    
    /**
     Construct a new class description by analyzing given class
     @param clazz The class to analyze
     */
    public VersionedClassDesc( Class clazz ) {
        this.clazz = clazz;  
        analyzeClass( clazz );
    }

    /**
     Get the fields in the described class
     @return Read-unly set of field names
     */
    public Set<String> getFieldNames() {
        return Collections.unmodifiableSet( fields.keySet() );
    }
    
    public Class getDescribedClass() {
        return clazz;
    }
    
    /**
     Get the class that is used for converting given field between DTO format 
     and working copy representation.
     @param fieldName Name of the field
     @return DTO class for field named in in fieldName
     @throws IllegalArgumentException if the field name is not known.
     */
    public Class<? extends DTOResolver> getFieldResolverClass( String fieldName ) {
        FieldDesc fd = fields.get( fieldName );
        if ( fd == null ) {
            throw new IllegalArgumentException( "Field " + fieldName + " not found" );
        } 
        return fd.dtoResolverClass;
    }

    /**
     Analyze the annotations in described class and populate this object
     based on results
     @param cl the clas to analyze
     */
    private void analyzeClass( Class cl ) {
        Class scl = cl.getSuperclass();
        if ( scl != null ) {
            analyzeClass( scl );
        }
        Versioned info = (Versioned) cl.getAnnotation( Versioned.class );
        if ( info != null ) {
            editorIntf = info.editor();
        }
        for ( Method m : cl.getDeclaredMethods() ) {
            Setter s = m.getAnnotation( Setter.class );
            if ( s != null ) {
                String fieldName = s.field();
                Class types[] = m.getParameterTypes();
                Class fieldType = null;
                if ( types.length == 1 ) {
                    fieldType = types[0];
                }
                ValueFieldDesc fd = (ValueFieldDesc) fields.get( fieldName );
                if ( fd == null ) {
                    fd = new ValueFieldDesc( this, fieldType, fieldName, s.dtoResolver(), editorIntf );
                    fd.name = fieldName;
                    fields.put( fieldName, fd );
                }
            }
        }
    }
    
    /**
     Set field value in an object of the class described by this instance
     @param target The target object
     @param fieldName Name of the field to set
     @param value New value for the field
     
     @throws IllegalArgumentException if the field name is not knowns.
     @throws IllegalStateException if the setter cannot be invoked using 
     reflection.
     */
    public void setFieldValue( Object target, String fieldName, Object value ) {
        FieldDesc fd = fields.get( fieldName );
        if ( fd == null ) {
            throw new IllegalArgumentException( 
                    "Field " + fieldName + " not found" );
        }
        try {
            ((ValueFieldDesc)fd).setter.invoke( target, value );
        } catch ( IllegalAccessException ex ) {
            throw new IllegalStateException( "Cannot access setter", ex );
        }  catch ( InvocationTargetException ex ) {
            throw new IllegalStateException( 
                    "InvocationTargetException while setting field",
                    ex );
        }
    }
    
    /**
     Get field value in an object of the class descriobed by this instance.
     @param target Target object
     @param fieldName Name of the field
     @return Value of the field

     @throws IllegalArgumentException if the field name is not knowns.
     @throws IllegalStateException if the setter cannot be invoked using 
     reflection.
     */
    public Object getFieldValue( Object target, String fieldName ) {
        FieldDesc fd = fields.get( fieldName );
        if ( fd == null ) {
            throw new IllegalArgumentException( 
                    "Field " + fieldName + " not found" );
        }
        try {
            return fd.getter.invoke( target );
        } catch ( IllegalAccessException ex ) {
            throw new IllegalStateException( "Cannot access getter", ex );
        }  catch ( InvocationTargetException ex ) {
            throw new IllegalStateException( 
                    "InvocationTargetException while getting field",
                    ex );
        }
    }
    
    /**
     Apply a field change to given oject
     @param target The object that is going to be change
     @param change The field change
     @param resolverFactory resolver factory used to resolve data transfer 
     objects to correct context
     */
    void applyChange( Object target, FieldChange change,
            DTOResolverFactory resolverFactory ) {
        FieldDesc fd = fields.get( change.getName() );
        fd.applyChange( target, change, resolverFactory );
    }
    
    /**
     Descriptors of all versioned fields of this class
     */
    private Map<String,FieldDesc> fields = new HashMap<String, FieldDesc>();
    
    /**
     Handlers for editor interface methods
     */
    private Map<Method, ProxyMethodHandler> editorMethodHandlers = 
            new HashMap<Method, ProxyMethodHandler>();
    
    /**
     Register handler for a method in the editor interface
     @param m The method
     @param h Handler for m
     */
    void setEditorMethodHandler( Method m, ProxyMethodHandler h ) {
        editorMethodHandlers.put( m, h );
    }
    
    /**
     Get the handler for certain method of editor interface
     @param m The editor method
     @return Handler for the method or <code>null</code> if none is defined
     */
    ProxyMethodHandler getEditorMethodHandler( Method m ) {
        return editorMethodHandlers.get( m );
    }
    
    Class getEditorClass() {
        return editorIntf;
    }

}
