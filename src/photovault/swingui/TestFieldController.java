// TestFieldController.java

package photovault.swingui;

import java.io.*;
import junit.framework.*;
import java.util.*;

public class TestFieldController extends TestCase {

    private class TestObject {
	public String field;
	public TestObject() {
	    field = new String();
	}
	public void setField( String value ) {
	    field = value;
	}
	public String getField() {
	    return field;
	}
    }

    private TestObject testObject = null;
    private FieldController fieldCtrl = null;
    private Collection views = null;
    private TestObject view1 = null;
    private TestObject view2 = null;
    
    public void setUp() {
	testObject = new TestObject();
	views = new HashSet();
	view1 = new TestObject();
	views.add( view1 );
	view2 = new TestObject();
	views.add( view2 );
	
	fieldCtrl = new  FieldController ( testObject ) {
		protected void setModelValue() {
		    TestObject obj = (TestObject) model;
		    obj.setField( (String) value );
		}
		protected Object getModelValue() {
		    TestObject obj = (TestObject) model;
		    return obj.getField();
		}
		protected void updateView( Object view ) {
		    TestObject obj = (TestObject) view;
		    obj.setField( (String) value );
		}
	    };
	fieldCtrl.setViews( views );
    }

    public void testFieldSetting() {
	assertFalse( fieldCtrl.isModified() );
	fieldCtrl.setValue( "Moi" );
	assertTrue( "FieldCtrl does calins it has not been modified", fieldCtrl.isModified() );
	assertEquals( "Modification not OK", "Moi", fieldCtrl.getValue() );
	assertEquals( "Modification in controller not yet saved", new String(), testObject.getField() );
	assertEquals( "Movifications not carried to views", "Moi", view1.getField() );
	
	fieldCtrl.save();

	assertEquals( "Modification not saved correctly", "Moi", testObject.getField() );
	assertFalse( "isModified should be false after save", fieldCtrl.isModified() );
    }

    public void testSaveWithoutModification() {

	// Set the test object field to some new value so that we detect attempt to save	
	testObject.setField( "New value" );
	fieldCtrl.save();
	assertEquals( "Saving wihtout modification should not change model object!!!", "New value", testObject.getField() );

    }

    public void testFieldModification() {
	view1.setField( "View1" );
	// Generate a bbogus event to demonstrate that this should propagate to view2 but not to view1
	fieldCtrl.setValue( view1, "View2" );
	assertEquals( "View2 not changed by updating view1", "View2", view2.getField() );
	assertEquals( "View1 should not be changed", "View1", view1.getField() );
    }

    public static Test suite() {
	return new TestSuite( TestFieldController.class );
    }
    
}

	
