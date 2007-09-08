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

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.photovault.command.PhotovaultCommandHandler;
import org.photovault.common.JUnitHibernateManager;
import org.photovault.common.PhotovaultException;
import org.photovault.image.ChannelMapOperation;
import org.photovault.image.ChannelMapOperationFactory;
import org.photovault.image.ColorCurve;
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

        vol1 = new Volume();
        vol1.setBaseDir( new File( "/tmp") );
        vol1.setName( "vol1" );
        session.save( vol1 );
    }

    @AfterTest
    public void tearDown() throws Exception {
        session.delete( vol1 );
        session.close();
    }
   
    @Test
    public void testImageFileCreate() {
        Transaction tx = session.beginTransaction();
        ImageFile i = new ImageFile();
        i.setId( UUID.randomUUID() );
        i.setFileSize( 1000000 );
        Date lastChecked = new Date();
        ExternalVolume vol2 = new ExternalVolume();
        vol2.setBaseDir( "/usr/tmp" );
        vol2.setName( "vol2" );
        session.save( vol2 );
        FileLocation location = new FileLocation( vol1, "testfile1" );
        location.setLastChecked( lastChecked );
        location.setLastModified( lastChecked.getTime() );
        FileLocation location2 = new FileLocation( vol2, "testfile1" );
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
    
    @Test
    public void testImageDescriptorPersistence() {
        Transaction tx = session.beginTransaction();
        ImageFileDAO ifDAO = daoFactory.getImageFileDAO();
        ImageDescriptorDAO idDAO = daoFactory.getImageDescriptorDAO();
        ImageFile f1 = new ImageFile();
        f1.setId( UUID.randomUUID() );
        ImageFile f2 = new ImageFile();
        f2.setId( UUID.randomUUID() );
        ifDAO.makePersistent( f1 );
        ifDAO.makePersistent( f2 );
        
        OriginalImageDescriptor i11 = new OriginalImageDescriptor( f1, "image#0" );
        i11.setWidth( 1200 );
        i11.setHeight( 1400 );
        idDAO.makePersistent( i11 );
        CopyImageDescriptor i12 = new CopyImageDescriptor( f1, "image#1", i11 );
        i12.setCropArea( new Rectangle2D.Double( 0.1, 0.2, 0.3, 0.4 ) );
        i12.setWidth( 300 );
        i12.setHeight( 400 );
        idDAO.makePersistent( i12 );
        OriginalImageDescriptor i21 = new OriginalImageDescriptor( f2, "image#0" );
        i21.setWidth( 2000 );
        i21.setHeight( 3000 );
        idDAO.makePersistent( i21 );
        CopyImageDescriptor i22 = new CopyImageDescriptor( f2, "image#1", i11 );
        i22.setWidth( 400 );
        i22.setHeight( 400 );
        i22.setRotation( 90.0 );
        ChannelMapOperationFactory f = new ChannelMapOperationFactory();
        ColorCurve c = new ColorCurve();
        c.addPoint( 0.0, 0.1 );
        c.addPoint( 1.0, 0.9 );
        f.setChannelCurve( "value", c );
        ChannelMapOperation cm = f.create();
        i22.setColorChannelMapping( cm );
        idDAO.makePersistent( i22 );
        
        session.flush();
        tx.commit();
        
        // Verify that changes were persisted correctly
        session.clear();
        ImageFile vf1 = ifDAO.findById( f1.getId(), false );
        int vf1isize = vf1.getImages().size();
        assert vf1.getImages().size() == 2;
        OriginalImageDescriptor vi11 = (OriginalImageDescriptor) vf1.getImages().get( "image#0" );
        CopyImageDescriptor vi12 = (CopyImageDescriptor) vf1.getImages().get( "image#1" );
        assertEquals( 2, vi11.getCopies().size() );
        assertTrue( vi12.getOriginal() == vi11 );
        ImageFile vf2 = ifDAO.findById( f2.getId(), false );
        assertEquals( 2, vf2.getImages().size() );
        OriginalImageDescriptor vi21 = (OriginalImageDescriptor) vf2.getImages().get( "image#0" );
        assertEquals( 0, vi21.getCopies().size() );
        CopyImageDescriptor vi22 = (CopyImageDescriptor) vf2.getImages().get( "image#1" );
        assert vi22.getOriginal() == vi11;
        assertEquals( 0.1, vi12.getCropArea().getMinX() );
        assertEquals( cm, vi22.getColorChannelMapping() );
    }
    
    @Test
    public void testImageCreationCmd() throws PhotovaultException, IOException {
        File testDir = new File( System.getProperty( "basedir" ), "testfiles" );
        File testFile = new File( testDir, "test1.jpg" );
        CreateImageInstanceCommand cmd = 
                new CreateImageInstanceCommand( null, testFile, null, 
                ImageInstance.INSTANCE_TYPE_ORIGINAL );
        PhotovaultCommandHandler cmdHandler = new PhotovaultCommandHandler( null );
        cmdHandler.executeCommand( cmd );
        
        ImageFile f = cmd.getImageFile();
        UUID fileId = f.getId();
        // UUID instanceId = cmd.getImageInstance().getUUID();
        
        f = ifDAO.findById( fileId, false );
        assert f.getFileSize() == testFile.length();
        assert f.getImages().size() == 1;
        assertEquals( 0, f.getLocations().size() );        
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
