// TestDateRangeQuery.java

package imginfo;

import junit.framework.*;
import java.util.*;

public class TestDateRangeQuery extends TestCase {

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
	photo.updateDB();
	int uid = photo.getUid();
	System.err.println( "Created photo " + uid + " " + photo.getShootTime() );
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

    void checkResults( DateRangeQuery q, boolean[] expected ) {
	System.err.println( "Checking results" );
	for( int n = 0; n < q.getPhotoCount(); n++ ) {
	    PhotoInfo photo = q.getPhoto( n );
	    int m = uids.indexOf( new Integer( photo.getUid() ) );
	    System.err.println( "Getting photo " + photo.getUid() + " " + photo.getShootTime() + " " + m );
	    if ( m >= 0 ) {
		if ( expected[m] ) {
		    expected[m] = false;
		    System.err.println( "Photo " + photo.getUid() + " found" );
		} else {
		    fail( "Photo dated " + photo.getShootTime().toString() + " not expected!!!" );
		}
	    }
	}
	// Check that all photos were found
	System.err.println( "Checking that all are found" );
	for ( int n = 0; n < expected.length; n++ ) {
	    if ( expected[n] ) {
		fail( "Photo "+ n + " (id" + ((PhotoInfo)photos.elementAt( n )).getUid() + ") not included in result set" );
	    }
	}
    }

	
    public static Test suite() {
	return new TestSuite( TestDateRangeQuery.class );
    }
}
