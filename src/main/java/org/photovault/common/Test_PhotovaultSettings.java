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

import org.photovault.dbhelper.ODMG;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoNotFoundException;
import org.photovault.imginfo.Thumbnail;
import org.photovault.imginfo.Volume;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import junit.framework.*;
/**
 *
 * @author harri
 */
public class Test_PhotovaultSettings extends TestCase {
    
    /** Creates a new instance of Test_PhotovaultSettings */
    public Test_PhotovaultSettings() {
    }
    
    PhotovaultSettings oldSettings = null;
    Properties oldSysProperties = null;
    public void setUp() {
        oldSettings = PhotovaultSettings.settings;
        oldSysProperties = System.getProperties();
    }
    
    public void tearDown() {
        PhotovaultSettings.settings = oldSettings;
        System.setProperties( oldSysProperties );
    }
    
    /**
     * Verify that settings file is loaded from home directory.
     */
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
    
    public void testSpecialSettingsFile() {
        System.setProperty( "photovault.configfile", "testfiles/testconfig.xml");
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        PVDatabase db = settings.getDatabase( "paav_junit" );
        assertNotNull( db );
    }
    
    /**
     * Test creation of a new database
     */ 
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
        db.setEmbeddedDirectory( dbDir );
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
        
        // try creating another database with the same name
        PVDatabase db2 = new PVDatabase();
        db2.setName( "testing" );
        db2.setInstanceType( PVDatabase.TYPE_EMBEDDED );
        db2.setEmbeddedDirectory( dbDir );
        boolean throwsException = false;
        try {
            settings.addDatabase( db2 );
        } catch (PhotovaultException ex) {
            throwsException = true;
        }
        assertTrue( "Adding another database with same name must throw exception",
                throwsException );
        
        db.createDatabase( "harri", "" );
        try {
            
            // Verify that the database can be used by importing a file
            ODMG.initODMG( "harri", "", db );
        } catch (PhotovaultException ex) {
            fail( ex.getMessage() );
        }
        File photoFile = new File( "testfiles/test1.jpg" );
        PhotoInfo photo = null;
        try {
            photo = PhotoInfo.addToDB(photoFile);
        } catch (PhotoNotFoundException ex) {
            ex.printStackTrace();
            fail( ex.getMessage() );
        }
        photo.setPhotographer( "test" );
        try {
            
            PhotoInfo photo2 = PhotoInfo.retrievePhotoInfo( photo.getUid() );
            Thumbnail thumb = photo2.getThumbnail();
            this.assertFalse( "Default thumbnail returned", thumb == Thumbnail.getDefaultThumbnail() );
        } catch (PhotoNotFoundException ex) {
            fail( "Photo not found in database" );
        }
    }
    
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
    
    public static void main( String[] args ) {
	junit.textui.TestRunner.run( suite() );
    }
    
    public static Test suite() {
	return new TestSuite( Test_PhotovaultSettings.class );
    }    
    
}

    