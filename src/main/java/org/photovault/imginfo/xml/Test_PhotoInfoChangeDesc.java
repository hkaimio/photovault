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

package org.photovault.imginfo.xml;

import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.photovault.command.CommandException;
import org.photovault.command.PhotovaultCommandHandler;
import org.photovault.imginfo.ChangePhotoInfoCommand;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;
import org.photovault.test.PhotovaultTestCase;
import org.testng.annotations.Test;

/**
 Test cases for PhotoInfoChangeDesc
 @author Harri Kaimio
 @since 0.6.0
 */
public class Test_PhotoInfoChangeDesc extends PhotovaultTestCase {
    
    private static Log log 
            = LogFactory.getLog( Test_PhotoInfoChangeDesc.class.getName() );

    /**
     Simple test of change record creation and persistence.
     */
    @Test
    public void testChangeRecordCreation() {
        PhotovaultCommandHandler cmdHandler = new PhotovaultCommandHandler( null );
        
	ChangePhotoInfoCommand photoCreateCmd = new ChangePhotoInfoCommand( );
        try {
            cmdHandler.executeCommand( photoCreateCmd );
        } catch (CommandException ex) {
            fail( ex.getMessage() );
        }
        
        PhotoInfo p = photoCreateCmd.getChangedPhotos().iterator().next();
        
	ChangePhotoInfoCommand photoChangeCmd = 
                new ChangePhotoInfoCommand( p.getId() );
        photoChangeCmd.setPhotographer("Harri" );
        try {
            cmdHandler.executeCommand( photoChangeCmd );
        } catch (CommandException ex) {
            fail( ex.getMessage() );
        }
        
        Session session = HibernateUtil.getSessionFactory().openSession();
        HibernateDAOFactory hdf = (HibernateDAOFactory) DAOFactory.instance( HibernateDAOFactory.class );
        hdf.setSession( session );
        
        PhotoInfoDAO photoDAO = hdf.getPhotoInfoDAO();
        p = photoDAO.findByUUID( p.getUuid() );
        ChangeDesc v = p.getVersion();
        assert v.getTargetUuid().equals(p.getUuid());
        Set<ChangeDesc> vp = v.getPrevChanges();
        assert vp.size() == 1;
        
        ChangeDesc v2 = vp.iterator().next();
        assert v2.getTargetUuid().equals( p.getUuid() );
        assert v2.getPrevChanges().size() == 0;
    }
}
