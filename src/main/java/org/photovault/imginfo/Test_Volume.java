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

import java.io.*;
import junit.framework.*;
import java.util.*;
import org.photovault.test.PhotovaultTestCase;

public class Test_Volume extends PhotovaultTestCase {

    /**
       Sets ut the test environment
    */
    public void setUp() {
	String volumeRoot =  "c:\\temp\\photoVaultVolumeTest";
	volume = new Volume( "testVolume", volumeRoot );
    }
    private Volume volume;
    
    /**
       Tears down the testing environment
    */
    public void tearDown() {
	deleteTree( volume.getBaseDir() );
    }

    protected boolean deleteTree( File root ) {
	boolean success = true;
	if ( root.isDirectory() ) {
	    File entries[] = root.listFiles();
	    for ( int n = 0; n < entries.length; n++ ) {
		if ( !deleteTree( entries[n] ) ) {
		    success = false;
		}
	    }
	}
	if ( !root.delete() ) {
	    success = false;
	}
	return success;
    }

    public void testFnameCreation() {
	File f = null;

	try {
	    f = File.createTempFile( "volumeTest", ".jpg" );
	} catch (IOException e ) {
	    fail( "Temp file could not be created" );
	}
	
	Calendar cal = Calendar.getInstance();
	// The time will be 13 Dec 2002 (Java numbers months from 0 onwards!!!)
	cal.set( 2002, 11, 13 );
	f.setLastModified( cal.getTimeInMillis() );

	// Get a name for the file and check that it is what is expected
	File volFile = volume.getFilingFname( f );
	System.out.println( volFile.getPath() );
	assertEquals( "File name: ", "20021213_00001.jpg", volFile.getName() );
	File monthDir = volFile.getParentFile();
	assertEquals( "Month dir: ", "200212", monthDir.getName() );
	File yearDir = monthDir.getParentFile();
	assertEquals( "Year dir: ", "2002", yearDir.getName() );
	File rootDir = yearDir.getParentFile();
	assertEquals( "Volume dir: ", volume.getBaseDir().getName(), rootDir.getName() );
    }

    public void testManyFileCreation() {
	File f = null;

	try {
	    f = File.createTempFile( "volumeTest", ".jpg" );
	} catch (IOException e ) {
	    fail( "Temp file could not be created" );
	}
	
	Calendar cal = Calendar.getInstance();
	// The time will be 13 Dec 2002 (Java numbers months from 0 onwards!!!)
	cal.set( 2002, 11, 13 );
	f.setLastModified( cal.getTimeInMillis() );

	// Get a name for the file and check that it is what is expected
	File volFile = volume.getFilingFname( f );
	try {
	    volFile.createNewFile();
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}
	assertEquals( "File name: ", "20021213_00001.jpg", volFile.getName() );
	volFile = volume.getFilingFname( f );
	try {
	    volFile.createNewFile();
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}
	assertEquals( "File name: ", "20021213_00002.jpg", volFile.getName() );
	volFile = volume.getFilingFname( f );
	try {
	    volFile.createNewFile();
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}
	assertEquals( "File name: ", "20021213_00003.jpg", volFile.getName() );
    }
    
    public void testManyDateCreation() {
	File f = null;

	try {
	    f = File.createTempFile( "volumeTest", ".jpg" );
	} catch (IOException e ) {
	    fail( "Temp file could not be created" );
	}
	
	Calendar cal = Calendar.getInstance();
	// The time will be 13 Dec 2002 (Java numbers months from 0 onwards!!!)
	cal.set( 2002, 11, 13 );
	f.setLastModified( cal.getTimeInMillis() );

	// Get a name for the file and check that it is what is expected
	File volFile = volume.getFilingFname( f );
	assertEquals( "File name: ", "20021213_00001.jpg", volFile.getName() );
	try {
	    volFile.createNewFile();
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}

	// Set a new date
	cal.set( 2002, 11, 14 );
	f.setLastModified( cal.getTimeInMillis() );
	volFile = volume.getFilingFname( f );
	try {
	    volFile.createNewFile();
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}
	assertEquals( "File name: ", "20021214_00001.jpg", volFile.getName() );

	// Set the original date and check that the files are nombered correctly
	cal.set( 2002, 11, 13 );
	f.setLastModified( cal.getTimeInMillis() );
	volFile = volume.getFilingFname( f );
	try {
	    volFile.createNewFile();
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}
	assertEquals( "File name: ", "20021213_00002.jpg", volFile.getName() );
    }
    
