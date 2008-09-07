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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashSet;
import java.util.Set;

/**
 Description to a change made to a set of objects.
 
 @author Harri Kaimio
 @since 0.6.0
 */
class SetChange extends FieldChange implements Externalizable {
        
    /**
     Objects added to the set
     */
    private Set addedItems = new HashSet();
    
    /**
     Objects removed from the set
     */
    private Set removedItems = new HashSet();
    
    /**
     Constructor
     @param name Name of the set
     */
    SetChange( String name ) {
        super( name );
    }
    
    /**
     Add item to the set
     @param itemDto Data transfer obejct of the object added
     */
     
    void addItem( Object itemDto ) {
        removedItems.remove( itemDto );
        addedItems.add( itemDto );
    }
    
    /**
     Remove an item from the set
     @param itemDto Data transfer obejct of the object added
     */
    void removeItem( Object itemDto ) {
        addedItems.remove( itemDto );
        removedItems.add( itemDto );
    }

    /**
     Externalize the change
     @param out
     @throws java.io.IOException
     */
    public void writeExternal( ObjectOutput out ) throws IOException {
        out.writeObject( getName() );
        out.writeInt( addedItems.size() );
        for ( Object o : addedItems ) {
            out.writeObject( o );
        }
        out.writeInt( removedItems.size() );
        for ( Object o : removedItems ) {
            out.writeObject( o );
        }
    }

    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
        name = (String) in.readObject();
        int addedCount = in.readInt();
        for ( int n = 0; n < addedCount ; n++ ) {
            addedItems.add( in.readObject() );
        }
        int removedCount = in.readInt();
        for ( int n = 0; n < removedCount ; n++ ) {
            removedItems.add( in.readObject() );
        }
    }

    @Override
    public boolean conflictsWith( FieldChange ch ) {
        if ( !( ch instanceof SetChange ) ) {
            return true;
        }
        SetChange sch = (SetChange) ch;
        for ( Object o : removedItems ) {
            if ( sch.addedItems.contains( o ) ) {
                return true;
            }
        }
        for ( Object o : addedItems ) {
            if ( sch.removedItems.contains( o ) ) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addChange( FieldChange ch ) {
        if ( !( ch instanceof SetChange ) ) {
            throw new IllegalArgumentException( "Tried to add " + 
                    ch.getClass().getName() + " to set change" );
        }
        SetChange sch = (SetChange) ch;
        for ( Object o : sch.removedItems ) {
            addedItems.remove( o );
            removedItems.add( o );
        }
        for ( Object o : sch.addedItems ) {
            removedItems.remove( o );
            addedItems.add( o );
        }        
    }
    

    @Override
    public void addEarlier( FieldChange arg0 ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }    

    
    public FieldChange getReverse( Change baseline ) {
        throw new UnsupportedOperationException( "Not supported yet." );        
    }
    
    @Override
    public String toString() {
        StringBuffer b = new StringBuffer();
        if ( !addedItems.isEmpty() ) {
            b.append( "added: [" );
            boolean first = true;
            for ( Object o : addedItems ) {
                if ( !first ) {
                    b.append( ", " );
                }
                b.append( o );
                first = false;
            }
            b.append(  "] " );
        }
        if ( !removedItems.isEmpty() ) {
            b.append( "removed: [" );
            boolean first = true;
            for ( Object o : removedItems ) {
                if ( !first ) {
                    b.append( ", " );
                }
                b.append( o );
                first = false;
            }
            b.append(  "]" );
        }
        return b.toString();
    }

    @Override
    public FieldChange merge( FieldChange ch ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }


}
