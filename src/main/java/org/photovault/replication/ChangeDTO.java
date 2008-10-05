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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.UUID;

/**
 Data transfer object for transferring change between persistence contexts. This
 object contains all state information about the change but replaces references 
 to other objects with theur global UUIDs.
 */
public class ChangeDTO<T> implements Serializable {
    
    static final long serialVersionUID = 3937344080753904527l;
    
    /**
     UUID of the change this DTO describes
     */
    transient UUID changeUuid;
    
    /**
     UUID of the target object
     */
    transient UUID targetUuid;
    
    /**
     Class of the {@link ChangeSupport} object that owns this change.
     */
    transient Class historyClass;
    
    transient String targetClassName;
    
    /**
     UUIDs of the parents of this change, sorted into increasing order.
     */
    transient List<UUID> parentIds;
    
    /**
     Fields changed by this change and their new values.
     */
    transient SortedMap<String, FieldChange> changedFields;
    
    /**
     Construct a DTO from a change.
     @param change
     */
    public ChangeDTO( Change<T> change ) {
        changeUuid = change.getUuid();
        targetUuid = change.getTargetHistory().getTargetUuid();
        historyClass = change.getTargetHistory().getClass();
        targetClassName = change.getTargetHistory().getTargetClassName();
        parentIds = new ArrayList<UUID>();
        for ( Change<T> parent : change.getParentChanges() ) {
            parentIds.add(  parent.getUuid() );
        }
        Collections.sort( parentIds );
        changedFields = new TreeMap<String, FieldChange>( change.getChangedFields() );
    }
    
    
    /**
     Serialize this object.
     <p>
     The object state is serialized in following order:
     <ul>
     <li>changeUUID</li>
     <li>class of the owning change history</li>
     <li>uuid of the owning change history (and the target object)</li>
     <li>Number of parent changes as integer</li>
     <li>UUIDs of every parent change</li>
     <li>Number of changed fields</li>
     <li>The field change objects, sorted by field name in ascending order</li>
     </ul>
     @param os The stream into which the change is written
     @throws java.io.IOException If writing the change fails.
     */
    private void writeObject( ObjectOutputStream os ) throws IOException {
        os.defaultWriteObject();
        os.writeObject( changeUuid );
        writeChangeMetadata( os );
        writeChange( os );
    }
    
    /**
     Write the meta data related to change (i.e. parents, object history that 
     owns this change) into a stream
     @param os
     @throws java.io.IOException
     */
    private void writeChangeMetadata( ObjectOutputStream os ) throws IOException {
        os.writeObject( historyClass );
        os.writeObject( targetClassName );
        os.writeObject( targetUuid );
        os.writeInt( parentIds.size() );
        for ( UUID parentId : parentIds ) {
            os.writeObject( parentId );
        }        
    }
    
    /**
     Write the description of actual change operations to a stream.
     @param os
     @throws java.io.IOException
     */
    private void writeChange( ObjectOutputStream os ) throws IOException {
        os.writeInt( changedFields.size() );
        for ( Map.Entry<String,FieldChange> e : changedFields.entrySet() ) {
            os.writeObject( e.getValue() );
        }
    }
    
    public UUID calcUuid() throws IOException {
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream( s );
        writeChangeMetadata( os );
        /*
         Reset the strea to ensure that field values of e.g. targetUuid are written
         again and not as object references.
         */         
        os.reset();
        writeChange( os );
        byte[] serialized = s.toByteArray();
        return UUID.nameUUIDFromBytes( serialized );
    }
    
    public void verify() throws IOException {
        UUID checksum = calcUuid();
        if ( !checksum.equals( changeUuid ) ) {
            throw new IllegalStateException( "UUID of change is incorrect - excepted" + 
                    changeUuid + " but calculated " + checksum );
        }
    }
    
    /**
     Deserialize the change from a stream
     
     @param is
     @throws java.io.IOException
     @throws java.lang.ClassNotFoundException
     @throws IllegalStateException If the uuid of the read change does not match 
     change state.
     */
    private void readObject( ObjectInputStream is ) throws IOException, ClassNotFoundException {
        is.defaultReadObject();
        changeUuid = (UUID) is.readObject();
        historyClass = (Class) is.readObject();
        targetClassName = (String) is.readObject();
        targetUuid = (UUID) is.readObject();
        int parentCount = is.readInt();
        parentIds = new ArrayList<UUID>( parentCount );
        for ( int n = 0 ; n < parentCount ; n++ ) {
            parentIds.add( (UUID) is.readObject() );
        }
        int changedFieldCount = is.readInt();
        changedFields = new TreeMap();
        for ( int n = 0 ; n < changedFieldCount ; n++ ) {
            FieldChange val = (FieldChange) is.readObject();
            changedFields.put(  (String) val.getName(), val);
        }
        verify();
    }

}
