// $Id: TestPhotoFolder.java,v 1.4 2003/02/25 20:57:11 kaimio Exp $

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
       Sets up the test environment. retrieves from database the hierarchy with 
       "subfolderTest" as root and creates a TreeModel from it
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

// 	tx = odmg.newTransaction();
// 	tx.begin();
    }

    public void tearDown() {
    }

    public static Test suite() {
	return new TestSuite( TestPhotoFolder.class );
    }

    public static void main( String[] args ) {
	//	org.apache.log4j.BasicConfigurator.configure();
	log.setLevel( org.apache.log4j.Level.DEBUG );
	org.apache.log4j.Logger folderLog = org.apache.log4j.Logger.getLogger( PhotoFolder.class.getName() );
	folderLog.setLevel( org.apache.log4j.Level.DEBUG );
	junit.textui.TestRunner.run( suite() );
    }

    public void testCreate() {
	
	PhotoFolder folder = PhotoFolder.create( "Top", null );
	Transaction tx = odmg.newTransaction();
	log.debug( "Changing folder name for " + folder.getFolderId() );
	tx.begin();
	folder.setName( "testTop" );
	tx.commit();
	log.debug( "Folder name changed" );
	
	// Try to find the object from DB
	DList folders = null;
	try {
	    OQLQuery query = odmg.newOQLQuery();
	    query.create( "select folders from " + PhotoFolder.class.getName() + " where folderId = " + folder.getFolderId() );
	    folders = (DList) query.execute();
	} catch ( Exception e ) {
	    fail( e.getMessage() );
	}

	Iterator iter = folders.iterator();
	while ( iter.hasNext() ) {
	    PhotoFolder folder2 = (PhotoFolder) iter.next();
	    log.debug( "found top, id = " + folder2.getFolderId() );
	    assertEquals( "Folder name does not match", folder2.getName(), "testTop" );
	    log.debug( "Modifying desc" );
	    folder2.setDescription( "Test description" );
	}
    }
    
    // Tests the retrieval of existing folder from database
    public void testRetrieve() {
	DList folders = null;
	Transaction tx = odmg.newTransaction();
	tx.begin();
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
	Transaction tx = odmg.newTransaction();
	tx.begin();
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

    class TestListener implements PhotoFolderChangeListener {
	// implementation of imginfo.PhotoCollectionChangeListener interface

	public boolean modified = false;
	public boolean subfolderModified= false;
	public boolean structureModified = false;
	public PhotoFolder changedFolder = null;
	public PhotoFolder structChangeFolder = null;
	/**
	 *
	 * @param param1 <description>
	 */
	public void photoCollectionChanged(PhotoCollectionChangeEvent param1)
	{
	    modified = true;
	}

	public void subfolderChanged( PhotoFolderEvent e ) {
	    subfolderModified = true;
	    changedFolder = e.getSubfolder();
	}

	public void structureChanged( PhotoFolderEvent e ) {
	    structureModified = true;
	    structChangeFolder = e.getSubfolder();
	}
	
    }
    
    class TestCollectionListener implements PhotoCollectionChangeListener {
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
	TestCollectionListener l3 = new TestCollectionListener();

	folder.addPhotoCollectionChangeListener( l1 );
	folder.addPhotoCollectionChangeListener( l2 );
	folder.addPhotoCollectionChangeListener( l3 );

	folder.setName( "testLiistener" );
	assertTrue( "l1 not called", l1.modified );
	assertTrue( "l2 not called", l2.modified );
	assertTrue( "l3 not called", l3.modified );
	folder.setName( "testListener" );
	l1.modified = false;
	l2.modified = false;
	
	folder.removePhotoCollectionChangeListener( l2 );
	folder.setDescription( "Folder usded to test listener support" );
	assertTrue( "l1 not called", l1.modified );
	assertFalse( "l2 should not have been called", l2.modified );

	// Test creation of a new subfolder
	PhotoFolder subfolder = PhotoFolder.create( "New subfolder", folder );
	assertTrue( "Not notified of subfolder structure change", l1.structureModified );
	assertEquals( "subfolder info not correct", folder, l1.structChangeFolder );
	l1.structureModified = false;
	l1.changedFolder = null;

	subfolder.setDescription( "Changed subfolder" );
	assertTrue( "l1 not called for subfolder modification", l1.subfolderModified );
	assertEquals( "subfolder info not correct", subfolder, l1.changedFolder );
	l1.subfolderModified = false;
	l1.changedFolder = null;
	
	subfolder.delete();
	assertTrue( "Not notified of subfolder structure change", l1.structureModified );
	assertEquals( "subfolder info not correct", folder, l1.structChangeFolder );

	// TODO: test other fields
    }
}
