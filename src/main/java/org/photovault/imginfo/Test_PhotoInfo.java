/*
  Copyright (c) 2006 Harri Kaimio
  
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

package org.photovault.imginfo;

import java.awt.geom.Rectangle2D;
import java.io.*;
import junit.framework.*;
import java.util.*;
import java.sql.*;
import java.awt.image.*;
import javax.imageio.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.photovault.command.CommandException;
import org.photovault.command.PhotovaultCommandHandler;
import org.photovault.common.PhotovaultException;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;
import org.photovault.test.PhotovaultTestCase;

public class Test_PhotoInfo extends PhotovaultTestCase {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Test_PhotoInfo.class.getName() );

    String testImgDir = "testfiles";
    String nonExistingDir = "/tmp/_dirThatDoNotExist";
    Session session = null;
    Transaction tx = null;
    
    DAOFactory daoFactory;
    PhotoInfoDAO photoDAO;
    
    /**
     * Default constructor to set up OJB environment
     */
    public Test_PhotoInfo() {
        super();
    }

    /**
     Sets ut the test environment
     */
    public void setUp() {
        session = HibernateUtil.getSessionFactory().openSession();
        HibernateDAOFactory hdf = (HibernateDAOFactory) DAOFactory.instance( HibernateDAOFactory.class );
        hdf.setSession( session );
        daoFactory = hdf;
        photoDAO = daoFactory.getPhotoInfoDAO();
        tx = session.beginTransaction();
    }
    
    /**
     Tears down the testing environment
     */
    public void tearDown() {
        tx.commit();
        session.close();
    }   

    
    //    File testRefImageDir = new File( "c:\\java\\photovault\\tests\\images\\photovault\\imginfo" );
    File testRefImageDir = new File( "tests/images/photovault/imginfo" );
    /**
       Test case that verifies that an existing photo infor record 
       can be loaded successfully
    */
    public void testRetrievalSuccess() {
	int photoId = 1;
        PhotoInfo photo = null;
        photo = photoDAO.findById( Integer.valueOf( photoId ), false );
        assertNotNull( photo );
	// TODO: check some other properties of the object

    }

    /**
       Test case that verifies that an existing photo info record 
       can be loaded successfully
    */
    // Not valid anymore due to lazy initialization logic.
