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
	PhotoInfo photo = PhotoInfo.retrievePhotoInfo( "20021129_0001" );
	assertTrue(photo != null );
	// TODO: check some other properties of the object
    }

    public static Test suite() {
	return new TestSuite( PhotoInfoTest.class );
    }

}
