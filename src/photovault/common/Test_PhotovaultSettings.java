/*
 * Test_PhotovaultSettings.java
 *
 * Created on 18. marraskuuta 2005, 18:41
 *
 */

package photovault.common;

import java.io.File;
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
    
    
    public static void main( String[] args ) {
	junit.textui.TestRunner.run( suite() );
    }
    
    public static Test suite() {
	return new TestSuite( Test_PhotovaultSettings.class );
    }    
    
}

    