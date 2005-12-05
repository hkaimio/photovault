// Test_PhotoInfoController.java

package imginfo;

import java.io.*;
import junit.framework.*;
import java.util.*;

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
    
    private void checkParsingBoundaries( String fuzzyDateStr, Date min, Date max ) {
        FuzzyDate fd = FuzzyDate.parse( fuzzyDateStr );
        long min1 = min.getTime();
        long min2 = fd.getMinDate().getTime();
        if ( Math.abs( min1-min2 ) > 10000 ) {
            fail( "Min date " + fd.getMinDate() + ", expected " + min + ", difference " + (min1-min2) );
        }
        long max1 = max.getTime();
        long max2 = fd.getMaxDate().getTime();
        if ( Math.abs( max1-max2 ) > 10000 ) {
            fail( "Max date " + fd.getMaxDate() + ", expected " + max + ", difference " + (max1-max2) );
        }
    }
    
    static Calendar cal = Calendar.getInstance();
    
    private Date createDate( int year, int month, int day, int hour, int minute, int second ) {
        cal.set( year, month, day, hour, minute, second );
        return cal.getTime();
    }
    
    /**
     Test tar date ranges are parsed correctly
     */
    public void testParseDateRange() {
	Calendar cal = Calendar.getInstance();        
        checkParsingBoundaries( "2.3.2005 - 13.4.2005", createDate( 2005, 2, 2, 0, 0, 0), createDate( 2005, 3, 13, 23, 59, 59 ) );
    }

    public void testParseWeek() {
	Calendar cal = Calendar.getInstance();        
        checkParsingBoundaries( "wk 44 2005", createDate( 2005, 9, 31, 0, 0, 0), createDate( 2005, 10, 6, 23, 59, 59 ) );
    }

    public void testParseMonth() {
	Calendar cal = Calendar.getInstance();        
        checkParsingBoundaries( "tammikuu 2005", createDate( 2005, 0, 1, 0, 0, 0), createDate( 2005, 1, 1, 0, 0, 0 ) );
        checkParsingBoundaries( "helmikuu 2005", createDate( 2005, 1, 1, 0, 0, 0), createDate( 2005, 2, 1, 0, 0, 0 ) );
        checkParsingBoundaries( "maaliskuu 2005", createDate( 2005, 2, 1, 0, 0, 0), createDate( 2005, 3, 1, 0, 0, 0 ) );
        checkParsingBoundaries( "huhtikuu 2005", createDate( 2005, 3, 1, 0, 0, 0), createDate( 2005, 4, 1, 0, 0, 0 ) );
        checkParsingBoundaries( "toukokuu 2005", createDate( 2005, 4, 1, 0, 0, 0), createDate( 2005, 5, 1, 0, 0, 0 ) );
        checkParsingBoundaries( "kesäkuu 2005", createDate( 2005, 5, 1, 0, 0, 0), createDate( 2005, 6, 1, 0, 0, 0 ) );
        checkParsingBoundaries( "heinäkuu 2005", createDate( 2005, 6, 1, 0, 0, 0), createDate( 2005, 7, 1, 0, 0, 0 ) );
        checkParsingBoundaries( "elokuu 2005", createDate( 2005, 7, 1, 0, 0, 0), createDate( 2005, 8, 1, 0, 0, 0 ) );
        checkParsingBoundaries( "syyskuu 2005", createDate( 2005, 8, 1, 0, 0, 0), createDate( 2005, 9, 1, 0, 0, 0 ) );
        checkParsingBoundaries( "lokakuu 2005", createDate( 2005, 9, 1, 0, 0, 0), createDate( 2005, 10, 1, 0, 0, 0 ) );
        checkParsingBoundaries( "marraskuu 2005", createDate( 2005, 10, 1, 0, 0, 0), createDate( 2005, 11, 1, 0, 0, 0 ) );
        checkParsingBoundaries( "joulukuu 2005", createDate( 2005, 11, 1, 0, 0, 0), createDate( 2006, 0, 1, 0, 0, 0 ) );
    }

    public void testParseYear() {
	Calendar cal = Calendar.getInstance();        
        checkParsingBoundaries( "2005", createDate( 2005, 0, 1, 0, 0, 0), createDate( 2005, 11, 31, 23, 59, 59 ) );
        /* Test leap year */
        checkParsingBoundaries( "2004", createDate( 2004, 0, 1, 0, 0, 0), createDate( 2004, 11, 31, 23, 59, 59 ) );
        /* 2000 was not a leap year */
        checkParsingBoundaries( "2000", createDate( 2000, 0, 1, 0, 0, 0), createDate( 2000, 11, 31, 23, 59, 59 ) );
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
    
    
    public static void main( String[] args ) {
	junit.textui.TestRunner.run( suite() );
    }
    
    public static Test suite() {
	return new TestSuite( Test_FuzzyDate.class );
    }

}