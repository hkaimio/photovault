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
	FileUtils.copyFile( testFile, instanceFile );
	ImageInstance f = ImageInstance.create( volume, instanceFile, photo );
	assertNotNull( f );
	f.delete();
    }
		  
    public void testImageInstanceUpdate() {
	
	File testFile = new File( "c:\\java\\photovault\\testfiles\\test1.jpg" );
	File instanceFile = volume.getFilingFname( testFile );
	FileUtils.copyFile( testFile, instanceFile );
	ImageInstance f = ImageInstance.create( volume, instanceFile, photo );
	assertNotNull( f );
	int width = f.getWidth();
	int height = f.getHeight();
	int hist = f.getInstanceType();
	f.setHeight( height + 1 );
	f.setWidth( width + 1 );
	f.setInstanceType( ImageInstance.INSTANCE_TYPE_THUMBNAIL );
	f.updateDB();

	// Reload the object from database and check that the modifications are OK
	try {
	    f = ImageInstance.retrieve( "c:\\java\\photovault\\testfiles", "test1.jpg" );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Image file not found after update" );
	}
	assertNotNull( f );
	assertEquals( f.getWidth(), width+1 );
	assertEquals( f.getHeight(), height+1 );
	assertEquals( f.getInstanceType(), ImageInstance.INSTANCE_TYPE_THUMBNAIL );
	// Tidy up after execution
	f.delete();
    }

    public void testImageInstanceDelete() {
	File testFile = new File( "c:\\java\\photovault\\testfiles\\test1.jpg" );
	File instanceFile = volume.getFilingFname( testFile );
	FileUtils.copyFile( testFile, instanceFile );
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

    

