// TestFieldController.java

package photovault.swingui;

import java.io.*;
import junit.framework.*;
import java.util.*;

public class Test_FieldController extends TestCase {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Test_FieldController.class.getName() );

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
		protected void setModelValue( Object modelObject ) {
		    TestObject obj = (TestObject) modelObject;
		    obj.setField( (String) value );
		}
		protected Object getModelValue( Object modelObject ) {
		    TestObject obj = (TestObject) modelObject;
		    return obj.getField();
		}
		protected void updateView( Object view ) {
		    TestObject obj = (TestObject) view;
		    obj.setField( (String) value );
		}
		protected void updateValue( Object view ) {
		    TestObject obj = (TestObject) view;
		    value = obj.getField();
		    
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
	fieldCtrl.viewChanged( view1, "View2" );
	assertEquals( "View2 not changed by updating view1", "View2", view2.getField() );
	assertEquals( "View1 should not be changed", "View1", view1.getField() );
    }

    /**
       Test that when a new view is added it is updated to match the controller state
    */
    public void testViewsModification() {
	fieldCtrl.setValue( "value1" );
	TestObject view3 = new TestObject();
	views.add( view3 );
	fieldCtrl.updateAllViews();
	assertEquals( "view3 not updated to match model", "value1", view3.getField() );
    }
    
    public void testNullModel() {
	fieldCtrl.setModel( null );
	fieldCtrl.setValue( "Moi" );
	assertTrue( "FieldCtrl does calins it has not been modified", fieldCtrl.isModified() );
	assertEquals( "Modification not OK", "Moi", fieldCtrl.getValue() );

	// saving the ctrl should not crash
	fieldCtrl.save();
	
	// Set the new model but preserve field controller state. After this the model can be saved
	fieldCtrl.setModel( testObject, true );
	fieldCtrl.save();
	assertEquals( "Modification not saved correctly", "Moi", testObject.getField() );
	assertFalse( "isModified should be false after save", fieldCtrl.isModified() );
    }

    public void testModelChange() {
	fieldCtrl.setModel( testObject );
	
	// set the testObject to a new value
	TestObject model2 = new TestObject();
	
	model2.setField( "Modified" );
	fieldCtrl.setModel( model2 );

	assertEquals( "Views should be modified", "Modified", view1.getField() );
    }

    public void testMultiModel() {
	Object[] model = new Object[2];
	model[0] = testObject;
	TestObject model2 = new TestObject();
	model[1] = model2;

	testObject.setField( "Value" );
	model2.setField( "Value" );

	fieldCtrl.setModel( model, false );
	assertEquals( "Field controller not updated when values are equal", "Value", fieldCtrl.getValue() );

	fieldCtrl.setValue( "Value2" );
	fieldCtrl.save();
	assertEquals( "model not changed", "Value2", testObject.getField() );
	assertEquals( "model not changed", "Value2", model2.getField() );

	// Check that if model values differ they are not copied to controller
	model2.setField( "Value3" );
	fieldCtrl.setModel( model, false );
	assertEquals( "model value should be null if there are several alterantives", null, fieldCtrl.getValue() );
	fieldCtrl.save();
	assertEquals( "model changed", "Value2", testObject.getField() );
	assertEquals( "model changed", "Value3", model2.getField() );
	
	fieldCtrl.setValue( "Value4" );
	fieldCtrl.save();
	assertEquals( "model not changed", "Value4", testObject.getField() );
	assertEquals( "model not changed", "Value4", model2.getField() );

	// Check that a null value does not cause problems
	testObject.setField( null );
	fieldCtrl.setModel( model, false );
	assertEquals( "model value should be null if there are several alterantives", null, fieldCtrl.getValue() );

	testObject.setField( "Value5" );
	model2.setField( null );
	fieldCtrl.setModel( model, false );
	assertEquals( "model value should be null if there are several alterantives", null, fieldCtrl.getValue() );
	
    }
    
    public static void main( String[] args ) {
	//	org.apache.log4j.BasicConfigurator.configure();
	log.setLevel( org.apache.log4j.Level.DEBUG );
	org.apache.log4j.Logger fieldCtrlLog = org.apache.log4j.Logger.getLogger( FieldController.class.getName() );
	fieldCtrlLog.setLevel( org.apache.log4j.Level.DEBUG );
	junit.textui.TestRunner.run( suite() );
    }
    
    public static Test suite() {
	return new TestSuite( Test_FieldController.class );
    }
    
}

	