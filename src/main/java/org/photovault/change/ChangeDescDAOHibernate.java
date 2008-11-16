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
import org.hibernate.Query;
import org.photovault.imginfo.xml.ChangeDesc;
import org.photovault.persistence.GenericHibernateDAO;

/**
 Hibernate implementation of ChangeDescDAO.
 @author Harri Kaimio
 @since 0.6.0
 */
public class ChangeDescDAOHibernate extends GenericHibernateDAO<ChangeDesc, UUID>
        implements ChangeDescDAO {

    /**
     * Find an existing ChangeDesc object with given uuid
     * @param uuid UUID of the object to find
     * @return The matching object or <code>null</code> if such obejct does not
     * exist.
     */
    public ChangeDesc findChange( UUID uuid ) {
        Query q = getSession().createQuery( "from ChangeDesc where uuid = :uuid" );
        q.setParameter("uuid", uuid );
        return (ChangeDesc) q.uniqueResult();                
    }
}