//    public void testRetrievalNotFound() {
//	int photoId = -1;
//        PhotoInfo photo = null;
//        photo = photoDAO.findById( Integer.valueOf( photoId ), false );
//        assertNull( "Image " + photoId + " should not exist." , photo );
//    }

    /** 
	Test updating object to DB
    */
    public void testUpdate() {
	int photoId = 1;
        PhotoInfo photo = null;
        photo = photoDAO.findById( Integer.valueOf( photoId ), false );
        assertTrue(photo != null );
        
	// Update the photo
	String shootingPlace = photo.getShootingPlace();
	String newShootingPlace = "Testipaikka";
	photo.setShootingPlace( newShootingPlace );
        photoDAO.flush();
        assertMatchesDb( photo );
    }

    /** 
	Test updating object to DB when shooting date has not been specified
    */
    public void testNullShootDateUpdate() {
	int photoId = 1;
	PhotoInfo photo = null;	
	Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
	try {
	    photo = PhotoInfo.findPhotoInfo( session, photoId );
	    assertTrue(photo != null );	    
	} catch (PhotoNotFoundException e) {
	    fail( "Photo " + photoId + " not found" );
	}
	
	java.util.Date origTime = photo.getShootTime();
	// Update the photo
	photo.setShootTime( null );
        tx.commit();
        session.close();
        
	// retrieve the updated photo from DB and chech that the
	// modification has been done
        session = HibernateUtil.getSessionFactory().openSession();
        tx = session.beginTransaction();	
	try {
	    photo = PhotoInfo.findPhotoInfo( session, photoId );
	    assertNull( "Shooting time was supposedly set to null", photo.getShootTime() );	    
	} catch (PhotoNotFoundException e) {
	    fail( "Photo " + photoId + " not found after updating" );
	}


	// restore the shooting place
	photo.setShootTime( origTime );
        tx.commit();
        session.close();
    }

    /**
       Test normal creation of a persistent PhotoInfo object
    */
    public void testPhotoCreation() {
        
	PhotoInfo photo = new PhotoInfo();
	Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        try {
            session.save( photo );
        } catch ( Throwable t ) {
            fail( t.getMessage() );
        }
	assertNotNull( photo );
        photo.setUuid( UUID.randomUUID() );
	photo.setPhotographer( "TESTIKUVAAJA" );
	photo.setShootingPlace( "TESTPLACE" );
	photo.setShootTime( new java.util.Date() );
	photo.setFStop( 5.6 );
	photo.setShutterSpeed( 0.04 );
	photo.setFocalLength( 50 );
	photo.setCamera( "Canon FTb" );
	photo.setFilm( "Tri-X" );
	photo.setFilmSpeed( 400 );
	photo.setLens( "Canon FD 50mm/F1.4" );
        photo.setCropBounds( new Rectangle2D.Double( 0.1, 0.2, 0.5, 0.7 ) );
	photo.setDescription( "This is a long test description that tries to verify that the description mechanism really works" );
	//	photo.updateDB();
        tx.commit();
        session.close();
        
        Session session2 = HibernateUtil.getSessionFactory().openSession();
        tx = session2.beginTransaction();
        
        PhotoInfo photo2 = null;
        try {
	    photo2 = PhotoInfo.findPhotoInfo( session2, photo.getUid() );

	    assertEquals( photo.getPhotographer(), photo2.getPhotographer() );
	    assertEquals( photo.getShootingPlace(), photo2.getShootingPlace() );
	    // assertEquals( photo.getShootTime(), photo2.getShootTime() );
	    assertEquals(photo.getDescription(), photo2.getDescription() );
	    assertEquals( photo.getCamera(), photo2.getCamera() );
	    assertEquals( photo.getLens(), photo2.getLens() );
	    assertEquals( photo.getFilm(), photo2.getFilm() );
	    assertTrue( photo.getShutterSpeed() == photo2.getShutterSpeed() );
	    assertTrue( photo.getFilmSpeed() == photo2.getFilmSpeed() );
	    assertTrue( photo.getFocalLength() == photo2.getFocalLength() );
	    assertTrue( photo.getFStop() == photo2.getFStop() );
	    assertTrue( photo.getUid() == photo2.getUid() );
	    assertTrue( photo.getUuid().equals( photo2.getUuid() ) );
            assertTrue( photo.getCropBounds().equals( photo2.getCropBounds() ) );
            
	    //	    assertTrue( photo.equals( photo2 ));
			
	} catch ( PhotoNotFoundException e ) {
	    fail ( "inserted photo not found" );
	}
        session2.delete( photo2 );
        tx.commit();
        session2.close();
    }

    /**
       Test normal creation of a persistent PhotoInfo object
    */
    public void testChangeCommand() {
        PhotovaultCommandHandler cmdHandler = new PhotovaultCommandHandler( null );
        
	ChangePhotoInfoCommand photoCreateCmd = new ChangePhotoInfoCommand( );
        try {
            cmdHandler.executeCommand( photoCreateCmd );
        } catch (CommandException ex) {
            fail( ex.getMessage() );
        }
        Set<PhotoInfo> photos = photoCreateCmd.getChangedPhotos();
        assertEquals( 1, photos.size() );
        PhotoInfo photo = photos.toArray( new PhotoInfo[1] )[0];
        
        Session session = HibernateUtil.getSessionFactory().openSession();
        // Transaction tx = session.beginTransaction();
        photo = (PhotoInfo) session.merge( photo );
        
	assertNotNull( photo );
        
	ChangePhotoInfoCommand photoChangeCmd = new ChangePhotoInfoCommand( photo.getId() );
        
        photoChangeCmd.setUUID( UUID.randomUUID() );
	photoChangeCmd.setPhotographer( "TESTIKUVAAJA" );
	photoChangeCmd.setShootingPlace( "TESTPLACE" );
	photoChangeCmd.setShootTime( new java.util.Date() );
	photoChangeCmd.setFStop( 5.6 );
	photoChangeCmd.setShutterSpeed( 0.04 );
	photoChangeCmd.setFocalLength( 50 );
	photoChangeCmd.setCamera( "Canon FTb" );
	photoChangeCmd.setFilm( "Tri-X" );
	photoChangeCmd.setFilmSpeed( 400 );
	photoChangeCmd.setLens( "Canon FD 50mm/F1.4" );
        photoChangeCmd.setCropBounds( new Rectangle2D.Double( 0.1, 0.2, 0.5, 0.7 ) );
	photoChangeCmd.setDescription( "This is a long test description that tries to verify that the description mechanism really works" );

        try {
            cmdHandler.executeCommand( photoChangeCmd );
        } catch (CommandException ex) {
            fail( ex.getMessage() );
        }
        photos = photoChangeCmd.getChangedPhotos();
        assertEquals( 1, photos.size() );
        photo = photos.toArray( new PhotoInfo[1] )[0];
        photo = (PhotoInfo) session.merge( photo );
        
        
        // tx.commit();
        // session.close();
        
        Session session2 = HibernateUtil.getSessionFactory().openSession();
        Transaction tx2 = session2.beginTransaction();
        
        PhotoInfo photo2 = null;
        try {
	    photo2 = PhotoInfo.findPhotoInfo( session2, photo.getUid() );

	    assertEquals( photo.getPhotographer(), photo2.getPhotographer() );
	    assertEquals( photo.getShootingPlace(), photo2.getShootingPlace() );
	    // assertEquals( photo.getShootTime(), photo2.getShootTime() );
	    assertEquals(photo.getDescription(), photo2.getDescription() );
	    assertEquals( photo.getCamera(), photo2.getCamera() );
	    assertEquals( photo.getLens(), photo2.getLens() );
	    assertEquals( photo.getFilm(), photo2.getFilm() );
	    assertTrue( photo.getShutterSpeed() == photo2.getShutterSpeed() );
	    assertTrue( photo.getFilmSpeed() == photo2.getFilmSpeed() );
	    assertTrue( photo.getFocalLength() == photo2.getFocalLength() );
	    assertTrue( photo.getFStop() == photo2.getFStop() );
	    assertTrue( photo.getUid() == photo2.getUid() );
	    assertTrue( photo.getUuid().equals( photo2.getUuid() ) );
            assertTrue( photo.getCropBounds().equals( photo2.getCropBounds() ) );
            
	    //	    assertTrue( photo.equals( photo2 ));
			
	} catch ( PhotoNotFoundException e ) {
	    fail ( "inserted photo not found" );
	}
        tx2.commit();
        session2.close();
    }

    public void testPhotoDeletion() {
	PhotoInfo photo = new PhotoInfo();
	Session session = HibernateUtil.getSessionFactory().openSession();
        Transaction tx = session.beginTransaction();
        session.save( photo );
        tx.commit();
        
	// Check that the photo can be retrieved from DB

	Connection conn = session.connection();
	String sql = "SELECT * FROM photos WHERE photo_id = " + photo.getUid();
	Statement stmt = null;
	ResultSet rs = null;
	try {
	    stmt = conn.createStatement();
	    rs = stmt.executeQuery( sql );
	    if ( !rs.next() ) {
		fail( "Matching DB record not found" );
	    }
	} catch ( SQLException e ) {
	    fail( "DB error:; " + e.getMessage() );
	} finally {
	    if ( rs != null ) {
		try {
		    rs.close();
		} catch ( Exception e ) {}
	    }
	    if ( stmt != null ) {
		try {
		    stmt.close();
		} catch ( Exception e ) {}
	    }
	}

        tx = session.beginTransaction();
        session.delete( photo );
        tx.commit();
        // Check that the photo is deleted from the database
	try {
	    stmt = conn.createStatement();
	    rs = stmt.executeQuery( sql );
	    if ( rs.next() ) {
		fail( "Found matching DB record after delete" );
	    }
	} catch ( SQLException e ) {
	    fail( "DB error:; " + e.getMessage() );
	} finally {
	    if ( rs != null ) {
		try {
		    rs.close();
		} catch ( Exception e ) {}
	    }
	    if ( stmt != null ) {
		try {
		    stmt.close();
		} catch ( Exception e ) {}
	    }
	}
        session.close();
    }
    
    public void testInstanceAddition() {
        File testFile = new File( testImgDir, "test1.jpg" );
	File instanceFile = VolumeBase.getDefaultVolume().getFilingFname( testFile );
	try {
	    FileUtils.copyFile( testFile, instanceFile );
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}  
        
	PhotoInfo photo = PhotoInfo.create();
	assertNotNull( photo );
        photo = photoDAO.makePersistent( photo );
        
	int numInstances = photo.getNumInstances();
	photo.addInstance( VolumeBase.getDefaultVolume(), instanceFile, ImageInstance.INSTANCE_TYPE_ORIGINAL );
	// Check that number of instances is consistent with addition
	assertEquals( numInstances+1, photo.getNumInstances() );
	Set<ImageInstance> instances = photo.getInstances();
	assertEquals( instances.size(), numInstances+1 );

        // Add another instance using different method
        File testFile2 = new File( testImgDir, "test2.jpg" );
	File instanceFile2 = VolumeBase.getDefaultVolume().getFilingFname( testFile2 );
	try {
	    FileUtils.copyFile( testFile2, instanceFile2 );
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}  
        
	numInstances = photo.getNumInstances();
        ImageInstance inst = ImageInstance.create( VolumeBase.getDefaultVolume(), instanceFile2 );
	photo.addInstance( inst );
	// Check that number of instances is consistent with addition
	assertEquals( numInstances+1, photo.getNumInstances() );
	instances = photo.getInstances();
	assertEquals( instances.size(), numInstances+1 );
        
	// Try to find the instance
        boolean found1 = false;
        boolean found2 = false;

        instances = photo.getInstances();
        
	for ( ImageInstance ifile : instances ) {
	    if ( ifile.getImageFile().equals( instanceFile ) ) {
		found1 = true;
	    }
	    if ( ifile.getImageFile().equals( instanceFile2 ) ) {
		found2 = true;
	    }
            
	}
        assertTrue( "Image instance 1 not found", found1 );
        assertTrue( "Image instance 2 not found", found2 );
        session.flush();
        assertMatchesDb( photo );
	// Clean the DB
	photo.delete();
    }
	
    public void testCreationFromImage() {
	String fname = "test1.jpg";
	File f = new File( testImgDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Could not find photo: " + e.getMessage() );
	}
	assertNotNull( photo );
	assertTrue( photo.getNumInstances() > 0 );
	photo.delete();
    }

    /**
       Test that an exception is generated when trying to add
       nonexisting file to DB
    */
    public void testfailedCreation() {
	String fname = "test1.jpg";	
	File f = new File( nonExistingDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	    // Execution should never proceed this far since addToDB
	    // should produce exception
	    fail( "Image file should have been nonexistent" );
	} catch ( PhotoNotFoundException e ) {
	    // This is what we except
	}
    }

    /**
       Test that creating a new thumbnail using createThumbnail works
     */
    public void testThumbnailCreate() {
	String fname = "test1.jpg";
	File f = new File( testImgDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Could not find photo: " + e.getMessage() );
	}
	assertNotNull( photo );
        photo = photoDAO.makePersistent( photo );
	int instanceCount = photo.getNumInstances();
	photo.createThumbnail();
	assertEquals( "InstanceNum should be 1 greater after adding thumbnail",
		     instanceCount+1, photo.getNumInstances() );
	// Try to find the new thumbnail
	boolean foundThumbnail = false;
	ImageInstance thumbnail = null;
	for ( ImageInstance instance : photo.getInstances() ) {
	    if ( instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_THUMBNAIL ) {
		foundThumbnail = true;
		thumbnail = instance; 
		break;
	    }
	}
	assertTrue( "Could not find the created thumbnail", foundThumbnail );
	assertEquals( "Thumbnail width should be 100", 100, thumbnail.getWidth() );
	File thumbnailFile = thumbnail.getImageFile();
	assertTrue( "Image file does not exist", thumbnailFile.exists() );

	// Test the getThumbnail method
	Thumbnail thumb = photo.getThumbnail();
	assertNotNull( thumb );
	assertFalse( "Thumbnail exists, should not return default thumbnail",
		     thumb == Thumbnail.getDefaultThumbnail() );
	assertEquals( "Thumbnail exists, getThumbnail should not create a new instance",
		     instanceCount+1, photo.getNumInstances() );
	
        session.flush();
        assertMatchesDb( photo );
        
//	// Assert that the thumbnail is saved correctly to the database
//	PhotoInfo photo2 = photoDAO.findById( photo.getId(), false );
//						
//	// Try to find the new thumbnail
//	foundThumbnail = false;
//	ImageInstance thumbnail2 = null;
//	for ( ImageInstance instance : photo2.getInstances()  ) {
//	    if ( instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_THUMBNAIL ) {
//		foundThumbnail = true;
//		thumbnail2 = instance; 
//		break;
//	    }
//	}
//	assertTrue( "Could not find the created thumbnail", foundThumbnail );
//	assertEquals( "Thumbnail width should be 100", 100, thumbnail2.getWidth() );
//	assertTrue( "Thumbnail filename not saved correctly", thumbnailFile.equals( thumbnail2.getImageFile() ));
	photo.delete();
	assertFalse( "Image file does exist after delete", thumbnailFile.exists() );
    }


    /**
       Tests thumbnail creation when there are no photo instances.
    */
    public void testThumbnailCreateNoInstances() throws Exception {
	PhotoInfo photo = PhotoInfo.create();
	try {
	    photo.createThumbnail();
	    assertEquals( "Should not create a thumbnail instance when there are no original",
			  0, photo.getNumInstances() );
	} catch (Exception e ) {
	    throw e;
	} finally {
	    photo.delete();
	}
    }

    /**
       Tests thumbnail creation when the database is corrupted & files
       that photo instances refer to do not exist.
    */
    public void testThumbnailCreateCorruptInstances() throws Exception {	
	String fname = "test1.jpg";
	File f = new File( testImgDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Could not find photo: " + e.getMessage() );
	}

	// Corrupt the database by deleting the actual image files
	// that instances refer to
	for ( ImageInstance instance : photo.getInstances() ) {
	    File instFile = instance.getImageFile();
	    instFile.delete();
	}

        int numInstances = photo.getNumInstances();
	// Create the thumbnail
	photo.createThumbnail();

	try {
	    Thumbnail thumb = photo.getThumbnail();
	    assertNotNull( thumb );
	    assertTrue( "Database is corrupt, should return error thumbnail",
			thumb == Thumbnail.getErrorThumbnail() );
	    assertEquals( "Database is corrupt, getThumbnail should not create a new instance",
			  numInstances, photo.getNumInstances() );
	    
	} finally {
	    // Clean up in any case
	    photo.delete();
	}
    }
    
    /**
       Test that creating a new thumbnail using getThumbnail works
     */
    public void testGetThumbnail() {
	String fname = "test1.jpg";
	File f = new File( testImgDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Could not find photo: " + e.getMessage() );
	}

	assertNotNull( photo );
	int instanceCount = photo.getNumInstances();
	Thumbnail thumb = photo.getThumbnail();
	assertNotNull( thumb );
	assertFalse( "Thumbnail exists, should not return default thumbnail",
		     thumb == Thumbnail.getDefaultThumbnail() );
	assertEquals( "Thumbnail exists, getThumbnail should not create a new instance",
		     instanceCount+1, photo.getNumInstances() );
	
	assertEquals( "InstanceNum should be 1 greater after adding thumbnail",
		     instanceCount+1, photo.getNumInstances() );
	// Try to find the new thumbnail
	boolean foundThumbnail = false;
	ImageInstance thumbnail = null;
	for ( ImageInstance instance : photo.getInstances() ) {
	    if ( instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_THUMBNAIL ) {
		foundThumbnail = true;
		thumbnail = instance; 
		break;
	    }
	}
	assertTrue( "Could not find the created thumbnail", foundThumbnail );
	assertEquals( "Thumbnail width should be 100", 100, thumbnail.getWidth() );
	File thumbnailFile = thumbnail.getImageFile();
	assertTrue( "Image file does not exist", thumbnailFile.exists() );
	photo.delete();
	assertFalse( "Image file does exist after delete", thumbnailFile.exists() );
    }

    /**
       Test getThumbnail in situation where there is no image instances for the PhotoInfo
    */
    public void testThumbWithNoInstances() {
	log.setLevel( org.apache.log4j.Level.DEBUG );
	org.apache.log4j.Logger photoLog = org.apache.log4j.Logger.getLogger( PhotoInfo.class.getName() );
	photoLog.setLevel( org.apache.log4j.Level.DEBUG );
        
	PhotoInfo photo = PhotoInfo.create();
	Thumbnail thumb = photo.getThumbnail();
        // TODO: Should getThumbnail really return defaultThumbnail in this situation?
	assertTrue( "getThumbnail should return error thumbnail",
		    thumb == Thumbnail.getErrorThumbnail() ) ;
	assertEquals( "No new instances should have been created", 0, photo.getNumInstances() );

	// Create a new instance and check that a valid thumbnail is returned after this
	File testFile = new File( testImgDir, "test1.jpg" );
        if ( !testFile.exists() ) {
            fail( "could not find test file " + testFile );
        }
	File instanceFile = VolumeBase.getDefaultVolume().getFilingFname( testFile );
	try {
	    FileUtils.copyFile( testFile, instanceFile );
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}
	photo.addInstance( VolumeBase.getDefaultVolume(), instanceFile, ImageInstance.INSTANCE_TYPE_ORIGINAL );
	Thumbnail thumb2 = photo.getThumbnail();
	log.setLevel( org.apache.log4j.Level.WARN );
	photoLog.setLevel( org.apache.log4j.Level.WARN );
   
        assertFalse( "After instance addition, getThumbnail should not return default thumbnail",
			thumb == thumb2 );
	assertEquals( "There should be 2 instances: original & thumbnail", 2, photo.getNumInstances() );

	photo.delete();
		      
    }

    /**
       Test that thumbnail is rotated if prefRotation is nonzero
    */

    public void testThumbnailRotation() {
	String fname = "test1.jpg";
	File f = new File( testImgDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Could not find photo: " + e.getMessage() );
	}
	photo.setPrefRotation( -45 );

	Thumbnail thumb = photo.getThumbnail();

	// Compare thumbnail to the one saved
	File testFile = new File ( testRefImageDir, "thumbnailRotation1.png" );
	assertTrue( "Thumbnail with 45 deg rotation does not match",
		    org.photovault.test.ImgTestUtils.compareImgToFile( thumb.getImage(), testFile ) );

	photo.setPrefRotation( -90 );
	thumb = photo.getThumbnail();
	testFile = new File ( testRefImageDir, "thumbnailRotation2.png" );
	assertTrue( "Thumbnail with 90 deg rotation does not match",
		    org.photovault.test.ImgTestUtils.compareImgToFile( thumb.getImage(), testFile ) );

	photo.delete();
    }

    /**
       PhotoInfoListener used for test cases
    */
    class TestListener implements PhotoInfoChangeListener {
	public boolean isNotified = false;
	
	public void photoInfoChanged( PhotoInfoChangeEvent e ) {
	    isNotified = true;
	}
    }
    
    /**
       Tests that the listener is working correctly
    */
    public void testListener() {

	PhotoInfo photo = PhotoInfo.create();
	TestListener l1 = new TestListener();
	TestListener l2 = new TestListener();
	photo.addChangeListener( l1 );
	photo.addChangeListener( l2 );

	// Test that the listeners are notified
	photo.setPhotographer( "TEST" );
	assertTrue( "l1 was not notified", l1.isNotified );
	assertTrue( "l2 was not notified", l2.isNotified );

	// Test that the listeners are removed correctly
	photo.removeChangeListener( l2 );
	l1.isNotified = false;
	l2.isNotified = false;
	photo.setPhotographer( "TEST2" );
	assertTrue( "l1 was not notified", l1.isNotified );
	assertFalse( "l2 was not supposed to be notified", l2.isNotified );

	// Test all object fields, one by one
	l1.isNotified = false;
	photo.setShootingPlace( "TEST" );
	assertTrue( "no notification when changing shootingPlace", l1.isNotified );
	l1.isNotified = false;
	photo.setFStop( 12 );
	assertTrue( "no notification when changing f-stop", l1.isNotified );
	l1.isNotified = false;
	photo.setFocalLength( 10 );
	assertTrue( "no notification when changing focalLength", l1.isNotified );
	l1.isNotified = false;
	photo.setShootTime( new java.util.Date() );
	assertTrue( "no notification when changing shooting time", l1.isNotified );
	l1.isNotified = false;
	photo.setShutterSpeed( 1.0 );
	assertTrue( "no notification when changing shutter speed", l1.isNotified );
	l1.isNotified = false;
	photo.setCamera( "Leica" );
	assertTrue( "no notification when changing camera", l1.isNotified );
	l1.isNotified = false;
	photo.setLens( "TESTLENS" );
	assertTrue( "no notification when changing lens", l1.isNotified );
	l1.isNotified = false;
	photo.setFilm( "Pan-X"  );
	assertTrue( "no notification when changing film", l1.isNotified );
	l1.isNotified = false;
	photo.setFilmSpeed( 160  );
	assertTrue( "no notification when changing film speed", l1.isNotified );
	l1.isNotified = false;
	photo.setPrefRotation( 107 );
	assertTrue( "no notification when changing preferred rotation", l1.isNotified );
	l1.isNotified = false;
	photo.setDescription( "Test with lots of characters" );
	assertTrue( "no notification when changing description", l1.isNotified );
	photo.delete();
    }

    /**
       Test normal case of exporting image from database
    */
    public void testExport() {
	String fname = "test1.jpg";
	File f = new File( testImgDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Could not find photo: " + e.getMessage() );
	}
	photo.setPrefRotation( -90 );

	File exportFile = new File( "/tmp/exportedImage.png" );
// 	try {
// 	    exportFile = File.createTempFile( "testExport", ".jpg" );
// 	} catch ( IOException e ) {
// 	    fail( "could not create export file: " + e.getMessage() );
// 	}
        try {
            photo.exportPhoto( exportFile, 400, 400 );
        } catch (PhotovaultException e ) {
            fail( e.getMessage() );
        }
	// Read the exported image
	BufferedImage exportedImage = null;
	try {
	    exportedImage = ImageIO.read( exportFile );
	} catch ( IOException e ) {
	    fail( "Could not read the exported image " + exportFile );
	}

	File exportRef = new File( testRefImageDir, "exportedImage.png" );
	    
	// Verify that the exported image matches the reference
	assertTrue( "Exported image " + exportFile + " does not match reference " + exportRef,
		    org.photovault.test.ImgTestUtils.compareImgToFile( exportedImage, exportRef ) );

	photo.delete();
    }

    /**
       Test exporting an image to a file name that cannot be created
    */
