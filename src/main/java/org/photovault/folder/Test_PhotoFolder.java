/*
  Copyright (c) 2006 Harri Kaimio
  
  This file is part of Photovault.

  Photovault is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  Photovault is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.folder;

import junit.framework.*;
import org.hibernate.Query;
import org.hibernate.Session;
import java.util.*;
import java.sql.*;
import java.io.*;
import org.hibernate.Transaction;
import org.hibernate.context.ManagedSessionContext;
import org.photovault.command.Command;
import org.photovault.command.CommandException;
import org.photovault.command.PhotovaultCommandHandler;
import org.photovault.dbhelper.ImageDb;
import org.photovault.imginfo.*;
import org.photovault.imginfo.PhotoCollectionChangeEvent;
import org.photovault.imginfo.PhotoCollectionChangeListener;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoNotFoundException;
import org.photovault.common.PhotovaultSettings;
import org.photovault.persistence.HibernateUtil;
import org.photovault.test.PhotovaultTestCase;

public class Test_PhotoFolder extends PhotovaultTestCase {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Test_PhotoFolder.class.getName() );

    String testImgDir = "testfiles";
    PhotoFolderDAO folderDAO = new PhotoFolderDAOHibernate();
    Session session = null;
    Transaction tx = null;
    
    /**
       Sets up the test environment. retrieves from database the hierarchy with 
       "subfolderTest" as root and creates a TreeModel from it
    */
    public void setUp() {
        session = HibernateUtil.getSessionFactory().openSession();
        ManagedSessionContext.bind( (org.hibernate.classic.Session) session);
        tx = session.beginTransaction();
    }

    public void tearDown() {
        tx.commit();
        session.close();
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
        folder = folderDAO.makePersistent( folder );
        folderDAO.flush();
        assertMatchesDb( folder, session );
        
        log.debug( "Changing folder name for " + folder.getFolderId() );
	folder.setName( "testTop" );
	log.debug( "Folder name changed" );
	
	// Try to find the object from DB
	List folders = null;
	try {
	    folders = session.createQuery("from PhotoFolder where id = :id" ).
                    setInteger( "id", folder.getFolderId() ).list();
	} catch ( Exception e ) {
	    fail( e.getMessage() );
	}

        for ( Object o : folders ) {
            PhotoFolder folder2 = (PhotoFolder) o;
	    log.debug( "found top, id = " + folder2.getFolderId() );
	    assertEquals( "Folder name does not match", folder2.getName(), "testTop" );
	    log.debug( "Modifying desc" );
	    folder2.setDescription( "Test description" );
	}
    }
    
    // Tests the retrieval of existing folder from database
    public void testRetrieve() {
        List folders = null;
        PhotoFolder folder = folderDAO.findById( 1, false );
        assertMatchesDb( folder, session );
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
        PhotoFolder root = folderDAO.findRootFolder();
        folder = PhotoFolder.create( "PhotoAdditionTest", null );
        folder = folderDAO.makePersistent( folder );
        folder.reparentFolder( root );
        folder.addPhoto( photo );
        folderDAO.flush();
        assertMatchesDb( folder, session );
        
        assertEquals( "Photo not visible in folders' photo count", folder.getPhotoCount(), 1 );
        
        // Clean up the test folder
        PhotoFolder parent = folder.getParentFolder();
        parent.removeSubfolder( folder );
        folderDAO.makeTransient( folder );
    }


    public void testPhotoRetrieval() {
	// Find the corrent test case
        List folders = null;
        Query q = session.createQuery( "from PhotoFolder where name = :name" );
        q.setString( "name", "testPhotoRetrieval" );
        folders = q.list();
        PhotoFolder folder = (PhotoFolder) folders.get(0);
        assertMatchesDb( folder, session );
	
	// Check that the folder content is OK
	boolean found = false;
	for (PhotoInfo photo : folder.getPhotos() ) {
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
        f = folderDAO.makePersistent( f );
        folderDAO.flush();
	assertMatchesDb( f );

	// Test modifications without existing transaction context
	f.setName( "test name 2" );
	f.setDescription( "Description" );
        folderDAO.flush();
	assertMatchesDb( f );

	// Tets modifications in transaction context
	f.setName( "test name 3" );
	f.setDescription( "desc 3" );
        folderDAO.flush();
	assertMatchesDb( f );

    }
    
    public void testCreateCommand() {
        PhotovaultCommandHandler cmdHandler = new PhotovaultCommandHandler( null );
        CreatePhotoFolderCommand createCmd = new CreatePhotoFolderCommand( null, "command create", "desc" );
        try {
            cmdHandler.executeCommand( createCmd );
        } catch (CommandException ex) {
            fail( ex.getMessage() );
        }
        PhotoFolder createdFolder = createCmd.getCreatedFolder();
        assertMatchesDb( createdFolder );
        
        ChangePhotoFolderCommand changeCmd = new ChangePhotoFolderCommand( createdFolder );
        changeCmd.setName( "Name 2" );
        changeCmd.setDescription( "Decription 2" );
        changeCmd.setParent( folderDAO.findRootFolder() );        
        try {
            cmdHandler.executeCommand( changeCmd );
        } catch (CommandException ex) {
            fail( ex.getMessage() );
        }
        PhotoFolder changedFolder = changeCmd.getChangedFolder();
        PhotoFolder f = (PhotoFolder) session.merge( changedFolder );
        assertEquals( "Name 2", f.getName() );
	assertMatchesDb( f );
    }
    
    /**
       Utility
    */
    void assertMatchesDb( PhotoFolder folder ) {
        assertMatchesDb( folder, session );
    }
	

	
    
    /**
       Test that subfolders are created correctly
    */
    public void testSubfolders() {
        Query q = session.createQuery( "from PhotoFolder where name = :name" );
        q.setString("name", "subfolderTest" );
	PhotoFolder topFolder = (PhotoFolder) q.uniqueResult();
	assertEquals( "Top folder name invalid", "subfolderTest",topFolder.getName() );
	assertEquals( "topFolder should have 4 subfolders", 4, topFolder.getSubfolderCount() );

	String[] subfolderNames = {"Subfolder1", "Subfolder2", "Subfolder3", "Subfolder4"};

	// Check subfolder addition
	PhotoFolder newFolder = PhotoFolder.create( "Subfolder5", topFolder );
        folderDAO.flush();
	assertEquals( "New subfolder added", 5, topFolder.getSubfolderCount() );
        assertMatchesDb( topFolder, session );
        assertMatchesDb( newFolder, session );

	newFolder.delete();
        folderDAO.makeTransient( newFolder );
        folderDAO.flush();
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
        folderDAO.makePersistent( folder );
	String fname = "test1.jpg";
	File f = new File( testImgDir, fname );
	PhotoInfo photo = null;
	try {
	    photo = PhotoInfo.addToDB( f );
	} catch ( PhotoNotFoundException e ) {
	    fail( "Could not find photo: " + e.getMessage() );
	}

	folder.addPhoto( photo );
        folderDAO.flush();
	photo.delete();
        folderDAO.flush();
        // TOOD: CHEC DATABASE STATE!!!
	assertEquals( "After deleting the photo there should be no photos in the folder",
		      folder.getPhotoCount(), 0 );
	
    }

    /**
       Tests that getRoot() method returns the root folder and that it returns the same
       instance all the time.
    */
    public void testGetRoot() {
	PhotoFolder root1 = folderDAO.findRootFolder();
	PhotoFolder root2 = folderDAO.findRootFolder();
	assertTrue( "several instances of root created", root1==root2 );
        
        assertEquals( root1.getName(), "Top" );
        assertNull( root1.getParentFolder() );
    }
    

}

