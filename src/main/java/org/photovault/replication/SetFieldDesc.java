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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 Descriptor of version controlled field with set semantics
 
 @author Harri Kaimio
 @since 0.6.0
 */
class SetFieldDesc extends FieldDesc {

    static private Log log = LogFactory.getLog( SetFieldDesc.class.getName() );
    
    /**
     Method in target class that is used to add new element to the set
     */
    private Method addMethod;
    
    /**
     Method in target class that is used to remove an element to the set
     */
    private Method removeMethod;
    
    /**
     Constructor
     @param clDesc Class descriptor for the class that owns this field
     @param fieldName Name of this field
     @param elementType Thpe of elements in the set
     @param elementResolverClass DTO esolver for elements
     @param editorIntf Editor interface
     */
    SetFieldDesc( VersionedClassDesc clDesc, String fieldName,
            Class elementType, Class elementResolverClass,
            Class editorIntf ) {
        super( clDesc, fieldName, elementType, elementResolverClass );
        analyzeField( editorIntf );
    }

    /**
     Apply change to this field in given object
     @param target The object that will be changed
     @param ch Change to this field
     @param resolverFactory DTO resolver factory used
     */
    @Override
    void applyChange(  Object target, FieldChange ch,
            DTOResolverFactory resolverFactory ) {
        SetChange sc = (SetChange) ch;
        try {
            for ( Object item : sc.getAddedItems() ) {
                addMethod.invoke( target, item );
            }
            for ( Object item : sc.getRemovedItems() ) {
                removeMethod.invoke( target, item );
            }
        } catch ( IllegalAccessException ex ) {
            log.error( ex );
            throw new IllegalStateException( "Invalid set handling method", ex );
        } catch ( IllegalArgumentException ex ) {
            log.error( ex );
            throw new IllegalStateException( "Invalid set handling method", ex );
        } catch ( InvocationTargetException ex ) {
            log.error( ex );
            throw new IllegalStateException( "Invalid set handling method", ex );
        }


    }

    /**
     Analyzie the class to fild methods that are used to manipulate this field.
     
     TODO: Annotation semantics are currently confusing, think the over!!!
     
     @param editorIntf
     */
    private void analyzeField( Class editorIntf ) {
        String nameBase = name.substring( 0, 1 ).toUpperCase() +
                name.substring( 1 );
        if ( nameBase.endsWith( "s" ) ) {
            nameBase = nameBase.substring( 0, nameBase.length()-1 );
        }
        String getterName = "get" + nameBase + "s";
        String addMethodName = "add" + nameBase;
        String removeMethodName = "remove" + nameBase;
        try {
            getter = clDesc.getDescribedClass().getMethod( getterName );
            SetField ann = getter.getAnnotation( SetField.class );
            clazz = ann.elemClass();
            
        } catch ( NoSuchMethodException ex ) {
            // No action for now
        } 
        try {
            addMethod = clDesc.getDescribedClass().getMethod( addMethodName, clazz );
        } catch ( NoSuchMethodException ex ) {
            // No action for now
        } 
        try {
            removeMethod = clDesc.getDescribedClass().getMethod( removeMethodName, clazz );
        } catch ( NoSuchMethodException ex ) {
            // No action for now
        } 
        
        // Create handlers for the editor methods
        try {
            Method m = editorIntf.getMethod( getterName );
            clDesc.setEditorMethodHandler( m, new ProxyMethodHandler( this ) {

                @Override
                Object methodInvoked( VersionedObjectEditor editor, Object[] args ) {
                    return editor.getField( fd.name );
                }
            } );
        } catch ( NoSuchMethodException e ) {}
        
        try {
            Method m = editorIntf.getMethod( addMethodName, clazz );
            clDesc.setEditorMethodHandler( m, new ProxyMethodHandler( this ) {

                @Override
                Object methodInvoked( VersionedObjectEditor editor, Object[] args ) {
                    editor.addToSet( fd.name, args[0] );
                    return null;
                }
            } );
        } catch ( NoSuchMethodException e ) {}
        
        try {
            Method m = editorIntf.getMethod( removeMethodName, clazz );
            clDesc.setEditorMethodHandler( m, new ProxyMethodHandler( this ) {

                @Override
                Object methodInvoked( VersionedObjectEditor editor, Object[] args ) {
                    editor.removeFromSet( fd.name, args[0] );
                    return null;
                }
            } );
        } catch ( NoSuchMethodException e ) {}
    }

}
