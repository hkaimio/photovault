// Test_PhtoQuery.java

package imginfo;

import junit.framework.*;
import java.util.*;
import photovault.folder.*;
import photovault.swingui.FuzzyDate;
import photovault.test.PhotovaultTestCase;

public class Test_PhotoQuery extends PhotovaultTestCase {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Test_PhotoQuery.class.getName() );

    Vector photos = null;
    Vector uids = null;
    PhotoFolder folder = null;
    PhotoFolder subfolder = null;
    
    public void setUp() {

	// Create several photos with different shooting dates
	// Add them to a collection so that they are easy to delete afterwards
	Calendar cal = Calendar.getInstance();
	photos = new Vector();
	uids = new Vector();
	cal.set( 2002, 11, 23 );
	makePhoto( cal, 1, "Katsokaa kun Lassi ui" );
	cal.set( 2002, 11, 24 );
	makePhoto( cal, 1, "Lassille kuuluu hyvaa" );
	makePhoto( cal, 2, "" );
	cal.set( 2002, 11, 25 );
	makePhoto( cal, 2, "" );

        folder = PhotoFolder.create( "QueryTest", PhotoFolder.getRoot() );
	subfolder = PhotoFolder.create( "QueryTest subfolder", folder );
	folder.addPhoto( (PhotoInfo)photos.get(0) );
	subfolder.addPhoto( (PhotoInfo)photos.get(3) );
	folder.addPhoto( (PhotoInfo)photos.get(2) );
	

    }

    PhotoInfo makePhoto( Calendar cal, double accuracy, String desc ) {
	PhotoInfo photo = PhotoInfo.create();
	photo.setShootTime( cal.getTime() );
	photo.setTimeAccuracy( accuracy );
	photo.setDescription( desc );
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

	folder.delete();
	
    }

    public void testUpperUnboundedRange() {
	PhotoQuery q = new PhotoQuery();
	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	// First the case in which there is only lower bound
	q.setFieldCriteriaRange( PhotoQuery.FIELD_SHOOTING_TIME, cal.getTime(), null );
	
	boolean[] expected1 = { false, true, true, true };
	checkResults( q, expected1 );
    }
    
    public void testBoundedRange() {
	PhotoQuery q = new PhotoQuery();
	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	// First the case in which there is only lower bound
	q.setFieldCriteriaRange( PhotoQuery.FIELD_SHOOTING_TIME, cal.getTime(), cal.getTime() );
	boolean[] expected2 = { false, true, true, false };
	checkResults( q, expected2 );
    }

    public void testFuzzyDateProbable() {
	PhotoQuery q = new PhotoQuery();

	// First, check cases where the midpoint belongs to the fuzziness range
 	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	FuzzyDate fd1 = new FuzzyDate( cal.getTime(), 1.0 );
	// First the case in which there is only lower bound
 	q.setFuzzyDateCriteria( PhotoQuery.FIELD_SHOOTING_TIME,
				PhotoQuery.FIELD_SHOOTING_TIME_ACCURACY,
				fd1, QueryFuzzyTimeCriteria.INCLUDE_PROBABLE );
	// Image 2 should not be included since its fuzzy range is
	// larger that search range
	boolean[] expected1 = { true, true, false, true };
	checkResults( q, expected1 );
    }
    
    public void testFuzzyDatePossible() {
	PhotoQuery q = new PhotoQuery();

	// First, check cases where the midpoint belongs to the fuzziness range
 	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	FuzzyDate fd1 = new FuzzyDate( cal.getTime(), 1.0 );
 	q.setFuzzyDateCriteria( PhotoQuery.FIELD_SHOOTING_TIME,
				PhotoQuery.FIELD_SHOOTING_TIME_ACCURACY,
				fd1, QueryFuzzyTimeCriteria.INCLUDE_POSSIBLE );
	// All expected to be included with INCLUDE_POSSIBLE
	boolean[] expected2 = { true, true, true, true };
	checkResults( q, expected2 );
    }

    public void testFuzzyDateCertain() {
	PhotoQuery q = new PhotoQuery();

	// First, check cases where the midpoint belongs to the fuzziness range
 	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	FuzzyDate fd1 = new FuzzyDate( cal.getTime(), 1.0 );
	q.setFuzzyDateCriteria( PhotoQuery.FIELD_SHOOTING_TIME,
				PhotoQuery.FIELD_SHOOTING_TIME_ACCURACY,
				fd1, QueryFuzzyTimeCriteria.INCLUDE_CERTAIN );
	// All expected to be included with INCLUDE_POSSIBLE
	boolean[] expected3 = { false, true, true, false };
	checkResults( q, expected3 );
	

    }

    public void testLowerUnboundedRange() {
	PhotoQuery q = new PhotoQuery();
	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	// First the case in which there is only lower bound
	q.setFieldCriteriaRange( PhotoQuery.FIELD_SHOOTING_TIME, null, cal.getTime() );
	boolean[] expected3 = { true, true, true, false };
	checkResults( q, expected3 );
    }

    public void testFulltext() {
	PhotoQuery q = new PhotoQuery();
	q.setFulltextCriteria( "Lassi" );
	boolean[] expected3 = { true, true, false, false };
	checkResults( q, expected3 );
    }
    
    public void testLike() {
	PhotoQuery q = new PhotoQuery();
	q.setLikeCriteria( PhotoQuery.FIELD_DESCRIPTION, "*Lassi*" );
	boolean[] expected3 = { true, true, false, false };
	checkResults( q, expected3 );
    }

    public void testFolderLimit() {
	PhotoQuery q = new PhotoQuery();
	q.setLikeCriteria( PhotoQuery.FIELD_DESCRIPTION, "*Lassi*" );
	q.limitToFolder( folder );
	boolean[] expected3 = { true, false, false, false };
	checkResults( q, expected3 );
    }
    
    public void testFolderLimitSubfolders() {
	PhotoQuery q = new PhotoQuery();
	q.limitToFolder( folder );
	boolean[] expected3 = { true, false, true, true };
	checkResults( q, expected3 );
    }
    
    
    /**
       This query checks that query can be modified and that the results are shown correctly
       Tjis is originally implemented to find demonstrate a defect in which the reuslt set was not cleaned
       before the new query.
    */
    public void testQueryModification() {
	PhotoQuery q = new PhotoQuery();
	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	// First the case in which there is only lower bound
	q.setFieldCriteriaRange( PhotoQuery.FIELD_SHOOTING_TIME, null, cal.getTime() );
	boolean[] expected3 = { true, true, true, false };
	checkResults( q, expected3 );
	// Then add the lower bound, part of the photos should not be in result set this time
	q.setFieldCriteriaRange( PhotoQuery.FIELD_SHOOTING_TIME, cal.getTime(), cal.getTime() );
	boolean[] expected2 = { false, true, true, false };
	checkResults( q, expected2 );
    }

    void checkResults( PhotoQuery q, boolean[] expected ) {
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
	PhotoQuery q = new PhotoQuery();
	TestListener l1 = new TestListener();
	q.addPhotoCollectionChangeListener( l1 );
	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	// First the case in which there is only lower bound
	q.setFieldCriteriaRange( PhotoQuery.FIELD_SHOOTING_TIME, null, cal.getTime() );
	assertTrue( "setEndDate should notify listeners", l1.notified );
	assertEquals( "source not correct", q, l1.changedObject );
	l1.notified = false;

	q.setFieldCriteriaRange( PhotoQuery.FIELD_SHOOTING_TIME, cal.getTime(), cal.getTime() );
	assertTrue( "setstartDate should notify listeners", l1.notified );
	l1.notified = false;

	TestListener l2 = new TestListener();
	q.addPhotoCollectionChangeListener( l2 );
	q.removePhotoCollectionChangeListener( l1 );
	q.setFieldCriteriaRange( PhotoQuery.FIELD_SHOOTING_TIME, cal.getTime(), cal.getTime() );
	assertTrue( "setstartDate should notify listeners", l2.notified );
	assertFalse( "Removed listener notified", l1.notified );
    }
    

}