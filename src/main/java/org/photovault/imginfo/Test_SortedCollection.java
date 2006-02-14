/*
 * Test_SortedCollection.java
 *
 * Created on 29. tammikuuta 2006, 8:51
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.photovault.imginfo;

import java.util.Date;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.photovault.folder.PhotoFolder;
import org.photovault.test.PhotovaultTestCase;

/**
 * Test cases for testing @see SortedPhotoCollection
 * @author harri
 */
public class Test_SortedCollection extends PhotovaultTestCase {
    
    /** Creates a new instance of Test_SortedCollection */
    public Test_SortedCollection() {
    }
    
    PhotoFolder folder = null;
    PhotoInfo photo1 = null;
    PhotoInfo photo2 = null;
    PhotoInfo photo3 = null;
    
    public void setUp() {
        folder = PhotoFolder.create( "SortedCollectionTest", null );
        photo1 = PhotoInfo.create();
        photo2 = PhotoInfo.create();
        photo3 = PhotoInfo.create();
        
        photo1.setShootTime( new Date( 2000, 1, 1 ));
        photo1.setShootingPlace( "TESTPLACE B" );
        photo2.setShootTime( new Date( 2001, 1, 1 ));
        photo2.setShootingPlace( "TESTPLACE A" );
        photo3.setShootTime( new Date( 2001, 1, 1 ));
        photo3.setShootingPlace( "TESTPLACE B" );
        folder.addPhoto( photo1 );
        folder.addPhoto( photo2 );
        folder.addPhoto( photo3 );
    }
    
    public void tearDown() {
        folder.delete();
        photo1.delete();
        photo2.delete();
        photo3.delete();
    }
    
    public void testSorting() {
        SortedPhotoCollection collection = new SortedPhotoCollection( folder );
        TestChangeListener l = new TestChangeListener();
        collection.addPhotoCollectionChangeListener( l );
        collection.setComparator( new ShootingDateComparator() );
        
        // Change listeners must be notified
        assertTrue( "Change listener not called after comparator change", l.isNotified );
        assertTrue( "Photo1 must be 1st in collection", collection.getPhoto( 0 ) == photo1 );
        assertTrue( "Photo2 must be 2nd in collection", 
                (collection.getPhoto( 2 ) == photo2) || (collection.getPhoto( 2 ) == photo3) );
        
        l.isNotified = false;
        collection.setComparator( new ShootingPlaceComparator() );
        // Change listeners must be notified
        assertTrue( "Change listener not called after comparator change", l.isNotified );
        assertTrue( "Photo2 must be 1st in collection", collection.getPhoto( 0 ) == photo2 );
        assertTrue( "Photo1 must be 2nd in collection", 
                (collection.getPhoto( 2 ) == photo1) || (collection.getPhoto( 2 ) == photo3) );
        
    }
    /**
     * Simple test listener
     */
    private class TestChangeListener implements PhotoCollectionChangeListener {
        public boolean isNotified = false;
        public void photoCollectionChanged( PhotoCollectionChangeEvent ev ) {
            isNotified = true;
        }
    }
    
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Test_SortedCollection.class.getName() );

    public static void main( String[] args ) {
	//	org.apache.log4j.BasicConfigurator.configure();
	log.setLevel( org.apache.log4j.Level.DEBUG );
	org.apache.log4j.Logger photoLog = org.apache.log4j.Logger.getLogger( PhotoInfo.class.getName() );
	photoLog.setLevel( org.apache.log4j.Level.DEBUG );
	junit.textui.TestRunner.run( suite() );
    }
    public static Test suite() {
	return new TestSuite( Test_SortedCollection.class );
    }
}
