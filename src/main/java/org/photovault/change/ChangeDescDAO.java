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

package org.photovault.change;

import java.util.UUID;
import org.photovault.imginfo.xml.ChangeDesc;
import org.photovault.persistence.GenericDAO;

/**
 Data access object for ChangeDesc
 @author harri
 */
public interface ChangeDescDAO extends GenericDAO<ChangeDesc, UUID> {

    /**
     Find change with given UUID. Unlike the fidnById method this method does 
     not return a proxy if change descriptor is not found in local database
     @param uuid UUID of the change descriptor to find.
     @return The change descriptor if it exists or <code>null</code> if there is 
     no matching local copy.
     */
    public ChangeDesc findChange( UUID uuid );

}
