// TestDateRangeQuery.java

package imginfo;

import junit.framework.*;
import java.util.*;

public class TestDateRangeQuery extends TestCase {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( TestDateRangeQuery.class.getName() );


    Vector photos = null;
    Vector uids = null;
    
    public void setUp() {

	// Create several photos with different shooting dates
	// Add them to a collection so that they are easy to delete afterwards
	Calendar cal = Calendar.getInstance();
	photos = new Vector();
	uids = new Vector();
	cal.set( 2002, 11, 23 );
	makePhoto( cal );
	cal.set( 2002, 11, 24 );
	makePhoto( cal );
	makePhoto( cal );
	cal.set( 2002, 11, 25 );
	makePhoto( cal );
	
    }

    PhotoInfo makePhoto( Calendar cal ) {
	PhotoInfo photo = PhotoInfo.create();
	photo.setShootTime( cal.getTime() );
	int uid = photo.getUid();
	log.debug( "Created photo " + uid + " " + photo.getShootTime() );
	photos.add( photo );
	uids.add( new Integer( photo.getUid() ) );
	return photo;
    }

    public void tearDown() {
	Iterator iter = photos.iterator();
	while ( iter.hasNext() ) {
	    PhotoInfo photo = (PhotoInfo) iter.next();
	    photo.delete();
	}
    }

    public void testUpperUnboundedRange() {
	DateRangeQuery q = new DateRangeQuery();
	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	// First the case in which there is only lower bound
	q.setStartDate( cal.getTime() );
	
	boolean[] expected1 = { false, true, true, true };
	checkResults( q, expected1 );
    }
    
    public void testBoundedRange() {
	DateRangeQuery q = new DateRangeQuery();
	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	// First the case in which there is only lower bound
	q.setStartDate( cal.getTime() );
	q.setEndDate( cal.getTime() );
	boolean[] expected2 = { false, true, true, false };
	checkResults( q, expected2 );
    }

    public void testLowerUnboundedRange() {
	DateRangeQuery q = new DateRangeQuery();
	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	// First the case in which there is only lower bound
	q.setEndDate( cal.getTime() );
	q.setStartDate( null );
	boolean[] expected3 = { true, true, true, false };
	checkResults( q, expected3 );
    }

    /**
       This query checks that query can be modified and that the results are shown correctly
       Tjis is originally implemented to find demonstrate a defect in which the reuslt set was not cleaned
       before the new query.
    */
    public void testQueryModification() {
	DateRangeQuery q = new DateRangeQuery();
	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	// First the case in which there is only lower bound
	q.setEndDate( cal.getTime() );
	q.setStartDate( null );
	boolean[] expected3 = { true, true, true, false };
	checkResults( q, expected3 );
	// Then add the lower bound, part of the photos should not be in result set this time
	q.setStartDate( cal.getTime() );
	boolean[] expected2 = { false, true, true, false };
	checkResults( q, expected2 );
    }

    void checkResults( DateRangeQuery q, boolean[] expected ) {
	log.debug( "Checking results" );
	for( int n = 0; n < q.getPhotoCount(); n++ ) {
	    PhotoInfo photo = q.getPhoto( n );
	    int m = uids.indexOf( new Integer( photo.getUid() ) );
	    log.debug( "Getting photo " + photo.getUid() + " " + photo.getShootTime() + " " + m );
	    if ( m >= 0 ) {
		if ( expected[m] ) {
		    expected[m] = false;
		    log.debug( "Photo " + photo.getUid() + " found" );
		} else {
		    fail( "Photo dated " + photo.getShootTime().toString() + " not expected!!!" );
		}
	    }
	}
	// Check that all photos were found
	log.debug( "Checking that all are found" );
	for ( int n = 0; n < expected.length; n++ ) {
	    if ( expected[n] ) {
		fail( "Photo "+ n + " (id" + ((PhotoInfo)photos.elementAt( n )).getUid() + ") not included in result set" );
	    }
	}
    }

    class TestListener implements PhotoCollectionChangeListener {
	public boolean notified = false;
	public PhotoCollection changedObject = null;
	public void photoCollectionChanged( PhotoCollectionChangeEvent e ) {
	    notified = true;
	    changedObject = (PhotoCollection) e.getSource();
	}
    }

    public void testNotification() {
	DateRangeQuery q = new DateRangeQuery();
	TestListener l1 = new TestListener();
	q.addPhotoCollectionChangeListener( l1 );
	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	// First the case in which there is only lower bound
	q.setEndDate( cal.getTime() );
	assertTrue( "setEndDate should notify listeners", l1.notified );
	assertEquals( "source not correct", q, l1.changedObject );
	l1.notified = false;

	q.setStartDate( cal.getTime() );
	assertTrue( "setstartDate should notify listeners", l1.notified );
	l1.notified = false;

	TestListener l2 = new TestListener();
	q.addPhotoCollectionChangeListener( l2 );
	q.removePhotoCollectionChangeListener( l1 );
	q.setStartDate( cal.getTime() );
	assertTrue( "setstartDate should notify listeners", l2.notified );
	assertFalse( "Removed listener notified", l1.notified );
    }
	
    public static void main( String[] args ) {
	//	org.apache.log4j.BasicConfigurator.configure();
	log.setLevel( org.apache.log4j.Level.DEBUG );
	org.apache.log4j.Logger photoLog = org.apache.log4j.Logger.getLogger( DateRangeQuery.class.getName() );
	photoLog.setLevel( org.apache.log4j.Level.DEBUG );
	junit.textui.TestRunner.run( suite() );
    }
	
	
    public static Test suite() {
	return new TestSuite( TestDateRangeQuery.class );
    }
}
