// $Id: TestPhotoFolder.java,v 1.2 2003/02/22 13:10:55 kaimio Exp $

package photovault.folder;

import junit.framework.*;
import org.odmg.*;
import java.util.*;
import org.odmg.Database;
import org.odmg.Implementation;
import org.apache.ojb.odmg.*;
import imginfo.*;

public class TestPhotoFolder extends TestCase {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( TestPhotoFolder.class.getName() );

    Implementation odmg = null;
    Database db = null;
    Transaction tx = null;

    /**
       Sets up the test environment. retrieves from database the hierarchy with "subfolderTest" as root and creates a TreeModel from it
    */
    public void setUp() {
	odmg = OJB.getInstance();
	db = odmg.newDatabase();
	try {
	    db.open( "repository.xml", Database.OPEN_READ_WRITE );
	} catch ( ODMGException e ) {
	    //	    log.warn( "Could not open database: " + e.getMessage() );
	    db = null;
	}

	tx = odmg.newTransaction();
	tx.begin();
    }

    public void tearDown() {
    }

    public static Test suite() {
	return new TestSuite( TestPhotoFolder.class );
    }

    public static void main( String[] args ) {
	org.apache.log4j.BasicConfigurator.configure();
	log.setLevel( org.apache.log4j.Level.DEBUG );
	junit.textui.TestRunner.run( suite() );
    }

    public void testCreate() {
	
	PhotoFolder folder = PhotoFolder.create( "Top", null );

	// Try to find the object from DB
	DList folders = null;
	Transaction tx = odmg.newTransaction();
	tx.begin();
	try {
	    OQLQuery query = odmg.newOQLQuery();
	    query.create( "select folders from " + PhotoFolder.class.getName() + " where folderId = " + folder.getFolderId() );
	    folders = (DList) query.execute();
	} catch ( Exception e ) {
	    tx.abort();
	    fail( e.getMessage() );
	}

	Iterator iter = folders.iterator();
	boolean found = false;
	while ( iter.hasNext() ) {
	    PhotoFolder folder2 = (PhotoFolder) iter.next();
	    if ( folder2.getName().equals( "Top" ) ) {
		found = true;
		log.debug( "found top, id = " + folder2.getFolderId() );
		assertEquals( "Folder not found in DB", folder2.getName(), "Top" );
		log.debug( "Modifying desc" );
		folder2.setDescription( "Test description" );
		tx.lock( folder2, Transaction.WRITE );
	    }
	}
	tx.commit();
    }
    
    // Tests the retrieval of existing folder from database
    public void testRetrieve() {
	DList folders = null;
	try {
	    OQLQuery query = odmg.newOQLQuery();
	    query.create( "select folders from " + PhotoFolder.class.getName() );
	    folders = (DList) query.execute();
	    tx.commit();
	} catch ( Exception e ) {
	    tx.abort();
	    fail( e.getMessage() );
	}

	Iterator iter = folders.iterator();
	boolean found = false;
	while ( iter.hasNext() ) {
	    PhotoFolder folder = (PhotoFolder) iter.next();
	    if ( folder.getName().equals( "Top" ) ) {
		found = true;
		log.debug( "found top, id = " + folder.getFolderId() );
		assertEquals( "Folder with id 0 should be the top", folder.getName(), "Top" );
	    }
	}
	assertTrue( "Top folder not found", found );
    }

    /**
       Test that subfolders are created correctly
    */
    public void testSubfolders() {
	DList folders = null;
	try {
	    OQLQuery query = odmg.newOQLQuery();
	    query.create( "select folders from " + PhotoFolder.class.getName()  + " where name = \"subfolderTest\"" );
	    folders = (DList) query.execute();
	    tx.commit();
	} catch ( Exception e ) {
	    tx.abort();
	    fail( e.getMessage() );
	}
	
	PhotoFolder topFolder = (PhotoFolder) folders.get( 0 );
	assertEquals( "Top folder name invalid", "subfolderTest",topFolder.getName() );
	assertEquals( "topFolder should have 4 subfolders", 4, topFolder.getSubfolderCount() );

	String[] subfolderNames = {"Subfolder1", "Subfolder2", "Subfolder3", "Subfolder4"};
	// Check that all subfolder are found
	for ( int n = 0; n < topFolder.getSubfolderCount(); n++ ) {
	    PhotoFolder subfolder = topFolder.getSubfolder( n );
	    assertEquals( "Subfolder name incorrect", subfolderNames[n], subfolder.getName() );
	}

	// Check subfolder addition
	PhotoFolder newFolder = PhotoFolder.create( "Subfolder5", topFolder );
	assertEquals( "New subfolder added", 5, topFolder.getSubfolderCount() );

	newFolder.delete();
	assertEquals( "Subfolder deleted", 4, topFolder.getSubfolderCount() );
    }

    class TestListener implements PhotoCollectionChangeListener {
	// implementation of imginfo.PhotoCollectionChangeListener interface

	public boolean modified = false;
	/**
	 *
	 * @param param1 <description>
	 */
	public void photoCollectionChanged(PhotoCollectionChangeEvent param1)
	{
	    modified = true;
	}
    }
    
    public void testListener() {
	PhotoFolder folder = PhotoFolder.create( "testListener", null );
	TestListener l1 = new TestListener();
	TestListener l2 = new TestListener();

	folder.addPhotoCollectionChangeListener( l1 );
	folder.addPhotoCollectionChangeListener( l2 );

	folder.setName( "testLiistener" );
	assertTrue( "l1 not called", l1.modified );
	assertTrue( "l2 not called", l2.modified );
	l1.modified = false;
	l2.modified = false;
	
	folder.removePhotoCollectionChangeListener( l2 );
	folder.setDescription( "Folder usded to test listener support" );
	assertTrue( "l1 not called", l1.modified );
	assertFalse( "l2 should not have been called", l2.modified );

	// TODO: test other fields
    }
}
