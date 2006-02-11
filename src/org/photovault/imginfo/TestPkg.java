// TestImgInfoPkg.java
package org.photovault.imginfo;

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
	TestSuite s = new TestSuite( "imginfo unit tests" );
	s.addTestSuite( Test_PhotoInfo.class );
	s.addTestSuite( Test_ImageInstance.class );
	s.addTestSuite( Test_Volume.class );
	s.addTestSuite( Test_DateRangeQuery.class );
	return s;
    }
}
