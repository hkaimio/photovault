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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.MapKey;

/**
 Description of a change made to an object.
 
 Change is an immutable object describing some atomic change made to another 
 object. In that way, it can ve considered also as a version identifier for the 
 object state at certain moment of time.
 <p>
 It consists of information about changed status (field value changes and changed 
 associations to other objects) and reference to the change identifying the version
 of changed object before the change. Based on these, an unique identifier is 
 calculated.
 <p>
 The change object will have two distinct phases in its life cycle. When it is 
 created, it is in unfrozen start, and information about the state change can be 
 modified. After the object is frozed by calling the freeze() method, it cannot 
 be modified anymore.
 
 @see ChangeSupport
 
 @author Harri Kaimio
 @since 0.6
 
 @param <T> Class of the object whose changes are being tracked.
 @param <F> Class that describes individual fields of the change.
 */
@Entity
@Table( name="changes" )
public class Change<T, F extends Comparable> {
    
    static private UUID NULL_UUID = UUID.fromString( "00000000-0000-0000-0000-000000000000" );
    
    static private Log log = LogFactory.getLog( Change.class.getName() );
    
    /**
     ChangeSupport object handling change history of the target object
     */
    private ChangeSupport targetHistory;

    /**
     Has this change been frozen?
     */
    private boolean frozen = false;
    
    /**
     Unique ID for this change, calculated when the change is frozen
     */
    private UUID uuid;
    
    /**
     Previous change on top of which this change can be applied
     */
    private Change prevChange;
    
    /**
     If this is a merge change, the change that is merged with {@link prevChange}.
     Otherwise <code>null</code>
     */
    private Change mergeChange;
    
    /**
     True if this change is a head of a branch (i.e. it does not have any child 
     changes
     */
    private boolean head = true;
    
    /**
     Changes that are based on this change
     */
    private Set<Change<T,F>> childChanges = new HashSet<Change<T,F>>();
    
    private Set<Change<T,F>> parentChanges = new HashSet<Change<T,F>>();
    
    /**
     Fields of {@link target} that have been changed by this change
     */
    private Map<F, Object> changedFields = 
            new HashMap<F,Object>();
    
    /**
     If this is a merge change, conflicting fields
     */
    private Map<F, FieldConflict> fieldConflicts = 
            new HashMap<F, FieldConflict>();
    
    
    /**
     Default constructor, for construction by Hibernate or {@link ChangeFactory}.
     This constructor set the frozen field to true, so it is extremely important 
     that the change must not be accessed by user code before second phase of 
     construction has been finalized!!!
     */
    Change() {
        frozen = true;
    }
    
    /**
     Constructor. This constructor is used by ChangeSupport class to create 
     new changes
     
     @param t The ChangeSupport object handling change history of target
     */
    Change( ChangeSupport<T,F> t ) {
        targetHistory = t;
    }
    
    @Id
    @Column( name="change_uuid" )
    @org.hibernate.annotations.Type( type = "org.photovault.persistence.UUIDUserType" )
    public UUID getUuid() {
        return uuid;
    }
    
    void setUuid( UUID uuid ) {
        this.uuid = uuid;
    }
    
    @ManyToOne( targetEntity=ChangeSupport.class )
    @JoinColumn( name="target_uuid" )
    ChangeSupport<T, F> getTargetHistory() {
        return targetHistory;
    }

    void setTargetHistory( ChangeSupport<T, F> h ) {
        targetHistory = h;
    }

    /**
     Set field value. 
     @param field The field to be changed
     @param newValue New value for the field
     @throws IllegalStateException if the change has laready been frozen.
     */
    public void setField( F field, Object newValue ) {
        assertNotFrozen();
        changedFields.put( field, newValue );
    }

