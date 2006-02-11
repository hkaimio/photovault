// TestImgInfoPkg.java
package photovault;

import junit.framework.*;
import org.photovault.imginfo.*;

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
	TestSuite s = new TestSuite( "photovault unit tests" );
	s.addTest( org.photovault.swingui.TestPkg.suite() );
	s.addTest( org.photovault.image.TestPkg.suite() );
	s.addTest( org.photovault.imginfo.TestPkg.suite() );
	return s;
    }
}
