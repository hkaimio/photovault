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

import junit.framework.*;
import java.util.List;
import java.io.StringWriter;
import java.io.IOException;
import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.File;
import org.photovault.imginfo.ExternalVolume;

import org.photovault.imginfo.Volume;
import org.photovault.imginfo.VolumeBase;

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
        db.setHost( "" );
        db.setDbName( "test" );
        
        Volume v = new Volume( "test", "c:/temp" );
        try {
            db.addVolume( v );
        } catch (PhotovaultException ex) {
            fail( ex.getMessage() );
        }
        List volumes = db.getVolumes();
        assertTrue( volumes.get( 0 ) == v );
        assertTrue( volumes.size() == 1 );
    }

    public void testDatabaseCollection() {
        PhotovaultDatabases pvd = new PhotovaultDatabases();
        
        PVDatabase db1 = new PVDatabase();
        db1.setName( "test1" );
        db1.setDbName( "database1" );
        db1.setHost( "machine" );
        try {
            pvd.addDatabase( db1 );
        } catch (PhotovaultException ex) {
            fail( "Exception while registering database: " + ex.getMessage() );
        }
        PVDatabase db2 = new PVDatabase();
        db2.setName( "test2" );
        db2.setDbName( "database2" );
        db2.setHost( "machine2" );
        try {
            pvd.addDatabase( db2 );
        } catch (PhotovaultException ex) {
            fail( "Exception while registering database: " + ex.getMessage() );
        }
        
        File f = null;
        try {
            f = File.createTempFile( "photovault_settings_", ".xml" );
        } catch ( Exception e ) {
            fail( e.getMessage());
        }
        
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
        pvd.setDataDirectory( dbDir );
        pvd.createDatabase( "", "" );
    }
    
    public static void main( String[] args ) {
	junit.textui.TestRunner.run( suite() );
    }
    
    public static Test suite() {
	return new TestSuite( Test_PVDatabase.class );
    }    
}
