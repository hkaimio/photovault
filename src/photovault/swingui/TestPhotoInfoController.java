// TestPhotoInfoController.java

package photovault.swingui;

import java.io.*;
import junit.framework.*;
import java.util.*;
import imginfo.*;

public class TestPhotoInfoController extends TestCase {

    PhotoInfo photo = null;
    PhotoInfoController ctrl = null;
    public void setUp() {
	photo = PhotoInfo.create();

	photo.setPhotographer( "TESTIKUVAAJA" );
	photo.setFStop( 5.6 );
	photo.updateDB();

	ctrl = new PhotoInfoController();
    }

    public void tearDown() {
	photo.delete();
    }

    public static Test suite() {
	return new TestSuite( TestFieldController.class );
    }

    public void testPhotoModification() {
	ctrl.setPhoto( photo );

	String oldValue = photo.getPhotographer();
	String newValue = "Test photographer 2";
	ctrl.setField( PhotoInfoController.PHOTOGRAPHER, newValue );
	assertEquals( "PhotoInfo should not be modified at this stage", oldValue, photo.getPhotographer() );
	assertEquals( "Ctrl should reflect the modification", newValue, ctrl.getField( PhotoInfoController.PHOTOGRAPHER ));

	ctrl.save();
	assertEquals( "After save photo should also reflect the modifications", newValue, photo.getPhotographer() );

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
	ctrl.setField( PhotoInfoController.PHOTOGRAPHER, newValue );

	ctrl.discard();
	assertEquals( "PhotoInfo should not be modified", oldValue, photo.getPhotographer() );
	assertEquals( "Ctrl should have the old value after discard", oldValue, ctrl.getField( PhotoInfoController.PHOTOGRAPHER ));
    }

    public void testNewPhotoCreation() {
	ctrl.createNewPhoto();
	String photographer = "Test photographer";
	ctrl.setField( PhotoInfoController.PHOTOGRAPHER, photographer );
	assertEquals( photographer, ctrl.getField( PhotoInfoController.PHOTOGRAPHER ) );

	// Saving the ctrl state should create a new photo object
	ctrl.save();
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
	
    }
}
