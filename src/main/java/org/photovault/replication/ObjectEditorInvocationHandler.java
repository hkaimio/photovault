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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 Base class for handling method invocations for editors of replicable objects
 Since replicable object must be modified only via {@link Change}s it can not
 have normal setter methods. Therefore a special editor object can be constructed
 that implements Java Bean style set/get methods and stores changes to a change
 <p>
 This class works with java Proxy object framework to intercept methods that are 
 annotated with {@link Setter} annotation and updates the associated change 
 based on these.
 */
public abstract class ObjectEditorInvocationHandler<T,F extends Comparable> 
        implements InvocationHandler {
    
    /**
     Target object
     */
    T target;
    /**
     Locally known history for {@link target}
     */
    ChangeSupport<T,F> targetHistory;
    /**
     Change being constructed
     */
    Change<T,F> change;
    
    /**
     Create a new invocation handler
     @param targetHistory History of the object being edited
     */
    public ObjectEditorInvocationHandler( ChangeSupport<T,F> targetHistory ) {
        this.targetHistory = targetHistory;
        this.target = targetHistory.getOwner();
        this.change = targetHistory.createChange();
    }
    
    /**
     Get the field that is going to be set by a method
     @param method The method that was called
     @return Field that is going to be set by this method of <code>null</code>
     otherwise.
     */
    private F getField( Method method ) {
        if ( method.isAnnotationPresent( Setter.class ) ) {
            Setter s = method.getAnnotation( Setter.class );
            return getFieldForName( s.field() );
        }
        return null;
    }
    
    /**
     Returns the target object
     */
    public T getTarget() {
        return target;
    }
    
    /**
     Returns the change being constructed
     */
    public Change<T,F> getChange() {
        return change;
    }
    
    /**
     Get the field based on its name. Derived classes must override this to map 
     the name to the identifier used by that class.
     @param name Name of the field
     @return The field or <code>null</code> if no field with the given name 
     exists.
     */
    protected abstract F getFieldForName( String name );
    
    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
        String name = method.getName();
        Object ret = null;
        if ( method.isAnnotationPresent( Setter.class ) ) {
            Setter ann = method.getAnnotation( Setter.class );
            String fieldName = ann.field();
            F field = getFieldForName( fieldName );
            Class dtoResolvClass = ann.dtoResolver();
            DTOResolver dtoResovler = null;
            Object val = args[0];
            if ( dtoResolvClass != DefaultDtoResolver.class ) {
                dtoResovler = (DTOResolver) dtoResolvClass.newInstance();
                val = dtoResovler.getDtoFromObject( val );
            }
            change.setField( field, val );
        } else {
            ret = method.invoke( target, method );
        }
        return ret;
    }
}
