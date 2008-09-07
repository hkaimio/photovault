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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 Change to a value field, i.e. field whose value does not contain any substate.
 
 @author Harri Kaimio
 @since 0.6.0
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
    
    void setValue( Object value ) {
        this.value = value;
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
    public void addEarlier( FieldChange ch ) {
        // No-op, as this change will replace state set by ch
    }

    
    @Override
    public FieldChange merge( FieldChange ch ) {
        if ( ! ( ch instanceof ValueChange ) ) {
            throw new IllegalArgumentException( 
                    "Cannot merge " + ch.getClass().getName() + " to ValueChange" );
        }
        ValueChange vc = (ValueChange) ch;
        Object vcVal = vc.getValue();
        ValueChange ret = new ValueChange( name, value );
        if ( value != vcVal && ( value == null || !value.equals( vcVal ) ) ) {
            List values = new ArrayList( 2 );
            values.add( value );
            values.add( vcVal );
            ret.addConflict( new FieldConflict( ret, values ) );
        }
        return ret;
    }
    
    @Override
    public FieldChange getReverse( Change baseline ) {
        Object prevValue  = null;
        for ( Change c = baseline ; c != null ; c = c.getPrevChange() ) {
            ValueChange fc = (ValueChange) c.getFieldChange( name );
            if ( fc != null ) {
                prevValue = fc.getValue();
                break;
            }
        }
        return new ValueChange( name, prevValue );
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
