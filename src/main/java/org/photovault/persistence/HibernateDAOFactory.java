/*
  Copyright (c) 2007 Harri Kaimio
  
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

package org.photovault.persistence;

import org.hibernate.Session;
import org.photovault.folder.PhotoFolderDAO;
import org.photovault.folder.PhotoFolderDAOHibernate;
import org.photovault.imginfo.ImageFileDAO;
import org.photovault.imginfo.ImageFileDAOHibernate;
import org.photovault.imginfo.ImageInstanceDAO;
import org.photovault.imginfo.ImageInstanceDAOHibernate;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.imginfo.PhotoInfoDAOHibernate;

/**
 Factory for creating Hibernate data access objects for Photovault.
 */
public class HibernateDAOFactory extends DAOFactory {
    
    /**
     * Creates a new instance of HibernateDAOFactory
     */
    public HibernateDAOFactory() {
    }

    private GenericHibernateDAO instantiateDAO( Class daoClass ) {
        try {
            GenericHibernateDAO dao = (GenericHibernateDAO)daoClass.newInstance();
            if ( session != null ) {
                dao.setSession( session );
            }
            return dao;
        } catch ( Exception e ) {
            throw new RuntimeException( "Could not instantiate DAO factory " + daoClass, e );
        }
    }
    
    Session session = null;
    
    public void setSession( Session session ) {
        this.session = session;
    }
    
    
    public PhotoInfoDAO getPhotoInfoDAO() {
        return (PhotoInfoDAO) instantiateDAO( PhotoInfoDAOHibernate.class );
    }
    
    public PhotoFolderDAO getPhotoFolderDAO() {
        return (PhotoFolderDAO) instantiateDAO( PhotoFolderDAOHibernate.class );
    }
    
    public ImageInstanceDAO getImageInstanceDAO() {
        return (ImageInstanceDAO) instantiateDAO( ImageInstanceDAOHibernate.class );
    }

    public ImageFileDAO getImageFileDAO() {
        return (ImageFileDAO) instantiateDAO( ImageFileDAOHibernate.class );        
    }
    
}
