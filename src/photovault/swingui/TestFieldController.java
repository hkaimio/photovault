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
    public void setUp() {
	testObject = new TestObject();
	fieldCtrl = new  FieldController ( testObject ) {
		protected void setModelValue() {
		    TestObject obj = (TestObject) model;
		    obj.setField( (String) value );
		}
		protected Object getModelValue() {
		    TestObject obj = (TestObject) model;
		    return obj.getField();
		}
	    };
    }

    public void testFieldSetting() {
	assertFalse( fieldCtrl.isModified() );
	fieldCtrl.setValue( "Moi" );
	assertTrue( "FieldCtrl does calins it has not been modified", fieldCtrl.isModified() );
	assertEquals( "Modification not OK", "Moi", fieldCtrl.getValue() );
	assertEquals( "Modification in controller not yet saved", new String(), testObject.getField() );

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


    public static Test suite() {
	return new TestSuite( TestFieldController.class );
    }
    
}

	
