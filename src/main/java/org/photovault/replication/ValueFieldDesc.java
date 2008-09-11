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

/**
 Description of a versioned field with single value (i.e. no sub-state)

 @author Harri Kaimio
 @sice 0.6.0
 */
class ValueFieldDesc extends FieldDesc {

    /**
     Method used to set the value of this field
     */
    public Method setter;

    /**
     Default constructor
     */
    ValueFieldDesc() {
        super();
    }

    /**
     Constructor
     @param clDesc Class descriptor that owns this field
     @param cl Type of this field
     @param fieldName Name of the field
     @param dtoResolverClass DTO resolver used to convert this field to DTO and 
     back
     @param editorIntf Interface used to construct the editor proxy for the owner 
     class
     */
    ValueFieldDesc( VersionedClassDesc clDesc, Class cl, String fieldName, Class dtoResolverClass,
            Class editorIntf ) {
        super( clDesc, fieldName, cl, dtoResolverClass );
        String getterName =
                "get" + fieldName.substring( 0, 1 ).toUpperCase() +
                fieldName.substring( 1 );
        String setterName =
                "set" + fieldName.substring( 0, 1 ).toUpperCase() +
                fieldName.substring( 1 );
        try {
            getter = clDesc.getDescribedClass().getMethod( getterName );
        } catch ( NoSuchMethodException ex ) {}
        try {
            setter = clDesc.getDescribedClass().getMethod(  setterName, cl );
        } catch ( NoSuchMethodException ex ) {
        }
        if ( editorIntf != null ) {
            try {
                Method editorSetter = editorIntf.getMethod( setterName, clazz );
                clDesc.setEditorMethodHandler( 
                        editorSetter, new ProxyMethodHandler( this ) {

                    @Override
                    Object methodInvoked(  VersionedObjectEditor e,
                            Object[] args ) {
                        e.setField( fd.name, args[0] );
                        return null;
                    }
                } );
            } catch ( NoSuchMethodException ex ) {
            }

        }
    }

    /**
     Applies a change of this field change to an object
     @param target The object that is modified
     @param ch The change wih new value for the field
     @param resolverFactory Resolver factory to be used
     */
    void applyChange( Object target, FieldChange ch,
            DTOResolverFactory resolverFactory ) {
        DTOResolver resolver = resolverFactory.getResolver( dtoResolverClass );
        Object fieldVal =
                resolver.getObjectFromDto( ((ValueChange) ch).getValue() );
        try {
            setter.invoke( target, fieldVal );
        } catch ( IllegalAccessException ex ) {
            throw new IllegalStateException( "Cannot access setter", ex );
        } catch ( InvocationTargetException ex ) {
            throw new IllegalStateException( "InvocationTargetException while setting field",
                    ex );
        }
    }

}
