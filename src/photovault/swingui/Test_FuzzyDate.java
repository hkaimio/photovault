// Test_PhotoInfoController.java

package photovault.swingui;

import java.io.*;
import junit.framework.*;
import java.util.*;
import imginfo.*;

public class Test_FuzzyDate extends TestCase {
    
    public void setUp() {

    }

    public void tearDown() {

    }


    /**
       Verify that FuzzyDate behaves well if date is null
    */
    public void testNullDate() {
	FuzzyDate fd = new FuzzyDate( null, 0 );
	Date maxDate = fd.getMaxDate();
	assertNull( "maxDate", maxDate );
	Date minDate = fd.getMinDate();
	assertNull( "minDate", minDate );
	String asStr = fd.format();
	assertEquals( asStr, "" );
    }

    public void testEquals() {
	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 23 );
	
	FuzzyDate fd1 = new FuzzyDate( cal.getTime(), 0 );
	FuzzyDate fd2 = new FuzzyDate( cal.getTime(), 0 );
	FuzzyDate fd3 = new FuzzyDate( cal.getTime(), 100 );
	cal.set( 2002, 11, 24 );
	FuzzyDate fd4 = new FuzzyDate( cal.getTime(), 0 );
	FuzzyDate fd5 = new FuzzyDate( null, 0 );
	FuzzyDate fd6 = new FuzzyDate( null, 0 );
	FuzzyDate fd7 = new FuzzyDate( null, 100 );

	assertEquals( fd1, fd2 );
	assertFalse( fd1.equals( fd3 ) );
	assertFalse( fd1.equals( fd4 ) );
	assertFalse( fd1.equals( null ) );
	assertEquals( fd5, fd6 );
	assertFalse( fd5.equals( fd7 ) );
	assertFalse( fd4.equals( fd5 ) );
	assertFalse( fd5.equals( fd4 ) );
    }

}