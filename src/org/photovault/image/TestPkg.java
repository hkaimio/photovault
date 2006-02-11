// $Id: TestPkg.java,v 1.1 2003/02/14 19:22:25 kaimio Exp $


package org.photovault.image;



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
	TestSuite s = new TestSuite( "photovault.image unit tests" );
	s.addTestSuite( Test_ImageXform.class );
	return s;
    }
}
