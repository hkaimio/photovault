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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Table;
import javax.persistence.Transient;

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
@Entity
@Table(name = "version_histories")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "class_discriminator", discriminatorType = DiscriminatorType.STRING)
public abstract class ChangeSupport<T, F extends Comparable> {

    /**
     Target obejct
     */ 
    T target;
    
    /**
     Identifier of this change history. THis is the same as the UUID of the 
     target object
     */
    private UUID uuid;
    
    /**
     Head changes (i.e. changes that do not have children)
     */
    Set<Change<T, F>> heads = new HashSet<Change<T, F>>();
    
    /**
     All changes to target obejct that are known 
     */
    Set<Change<T, F>> allChanges = new HashSet<Change<T, F>>();


    /**
     Default constructor for persistence & replication layers, do not use otherwise
     */
    public ChangeSupport() {}
    
    /**
     Constructor
     @param target
     */
    public ChangeSupport( T target ) {
        this.target = target;
    }
    

    /**
     Creates a new local replica of the object associated with this change 
     history. This method is called by {@link ChangeFactory} when it encounters 
     a change that is not known locally. The object is created by calling 
     createTarget() method that derived classes must override.
     
     @param targetUuid UUID of the unknown object
     */
    void initLocalReplica( UUID targetUuid ) {
        this.uuid = targetUuid; 
        target = createTarget();
    }    
    
    /**
     Returns the UUID of the target obejct
     */
    @Id
    @Column( name = "uuid" )
    @org.hibernate.annotations.Type( type = "org.photovault.persistence.UUIDUserType" )    
    public UUID getTargetUuid() {
        return uuid;
    } 
    
    protected void setTargetUuid( UUID uuid ) {
        this.uuid = uuid;
    }

    /**
     Add a new change to change history. Called bu {@link Change} when it is 
     freezed
     @param c The new change
     */
    void addChange( Change<T, F> c ) {
        for ( Change<T,F> parent : c.getParentChanges() ) {
            heads.remove( parent );
        }
        heads.add( c );
        allChanges.add( c );
    }
    
    /**
     Initialize the first change by setting all fields in it to current values 
     of target object fields
     @param c
     @deprecated Since initFirstChange does not have resolver context, it cannot 
     initialize fields with special resolver properly. So this should be removed.
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
        Change<T,F> currentVersion = getVersion();
        if ( currentVersion == null ? !c.getParentChanges().isEmpty() :
                !c.getParentChanges().contains( currentVersion ) ) {
            throw new IllegalStateException( "Cannot apply change on top of unrelated change" );
        }
        for ( Map.Entry<F,Object> fc : c.getChangedFields().entrySet() ) {
            setField(fc.getKey(), fc.getValue() );
        }
        setVersion( c );        
    }

    /**
     Create a new empty change object for target object
     @return A new, unfrozen Change object
     */
    public Change<T, F> createChange() {
        return new Change<T, F>( this );
    }

    /**
     Returns the target object.
     */
    @Transient
    public T getOwner() {
        return target;
    }
    
    /**
     Set the owner of this change history
     */
    protected void setOwner( T owner ) {
        this.target = owner;
    }


    /**
     Modify the target object so that its state matches given version
     @param newVersion The new version for the target object
     @deprecated Use VersionedObjectEditor#changeToVersion instead.s
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

    /**
     Returns all known changes in target objects's history. 
     */
    @OneToMany( mappedBy="targetHistory", cascade=CascadeType.ALL )
    public Set<Change<T,F>> getChanges() {
        return this.allChanges;
    }

    
    void setChanges( Set c ) {
        allChanges=c;
    }

    /**
     Returns the set of head changes, i.e. changes that do not have a child.
     */
    @OneToMany
    @JoinTable( name="change_unmerged_branches", 
                joinColumns=@JoinColumn( name="target_uuid" ), 
                inverseJoinColumns=@JoinColumn( name = "change_uuid" ) )
    public Set<Change<T,F>> getHeads() {
        return heads;
    }
    
    void setHeads( Set heads ) {
        this.heads = heads;
    }

    public Change mergeHeads() {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    protected abstract T createTarget();
    
    /**
     Returns the uuid of the target obejct
     */
    @Transient
    public abstract UUID getGlobalId();

    /**
     Returns current value of given field in the target obejct
     @param field Field identifier
     @return Current value of given field in target
     */
    @Transient
    protected abstract Object getField( F field );
    
    protected abstract FieldDescriptor<T> getFieldDescriptor( String field );
    
    @Transient
    protected abstract Map<String, FieldDescriptor<T>> getFieldDescriptors();
    
    /**
     Set value of a field in target object
     @param field Identifier of the field
     @param val New value for the field
     */
    @Transient
    protected abstract void setField( F field, Object val );
    
    /**
     Get identifiers of all fields.
     @return Set of all field ids, for example, if the fields are described by 
     enumeration, return EnumSet.allOf( F.class )
     */
    @Transient
    protected abstract Set<F> allFields();
    
    /**
     Set the version of target obejct
     @param version The currently applied change
     */
    protected abstract void setVersion( Change<T,F> version );
    
    /**
     Returns the current version of target obejct
     */
    @OneToOne
    @JoinColumn( name = "version_uuid" )
    protected abstract Change<T,F> getVersion( );
}
