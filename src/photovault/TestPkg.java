// TestImgInfoPkg.java
package photovault;

import junit.framework.*;
import imginfo.*;

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
	s.addTest( photovault.swingui.TestPkg.suite() );
	s.addTest( imginfo.TestPkg.suite() );
	return s;
    }
}