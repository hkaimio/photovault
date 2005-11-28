/*
 * Test_PhotovaultSettings.java
 *
 * Created on 18. marraskuuta 2005, 18:41
 *
 */

package photovault.common;

import dbhelper.ODMG;
import imginfo.PhotoInfo;
import imginfo.PhotoNotFoundException;
import imginfo.Thumbnail;
import imginfo.Volume;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
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
        assert( dbs.size() == 0 );
        
        PVDatabase db = new PVDatabase();
        db.setName( "testing" );
        db.setDbHost( "thishost" );
        settings.addDatabase( db );
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
        db.setDbName( "pc_testest");
        Volume vol = new Volume( "testvolume", dbDir.getAbsolutePath() );
        db.addVolume( vol );
        settings.addDatabase( db );
        settings.saveConfig();
        
        db.createDatabase( "harri", "" );
        
        // Verify that the database can be used by importing a file
        ODMG.initODMG( "harri", "", db );
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
    public static void main( String[] args ) {
	junit.textui.TestRunner.run( suite() );
    }
    
    public static Test suite() {
	return new TestSuite( Test_PhotovaultSettings.class );
    }    
    
}

    