/*
  Copyright (c) 2007 Harri Kaimio
 
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

package org.photovault.imginfo;

import java.util.List;
import org.photovault.persistence.GenericDAO;

/**
 *
 * @author harri
 */
public interface ImageInstanceDAO extends GenericDAO<ImageInstance, ImageInstance.InstanceId> {
    /**
     Find all instances of a given photo
     @param p The photo
     @return All known instances of p.
     */
    List findPhotoInstances(PhotoInfo p);
    
    ImageInstance getExistingInstance( VolumeBase volume, String fname );
}
