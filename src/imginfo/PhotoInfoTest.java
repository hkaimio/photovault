// PhotoInfoTest.java
package imginfo;

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
       Test case that verifies that an existing photo infor record can be loaded successfully
    */
    public void testRetrievalSuccess() {
	String photoId = "02120108000000001";
	try {
	    PhotoInfo photo = PhotoInfo.retrievePhotoInfo( photoId );
	    assertTrue(photo != null );
	} catch (PhotoNotFoundException e) {
	    fail( "Photo " + photoId + " not found" );
	}
	// TODO: check some other properties of the object
    }

    public static Test suite() {
	return new TestSuite( PhotoInfoTest.class );
    }

}
