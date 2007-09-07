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

import java.io.File;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.photovault.common.JUnitHibernateManager;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;
import org.photovault.test.PhotovaultTestCase;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;


/**
 * Unit test cases for{@link ImageFile}
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class Test_ImageFile extends PhotovaultTestCase {
    
    /** Creates a new instance of Test_ImageFile */
    public Test_ImageFile() {
    }
    
    HibernateDAOFactory daoFactory;
    ImageFileDAO ifDAO;
    Session session;
    VolumeBase vol1;
    VolumeBase vol2;
    
    @BeforeTest
    public void setUp() {
        JUnitHibernateManager.getHibernateManager();
        session = HibernateUtil.getSessionFactory().openSession();
        daoFactory = (HibernateDAOFactory) DAOFactory.instance( HibernateDAOFactory.class );    
        daoFactory.setSession( session );
        ifDAO = daoFactory.getImageFileDAO();
    }

    @AfterTest
    public void tearDown() throws Exception {
        session.close();
    }
   
    @Test
    public void testImageFileCreate() {
        Transaction tx = session.beginTransaction();
        ImageFile i = new ImageFile();
        i.setId( UUID.randomUUID() );
        i.setFileSize( 1000000 );
        Date lastChecked = new Date();
        Volume vol1 = new Volume();
        vol1.setBaseDir( new File( "/tmp") );
        vol1.setName( "vol1" );
        session.save( vol1 );
        ExternalVolume vol2 = new ExternalVolume();
        vol2.setBaseDir( "/usr/tmp" );
        vol2.setName( "vol2" );
        session.save( vol2 );
        FileLocation location = new FileLocation( i, vol1, "testfile1" );
        location.setLastChecked( lastChecked );
        location.setLastModified( lastChecked.getTime() );
        FileLocation location2 = new FileLocation( i, vol2, "testfile1" );
        location2.setLastChecked( lastChecked );
        location2.setLastModified( lastChecked.getTime() );
        i.addLocation( location );
        i.addLocation( location2 );
        ifDAO.makePersistent( i );
        session.flush();
        tx.commit();
        assertMatchesDb( i, session );
        
        // Try to reload the objects
        session.clear();
        
        ImageFile i2 = ifDAO.findById( i.getId(), false );
        assert i2.getFileSize() == i.getFileSize();
        // assert i2.getHash().equals( i.getHash() );
        Set<FileLocation> locations = i2.getLocations();
        assert locations.size() == 2;
        boolean vol1Found = false;
        boolean vol2Found = false;
        for ( FileLocation l : locations ) {
            VolumeBase vol = l.getVolume();
            if ( vol.getName().equals( "vol1" ) ) {
                assert vol instanceof Volume;
                assert vol.getBaseDir().equals( new File( "/tmp" ) ); 
                vol1Found = true;
            } else if ( vol.getName().equals( "vol2" ) ) {
                assert vol instanceof ExternalVolume;
                assert vol.getBaseDir().equals( new File( "/usr/tmp" ) ); 
                vol2Found = true;
            } else {
                fail( "Unknown volume" );
            }
        }
        assert vol1Found;
        assert vol2Found;
    }
    
    
    private void assertMatchesDb( ImageFile i, Session session ) {
        String query = "select ins.file_size file_size" +
                " from pv_image_files ins where ins.id = :id";
        Object result = 
                session.createSQLQuery( query ).
                addScalar( "file_size", Hibernate.LONG ).
                setString( "id", i.getId().toString()  ).
                uniqueResult();
        assert result.equals( i.getFileSize() );
    }
}
