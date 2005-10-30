// $Id: TestPhotoFolder.java,v 1.6 2003/03/04 19:35:44 kaimio Exp $

package photovault.folder;

import junit.framework.*;
import org.odmg.*;
import java.util.*;
import java.sql.*;
import java.io.*;
import org.odmg.Database;
import org.odmg.Implementation;
import org.apache.ojb.odmg.*;
import imginfo.*;
import dbhelper.*;
import photovault.common.PhotovaultSettings;
import photovault.test.PhotovaultTestCase;

public class Test_PhotoFolder extends PhotovaultTestCase {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Test_PhotoFolder.class.getName() );

    Implementation odmg = null;
    Database db = null;
    Transaction tx = null;
  String testImgDir = "testfiles";
  
    /**
       Sets up the test environment. retrieves from database the hierarchy with 
       "subfolderTest" as root and creates a TreeModel from it
    */
    public void setUp() {
//	PhotovaultSettings.setConfiguration( "pv_test" );
//	String sqldbName = PhotovaultSettings.getConfProperty( "dbname" );
//        ODMG.initODMG("harri", "r1t1rat1", sqldbName);
	odmg = ODMG.getODMGImplementation();
	db = ODMG.getODMGDatabase();
//	try {
//	    db.open( "repository.xml", Database.OPEN_READ_WRITE );
//	} catch ( ODMGException e ) {
//	    //	    log.warn( "Could not open database: " + e.getMessage() );
//	    db = null;
//	}

// 	tx = odmg.newTransaction();
// 	tx.begin();
    }

    public void tearDown() {
    }

    public static Test suite() {
	return new TestSuite( Test_PhotoFolder.class );
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
	    query.create( "select folders from " + PhotoFolder.class.getName() + " where folderId = 1" );
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
		assertEquals( "Folder with id 1 should be the top", folder.getName(), "Top" );
	    }
	}
	assertTrue( "Top folder not found", found );
    }

    
    /** A new photo is created & added to the folder. Verify that it
     * is both persistent & visible in the folder
     */

    public void testPhotoAddition() {
	String fname = "test1.jpg";
	File f = new File( testImgDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Could not find photo: " + e.getMessage() );
	}

	PhotoFolder folder = null;
	// Create a folder for the photo
	try {
	    folder = PhotoFolder.create( "PhotoAdditionTest", PhotoFolder.getRoot() );
	    folder.addPhoto( photo );
	    
	    assertEquals( "Photo not visible in folders' photo count", folder.getPhotoCount(), 1 );
	} finally {
	    // Clean up the test folder
	    PhotoFolder parent = folder.getParentFolder();
	    parent.removeSubfolder( folder );
	    photo.delete();
	}
    }


    public void testPhotoRetrieval() {
	// Find the corrent test case
	DList folders = null;
	Transaction tx = odmg.newTransaction();
	tx.begin();
	try {
	    OQLQuery query = odmg.newOQLQuery();
	    query.create( "select folders from " + PhotoFolder.class.getName() + " where name = \"testPhotoRetrieval\"" );
	    folders = (DList) query.execute();
	    tx.commit();
	} catch ( Exception e ) {
	    tx.abort();
	    fail( e.getMessage() );
	}
	PhotoFolder folder = (PhotoFolder) folders.get(0);

	assertEquals( "Number of photos in folder does not match", 2, folder.getPhotoCount() );
	
	// Check that the folder content is OK
	boolean found = false;
	for ( int i = 0; i < folder.getPhotoCount(); i++ ) {
	    PhotoInfo photo = folder.getPhoto( i );
	    if ( photo.getDescription().equals( "testPhotoRetrieval1" ) ) {
		found = true;
	    }
	}
	assertTrue( "Photo testRetrieval1 not found", found );

	// TODO: Check that a new photo added to the album is added to DB also

	// TODO: check that removing a photo from the folder removes the link in DB also

    }
    
    /**
       Tests that persistence operations succeed.
    */
    public void testPersistence() {
	// Test creation of a new folder
	PhotoFolder f = PhotoFolder.create( "persistenceTest", null );
	assertMatchesDb( f );

	// Test modifications without existing transaction context
	f.setName( "test name 2" );
	f.setDescription( "Description" );
	assertMatchesDb( f );

	// Tets modifications in transaction context
	Transaction tx = odmg.newTransaction();
	tx.begin();
	f.setName( "test name 3" );
	f.setDescription( "desc 3" );
	tx.commit();
	assertMatchesDb( f );

    }
    
    /**
       Utility
    */
    void assertMatchesDb( PhotoFolder folder ) {
	int id = folder.getFolderId();
	String sql = "select * from photo_collections where collection_id = " + id;
	Statement stmt = null;
	ResultSet rs = null;
	try {
	    stmt = ImageDb.getConnection().createStatement();
	    rs = stmt.executeQuery( sql );
	    if ( !rs.next() ) {
		fail( "rrecord not found" );
	    }
	    assertEquals( "name doesn't match", folder.getName(), rs.getString( "collection_name" ) );
	    assertEquals( "description doesn't match", folder.getDescription(), rs.getString( "collection_desc" ) );
	} catch ( SQLException e ) {
	    fail( e.getMessage() );
	} finally {
	    if ( rs != null ) {
		try {
		    rs.close();
		} catch ( Exception e ) {
		    fail( e.getMessage() );
		}
	    }
	    if ( stmt != null ) {
		try {
		    stmt.close();
		} catch ( Exception e ) {
		    fail( e.getMessage() );
		}
	    }
	}
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

	// Test that photo addition is notified
	String fname = "test1.jpg";
	File f = new File( testImgDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Could not find photo: " + e.getMessage() );
	}
	l1.modified = false;
	folder.addPhoto( photo );
	assertTrue( "l1 not called when adding photo", l1.modified );

	l1.modified = false;
	folder.removePhoto( photo );
	assertTrue( "l1 not called when removing photo", l1.modified );
	photo.delete();
	
	subfolder.delete();
	assertTrue( "Not notified of subfolder structure change", l1.structureModified );
	assertEquals( "subfolder info not correct", folder, l1.structChangeFolder );

	// TODO: test other fields
    }

    /**
       test that when a photo is deleted from database the folder is also modified
    */
    public void testPhotoDelete() {
	PhotoFolder folder = PhotoFolder.create( "testListener", null );
	String fname = "test1.jpg";
	File f = new File( testImgDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Could not find photo: " + e.getMessage() );
	}

	folder.addPhoto( photo );
	photo.delete();
	assertEquals( "After deleting the photo there should be no photos in the folder",
		      folder.getPhotoCount(), 0 );
	
    }

    /**
       Tests that getRoot() method returns the root folder and that it returns the same
       instance all the time.
    */
    public void testGetRoot() {
	PhotoFolder root1 = PhotoFolder.getRoot();
	PhotoFolder root2 = PhotoFolder.getRoot();
	assertTrue( "several instances of root created", root1==root2 );
    }
}

