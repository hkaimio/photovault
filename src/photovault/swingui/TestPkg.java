// TestPkg.java
package photovault.swingui;

import junit.framework.*;

public class TestPkg extends TestCase {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( TestPkg.class.getName() );

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
	s.addTestSuite( TestPhotoFolderTreeModel.class );
	return s;
    }

    public static void main( String[] args ) {
	org.apache.log4j.BasicConfigurator.configure();
	log.setLevel( org.apache.log4j.Level.DEBUG );
	junit.textui.TestRunner.run( suite() );
    }
    
}
