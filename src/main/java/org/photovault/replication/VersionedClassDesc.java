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
import java.util.logging.Level;
import java.util.logging.Logger;
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
        for ( Method m : cl.getDeclaredMethods() ) {
            Setter s = m.getAnnotation( Setter.class );
            if ( s != null ) {
                String fieldName = s.field();
                FieldDesc fd = fields.get( fieldName );
                if ( fd == null ) {
                    fd = new FieldDesc();
                    fields.put( fieldName, fd );
                }
                fd.setter = m;
                fd.dtoResolverClass = s.dtoResolver();
                Class types[] = m.getParameterTypes();
                if ( types.length == 1 ) {
                    fd.clazz = types[0];
                }
                String getterName = 
                        "get" + fieldName.substring( 0, 1 ).toUpperCase() + 
                        fieldName.substring( 1 );
                try {
                    fd.getter = clazz.getMethod( getterName );
                } catch ( NoSuchMethodException ex ) {
                    /*
                     TODO: This is actually an error, so maybe we should throw
                     an exception here instead of checking it in getFieldValue()?
                     */
                    log.warn( ex );
                } catch ( SecurityException ex ) {
                    log.warn( ex );
                }
                if ( fd.setter != null ) {
                    methodRoles.put(  fd.setter, new MethodRole( fd, FieldOper.SET ) );
                }
                if ( fd.getter != null ) {
                    methodRoles.put(  fd.getter, new MethodRole( fd, FieldOper.GET ) );
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
            fd.setter.invoke( target, value );
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
     Description of a single field
     */
    private static class FieldDesc {
        /**
         Setter for this field
         */
        public Method setter;
        /**
         Getter of this field
         */
        public Method getter;
        /**
         Class of the field
         */
        public Class clazz;
        /**
         Resolver used to convert the field between DTO and working copy 
         presentation
         */
        public Class dtoResolverClass;
        
        private FieldDesc() {}

        private FieldDesc( Class fieldClass, Class<DTOResolver> dtoResolverClass, Method setter, Method getter ) {
            clazz = fieldClass;
            this.dtoResolverClass = dtoResolverClass;
            this.setter = setter;
            this.getter = getter;
        }
    }
    
    /**
     Operations for the field
     */
    enum FieldOper {
        SET,
        GET
    }
    
    private static class MethodRole {
        FieldDesc field;
        FieldOper operation;
        
        public MethodRole( FieldDesc f, FieldOper o ) {
            field = f;
            operation = o;
        }
    }
    
    private Map<String,FieldDesc> fields = new HashMap<String, FieldDesc>();
    
    private Map<Method, MethodRole> methodRoles = new HashMap<Method,MethodRole>();

}
