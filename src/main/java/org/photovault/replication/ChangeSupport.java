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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 Manages version history and version changes for a single object.
 <p>
 ChangeSupport is an abstract class that encapsulates the logic for managing 
 version history and state changes of instances of target class. To use it with
 a class, you need to extend it and implement the needed abstract methods for 
 accessing the target object.
 <p>
 In additio, you currently need to create association between the derived
 and target object. The target object must be assigned to owner field of the base 
 class. This is an ugly hack since Hibernate seems not to handle properly
 cases in which derived class redefines mapping of base class field. TODO: Better 
 solution is needed...
 
 @author Harri Kaimio
 @since 0.6
 
 @param <T> Class of the target obejct
 */
@Entity
@Table(name = "pv_version_histories")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "class_discriminator", discriminatorType = DiscriminatorType.STRING)
public abstract class ChangeSupport<T> {

    static private Log log = LogFactory.getLog( ChangeSupport.class.getName() );
    /**
     Target obejct
     */ 
    T target;
    
    /**
     Name of the class of target
     */
    String targetClassName;    
    
    /**
     Identifier of this change history. THis is the same as the UUID of the 
     target object
     */
    private UUID uuid;
    
    /**
     Head changes (i.e. changes that do not have children)
     */
    Set<Change<T>> heads = new HashSet<Change<T>>();
    
    /**
     All changes to target obejct that are known 
     */
    Set<Change<T>> allChanges = new HashSet<Change<T>>();


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
        targetClassName = target.getClass().getName();
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
    
    public void setTargetUuid( UUID uuid ) {
        this.uuid = uuid;
    }

    /**
     Add a new change to change history. Called bu {@link Change} when it is 
     freezed
     @param c The new change
     */
    void addChange( Change<T> c ) {
        for ( Change<T> parent : c.getParentChanges() ) {
            heads.remove( parent );
        }
        heads.add( c );
        allChanges.add( c );
    }

    /**
     Create a new empty change object for target object
     @return A new, unfrozen Change object
     */
    public Change<T> createChange() {
        return new Change<T>( this );
    }

    /**
     Returns name of the target object's class
     */
    @Column( name = "target_class" )
    public String getTargetClassName() {
        return targetClassName;
    }
    
    /**
     Set the name of target object's class
     @param cl
     */
    public void setTargetClassName( String cl ) {
        targetClassName = cl;
    }
    
    /**
     Returns the target object. 
     */
    @Transient
    public T getOwner() {
        return target;
    }
    
    /**
     Set the owner of this change history. 
     */
    protected void setOwner( T owner ) {
        this.target = owner;
    }

    /**
     Returns all known changes in target objects's history. 
     */
    @OneToMany( mappedBy="targetHistory", cascade=CascadeType.ALL )
    public Set<Change<T>> getChanges() {
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
    public Set<Change<T>> getHeads() {
        return heads;
    }
    
    void setHeads( Set heads ) {
        this.heads = heads;
    }

    protected abstract T createTarget();
    
    /**
     Returns the uuid of the target obejct
     */
    @Transient
    public abstract UUID getGlobalId();
    
    /**
     Set the version of target obejct
     @param version The currently applied change
     */
    protected abstract void setVersion( Change<T> version );
    
    /**
     Returns the current version of target obejct
     */
    @OneToOne
    @JoinColumn( name = "version_uuid" )
    protected abstract Change<T> getVersion( );
}
