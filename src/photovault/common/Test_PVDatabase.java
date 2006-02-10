/*
 * Test_PVDatabase.java
 *
 * Created on 18. marraskuuta 2005, 21:00
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package photovault.common;

import junit.framework.*;
import java.util.List;
import java.io.StringWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.File;
import org.apache.commons.betwixt.io.BeanWriter;
import org.apache.commons.betwixt.io.BeanReader;

import org.photovault.imginfo.Volume;

/**
 *
 * @author harri
 */
public class Test_PVDatabase extends TestCase {
    
    /** Creates a new instance of Test_PVDatabase */
    public Test_PVDatabase() {
    }
    
    public void testVolumeAddition() {
        PVDatabase db = new PVDatabase();
        db.setDbHost( "" );
        db.setDbName( "test" );
        
        Volume v = new Volume( "test", "c:/temp" );
        db.addVolume( v );
        List volumes = db.getVolumes();
        assert( volumes.get( 0 ) == v );
        assert( volumes.size() == 1 );
    }

    public void testXMLOutput() {
        PVDatabase db = new PVDatabase();
        db.setDbHost( "" );
        db.setDbName( "test" );
        
        Volume v = new Volume( "test", "c:/temp" );
        db.addVolume( v );
        
        File tempFile = null;
        try {
            tempFile = File.createTempFile( "pv_settings_", ".xml" );
            tempFile.deleteOnExit();
        } catch ( Exception e ) {
            this.fail( e.getMessage() );
        }
        
        try {
            BufferedWriter outputWriter = new BufferedWriter( new FileWriter( tempFile ));
            outputWriter.write("<?xml version='1.0' ?>\n");
            BeanWriter beanWriter = new BeanWriter(outputWriter);
            beanWriter.getXMLIntrospector().setAttributesForPrimitives(true);
            beanWriter.setWriteIDs(false);
            beanWriter.enablePrettyPrint();
            beanWriter.write("database", db);
            beanWriter.close();
        } catch( Exception e ) {
            this.fail( e.getMessage() );
        }
       
        // Now try to read the info
        BeanReader beanReader = new BeanReader();
        beanReader.getXMLIntrospector().getConfiguration().setAttributesForPrimitives(true);
        beanReader.getBindingConfiguration().setMapIDs(false);
        try {
            beanReader.registerBeanClass( "database", PVDatabase.class );
            beanReader.registerBeanClass( "volume", Volume.class );
            PVDatabase readDB = (PVDatabase) beanReader.parse( tempFile );
            assertEquals( readDB.getDbName(), "test" );
            List readVolumes = readDB.getVolumes();
            assert( readVolumes.size() == 1 );
            Volume readVolume = (Volume) readVolumes.get(0);
            assertEquals( readVolume.getName(), "test" );
            assertEquals( readVolume.getBaseDir(), v.getBaseDir() );
        } catch ( Exception e ) {
            this.fail( e.getMessage() );
        }
    }

    public void testDatabaseCollection() {
        PhotovaultDatabases pvd = new PhotovaultDatabases();
        
        PVDatabase db1 = new PVDatabase();
        db1.setName( "test1" );
        db1.setDbName( "database1" );
        db1.setDbHost( "machine" );
        pvd.addDatabase( db1 );
        PVDatabase db2 = new PVDatabase();
        db2.setName( "test2" );
        db2.setDbName( "database2" );
        db2.setDbHost( "machine2" );
        pvd.addDatabase( db2 );
        
        File f = null;
        try {
            f = File.createTempFile( "photovault_settings_", ".xml" );
        } catch ( Exception e ) {
            fail( e.getMessage());
        }
        pvd.save( f );
        
        PhotovaultDatabases pvd2 = PhotovaultDatabases.loadDatabases( f );
        
    }
    
    public void testEmbeddedDatabaseCreation() {
        File dbDir = null;
        try {
            dbDir = File.createTempFile("pv_derby_instance", "");
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        dbDir.delete();
        PVDatabase pvd = new PVDatabase();
        pvd.setInstanceType( PVDatabase.TYPE_EMBEDDED );
        pvd.setEmbeddedDirectory( dbDir );
        pvd.createDatabase( "", "" );
    }
    
    public static void main( String[] args ) {
	junit.textui.TestRunner.run( suite() );
    }
    
    public static Test suite() {
	return new TestSuite( Test_PVDatabase.class );
    }    
}
