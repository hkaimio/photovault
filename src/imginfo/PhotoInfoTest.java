// PhotoInfoTest.java
package imginfo;

import java.io.*;
import junit.framework.*;
import java.util.*;



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
	photo.setShootingPlace( "TESTPLACE" );
	photo.setShootTime( new Date() );
	photo.setFStop( 5.6 );
	photo.setShutterSpeed( 0.04 );
	photo.setFocalLength( 50 );
	photo.setCamera( "Canon FTb" );
	photo.setFilm( "Tri-X" );
	photo.setFilmSpeed( 400 );
	photo.setLens( "Canon FD 50mm/F1.4" );
	photo.setDescription( "This is a long test description that tries to verify that the description mechanism really works" );
	photo.updateDB();
	try {
	    PhotoInfo photo2 = PhotoInfo.retrievePhotoInfo( photo.getUid() );

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
	    
	    //	    assertTrue( photo.equals( photo2 ));
			
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
	File testFile = new File( "c:\\java\\photovault\\testfiles\\test1.jpg" );
	File instanceFile = Volume.getDefaultVolume().getFilingFname( testFile );
	try {
	    FileUtils.copyFile( testFile, instanceFile );
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}
	PhotoInfo photo = PhotoInfo.create();
	assertNotNull( photo );

	int numInstances = photo.getNumInstances();
	photo.addInstance( Volume.getDefaultVolume(), instanceFile, ImageInstance.INSTANCE_TYPE_ORIGINAL );
	// Check that number of instances is consistent with addition
	assertEquals( numInstances+1, photo.getNumInstances() );
	ArrayList instances = photo.getInstances();
	assertEquals( instances.size(), numInstances+1 );

	// Try to find the instance
	boolean found = false;
	for ( int i = 0 ; i < photo.getNumInstances(); i++ ) {
	    ImageInstance ifile = photo.getInstance( i );
	    if ( ifile.getImageFile().equals( instanceFile ) ) {
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

    /**
       Test that an exception is generated when trying to add nonexisting file to DB
    */
    public void testfailedCreation() {
	String testImgDir = "c:\\_directoryThatDoesNotExist";
	String fname = "test1.jpg";	
	File f = new File( testImgDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	    // Execution should neverproceed this far since addToDB should produce exception
	    fail( "Image file should have been nonexistent" );
	} catch ( PhotoNotFoundException e ) {
	    // This is what we except
	}
    }

    /**
       Test that creating a new thumbnail using createThumbnail works
     */
    public void testThumbnailCreate() {
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
	int instanceCount = photo.getNumInstances();
	photo.createThumbnail();
	assertEquals( "InstanceNum should be 1 greater after adding thumbnail",
		     instanceCount+1, photo.getNumInstances() );
	// Try to find the new thumbnail
	boolean foundThumbnail = false;
	ImageInstance thumbnail = null;
	for ( int n = 0; n < instanceCount+1; n++ ) {
	    ImageInstance instance = photo.getInstance( n );
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
	

	// Assert that the thumbnail is saved correctly to the database
	PhotoInfo photo2 = null;
	try {
	    photo2 = PhotoInfo.retrievePhotoInfo( photo.getUid() );
	} catch( PhotoNotFoundException e ) {
	    fail( "Photo not storein into DB" );
	}
						
	// Try to find the new thumbnail
	foundThumbnail = false;
	ImageInstance thumbnail2 = null;
	for ( int n = 0; n < instanceCount+1; n++ ) {
	    ImageInstance instance = photo2.getInstance( n );
	    if ( instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_THUMBNAIL ) {
		foundThumbnail = true;
		thumbnail2 = instance; 
		break;
	    }
	}
	assertTrue( "Could not find the created thumbnail", foundThumbnail );
	assertEquals( "Thumbnail width should be 100", 100, thumbnail2.getWidth() );
	assertTrue( "Thumbnail filename not saved correctly", thumbnailFile.equals( thumbnail2.getImageFile() ));
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
       Test that creating a new thumbnail using getThumbnail works
     */
    public void testGetThumbnail() {
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
	for ( int n = 0; n < instanceCount+1; n++ ) {
	    ImageInstance instance = photo.getInstance( n );
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
	PhotoInfo photo = PhotoInfo.create();
	Thumbnail thumb = photo.getThumbnail();
	assertTrue( "getThumbnail should return default thumbnail",
		    thumb == Thumbnail.getDefaultThumbnail() ) ;
	assertEquals( "No new instances should have been created", 0, photo.getNumInstances() );

	// Create a new instance and check that a valid thumbnail is returned after this
	File testFile = new File( "c:\\java\\photovault\\testfiles\\test1.jpg" );
	File instanceFile = Volume.getDefaultVolume().getFilingFname( testFile );
	try {
	    FileUtils.copyFile( testFile, instanceFile );
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}
	photo.addInstance( Volume.getDefaultVolume(), instanceFile, ImageInstance.INSTANCE_TYPE_ORIGINAL );
	Thumbnail thumb2 = photo.getThumbnail();
	assertFalse( "After instance addition, getThumbnail should not return default thumbnail",
			thumb == thumb2 );
	assertEquals( "There should be 2 instances: original & thumbnail", 2, photo.getNumInstances() );

	photo.delete();
		      
    }
    
    public static Test suite() {
	return new TestSuite( PhotoInfoTest.class );
    }
    
    

}
