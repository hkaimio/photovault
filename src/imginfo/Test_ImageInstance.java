// TestImageFile.java

package imginfo;

import junit.framework.*;
import java.sql.*;
import dbhelper.*;
import java.io.*;

public class Test_ImageInstance extends TestCase {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Test_ImageInstance.class.getName() );
  
  String testImgDir = "/home/harri/projects/photovault/testfiles";
  String volumeRoot =  "/tmp/photoVaultImageInstanceTest";

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
	volume = new Volume( "imageInstanceTest", volumeRoot );
    }
    
    /**
       Tears down the testing environment
    */
    public void tearDown() {
	FileUtils.deleteTree( volume.getBaseDir() );
    }
    

    public void testImageInstanceCreate() {
	
      File testFile = new File( testImgDir, "test1.jpg" );
	File instanceFile = volume.getFilingFname( testFile );
	try {
	    FileUtils.copyFile( testFile, instanceFile );
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}
	ImageInstance f = ImageInstance.create( volume, instanceFile, photo );
	assertNotNull( "Image instance is null", f );
	assertMatchesDb( f );
	f.delete();
    }
		  
    public void testImageInstanceUpdate() {
	
      File testFile = new File( testImgDir, "test1.jpg" );
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
	File imgFile = f.getImageFile();

	assertMatchesDb( f );
	
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
      File testFile = new File( testImgDir, "test1.jpg" );
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
	    ResultSet rs = stmt.executeQuery( "SELECT * FROM image_instances WHERE dirname = \"" + testImgDir + "\" AND fname = \"test1.jpg\"" );
	    if ( rs.next() ) {
		fail( "Found matching DB record after delete" );
	    }
	} catch ( SQLException e ) {
	    fail( "DB error:; " + e.getMessage() );
	}
    }

    // TODO: test case that demonstrates that imageFile & volume attributes are not
    // initialized correctly if standard RowReader is used.

    
    /**
       Utility to check that the object in memory matches the DB
    */
    void assertMatchesDb( ImageInstance i ) {
	String volumeName = i.getVolume().getName();
	String fname = i.getImageFile().getName();
	String sql = "select * from image_instances where volume_id = \"" + volumeName + "\" and fname = \""
	    + fname + "\"";
	Statement stmt = null;
	ResultSet rs = null;
	try {
	    stmt = ImageDb.getConnection().createStatement();
	    rs = stmt.executeQuery( sql );
	    if ( !rs.next() ) {
		fail( "rrecord not found" );
	    }
	    // TODO: there is no pointer back from instance to photo so this cannot be checked
	    //	    assertEquals( "photo doesn't match", i.getPhoto .getUid(), rs.getInt( "photo_id" ) );
	    assertEquals( "width doesn't match", i.getWidth(), rs.getInt( "width" ) );
	    assertEquals( "height doesn't match", i.getHeight(), rs.getInt( "height" ) );
	    assertTrue( "rotated doesn't match", i.getRotated() == rs.getDouble( "rotated" ) );
	    int itype = i.getInstanceType();
	    switch ( itype ) {
	    case ImageInstance.INSTANCE_TYPE_ORIGINAL:
		assertEquals( "instance type does not match", "original", rs.getString( "instance_type" ) );
		break;
	    case ImageInstance.INSTANCE_TYPE_MODIFIED:
		assertEquals( "instance type does not match", "modified", rs.getString( "instance_type" ) );
		break;
	    case ImageInstance.INSTANCE_TYPE_THUMBNAIL:
		assertEquals( "instance type does not match", "thumbnail", rs.getString( "instance_type" ) );
		break;
	    default:
		fail( "Unknown image type " + itype );
	    }
	} catch ( SQLException e ) {
	    fail( e.getMessage() );
	} finally {
	    if ( rs != null ) {
		try {
		    rs.close();
		} catch ( Exception e ) {
		    fail( e.getMessage() );
		}
	    }
	    if ( stmt != null ) {
		try {
		    stmt.close();
		} catch ( Exception e ) {
		    fail( e.getMessage() );
		}
	    }
	}
    }
    
    public static void main( String[] args ) {
	//	org.apache.log4j.BasicConfigurator.configure();
	log.setLevel( org.apache.log4j.Level.DEBUG );
	org.apache.log4j.Logger instLog = org.apache.log4j.Logger.getLogger( ImageInstance.class.getName() );
	instLog.setLevel( org.apache.log4j.Level.DEBUG );
	junit.textui.TestRunner.run( suite() );
    }
    

    public static Test suite() {
	return new TestSuite( Test_ImageInstance.class );
    }

}

    

