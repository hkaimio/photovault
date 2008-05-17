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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;
import org.photovault.common.PVDatabase;
import org.photovault.common.PhotovaultException;
import org.photovault.common.PhotovaultSettings;
import org.photovault.test.PhotovaultTestCase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author harri
 */
public class Test_ExternalVolume extends PhotovaultTestCase {
    
    
    /** Creates a new instance of Test_ExternalVolume */
    public Test_ExternalVolume() {
    }
    
    /**
       Sets ut the test environment
    */
    @BeforeMethod
    @Override
    public void setUp() {
        try {
            volumeRoot = File.createTempFile( "photovaultVolumeTest", "" );
            volumeRoot.delete();
            volumeRoot.mkdir();
            extvolRoot = File.createTempFile( "photovaultVolumeTestExt", "" );
            extvolRoot.delete();
            extvolRoot.mkdir();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        try {
            volume = new Volume();
            volume.setName( "imageInstanceTest" );
            VolumeManager.instance().initVolume( volume, volumeRoot );
            extVol = new ExternalVolume();
            extVol.setName( "extVolume" );
            VolumeManager.instance().initVolume( extVol, extvolRoot );
        } catch ( Exception e ) {
            fail( "Exception while creating volumes" );
        }
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        PVDatabase curDb = settings.getCurrentDatabase();
        try {
            curDb.addVolume( extVol );
        } catch (PhotovaultException ex) {
            ex.printStackTrace();
        }
    }
    
    private Volume volume;
    private ExternalVolume extVol;
    File extvolRoot = null;
    File volumeRoot = null;
    
    /**
       Tears down the testing environment
    */
    @AfterMethod
    @Override
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
    
    protected void copyFile( File src, File dst ) throws FileNotFoundException, IOException {
        // Copy the file to the archive
        FileInputStream in = new FileInputStream( src );
        FileOutputStream out = new FileOutputStream( dst );
        byte buf[] = new byte[1024];
        int nRead = 0;
        int offset = 0;
        while ( (nRead = in.read( buf )) > 0 ) {
            out.write( buf, 0, nRead );
            offset += nRead;
        }
        out.close();
        in.close();
    }
    
    String testImgDir = "testfiles";

    /* Test cases */
    
    /**
     Test ta new he basic use case: Add a new photo 
     */
    @Test
    public void testAddPhotoInExtVol() throws IOException {
        File srcFile = new File( testImgDir, "test1.jpg" );
        File dstFile = new File( extvolRoot, "test1.jpg" );
        try {
            copyFile( srcFile, dstFile );
        } catch( Exception e ){
            fail( e.getMessage() );
        }
        PhotoInfo p = null;
        try {
            p = PhotoInfo.addToDB(dstFile);
        } catch (PhotoNotFoundException ex) {
            fail( ex.getMessage() );
        }

        // Check that the original instance is created in external volume
        OriginalImageDescriptor orig = p.getOriginal();
        Set<FileLocation> locations = orig.getFile().getLocations();
        boolean foundOriginal = false;
        for ( FileLocation loc : locations ) {
            if( loc.getVolume() == extVol ) {
                foundOriginal = true;
                assertEquals( loc.getFile().getCanonicalFile(), dstFile.getCanonicalFile() );
            }
        }
        assertTrue( "No original was found", foundOriginal );
    
    }
   
}
