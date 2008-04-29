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

/**
 Description of a merge conflict that occurs due to conflicting values in a field
 */
public class FieldConflict<F extends Comparable> extends ConflictBase {
    /**
     The conflicting field
     */
    private F field;
    
    /**
     Constructor.
     @param field The conflicting field
     @param mergeChange Change that merges the conflicting branches
     @param conflicts Conflicting changes
     */
    public FieldConflict( F field, Change mergeChange, Change[] conflicts ) {
        super( mergeChange, conflicts );
        this.field = field;
    }
    
    /**
     Returns the conflicting field 
     */
    public F getField() {
        return field;
    }
    
    /**
     Get calue of the field in one of the conflicting changes
     @param change The changes
     @return Value of field in the change
     */
    public Object getFieldValue( Change change ) {
        if ( !changes.contains(change) ) {
            throw new IllegalArgumentException( "Change not conflicting" );
        }
        return change.getField( field );
    }

    /**
     Resulve the conflict by setting field value to the same as it is in one
     of the conflicting changes
     @param winningChange 
     */
    @Override
    public void resolve( Change winningChange ) {
        Object value = getFieldValue(winningChange);
        mergeChange.setField(field, value);
        mergeChange.resolvedConflict( this );
                
    }

}
