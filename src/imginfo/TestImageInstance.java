// TestImageFile.java

package imginfo;

import junit.framework.*;
import java.sql.*;
import dbhelper.*;
import java.io.*;

public class TestImageInstance extends TestCase {

    PhotoInfo photo = null;

    Volume volume = null;
    /**
       Sets ut the test environment
    */
    public void setUp() {

	try {
	    photo = PhotoInfo.retrievePhotoInfo( 1 );
	} catch ( Exception e ) {
	    fail( "Unable to retrieve PhotoInfo object" );
	}
	String volumeRoot =  "c:\\temp\\photoVaultImageInstanceTest";
	volume = new Volume( "imageInstanceTest", volumeRoot );
    }
    
    /**
       Tears down the testing environment
    */
    public void tearDown() {
	FileUtils.deleteTree( volume.getBaseDir() );
    }
    

    public void testImageInstanceCreate() {
	
	File testFile = new File( "c:\\java\\photovault\\testfiles\\test1.jpg" );
	File instanceFile = volume.getFilingFname( testFile );
	try {
	    FileUtils.copyFile( testFile, instanceFile );
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}
	ImageInstance f = ImageInstance.create( volume, instanceFile, photo );
	assertNotNull( "Image instance is null", f );
	f.delete();
    }
		  
    public void testImageInstanceUpdate() {
	
	File testFile = new File( "c:\\java\\photovault\\testfiles\\test1.jpg" );
	File instanceFile = volume.getFilingFname( testFile );
	try {
	    FileUtils.copyFile( testFile, instanceFile );
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}
	ImageInstance f = ImageInstance.create( volume, instanceFile, photo );
	assertNotNull( "Image instance is null", f );
	int width = f.getWidth();
	int height = f.getHeight();
	int hist = f.getInstanceType();
	f.setHeight( height + 1 );
	f.setWidth( width + 1 );
	f.setInstanceType( ImageInstance.INSTANCE_TYPE_THUMBNAIL );
	f.updateDB();
	File imgFile = f.getImageFile();
	
	// Reload the object from database and check that the modifications are OK
	try {
	    f = ImageInstance.retrieve( volume, imgFile.getName() );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Image file not found after update" );
	}
	assertNotNull( "Image instance is null", f );
	assertEquals( "Width not updated", f.getWidth(), width+1 );
	assertEquals( "height not updated", f.getHeight(), height+1 );
	assertEquals( "Instance type not updated", f.getInstanceType(), ImageInstance.INSTANCE_TYPE_THUMBNAIL );
	File imgFile2 = f.getImageFile();
	assertTrue( "Image file does not exist", imgFile2.exists() );
	assertTrue( "Image file name not same after update", imgFile.equals( imgFile2 ) );
	// Tidy up after execution
	f.delete();
    }

    public void testImageInstanceDelete() {
	File testFile = new File( "c:\\java\\photovault\\testfiles\\test1.jpg" );
	File instanceFile = volume.getFilingFname( testFile );
	try {
	    FileUtils.copyFile( testFile, instanceFile );
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}
	ImageInstance f = ImageInstance.create( volume, instanceFile, photo );
	assertNotNull( f );
	f.delete();

	Connection conn = ImageDb.getConnection();
	try {
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery( "SELECT * FROM image_files WHERE dirname = \"c:\\java\\photovault\\testfiles\" AND fname = \"test1.jpg\"" );
	    if ( rs.next() ) {
		fail( "Found matching DB record after delete" );
	    }
	} catch ( SQLException e ) {
	    fail( "DB error:; " + e.getMessage() );
	}
    }
					      

    public static Test suite() {
	return new TestSuite( TestImageInstance.class );
    }

}

    

