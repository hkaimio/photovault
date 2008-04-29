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

import java.io.Serializable;

/**
 Test class of field descriptors
 @deprecated There isno need to use this class in production code
 */
public abstract class FieldDescriptor<T> implements Comparable, Serializable {
    
    private String name;
    
    public FieldDescriptor( String name ) {
        this.name = name;
    }

    abstract Object getValue( T target );
    
    abstract void setValue( T target, Object newValue );
    
    @Override
    public String toString() {
        return name;
    }
    
    public int compareTo( Object o ) {
        FieldDescriptor t = (FieldDescriptor)o;
        return name.compareTo( t.name );
    }

}
