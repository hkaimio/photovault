/*
  Copyright (c) 2008 Harri Kaimio
  
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


package org.photovault.replication;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 Manages version history and version changes for a single object.
 <p>
 ChangeSupport is an abstract class that encapsulates the logic for managing 
 version history and state changes of instances of target class. To use it with
 a class, you need to extend it and implement the needed abstract methods for 
 accessing the target object.
 
 @author Harri Kaimio
 @since 0.6
 
 @param <T> Class of the target obejct
 @param <F> Class used to describe the fields in the class. Typically an enum.
 */
public abstract class ChangeSupport<T, F extends Comparable> {

    /**
     Target obejct
     */
    T target;
    
    /**
     Head changes (i.e. changes that do not have children)
     */
    Set<Change<T, F>> heads = new HashSet<Change<T, F>>();
    
    /**
     All changes to target obejct that are known 
     */
    Set<Change<T, F>> allChanges = new HashSet<Change<T, F>>();

    public ChangeSupport( T target ) {
        this.target = target;
    }
    

    /**
     Add a new change to change history. Called bu {@link Change} when it is 
     freezed
     @param c The new change
     */
    void addChange( Change<T, F> c ) {
        if ( heads.contains( c.getPrevChange() ) ) {
            heads.remove( c.getPrevChange() );
        }
        heads.add( c );
        allChanges.add( c );
    }
    
    /**
     Initialize the first change by setting all fields in it to current values 
     of target object fields
     @param c
     */
    void initFirstChange( Change<T,F> c ) {
        Map<F,Object> changedFields = c.getChangedFields();
        for ( F f : allFields() ) {
            if ( !changedFields.containsKey( f ) ) {
                c.setField(f, getField(f) );
            }
        }
    }
    
    /**
     Apply a change to target object
     @param c The change that will be applied.
     @throws IllegalStateException if the change is not a child of current 
     version of target obejct
     */
    void applyChange( Change<T,F> c ) {
        if ( getVersion() != c.getPrevChange()) {
            throw new IllegalStateException( "Cannot apply change on top of unrelated change" );
        }
        for ( Map.Entry<F,Object> fc : c.getChangedFields().entrySet() ) {
            setField(fc.getKey(), fc.getValue() );
        }
        setVersion( c );        
    }

    /**
     Create a new empty change object for target obejct
     @return A new, unfrozen Change object
     */
    public Change<T, F> createChange() {
        return new Change<T, F>( this );
    }

    /**
     Get the target
     @return
     */
    T getOwner() {
        return target;
    }


    /**
     Modify the target obejct so that its state matches given version
     @param newVersion The new version for the target object
     */
    public void changeToVersion( Change<T,F> newVersion ) {
        Change<T,F> oldVersion = getVersion();
        
        if ( newVersion == oldVersion ) {
            return;
        }
        
        // Find the common ancestor between these versions
        Set<Change<T,F>> oldAncestors = new HashSet<Change<T,F>>();
        for ( Change<T,F> c = oldVersion ; c != null ; c = c.getPrevChange() ) {
            oldAncestors.add( c );
        }
        
        Change<T,F> commonBase = null;
        for ( Change<T,F> c = newVersion ; c != null ; c = c.getPrevChange() ) {
            if ( oldAncestors.contains( c ) ) {
                commonBase = c;
                break;
            }
        }
        
        /*
         Find out the fields that were changed between old version and common 
         base
         */
        Set<F> oldFieldChanges = new HashSet<F>();
        for ( Change<T,F> c = oldVersion ; c != commonBase ; c = c.getPrevChange() ) {
            oldFieldChanges.addAll( c.getChangedFields().keySet() );
        }
        
        /*
         Finally, set fields in target object to the newest value in path to new
         version if the field was set in either path after common base
         */
        Set<F> changedFields = new HashSet<F>();
        boolean commonBasePassed = false;
        for ( Change<T,F> c = newVersion ; c != null ; c = c.getPrevChange() ) {
            if ( c == commonBase ) {
                commonBasePassed = true;
            }
            for ( Map.Entry<F,Object> e : c.getChangedFields().entrySet() ) {
                F f = e.getKey();
                if ( !changedFields.contains( f ) ) {
                    if ( !commonBasePassed || oldFieldChanges.contains( f ) ) {
                        setField(f, e.getValue() );
                        changedFields.add( f );
                    }
                }
            }
        }   
        setVersion( newVersion );
    }

    public List<Change> getChanges() {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public List<Change> getHeads() {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public Change mergeHeads() {
        throw new UnsupportedOperationException( "Not supported yet." );
    }


    /**
     Returns the uuid of the target obejct
     */
    public abstract UUID getGlobalId();

    /**
     Returns current value of given field in the target obejct
     @param field Field identifier
     @return Current value of given field in target
     */
    protected abstract Object getField( F field );
    
    /**
     Set value of a field in target object
     @param field Identifier of the field
     @param val New value for the field
     */
    protected abstract void setField( F field, Object val );
    
    /**
     Get identifiers of all fields.
     @return Set of all field ids, for example, if the fields are described by 
     enumeration, return EnumSet.allOf( F.class )
     */
    protected abstract Set<F> allFields();
    
    /**
     Set the version of target obejct
     @param version The currently applied change
     */
    protected abstract void setVersion( Change<T,F> version );
    
    /**
     Returns the current version of target obejct
     */
    protected abstract Change<T,F> getVersion( );
}
