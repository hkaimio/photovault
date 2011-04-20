/*
  Copyright (c) 2011 Harri Kaimio
  
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

package org.photovault.swingui.volumetree;

import java.util.UUID;

/**
 * Data that describes a node in volume directory tree. This class is intended 
 * to be used as an user object of DefaultMutableTreeNode in the tree model.
 * @author Harri Kaimio
 * @since 0.6.0
 */
class VolumeTreeNode {
    UUID volId;
    String folderName;
    String path;
    NodeState state = NodeState.UNINITIALIZED;



    VolumeTreeNode( UUID volId, String name, String path ) {
        this.volId = volId;
        this.folderName = name != null ? name : "";
        this.path = path != null ? path : "";
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == null ) {
            return false;
        }
        if ( o.getClass() != getClass() ) {
            return false;
        }
        VolumeTreeNode other = (VolumeTreeNode) o;
        if ( volId == null ) {
            return other.volId == null;
        }
        return volId.equals( other.volId ) && path.equals( other.path );
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (this.volId != null ? this.volId.hashCode() : 0);
        hash = 79 * hash + (this.path != null ? this.path.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        if ( folderName != null ) {
            return folderName;
        }
        if ( volId != null ) {
            return volId.toString();
        }
        return null;
    }
}
