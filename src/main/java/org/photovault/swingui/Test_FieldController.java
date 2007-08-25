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
 
package org.photovault.swingui;

import com.sun.jdori.common.query.tree.ThisExpr;
import java.io.*;
import junit.framework.*;
import java.util.*;
import org.photovault.command.CommandException;
import org.photovault.command.DataAccessCommand;

public class Test_FieldController extends TestCase {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Test_FieldController.class.getName() );

    static public class TestObject {
	public String field;
	public boolean isMultivalued;
        public Object[] values = null;
	public TestObject() {
	    field = new String();
	}
	public void setField( String value ) {
	    field = value;
	}
	public String getField() {
	    return field;
	}

	public void setFieldMultivalued( boolean mv ) {
	    isMultivalued = mv;
            // this.values = values;
	}
    }
    
    static public class TestCommand extends DataAccessCommand {
        public void execute() throws CommandException {
        }
        
        public void setField( Object value ) {
            this.field = value;
        }
        
        Object field = null;
    }

    private TestObject testObject = null;
    private FieldController<TestObject,TestCommand> fieldCtrl = null;
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
	
	fieldCtrl = new  FieldController<TestObject, TestCommand> ( "field", testObject );
	fieldCtrl.setViews( views );
    }

    public void testFieldSetting() {
	assertFalse( fieldCtrl.isModified() );
	fieldCtrl.setValue( "Moi" );
	assertTrue( "FieldCtrl does calins it has not been modified", fieldCtrl.isModified() );
	assertEquals( "Modification not OK", "Moi", fieldCtrl.getValue() );
	assertEquals( "Modification in controller not yet saved", new String(), testObject.getField() );
	assertEquals( "Movifications not carried to views", "Moi", view1.getField() );
	
        TestCommand cmd = new TestCommand();
	fieldCtrl.save( cmd );

	assertEquals( "Modification not saved correctly", "Moi", cmd.field );
	assertFalse( "isModified should be false after save", fieldCtrl.isModified() );
    }

    public void testSaveWithoutModification() {

	// Set the test object field to some new value so that we detect attempt to save	
	testObject.setField( "New value" );
        TestCommand cmd = new TestCommand();
	fieldCtrl.save( cmd );
        assertNull( cmd.field );
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
    
    // TODO: how should this behave after migrating to commands usage???
    public void testNullModel() {
	fieldCtrl.setModel( null );
	fieldCtrl.setValue( "Moi" );
	assertTrue( "FieldCtrl does calins it has not been modified", fieldCtrl.isModified() );
	assertEquals( "Modification not OK", "Moi", fieldCtrl.getValue() );

	// saving the ctrl should not crash
        TestCommand cmd = new TestCommand();
	fieldCtrl.save( cmd );        
	
	// Set the new model but preserve field controller state. After this the model can be saved
	fieldCtrl.setModel( testObject, true );
        cmd = new TestCommand();
	fieldCtrl.save( cmd );
        assertEquals( "Modification not saved correctly", "Moi", cmd.field );
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
	List<TestObject> model = new ArrayList<TestObject>();
	model.add( testObject );
	TestObject model2 = new TestObject();
	model.add(  model2 );

	testObject.setField( "Value" );
	model2.setField( "Value" );

	fieldCtrl.setModel( model, false );
	assertEquals( "Field controller not updated when values are equal", "Value", fieldCtrl.getValue() );

	fieldCtrl.setValue( "Value2" );
        TestCommand cmd = new TestCommand();
	fieldCtrl.save( cmd );
        assertEquals( "model not changed", "Value2", cmd.field );

	// Check that if model values differ they are not copied to controller
	model2.setField( "Value3" );
	fieldCtrl.setModel( model, false );
	assertEquals( "model value should be null if there are several alterantives", null, fieldCtrl.getValue() );
        cmd = new TestCommand();
	fieldCtrl.save( cmd );
        assertNull( "model changed", cmd.field );
	// assertEquals( "model changed", "Value3", model2.getField() );
	
	fieldCtrl.setValue( "Value4" );
        cmd = new TestCommand();
	fieldCtrl.save( cmd );
	assertEquals( "model not changed", "Value4", cmd.field );

	// Check that a null value does not cause problems
	testObject.setField( null );
	fieldCtrl.setModel( model, false );
	assertEquals( "model value should be null if there are several alterantives", null, fieldCtrl.getValue() );
        assertTrue( view1.isMultivalued );
        boolean nullFound = false;
//        for ( Object o : view1.values ) {
//            if ( o == null ) {
//                nullFound = true;
//                break;
//            }
//        }
//        assertTrue( nullFound );
        
        view1.isMultivalued = false;
	testObject.setField( "Value5" );
	model2.setField( null );
	fieldCtrl.setModel( model, false );
	assertEquals( "model value should be null if there are several alternatives", null, fieldCtrl.getValue() );
        assertTrue( view1.isMultivalued );
//        nullFound = false;
//        for ( Object o : view1.values ) {
//            if ( o != null && o.equals( "Value5" ) ) {
//                nullFound = true;
//                break;
//            }
//        }
//        assertTrue( nullFound );
	
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

	
