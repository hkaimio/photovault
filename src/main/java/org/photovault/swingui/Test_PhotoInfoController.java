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

package org.photovault.swingui;

import java.io.*;
import junit.framework.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.imginfo.*;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoNotFoundException;
import org.photovault.swingui.selection.PhotoSelectionController;
import org.photovault.test.PhotovaultTestCase;

public class Test_PhotoInfoController extends PhotovaultTestCase {
    
    static Log log = LogFactory.getLog( Test_PhotoInfoController.class.getName() );

    PhotoInfo photo = null;
    PhotoSelectionController ctrl = null;
    String testImgDir = "testfiles";
  
    public void setUp() {
	photo = PhotoInfo.create();

	photo.setPhotographer( "TESTIKUVAAJA" );
	photo.setFStop( 5.6 );

	ctrl = new PhotoSelectionController( null );
    }

    public void tearDown() {
	photo.delete();
    }
    
    
    public static Test suite() {
	return new TestSuite( Test_PhotoInfoController.class );
    }
    
    public static void main( String[] args ) {
	junit.textui.TestRunner.run( suite() );
    }


    public void testPhotoModification() {
	ctrl.setPhoto( photo );

	String oldValue = photo.getPhotographer();
	String newValue = "Test photographer 2";
        
	ctrl.viewChanged( null, PhotoInfoFields.PHOTOGRAPHER, newValue );
	assertEquals( "PhotoInfo should not be modified at this stage", oldValue, photo.getPhotographer() );
	assertEquals( "Ctrl should reflect the modification", newValue, ctrl.getFieldValues( PhotoInfoFields.PHOTOGRAPHER ));

//	try {
//	    ctrl.save();
//	} catch ( Exception e ) {
//	    fail( "Exception while saving: " + e.getMessage() );
//	}
//	assertEquals( "After save photo should also reflect the modifications", newValue, photo.getPhotographer() );

	// Check that the value is also stored in DB
	try {
	    PhotoInfo photo2 = PhotoInfo.retrievePhotoInfo( photo.getUid() );
	    
	    assertEquals( photo2.getPhotographer(), photo.getPhotographer() );
	    assertTrue( photo2.getFStop() == photo.getFStop() );
	} catch ( PhotoNotFoundException e ) {
	    fail ( "inserted photo not found" );
	}	
    }

    public void testChangeDiscarding() {
	ctrl.setPhoto( photo );

	String oldValue = photo.getPhotographer();
	String newValue = "Test photographer 2";
	ctrl.viewChanged( null, PhotoInfoFields.PHOTOGRAPHER, newValue );

//	ctrl.discard();
	assertEquals( "PhotoInfo should not be modified", oldValue, photo.getPhotographer() );
	assertEquals( "Ctrl should have the old value after discard", oldValue, ctrl.getFieldValues( PhotoInfoFields.PHOTOGRAPHER ));
    }

    public void testNewPhotoCreation() {
      File testFile = new File( testImgDir, "test1.jpg" );
	
	ctrl.createNewPhoto( testFile );
	String photographer = "Test photographer";
	ctrl.viewChanged( null, PhotoInfoFields.PHOTOGRAPHER, photographer );
	assertEquals( photographer, ctrl.getFieldValues( PhotoInfoFields.PHOTOGRAPHER ) );

	// Saving the ctrl state should create a new photo object
//	try {
//	    ctrl.save();
//	} catch ( Exception e ) {
//	    e.printStackTrace();
//	    fail( "Exception while saving: " + e.getMessage() );
//	}
	PhotoInfo photo = ctrl.getPhoto();
	assertTrue( "getPhoto should return PhotoInfo object after save()", photo != null );
	assertEquals( "PhotoInfo fields should match ctrl",
		      photographer, photo.getPhotographer() );
	

	// Check the database also
	try {
	    PhotoInfo photo2 = PhotoInfo.retrievePhotoInfo( photo.getUid() );
	    
	    assertEquals( photo2.getPhotographer(), photo.getPhotographer() );
	    assertTrue( photo2.getFStop() == photo.getFStop() );
	} catch ( PhotoNotFoundException e ) {
	    fail ( "inserted photo not found" );
	}

	// Test modification to saved
	String newPhotographer = "New photographer";
	ctrl.viewChanged( null, PhotoInfoFields.PHOTOGRAPHER, newPhotographer );
//	try {
//	    ctrl.save();
//	} catch ( Exception e ) {
//	    fail( "Exception while saving: " + e.getMessage() );
//	}
	assertEquals( "PhotoInfo fields should match ctrl",
		      newPhotographer, photo.getPhotographer() );
	try {
	    PhotoInfo photo2 = PhotoInfo.retrievePhotoInfo( photo.getUid() );
	    
	    assertEquals( photo2.getPhotographer(), photo.getPhotographer() );
	    assertTrue( photo2.getFStop() == photo.getFStop() );
	} catch ( PhotoNotFoundException e ) {
	    fail ( "inserted photo not found" );
	}
	
	
	photo.delete();
	
    }


    /**
       Tests creation of PhotoInfo record without giving image file
    */
    public void testOnlyRecordCreation() {
	String photographer = "Test photographer";
	ctrl.viewChanged( null, PhotoInfoFields.PHOTOGRAPHER, photographer );
	assertEquals( photographer, ctrl.getFieldValues( PhotoInfoFields.PHOTOGRAPHER ) );

	// Saving the ctrl state should create a new photo object
//	try {
//	    ctrl.save();
//	} catch ( Exception e ) {
//	    fail( "Exception while saving: " + e.getMessage() );
//	}
	PhotoInfo photo = ctrl.getPhoto();
	assertTrue( "getPhoto should return PhotoInfo object after save()", photo != null );
	assertEquals( "PhotoInfo fields should match ctrl",
		      photographer, photo.getPhotographer() );
	

	// Check the database also
	try {
	    PhotoInfo photo2 = PhotoInfo.retrievePhotoInfo( photo.getUid() );
	    
	    assertEquals( photo2.getPhotographer(), photo.getPhotographer() );
	    assertTrue( photo2.getFStop() == photo.getFStop() );
	} catch ( PhotoNotFoundException e ) {
	    fail ( "inserted photo not found" );
	}
	photo.delete();
	
    }
	

}
