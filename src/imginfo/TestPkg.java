// TestImgInfoPkg.java
package imginfo;

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
	TestSuite s = new TestSuite();
	s.addTestSuite( PhotoInfoTest.class );
	s.addTestSuite( TestImageFile.class );
	s.addTestSuite( TestVolume.class );
	return s;
    }
}
