// TestVolume.java

package imginfo;

import java.io.*;
import junit.framework.*;
import java.util.*;
import photovault.test.PhotovaultTestCase;

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
}
	