    /**
     Get field value. The If the field is not modified by this change, return 
     the value of the field after previous change.
     @param field The field that will be returned
     @return Value that the field will have after the change is applied.
     @todo What if this is a merge change and there is a conflict
     */
    public Object getField( F field ) {
        Object ret = null;
        if ( changedFields.containsKey( field ) ) {
            return changedFields.get( field );
        }
//        if ( target.getVersion().equals( prevChange ) ) {
//            return field.getValue( target );
//        }
        if ( prevChange != null ) {
            return prevChange.getField( field );
        }
        return null;
    }
    
    @Column( name = "serialized" )
    byte[] getSerializedChange() {
        byte[] res = null;
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream( s );
            writeFieldChanges( os );
            res = s.toByteArray();
        } catch ( IOException ex ) {
            log.debug( ex.getMessage(), ex );
        }
        return res;        
    }

    void setSerializedChange( byte[] c ) {
        try {
            ByteArrayInputStream s = new ByteArrayInputStream( c );
            ObjectInputStream is = new ObjectInputStream(  s );
            readFieldChanges( is );
        } catch ( IOException ex ) {
            log.error( ex.getMessage(), ex );
        } catch ( ClassNotFoundException ex ) {
            log.error( ex.getMessage(), ex );
        }
    }
    
    
    /**
     Get all fields modified in this change
     @return Map from the field description objects to their new values.
     */
    @Transient
    public Map<F,Object> getChangedFields() {
        return Collections.unmodifiableMap( changedFields );
    }
    
    void setChangedFields( Map<F,Object> changes ) {
        changedFields = changes;
    }
    
    @ManyToMany( targetEntity=Change.class, cascade=CascadeType.ALL )
    @JoinTable(name = "change_relations",
        joinColumns = {@JoinColumn(name = "child_uuid")},
        inverseJoinColumns = {@JoinColumn(name = "parent_uuid")})
    Set<Change<T,F>> getParentChanges() {
        return parentChanges;
    }

    void setParentChanges(  Set changes ) {
        parentChanges = changes;
    }
    
    @Column( name = "head" )
    public boolean isHead() {
        return head;
    }
    
    void setHead( boolean b ) {
        head = b;
    }
    
    /**
     Returns the previous change on top of which this change is applied
     */
    @Transient
    public Change getPrevChange() {
        if ( prevChange == null && !parentChanges.isEmpty() ) {
            prevChange = parentChanges.iterator().next();
        }
        return prevChange;
    }

    /**
     Set the predecessor of this change
     @param c
     @throws IllegalStateException if the change has already been frozen
     */
    public void setPrevChange( Change<T,F> c ) {
        assertNotFrozen();
        if ( c.targetHistory != targetHistory ) {
            throw new IllegalArgumentException( "Cannot be based on change to different object" );
        }
        if ( prevChange != null ) {
            parentChanges.remove( prevChange );
            prevChange.removeChildChange( this );
        }
        parentChanges.add(  c );
        prevChange = c;        
    }
    
    /**
     Get the change that this change merges with previous change
     @return The merged change or <code>null</code> if this is not a merge 
     operation
     */
    @Transient
    public Change getMergedChange() {
        if ( mergeChange == null && parentChanges.size() > 1 ) {
            for ( Change<T,F> c : parentChanges ) {
                if ( c != getParentChanges() ) {
                    mergeChange = c;
                    break;
                }
            }
        }
        return mergeChange;
    }
    
    /**
     Returns the children of this change.
     */
    @ManyToMany( mappedBy="parentChanges", targetEntity=Change.class )
    public Set<Change<T,F>> getChildChanges() {
        return childChanges;
    }
    
    void setChildChanges( Set c ) {
        childChanges = c;
    }
    
    /**
     Add a child to thi change
     @param child The new child
     */
    void addChildChange( Change child ) {
        childChanges.add( child );
        head = false;
    }
    
    /**
     Removes a child change
     @param child The chipd to be removed
     */
    void removeChildChange( Change child ) {
        childChanges.remove( child );
        head = childChanges.isEmpty();
    }
    
    /**
     Returns <code>true</code> if this change has unresolved conflicts
     */
    public boolean hasConflicts() {
        return pendingConflictCount > 0;
    }
    
    /**
     Returns all field conflicts
     @return
     */
    @Transient
    public Collection<FieldConflict> getFieldConficts() {
        return Collections.unmodifiableCollection( fieldConflicts.values() );
    }
    
    /**
     Creates a change that merges 2 branches into one. The change will contain 
     needed operations so that it will change target object to same state 
     when applied to either branch head. If a field is modified in onluy one 
     branch it will be set in this change as well. If a field is modified 
     differently in the branches, a conflict is created.
     @param other The change that will be merged with this one
     @return A new merge change.
     */
    public Change<T,F> merge( Change<T,F> other ) {
        assertFrozen();
        if ( other.targetHistory != targetHistory ) {
            throw new IllegalArgumentException( "Cannot merge changes to separate objects" );
        }
        
        // Find the common ancestor version
        Set<Change> ancestors = new HashSet<Change>();
        for ( Change c = this ; c != null; c = c.getPrevChange() ) {
            ancestors.add( c );
        }
        Change<T,F> commonBase = null;
        Map<F, Object> changedFieldsOther = new HashMap();
        for ( Change<T,F> c = other ; c != null ; c = c.getPrevChange() ) {
            if ( ancestors.contains( c ) ) {
                commonBase = c;
                break;
            }
            
            // record all field changes that are still valid currently
            for ( Map.Entry<F,Object> e : c.changedFields.entrySet() ) {
                if ( !changedFieldsOther.containsKey( e.getKey() ) ) {
                    changedFieldsOther.put( e.getKey(), e.getValue() );
                }
            }
        }
        
        /*
         Common ancestor is now found, collect changes done between it and 
         this change
         */
        Map<F, Object> changedFieldsThis = new HashMap();
        for ( Change<T,F> c = this ; c != commonBase ; c = c.getPrevChange() ) {
            for ( Map.Entry<F,Object> e : c.changedFields.entrySet() ) {
                if ( !changedFieldsThis.containsKey( e.getKey() ) ) {
                    changedFieldsThis.put( e.getKey(), e.getValue() );
                }
            }            
        }
        
        /*
         Combine the changes. If field is changed only in one path or it gets 
         same value in both, it can be merged automatically. Otherwise, 
         conflict is created
         */
        Change<T,F> merged = new Change<T,F>( targetHistory );
        merged.parentChanges.add( this );
        merged.parentChanges.add( other );
        Set<F> allChangedFields = 
                new HashSet<F>( changedFieldsThis.keySet() );
        allChangedFields.addAll( changedFieldsOther.keySet() );
        for ( F f : allChangedFields ) {
            boolean changedInThis = changedFieldsThis.containsKey( f );
            boolean changedInOther = changedFieldsOther.containsKey( f );
            if ( changedInThis && !changedInOther ) {
                merged.setField(f, changedFieldsThis.get( f ) );
            } else if ( !changedInThis && changedInOther ) {                
                merged.setField(f, changedFieldsOther.get( f ) );
            } else {
                // The field has been changed in both paths
                Object valThis = changedFieldsThis.get( f );
                Object valOther = changedFieldsOther.get( f );
                if ( (valThis == valOther) || ( valThis != null && valThis.equals(valOther ) ) ) {
                    merged.setField(f, valThis );                            
                } else {
                    // Create conflict
                    System.out.println( "Conflict in field " + f + ": " + valThis + " <-> " + valOther );
                    FieldConflict conflict = 
                            new FieldConflict<F>( f, merged, 
                            new Change[]{ findLastFieldChange( f, this ), findLastFieldChange( f, other )} );
                    merged.addFieldConflict( conflict );
                }
                        
            }
                    
        }
         
        return merged;
    }


    /**
     Helper method to find the change in which a field was last modified
     @param f The field
     @param start The change whose ancestors are looked
     @return The change in which f was modified
     */
    private Change<T,F> findLastFieldChange( F f, Change<T,F> start ) {
        Change<T,F> c = start;
        while( !c.changedFields.containsKey( f ) ) {
            c = c.prevChange;
        }
        return c;
    }
    
    /**
     Count of unresolved conflicts
     */
    int pendingConflictCount = 0;
    
    /**
     Add a new conflict
     @param c The new conflict
     */
    private void addFieldConflict( FieldConflict<F> c ) {
        fieldConflicts.put( c.getField(), c );
        pendingConflictCount++;
    }

    /**
     A conflict was resolved
     @param c
     */
    void resolvedConflict( FieldConflict<F> c ) {
        pendingConflictCount--;
    }
        
    
    /**
     Calculate UUID for this change
     */
    private void calcUuid() {
        ChangeDTO<T,F> data = new ChangeDTO<T, F>( this );
        try {
            uuid = data.calcUuid();
        } catch ( IOException e ) {
        }
    }
    
    /**
     Serializes the field changes to a stream
     @param s
     */
    private void writeFieldChanges( ObjectOutputStream s ) throws IOException {
        s.writeInt( changedFields.size() );
        List<F> fieldsSorted = 
                new ArrayList<F>( changedFields.keySet() );
        Collections.sort( fieldsSorted );
        for ( F f : fieldsSorted ) {
            s.writeObject( f );
            s.writeObject( changedFields.get( f ) );
        }
    }
    
    private void readFieldChanges( ObjectInputStream s ) throws IOException, ClassNotFoundException {
        int count = s.readInt();
        changedFields.clear();
        for ( int n = 0; n < count; n++ ) {
            F f = (F) s.readObject();
            Object val = s.readObject();
            changedFields.put( f, val );
        }
    }
    

    /**
     Freeze this change. When change is freezed
     <ul>
     <li>If the prevChange has not been set, the current version of 
     target object is set as parent of this change.</li>
     <li>The change is added to change hsitory of the target obejct</li>
     <li>UUID of this change is calculated from serialized form of the cahnge</li>
     </ul>
     
     After freezing a change it cannot be modified anymore.
     */
    public void freeze() {
        if ( hasConflicts() ) {
            throw new IllegalStateException( "Cannot freeze change that has unresolved conflicts" );
        }

        // TODO: Parent change handling should probably be done in editor...
        if ( parentChanges.isEmpty() ) {
            Change<T, F> targetVersion = targetHistory.getVersion();
            if ( targetVersion != null ) {
                parentChanges.add( targetHistory.getVersion() );
            }
            /* This is an initial change so we must populate all fields with 
            their default values
             */
            targetHistory.initFirstChange( this );
        }

        calcUuid();
        frozen = true;
        for ( Change<T,F> parent : parentChanges ) {
            parent.addChildChange( this );
        }
        targetHistory.addChange( this );
//        targetHistory.applyChange( this );
    }
    
    /**
     Helper method to check that this cahnge is frozen and throw exception if it
     is not.
     @throws IllegalStateException if the change is not frozen
     */
    private void assertFrozen() {
        if ( !frozen ) {
            throw new IllegalStateException( "Change is not frozen" );
        }
    }
    
    /**
     Helper method to check that this cahnge is not frozen and throw exception 
     if it is.
     @throws IllegalStateException if the change is frozen
     */
    private void assertNotFrozen() {
        if ( frozen ) {
            throw new IllegalStateException( "Change is frozen" );
        }
    }
    
    @Override
    public boolean equals( Object o ) {
        if ( o == null ) {
            return false;
        }
        if ( ! (o instanceof Change) ) {
            return false;
        }
        Change c = (Change)o;
        if ( !c.frozen ) {
            throw new IllegalArgumentException( "Cannot compare equality to non-frozen change!!!");
        }
        return ( c.uuid == uuid || ( uuid != null && uuid.equals( c.uuid ) ) );
    }

    @Override
    public int hashCode() {
        if ( !frozen) {
            throw new IllegalArgumentException( "Cannot compare equality to non-frozen change!!!");
        }
        int hash = 3;
        hash = 89 * hash + (this.uuid != null ? this.uuid.hashCode() : 0);
        return hash;
    }
}