/*    public void testExportWriteNotAllowed() {
	fail ("Test case not implemented" );
    }
 */   
    /**
     
     */
        
    public void testOriginalHash() {
	String fname = "test1.jpg";
	File f = new File( testImgDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Could not find photo: " + e.getMessage() );
	}
        
        byte hash[] = photo.getOrigInstanceHash();
        byte instanceHash[] = null;
        assertNotNull( "No hash for original photo", hash );

        // If the original instance is deleted the hash value should still remain
        ImageInstance[] instances = (ImageInstance[]) photo.getInstances().
                toArray( new ImageInstance[photo.getNumInstances()]);
        for ( ImageInstance i : instances ) {
            photo.removeInstance( i );
            if ( i.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL ) {
                instanceHash = i.getHash();
            }
            i.delete();
        }
        assertTrue( "PhotoInfo & origInstance hashes differ", Arrays.equals( hash, instanceHash ) );
        
        byte hash2[] = photo.getOrigInstanceHash();
        assertTrue( "Hash after deleting instances is changed", Arrays.equals( hash, hash2 ) );
        
        photo.delete();
    }
    
    /**
      Test that it is possible to find a PhotoInfo based on original's hash code
     */
    
    public void testRetrievalByHash() {
	String fname = "test1.jpg";
	File f = new File( testImgDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Could not find photo: " + e.getMessage() );
	}
        
        photo = photoDAO.makePersistent( photo );
        
        byte[] hash = photo.getOrigInstanceHash();
        
        List photos = photoDAO.findPhotosWithOriginalHash( hash );
        this.assertTrue( "No Photos with matching hash found!!", photos.size() > 0 );
        boolean found = false;
        for ( Object o : photos ) {
            if ( o == photo ) {
                found = true;
            }
        }
        assertTrue( "Photo not found by original hash", found );
    }

    public void testRawSettings() {
        PhotoInfo p = PhotoInfo.create();
        p = photoDAO.makePersistent( p );
        double chanMul[] = { 
            1., .7, .5, .7
        };
        
        double daylightMul[] = {
            .3, .5, .7
        };
        RawConversionSettings rs = RawConversionSettings.create( chanMul, daylightMul, 
                16000, 0, -.5, 0., RawConversionSettings.WB_MANUAL, false );
        p.setRawSettings( rs );
        RawConversionSettings rs2 = p.getRawSettings();
        assertTrue( rs.equals( rs2 ) );
        assertEquals( 16000, rs2.getWhite() );
        session.flush();
        assertMatchesDb( p );
        List l = session.createQuery( "from RawConversionSettings where rawSettingId = :id" ).
                setInteger( "id", p.getRawSettings().getRawSettingId() ).list();
        assertEquals( 1, l.size() );
                
    }
    
    
    public void testRetrievalByHashNoPhoto() {

        byte[] hash = new byte[16];
        for ( int n = 0; n < 16; n++ ) {
            hash[n] = 0;
        }
        
        List photos = photoDAO.findPhotosWithOriginalHash( hash );
        assertEquals( "retrieveByOrigHash should result empty list", 0, photos.size() );
    }
    
    /**
     Utility to check that the object in memory matches the DB
     */
    void assertMatchesDb( PhotoInfo p ) {
        assertMatchesDb( p, session );
    }
    
        
    public static void main( String[] args ) {
	//	org.apache.log4j.BasicConfigurator.configure();
	log.setLevel( org.apache.log4j.Level.DEBUG );
	org.apache.log4j.Logger photoLog = org.apache.log4j.Logger.getLogger( PhotoInfo.class.getName() );
	photoLog.setLevel( org.apache.log4j.Level.DEBUG );
	junit.textui.TestRunner.run( suite() );
    }
    
    public static Test suite() {
	return new TestSuite( Test_PhotoInfo.class );
    }
    
    

}
