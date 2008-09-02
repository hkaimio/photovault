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

/**
 CHange to a value field, i.e. field whose value does not contain any substate
 */
final class ValueChange extends FieldChange implements Externalizable {
    /**
     New value for the field
     */
    private Object value;
    
    /**
     Default constructor should be used only by serialization.
     */
    public ValueChange() {
        super();
    }
    
    /**
     Constructor
     @param name Name of the field
     @param newValue New value for the field
     */
    public ValueChange( String name, Object newValue ) {
        super( name );
        value = newValue;
    }
    
    /**
     Returns the new value for changed field
     */
    public Object getValue() {
        return value;
    }

    @Override
    public boolean conflictsWith( FieldChange ch ) {
        if ( ch instanceof ValueChange ) {
            return value.equals( ((ValueChange)ch).value );
        }
        return true;
    }

    @Override
    public void addChange( FieldChange ch ) {
        if ( ch instanceof ValueChange ) {
            ValueChange vc = (ValueChange) ch;
            value = vc.value;
        }
    }

    @Override
    public void merge( FieldChange ch ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    @Override
    public String toString() {
        return "" + name + " -> " + value;
    }

    public void writeExternal( ObjectOutput out ) throws IOException {
        out.writeObject( name );
        out.writeObject( value );
    }

    public void readExternal( ObjectInput in ) throws IOException, ClassNotFoundException {
        name = (String) in.readObject();
        value = in.readObject();
    }
}
