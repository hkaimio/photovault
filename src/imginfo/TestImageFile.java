// TestImageFile.java

package imginfo;

import junit.framework.*;
import java.sql.*;
import dbhelper.*;


public class TestImageFile extends TestCase {

    PhotoInfo photo = null;
    /**
       Sets ut the test environment
    */
    public void setUp() {

	try {
	    photo = PhotoInfo.retrievePhotoInfo( 1 );
	} catch ( Exception e ) {
	    fail( "Unable to retrieve PhotoInfo object" );
	}
    }
    
    /**
       Tears down the testing environment
    */
    public void tearDown() {
    }

    public void testImageFileCreate() {
	
	ImageFile f = ImageFile.create( "c:\\temp", "test.jpg", photo );
	assertNotNull( f );
	f.delete();
    }
		  
    public void testImageFileUpdate() {
	ImageFile f = ImageFile.create( "c:\\temp", "test.jpg", photo );
	assertNotNull( f );
	int width = f.getWidth();
	int height = f.getHeight();
	int hist = f.getFileHistory();
	f.setHeight( height + 1 );
	f.setWidth( width + 1 );
	f.setFileHistory( ImageFile.FILE_HISTORY_THUMBNAIL );
	f.updateDB();

	// Reload the object from database and check that the modifications are OK
	try {
	    f = ImageFile.retrieve( "c:\\temp", "test.jpg" );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Image file not found after update" );
	}
	assertNotNull( f );
	assertEquals( f.getWidth(), width+1 );
	assertEquals( f.getHeight(), height+1 );
	assertEquals( f.getFileHistory(), ImageFile.FILE_HISTORY_THUMBNAIL );
	// Tidy up after execution
	f.delete();
    }

    public void testImageFileDelete() {
	ImageFile f = ImageFile.create( "c:\\temp", "test.jpg", photo );
	assertNotNull( f );
	f.delete();

	Connection conn = ImageDb.getConnection();
	try {
	    Statement stmt = conn.createStatement();
	    ResultSet rs = stmt.executeQuery( "SELECT * FROM image_files WHERE dirname = \"c:\\temp\" AND fname = \"test.jpg\"" );
	    if ( rs.next() ) {
		fail( "Found matching DB record after delete" );
	    }
	} catch ( SQLException e ) {
	    fail( "DB error:; " + e.getMessage() );
	}
    }
					      

    public static Test suite() {
	return new TestSuite( TestImageFile.class );
    }

}

    

