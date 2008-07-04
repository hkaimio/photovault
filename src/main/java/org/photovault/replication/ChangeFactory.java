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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 ChangeFactory provides a link between the change replication logic and local 
 persistence management layer. It can be used to create local persistent 
 instances from a serialized change.
 */
public class ChangeFactory<T,F extends Comparable> {
    
    /**
     DAO used for accessing local persistent data.
     */
    ChangeDAO<T,F> dao;
    
    /**
     Construct a new ChangeFactory
     @param dao The DAO that is used to access local persistent data
     */
    public ChangeFactory( ChangeDAO<T,F> dao ) {
        this.dao = dao;
    }
    
    /**
     Reads a serialized {@link ChangeDTO} from input stream and persists the 
     {@link Change} described by it in the context associated with this factory 
     if the change is not yet known in this context.
     @param is The stream from which the change is read
     @return The read change
     @throws IOException if reading the change fails
     @throws ClassNotFoundException if the change subclass or some of its fields
     are not known.
     @throws IllegalStateException if the serialized change is somehow corrupted
     (i.e. its content does not match its UUID)
     */
    public Change<T,F> readChange( ObjectInputStream is ) 
            throws IOException, ClassNotFoundException {
        ChangeDTO<T,F> data = (ChangeDTO<T, F>) is.readObject();
        UUID changeUuid = data.changeUuid;
        Change<T,F> existingChange = dao.findChange( changeUuid );
        if ( existingChange != null ) {
            return existingChange;
        }
        /*
         The change is not yet known in this context. Add it to the history of 
         the target object.
         */
        ChangeSupport<T,F> targetHistory = dao.findObjectHistory( data.targetUuid );
        if ( targetHistory == null ) {
            /*
             The target object was not known to local database.
             Create local copy
             */
            try {
               targetHistory = (ChangeSupport<T, F>) data.historyClass.newInstance();
               targetHistory.initLocalReplica( data.targetUuid );
               dao.makePersistent( targetHistory );
            } catch ( InstantiationException ex ) {
                throw new IOException( "Cannot instantiate history of class " + 
                        data.historyClass + " for object " + data.targetUuid, ex );
            } catch ( IllegalAccessException ex ) {
                throw new IOException( "Cannot instantiate history of class " + 
                        data.historyClass + " for object " + data.targetUuid, ex );
            }
            
        }
        Change<T,F> change = new Change<T, F>();
        change.setTargetHistory( targetHistory );
        change.setUuid( data.changeUuid );
        // Try to find parents of this change
        Set<Change<T,F>> parents = new HashSet<Change<T,F>>();
        for ( UUID parentId : data.parentIds ) {
           Change<T,F> parent = dao.findById( parentId, false );
           parents.add(  parent );
           parent.addChildChange( change );
        }
        change.setParentChanges( parents );
        change.setChangedFields( data.changedFields );
        dao.makePersistent( change );
        targetHistory.addChange( change );
        return change;
    }

}
