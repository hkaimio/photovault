// TestPkg.java
package photovault.swingui;

import junit.framework.*;

public class TestPkg extends TestCase {

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

    public static TestSuite suite() {
	TestSuite s = new TestSuite( "photovault.swingui unit tests" );
	s.addTestSuite( TestFieldController.class );
	s.addTestSuite( TestPhotoInfoController.class );
	s.addTestSuite( TestThumbnailView.class );
	return s;
    }
}