    public void testMassCreation() {
	File f = null;

	try {
	    f = File.createTempFile( "volumeTest", ".jpg" );
	} catch (IOException e ) {
	    fail( "Temp file could not be created" );
	}
	
	Calendar cal = Calendar.getInstance();
	// Create many files
	for ( int n = 0; n < 200; n++ ) {
	
	    // The time will be 13 Dec 2002 (Java numbers months from 0 onwards!!!)
	    cal.set( 2002, 11, 13 );
	    f.setLastModified( cal.getTimeInMillis() );

	    // Get a name for the file and check that it is what is expected
	    File volFile = volume.getFilingFname( f );
	    try {
		volFile.createNewFile();
	    } catch ( IOException e ) {
		fail( e.getMessage() );
	    }

	    // Set a new date
	    cal.set( 2002, 11, 14 );
	    f.setLastModified( cal.getTimeInMillis() );
	    volFile = volume.getFilingFname( f );
	    try {
		volFile.createNewFile();
	    } catch ( IOException e ) {
		fail( e.getMessage() );
	    }
	    if ( n == 499 ) {
		assertEquals( "File name: ", "20021214_00500.jpg", volFile.getName() );
	    }

	}
    }

    public void testFnameMapping() {
	File f = null;

	try {
	    f = File.createTempFile( "volumeTest", ".jpg" );
	} catch (IOException e ) {
	    fail( "Temp file could not be created" );
	}
	
	Calendar cal = Calendar.getInstance();
	// The time will be 13 Dec 2002 (Java numbers months from 0 onwards!!!)
	cal.set( 2002, 11, 13 );
	f.setLastModified( cal.getTimeInMillis() );

	// Get a name for the file and check that it is what is expected
	File volFile = volume.getFilingFname( f );
	try {
	    volFile.createNewFile();
	} catch ( IOException e ) {
	    fail( e.getMessage() );
	}

	File mappedFile = null;
	try {
	    mappedFile = volume.mapFileName( volFile.getName() );
	} catch ( FileNotFoundException e ) {
	    fail( "Mapped file not found: " + e.getMessage() );
	}
	assertEquals( "Mapped file does not match", volFile, mappedFile );
    }

    /**
     Test the functionality of isFileInVolume method
     */
    public void testIsFileInVolume() {
        File basedir = volume.getBaseDir();
        try {
            
            // Positive tests
            assertTrue( volume.isFileInVolume( new File( basedir, "testpicture.jpg ") ) );
            assertTrue( volume.isFileInVolume( basedir ) );
            
            // Negative tests
            assertFalse( volume.isFileInVolume( basedir.getParentFile() ));
            assertFalse( volume.isFileInVolume( File.listRoots()[0] ) );
        } catch (IOException ex) {
            fail( "IOError: " + ex.getMessage() );
        }
    }
    
    /**
     Test basic use cases for method getVolumeOfFile
     */
    
    public void testGetVolumeOfFile() {
        try {
            File testVolPath = File.createTempFile( "pv_voltest", "" );
            VolumeBase extVolume = new ExternalVolume( "extvol", testVolPath.getAbsolutePath() );
            
            File test1 = new File( volume.getBaseDir(), "testfile" );
            assertEquals( test1.getAbsolutePath() + " belongs to volume", 
                    volume, VolumeBase.getVolumeOfFile( test1 ) ); 
            File test2 = new File( testVolPath, "testfile" );
            assertEquals( test2.getAbsolutePath() + " belongs to volume", 
                    extVolume, VolumeBase.getVolumeOfFile( test2 ) ); 
            File test3 = new File( testVolPath.getParentFile(), "testfile" );
            this.assertNull( test3.getAbsoluteFile() + " does not belong to volume",
                    VolumeBase.getVolumeOfFile( test3 ) );
            // Test that null argument does not cause error
            VolumeBase.getVolumeOfFile( null );
        } catch (IOException ex) {
            fail( "IOError: " + ex.getMessage() );
        }
    }
    
    /**
       Test a special case - creating a file name for a PhotoInfo in which the shooting date has not been set.
       Criteria is simply that a valid file name is created and that no Exception is thrown.
    */
    
    public void testNamingWithNoDate() {
	PhotoInfo photo = PhotoInfo.create();
	File f = volume.getInstanceName( photo, "jpg" );
    }
	
    public static Test suite() {

	return new TestSuite( Test_Volume.class );
    }
    
    
    public static void main( String[] args ) {
	//	org.apache.log4j.BasicConfigurator.configure();
	// log.setLevel( org.apache.log4j.Level.DEBUG );
	org.apache.log4j.Logger instLog = org.apache.log4j.Logger.getLogger( Volume.class.getName() );
	instLog.setLevel( org.apache.log4j.Level.DEBUG );
	junit.textui.TestRunner.run( suite() );
    }
    

}
	
