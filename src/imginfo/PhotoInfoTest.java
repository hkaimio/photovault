// PhotoInfoTest.java
package imginfo;

import java.io.*;
import junit.framework.*;


public class PhotoInfoTest extends TestCase {

    /**
       Sets ut the test environment
    */
    public void setUp() {

	// TODO implement test suite
    }

    /**
       Tears down the testing environment
    */
    public void tearDown() {

    }

    /**
       Test case that verifies that an existing photo infor record 
       can be loaded successfully
    */
    public void testRetrievalSuccess() {
	int photoId = 1;
	try {
	    PhotoInfo photo = PhotoInfo.retrievePhotoInfo( photoId );
	    assertTrue(photo != null );
	} catch (PhotoNotFoundException e) {
	    fail( "Photo " + photoId + " not found" );
	}
	// TODO: check some other properties of the object

    }

    /**
       Test case that verifies that an existing photo infor record 
       can be loaded successfully
    */
    public void testRetrievalNotFound() {
	int photoId = -1;
	try {
	    PhotoInfo photo = PhotoInfo.retrievePhotoInfo( photoId );
	    assertTrue(photo != null );
	} catch (PhotoNotFoundException e) {
	    return;
	}
	// If execution comes here  the correct exception was not thrown
	fail( "Image " + photoId + " should not exist." );
    }

    /** 
	Test updating object to DB
    */
    public void testUpdate() {
	int photoId = 1;
	PhotoInfo photo = null;	
	try {
	    photo = PhotoInfo.retrievePhotoInfo( photoId );
	    assertTrue(photo != null );	    
	} catch (PhotoNotFoundException e) {
	    fail( "Photo " + photoId + " not found" );
	}

	// Update the photo
	String shootingPlace = photo.getShootingPlace();
	String newShootingPlace = "Testipaikka";
	photo.setShootingPlace( newShootingPlace );
	photo.updateDB();

	// retrieve the updated photo from DB and chech that the
	// modification has been done
	try {
	    photo = PhotoInfo.retrievePhotoInfo( photoId );
	    assertTrue(photo != null );	    
	} catch (PhotoNotFoundException e) {
	    fail( "Photo " + photoId + " not found after updating" );
	}

	assertEquals( newShootingPlace, photo.getShootingPlace() );

	// restore the shooting place
	photo.setShootingPlace( shootingPlace );
	photo.updateDB();
    }

    /** 
	Test updating object to DB when shooting date has not been specified
    */
    public void testNullShootDateUpdate() {
	int photoId = 1;
	PhotoInfo photo = null;	
	try {
	    photo = PhotoInfo.retrievePhotoInfo( photoId );
	    assertTrue(photo != null );	    
	} catch (PhotoNotFoundException e) {
	    fail( "Photo " + photoId + " not found" );
	}
	
	java.util.Date origTime = photo.getShootTime();
	// Update the photo
	photo.setShootTime( null );
	photo.updateDB();

	// retrieve the updated photo from DB and chech that the
	// modification has been done
	try {
	    photo = PhotoInfo.retrievePhotoInfo( photoId );
	    assertNull( "Shooting time was supposedly set to null", photo.getShootTime() );	    
	} catch (PhotoNotFoundException e) {
	    fail( "Photo " + photoId + " not found after updating" );
	}


	// restore the shooting place
	photo.setShootTime( origTime );
	photo.updateDB();
    }

    /**
       Test normal creation of a persistent PhotoInfo object
    */
    public void testPhotoCreation() {
	PhotoInfo photo = PhotoInfo.create();
	assertNotNull( photo );

	photo.setPhotographer( "TESTIKUVAAJA" );
	photo.setFStop( 5.6 );
	photo.updateDB();
	try {
	    PhotoInfo photo2 = PhotoInfo.retrievePhotoInfo( photo.getUid() );
	    
	    assertEquals( photo2.getPhotographer(), photo.getPhotographer() );
	    assertTrue( photo2.getFStop() == photo.getFStop() );
	} catch ( PhotoNotFoundException e ) {
	    fail ( "inserted photo not found" );
	}
	// Clean the DB
	photo.delete();
    }

    public void testPhotoDeletion() {
	PhotoInfo photo = PhotoInfo.create();
	assertNotNull( photo );

	// Check that the photo can be retrieved from DB
	try {
	    PhotoInfo photo2 = PhotoInfo.retrievePhotoInfo( photo.getUid() );
	} catch ( PhotoNotFoundException e ) {
	    fail ( "inserted photo not found" );
	}

	photo.delete();
	// Check that the photo can be retrieved from DB
	try {
	    PhotoInfo photo2 = PhotoInfo.retrievePhotoInfo( photo.getUid() );
	    fail ( "Photo found in DB after deletion" );
	} catch ( PhotoNotFoundException e ) {
	    // success!!!
	}
    }
    
    public void testInstanceAddition() {
	String dirname = "c:\\java\\photovault\\testfiles";
	String fname = "test1.jpg";
	PhotoInfo photo = PhotoInfo.create();
	assertNotNull( photo );

	int numInstances = photo.getNumInstances();
	photo.addInstance( dirname, fname, ImageFile.FILE_HISTORY_ORIGINAL );
	assertEquals( numInstances+1, photo.getNumInstances() );
	boolean found = false;
	for ( int i = 0 ; i < photo.getNumInstances(); i++ ) {
	    ImageFile ifile = photo.getInstance( i );
	    if ( ifile.getDirname().equals( dirname ) && ifile.getFname().equals( fname ) ) {
		found = true;
	    }
	}
	if ( found == false ) {
	    fail( "Created instance not found" );
	}
	// Clean the DB
	photo.delete();
    }
	
    public void testCreationFromImage() {
	String testImgDir = "c:\\java\\photovault\\testfiles";
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
    
    public static Test suite() {
	return new TestSuite( PhotoInfoTest.class );
    }

}
