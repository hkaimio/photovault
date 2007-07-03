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

package org.photovault.imginfo;

import junit.framework.*;
import java.sql.*;
import java.io.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.photovault.dbhelper.ImageDb;
import org.photovault.persistence.HibernateUtil;
import org.photovault.test.PhotovaultTestCase;

public class Test_ImageInstance extends PhotovaultTestCase {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Test_ImageInstance.class.getName() );
    
    String testImgDir = "testfiles";
    String volumeRoot =  "/tmp/photoVaultImageInstanceTest";
    
    PhotoInfo photo = null;
    
    Volume volume = null;
    
    Session session = null;
    Transaction tx = null;
    
    ImageInstanceDAO instDAO = new ImageInstanceDAOHibernate();
    
    public Test_ImageInstance() {
        super();
    }
    
    /**
     Sets ut the test environment
     */
    public void setUp() {
        session = HibernateUtil.getSessionFactory().getCurrentSession();
        tx = session.beginTransaction();
        try {
            photo = PhotoInfo.findPhotoInfo( session, 1 );
        } catch ( Exception e ) {
            fail( "Unable to retrieve PhotoInfo object" );
        }
        File volumeDir = new File( volumeRoot );
        if ( !volumeDir.exists() ) {
            volumeDir.mkdirs();
        }
        volume = new Volume( "imageInstanceTest", volumeRoot );
    }
    
    /**
     Tears down the testing environment
     */
    public void tearDown() {
        FileUtils.deleteTree( volume.getBaseDir() );
        tx.commit();
        // session.close();
    }
    
    
    public void testImageInstanceCreate() {
        
        File testFile = new File( testImgDir, "test1.jpg" );
        File instanceFile = volume.getFilingFname( testFile );
        try {
            FileUtils.copyFile( testFile, instanceFile );
        } catch ( IOException e ) {
            fail( e.getMessage() );
        }
        ImageInstance f = ImageInstance.create( volume, instanceFile, photo,
                ImageInstance.INSTANCE_TYPE_ORIGINAL );
        assertNotNull( "Image instance is null", f );
        try {
            instDAO.makePersistent( f );
        } catch ( Throwable t ) {
            fail( t.getMessage() );
        }
        instDAO.flush();
        assertMatchesDb( f );
        instDAO.makeTransient( f );
        instDAO.flush();
    }
    
    public void testImageInstanceUpdate() {
        
        File testFile = new File( testImgDir, "test1.jpg" );
        File instanceFile = volume.getFilingFname( testFile );
        try {
            FileUtils.copyFile( testFile, instanceFile );
        } catch ( IOException e ) {
            fail( e.getMessage() );
        }
        ImageInstance f = ImageInstance.create( volume, instanceFile, photo,
                ImageInstance.INSTANCE_TYPE_ORIGINAL  );
        instDAO.makePersistent( f );
        instDAO.flush();
        assertNotNull( "Image instance is null", f );
        int width = f.getWidth();
        int height = f.getHeight();
        int hist = f.getInstanceType();
        f.setHeight( height + 1 );
        f.setWidth( width + 1 );
        f.setInstanceType( ImageInstance.INSTANCE_TYPE_THUMBNAIL );
        File imgFile = f.getImageFile();
        instDAO.makePersistent( f );
        instDAO.flush();
        assertMatchesDb( f );
        
        // Reload the object from database and check that the modifications are OK
        ImageInstance.InstanceId id = new ImageInstance.InstanceId();
        id.setFname( imgFile.getName() );
        id.setVolume_id( volume.getName() );
        f = instDAO.findById( id, false );
        assertNotNull( "Image instance is null", f );
        assertEquals( "Width not updated", f.getWidth(), width+1 );
        assertEquals( "height not updated", f.getHeight(), height+1 );
        assertEquals( "Instance type not updated", f.getInstanceType(), ImageInstance.INSTANCE_TYPE_THUMBNAIL );
        File imgFile2 = f.getImageFile();
        assertTrue( "Image file does not exist", imgFile2.exists() );
        assertTrue( "Image file name not same after update", imgFile.equals( imgFile2 ) );
        // Tidy up after execution
        instDAO.makeTransient( f );
    }
    
    public void testImageInstanceDelete() {
        File testFile = new File( testImgDir, "test1.jpg" );
        File instanceFile = volume.getFilingFname( testFile );
        try {
            FileUtils.copyFile( testFile, instanceFile );
        } catch ( IOException e ) {
            fail( e.getMessage() );
        }
        ImageInstance f = ImageInstance.create( volume, instanceFile, photo,
                ImageInstance.INSTANCE_TYPE_ORIGINAL  );
        assertNotNull( f );
        instDAO.makePersistent( f );
        instDAO.flush();
        assertMatchesDb( f );
        instDAO.makeTransient( f );
        instDAO.flush();
        
        Connection conn = session.connection();
        Statement stmt =null;
        ResultSet rs =null;
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery( "SELECT * FROM image_instances WHERE volume_id = '" + volume.getName() + "' AND fname = 'test1.jpg'" );
            if ( rs.next() ) {
                fail( "Found matching DB record after delete" );
            }
        } catch ( SQLException e ) {
            fail( "DB error:; " + e.getMessage() );
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
    
    // TODO: test case that demonstrates that imageFile & volume attributes are not
    // initialized correctly if standard RowReader is used.
    
    
    /**
     Utility to check that the object in memory matches the DB
     */
    void assertMatchesDb( ImageInstance i ) {
        String volumeName = i.getVolume().getName();
        String fname = i.getImageFile().getName();
        String sql = "select * from image_instances where volume_id = '" + volumeName + "' and fname = '"
                + fname + "'";
        Statement stmt = null;
        ResultSet rs = null;
        try {
            stmt = session.connection().createStatement();
            rs = stmt.executeQuery( sql );
            if ( !rs.next() ) {
                fail( "rrecord not found" );
            }
            // TODO: there is no pointer back from instance to photo so this cannot be checked
            //	    assertEquals( "photo doesn't match", i.getPhoto .getUid(), rs.getInt( "photo_id" ) );
            assertEquals( "width doesn't match", i.getWidth(), rs.getInt( "width" ) );
            assertEquals( "height doesn't match", i.getHeight(), rs.getInt( "height" ) );
            assertTrue( "rotated doesn't match", i.getRotated() == rs.getDouble( "rotated" ) );
            assertTrue( "Photo not correct", i.getPhoto().getUid() == rs.getInt( "photo_id" ) );
            int itype = i.getInstanceType();
            switch ( itype ) {
                case ImageInstance.INSTANCE_TYPE_ORIGINAL:
                    assertEquals( "instance type does not match", "original", rs.getString( "instance_type" ) );
                    break;
                case ImageInstance.INSTANCE_TYPE_MODIFIED:
                    assertEquals( "instance type does not match", "modified", rs.getString( "instance_type" ) );
                    break;
                case ImageInstance.INSTANCE_TYPE_THUMBNAIL:
                    assertEquals( "instance type does not match", "thumbnail", rs.getString( "instance_type" ) );
                    break;
                default:
                    fail( "Unknown image type " + itype );
            }
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
    
    public static void main( String[] args ) {
        //	org.apache.log4j.BasicConfigurator.configure();
        log.setLevel( org.apache.log4j.Level.DEBUG );
        org.apache.log4j.Logger instLog = org.apache.log4j.Logger.getLogger( ImageInstance.class.getName() );
        instLog.setLevel( org.apache.log4j.Level.DEBUG );
        junit.textui.TestRunner.run( suite() );
    }
    
    
    public static Test suite() {
        return new TestSuite( Test_ImageInstance.class );
    }
    
}



