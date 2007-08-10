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
import org.hibernate.Query;
import org.photovault.persistence.GenericHibernateDAO;

/**
 *
 * @author harri
 */
public class ImageInstanceDAOHibernate
        extends GenericHibernateDAO<ImageInstance, ImageInstance.InstanceId>
        implements ImageInstanceDAO {
    
    /** Creates a new instance of ImageInstanceDAOHibernate */
    public ImageInstanceDAOHibernate() {
    }

    public List findPhotoInstances(PhotoInfo p) {
        Query q = getSession().createQuery( "from ImageInstance i where i.photo = :p" );
        q.setEntity( "p", p );
        return q.list();
    }
    
    public ImageInstance getExistingInstance( VolumeBase volume, String fname ) {
        ImageInstance.InstanceId id = new ImageInstance.InstanceId();
        id.setFname( fname );
        id.setVolume_id( volume.getName() );
        return (ImageInstance) getSession().get( ImageInstance.class, id );
    }
    
    
}
