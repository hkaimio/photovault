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

package org.photovault.imginfo.indexer;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.context.ManagedSessionContext;
import org.photovault.command.PhotovaultCommandHandler;
import org.photovault.common.PVDatabase;
import org.photovault.common.PhotovaultException;
import org.photovault.common.PhotovaultSettings;
import org.photovault.dbhelper.ODMG;
import org.photovault.folder.PhotoFolder;
import org.photovault.folder.PhotoFolderDAO;
import org.photovault.imginfo.ExternalVolume;
import org.photovault.imginfo.FileUtils;
import org.photovault.imginfo.ImageInstance;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;
import org.photovault.test.PhotovaultTestCase;

/**
 *
 * @author harri
 */
public class Test_ExtVolIndexer extends PhotovaultTestCase {
    
    private static Log log = LogFactory.getLog( Test_ExtVolIndexer.class );

    /** Creates a new instance of Test_ExtVolIndexer */
    public Test_ExtVolIndexer() {
    }
    
    
    DAOFactory daoFactory = DAOFactory.instance( HibernateDAOFactory.class );
    Session session = null;
    File testfile1 = null;
    File testfile2 = null;
    File testfile3 = null;
    
    /**
     Sets up the directory hiearchy used in test cases. Also deletes all th photos
     used in test from database so that hash lookups give expected results
     */
    public void setUp() {
        log.debug( "entry: setUp" );
        session = HibernateUtil.getSessionFactory().openSession();
        ManagedSessionContext.bind( (org.hibernate.classic.Session) session );
        ((HibernateDAOFactory) daoFactory).setSession( session );
        Transaction tx = session.beginTransaction();
        try {
            // Create the directories
            extVolDir = File.createTempFile( "pv_indexer_test_", "" );
            extVolDir.delete();
            extVolDir.mkdir();
            File extVolSubdir = new File( extVolDir, "test" );
            extVolSubdir.mkdir();
            testfile1 = new File("testfiles", "test1.jpg");
            testfile2 = new File( "testfiles", "test2.jpg" );
            testfile3 = new File( "testfiles", "test3.jpg" );
            photo1 = new File( extVolDir, "test1.jpg");
            FileUtils.copyFile( testfile1, photo1 );
            photo2inst1 = new File( extVolDir, "test2.jpg");
            FileUtils.copyFile( testfile2, photo2inst1 );
            photo2inst2 = new File( extVolSubdir, "test2.jpg");
            FileUtils.copyFile( testfile2, photo2inst2 );
            
            // Non-image file in volume
            File txtFile = new File( extVolDir, "test.txt" );
            FileWriter writer = new FileWriter( txtFile );
            writer.write( "Not an image");
            writer.close();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        // Create top folder for indexed files
        PhotoFolderDAO folderDAO = daoFactory.getPhotoFolderDAO();
        PhotoInfoDAO photoDAO = daoFactory.getPhotoInfoDAO();
        topFolder = PhotoFolder.create( "ExtVolTest", null );
        folderDAO.makePersistent( topFolder );
        topFolder.reparentFolder( folderDAO.findRootFolder() );
        
        // Remove the test files from database if they are already there
        hash1 = ImageInstance.calcHash( testfile1 );
        List photos1 = photoDAO.findPhotosWithOriginalHash( hash1 );
        if ( photos1.size() > 0 ) {
            for ( Object o : photos1 ) {
                photoDAO.makeTransient( (PhotoInfo) o );
            }
        }
        hash2 = ImageInstance.calcHash( testfile2 );
        List photos2 = photoDAO.findPhotosWithOriginalHash( hash2 );
        if ( photos2.size() > 0 ) {
            for ( Object o : photos2 ) {
                photoDAO.makeTransient( (PhotoInfo) o );
            }
        }
        hash3 = ImageInstance.calcHash( testfile3 );
        List photos3 = photoDAO.findPhotosWithOriginalHash( hash3 );
        if ( photos3.size() > 0 ) {
            for ( Object o : photos3 ) {
                photoDAO.makeTransient( (PhotoInfo) o );
            }
        }
        session.flush();
        tx.commit();
        log.debug( "exit: setUp" );
    }

    public void tearDown() {
        log.debug( "entry: tearDown" );
        FileUtils.deleteTree( extVolDir );
        PhotoFolderDAO folderDAO = daoFactory.getPhotoFolderDAO();        
        PhotoInfoDAO photoDAO = daoFactory.getPhotoInfoDAO();        
        topFolder.delete();
        folderDAO.makeTransient( topFolder );        
        
        // Remove the test files from database if they are already there
        hash1 = ImageInstance.calcHash( testfile1 );
        List photos1 = photoDAO.findPhotosWithOriginalHash( hash1 );
        if ( photos1.size() > 0 ) {
            for ( Object o : photos1 ) {
                photoDAO.makeTransient( (PhotoInfo) o );
            }
        }
        hash2 = ImageInstance.calcHash( testfile2 );
        List photos2 = photoDAO.findPhotosWithOriginalHash( hash2 );
        if ( photos2.size() > 0 ) {
            for ( Object o : photos2 ) {
                photoDAO.makeTransient( (PhotoInfo) o );
            }
        }
        hash3 = ImageInstance.calcHash( testfile3 );
        List photos3 = photoDAO.findPhotosWithOriginalHash( hash3 );
        if ( photos3.size() > 0 ) {
            for ( Object o : photos3 ) {
                photoDAO.makeTransient( (PhotoInfo) o );
            }
        }
        log.debug( "exit: tearDown" );
    }
    
    File extVolDir = null;
    byte[] hash1;
    byte[] hash2;
    byte[] hash3;
    
    File photo1 = null;
    File photo2inst1 = null;
    File photo2inst2 = null;

    private PhotoFolder topFolder = null;
    
    private class TestListener implements ExtVolIndexerListener {
        public int instanceCount = 0;
        public int photoCount = 0;
        boolean complete = false;
        
        public void fileIndexed(ExtVolIndexerEvent e) {
            switch ( e.getResult() ) {
                case ExtVolIndexerEvent.RESULT_NEW_PHOTO:
                    instanceCount++;
                    photoCount++;
                    break;
                case ExtVolIndexerEvent.RESULT_NEW_INSTANCE:
                    instanceCount++;
                    break;
            }
        }

        public void indexingComplete(ExtVolIndexer indexer) {
            complete = true;
        }

        public void indexingError(String message) {
        }
        
    }
    
    public void testIndexing() {
        int n;
        ExternalVolume v = new ExternalVolume( "extVol", extVolDir.getAbsolutePath() );
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        PVDatabase db = settings.getDatabase( "pv_junit" );
        try {
            db.addVolume( v );
        } catch (PhotovaultException ex) {
            fail( ex.getMessage() );
        }
        ExtVolIndexer indexer = new ExtVolIndexer( v );
        indexer.setTopFolder( topFolder );
        indexer.setCommandHandler( new PhotovaultCommandHandler( null ) );
        TestListener l = new TestListener();
        indexer.addIndexerListener( l );
        
        assertEquals( "Indexing not started -> completeness must be 0", 
                0, indexer.getPercentComplete() );
        assertNull( "StartTime must be null before starting", indexer.getStartTime() );
        log.debug( "running index #1" );
        indexer.run();
        log.debug( "done" );
        
        session.clear();
        log.debug( "Cleared session" );
        PhotoFolderDAO folderDAO = daoFactory.getPhotoFolderDAO();
        topFolder = folderDAO.findById( topFolder.getFolderId(), false );
        // Check that all the files can be found
        PhotoInfoDAO photoDAO = daoFactory.getPhotoInfoDAO();
        List photos1 = photoDAO.findPhotosWithOriginalHash( hash1 );
        assertNotNull( "photos1 = null", photos1 );
        assertEquals( "Only 1 photo per picture should be found", 1, photos1.size() );
        PhotoInfo p1 = (PhotoInfo) photos1.get(0);
        assertEquals( "2 instances should be found in photo 1", 1, p1.getNumInstances() );
        List photos2 = photoDAO.findPhotosWithOriginalHash( hash2 );
        assertNotNull( "photos1 = null", photos2 );
        assertEquals( "Only 1 photo per picture should be found", 1, photos2.size() );
        PhotoInfo p2 = (PhotoInfo) photos2.get(0);
        assertEquals( "3 instances should be found in photo 1", 2, p2.getNumInstances() );

        // Check that both instances of p2 can be found
        boolean found[] = {false, false};
        File files[] = {photo2inst1, photo2inst2};
        for ( ImageInstance i : p2.getInstances() ) {
            for ( int m = 0; m < found.length; m++ ) {
                if ( files[m].equals( i.getImageFile() ) ) {
                    found[m] = true;
                }
            }
        }
        for ( n = 0; n < found.length; n++ ) {
            assertTrue( "Photo " + n + " not found", found[n] );
        } 
        
        // Check that the folders have the correct photos
        PhotoInfo[] photosInTopFolder = { p1, p2 };
        assertFolderHasPhotos( topFolder, photosInTopFolder );

        PhotoFolder subFolder = topFolder.getSubfolder( 0 );
        assertEquals( "Subfolder name not correct", "test", subFolder.getName() );
        PhotoInfo[] photosInSubFolder = { p2 };
        assertFolderHasPhotos( subFolder, photosInSubFolder );   
        
        // Check that the listener was called correctly
        assertEquals( "Wrong photo count in listener", 2, l.photoCount );
        assertEquals( "Wrong photo count in indexer statistics", 2, indexer.getNewPhotoCount() );
        assertEquals( "Wrong instance count in listener", 3, l.instanceCount );
        assertEquals( "Wrong instance count in indexer statistics", 3, indexer.getNewInstanceCount() );
        
        assertEquals( "Indexing complete 100%", 100, indexer.getPercentComplete() );
        assertNotNull( "StartTime still null", indexer.getStartTime() );
            
        // Next, let's make some modifications to the external volume
        log.debug( "modifying volume" );
        try {
            // New file
            File testfile3 = new File( "testfiles", "test3.jpg" ); 
            File f3 = new File( extVolDir, "test3.jpg");
            FileUtils.copyFile( testfile3, f3 );
            
            // Replace the test1 file with test3
            File f1 = new File ( extVolDir, "test1.jpg" );
            FileUtils.copyFile( testfile3, f1 );
            
            // Remove 1 copy of test2
            File f2 = new File( extVolDir, "test2.jpg" );
            f2.delete();
        } catch (IOException ex) {
            fail( "IOException while altering external volume: " + ex.getMessage() );
        }
        
        indexer = new ExtVolIndexer( v );
        indexer.setTopFolder( topFolder );
        indexer.setCommandHandler( new PhotovaultCommandHandler( null ) );
        
        l = new TestListener();
        indexer.addIndexerListener( l );
    
        assertEquals( "Indexing not started -> completeness must be 0", 
                0, indexer.getPercentComplete() );
        assertNull( "StartTime must be null before starting", indexer.getStartTime() );
        log.debug( "indeing round #2" );
        indexer.run();
        log.debug( "indexing done" );
        
        // Clear session cache
        session.clear();
        log.debug( "session cleared" );
        folderDAO = daoFactory.getPhotoFolderDAO();
        topFolder = folderDAO.findById( topFolder.getFolderId(), false );
        
        // Check that the folders have the correct photos

        List photos3 = photoDAO.findPhotosWithOriginalHash( hash3 );
        assertEquals( "1 photo per picture should be found", 1, photos3.size() );
        PhotoInfo p3 = (PhotoInfo) photos3.get( 0 );        
        PhotoInfo photosInTopFolder2[] = { p3 };
        assertFolderHasPhotos( topFolder, photosInTopFolder2 );
        assertEquals( "More than 1 subfolder in topFolder", 1, topFolder.getSubfolderCount() );
        subFolder = topFolder.getSubfolder( 0 );
        assertEquals( "Subfolder name not correct", "test", subFolder.getName() );
        photos2 = photoDAO.findPhotosWithOriginalHash( hash2 );
        p2 = (PhotoInfo) photos2.get( 0 );
        PhotoInfo[] photosInSubFolder2 = { p2 };
        assertFolderHasPhotos( subFolder, photosInSubFolder2 );   
        Collection p2folders = p2.getFolders();
        assertFalse( "p2 must not be in topFolder", p2folders.contains( topFolder ) );
    }
    
    void assertFolderHasPhotos( PhotoFolder folder, PhotoInfo photos[] ) {
        boolean found[] = new boolean[photos.length];
        for ( int n = 0; n < photos.length; n++ ) {
            found[n] = false;
        }
        for ( PhotoInfo p : folder.getPhotos() ) {
            for ( int n = 0; n < photos.length; n++ ) {
                if ( p == photos[n] ) {
                    found[n] = true;
                }
            }
        }
        for ( int n = 0; n < photos.length; n++ ) {
            if ( !found[n] ) {
                fail( "Photo " + n + ": " + photos[n].getId() + " not found in folder " 
                        +  folder.getName() );
            }
        }
    }

    public static Test suite() {
	return new TestSuite( Test_ExtVolIndexer.class );
    }
    
    
    
    public static void main( String[] args ) {
        URL log4jPropertyURL = Test_ExtVolIndexer.class.getClassLoader().getResource( "photovault_log4j.properties");
        org.apache.log4j.PropertyConfigurator.configure( log4jPropertyURL );	        
	org.apache.log4j.Logger instLog = org.apache.log4j.Logger.getLogger( ExtVolIndexer.class.getName() );
	instLog.setLevel( org.apache.log4j.Level.DEBUG );
	junit.textui.TestRunner.run( suite() );
    }    
}
