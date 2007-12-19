/*
  Copyright (c) 2006-2007 Harri Kaimio
  
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


package org.photovault.common;

import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoNotFoundException;
import org.photovault.imginfo.Thumbnail;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import org.hibernate.Session;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;

/**
 *
 * @author harri
 */
public class Test_PhotovaultSettings {
    
    /** Creates a new instance of Test_PhotovaultSettings */
    public Test_PhotovaultSettings() {
    }
    
    PhotovaultSettings oldSettings = null;
    Properties oldSysProperties = null;
    
    @BeforeMethod
    public void setUp() {
        oldSettings = PhotovaultSettings.settings;
        oldSysProperties = System.getProperties();
    }
    
    @AfterMethod
    public void tearDown() {
        PhotovaultSettings.settings = oldSettings;
        System.setProperties( oldSysProperties );
    }
    
    /**
     * Verify that settings file is loaded from home directory.
     */
    @Test
    public void testSettingsFileInHomeDir() {
        System.setProperty( "user.home", "testfiles/testHomeDir" );
        
        /* Unset the photovault.configfile property so that the config file is
         * loaded from specified home directory (this property is set by unit test
         * Ant target)
         */
        System.getProperties().remove( "photovault.configfile" );
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        PVDatabase db = settings.getDatabase( "testdb" );
        assertNotNull( db );
    }
    
    @Test
    public void testNoSettingsFile() {
        try {
            File f = File.createTempFile( "photovault_settings", ".xml" );
            f.delete();
            System.setProperty( "photovault.configfile", f.getAbsolutePath() );
        } catch ( Exception e ) {
            fail( e.getMessage() );
        }
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        Collection dbs = settings.getDatabases();
        assertTrue( dbs.size() == 0 );
        
        PVDatabase db = new PVDatabase();
        db.setName( "testing" );
        db.setHost( "thishost" );
        try {
            settings.addDatabase( db );
        } catch (PhotovaultException ex) {
            fail( "Exception while creating database: " + ex.getMessage() );
        }
        settings.saveConfig();
        
        PhotovaultSettings.settings = null;
        
        settings = PhotovaultSettings.getSettings();
        PVDatabase db2 = settings.getDatabase( "testing" );
        assertEquals( db2.getName(), db.getName() );
    }
    
    @Test
    public void testSpecialSettingsFile() {
        System.setProperty( "photovault.configfile", "testfiles/testconfig.xml");
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        PVDatabase db = settings.getDatabase( "paav_junit" );
        assertNotNull( db );
    }
    
    /**
     * Test creation of a new database
     */ 
    @Test
    public void testCreateDB() {
        File confFile = null;
        File dbDir = null;
        // Create an empty configuration file & volume directory
        try {
            confFile = File.createTempFile( "photovault_settings_", ".xml" );
            confFile.delete();
            dbDir = File.createTempFile( "photovault_test_volume", "" );
            dbDir.delete();
            dbDir.mkdir();
        } catch (IOException ex) {
            ex.printStackTrace();
            fail( ex.getMessage() );
        }
        
        System.setProperty( "photovault.configfile", confFile.getAbsolutePath() );
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        PVDatabase db = new PVDatabase();
        db.setName( "testing" );
        db.setInstanceType( PVDatabase.TYPE_EMBEDDED );
        db.setDataDirectory( dbDir );
        db.addMountPoint( "/tmp/testing" );
        try {
            settings.addDatabase( db );
        } catch (PhotovaultException ex) {
            fail( "Exception while creating database: " + ex.getMessage() );
        }
        settings.saveConfig();
        PhotovaultSettings.settings = null;
        settings = PhotovaultSettings.getSettings();
        settings.setConfiguration( "testing" );     
        db = settings.getCurrentDatabase();
        Set<File> mounts = db.getMountPoints();
        assertEquals( 1, mounts.size() );
        assertTrue( mounts.contains( new File( "/tmp/testing" ) ) );
        
        // try creating another database with the same name
        PVDatabase db2 = new PVDatabase();
        db2.setName( "testing" );
        db2.setInstanceType( PVDatabase.TYPE_EMBEDDED );
        db2.setDataDirectory( dbDir );
        boolean throwsException = false;
        try {
            settings.addDatabase( db2 );
        } catch (PhotovaultException ex) {
            throwsException = true;
        }
        assertTrue( "Adding another database with same name must throw exception",
                throwsException );
        
        db.createDatabase( "", "" );
        try {
            HibernateUtil.init( "", "", db );
        } catch (PhotovaultException ex) {
            fail( ex.getMessage() );
        }
        Session session = HibernateUtil.getSessionFactory().openSession();
        HibernateDAOFactory daoFactory = (HibernateDAOFactory) DAOFactory.instance( HibernateDAOFactory.class );
        daoFactory.setSession( session );
        PhotoInfoDAO photoDAO = daoFactory.getPhotoInfoDAO();
        File photoFile = new File( "testfiles/test1.jpg" );
        PhotoInfo photo = null;
        try {
            photo = PhotoInfo.addToDB(photoFile);
            photo = photoDAO.makePersistent( photo );
        } catch (PhotoNotFoundException ex) {
            ex.printStackTrace();
            fail( ex.getMessage() );
        }
        photo.setPhotographer( "test" );
        PhotoInfo photo2 = photoDAO.findById( photo.getUid(), false );
        Thumbnail thumb = photo2.getThumbnail();
        assertFalse( "Default thumbnail returned", thumb == Thumbnail.getDefaultThumbnail() );
    }
    
    @Test
    public void testProperties() {
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        settings.setProperty( "test", "testing" );
        settings.setProperty( "test2", "testing2" );
        assertEquals( "testing", settings.getProperty( "test" ) );
        assertEquals( "testing2", settings.getProperty( "test2" ) );
        assertNull( settings.getProperty( "test3" ) );
        assertEquals( "testing3", settings.getProperty( "test3", "testing3" ) );
        settings.saveConfig();
        PhotovaultSettings.settings = null;
        settings = PhotovaultSettings.getSettings();
        assertEquals( "testing", settings.getProperty( "test" ) );
        assertEquals( "testing2", settings.getProperty( "test2" ) );
        assertNull( settings.getProperty( "test3" ) );
        assertEquals( "testing3", settings.getProperty( "test3", "testing3" ) );        
    }
        
}

    