// TestImageFile.java

package imginfo;

import junit.framework.*;


public class TestImageFile extends TestCase {

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

    public void testImageFileCreate() {
	
	PhotoInfo photo = null;
	try {
	    PhotoInfo.retrievePhotoInfo( 1 );
	} catch ( Exception e ) {
	    fail( "Unable to retrieve PhotoInfo object" );
	}
	ImageFile f = ImageFile.create( "c:\\temp", "test.jpg", photo );
	assertNotNull( f );

    }
		  

    public static Test suite() {
	return new TestSuite( TestImageFile.class );
    }

}

    

