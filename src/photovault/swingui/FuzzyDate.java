package photovault.swingui;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;

public class FuzzyDate {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( FuzzyDate.class.getName() );
    
    public FuzzyDate( Date date, double accuracy ) {
	this.date = date;
	this.accuracy = accuracy;
    }
    
    Date date;
    double accuracy;
    static final double MILLIS_IN_DAY = 24 * 3600 * 1000;

    
    static double accuracyFormatLimits[] = {0, 3, 14, 180};
    static String accuracyFormatStrings[] = {
	"dd.MM.yyyy",
	"'wk' w yyyy",
	"MMMM yyyy",
	"yyyy"
    };

    
    public Date getDate() {
	return date;
    }

    public double getAccuracy() {
	return accuracy;
    }


    /**
       Returns the earliest time that fits into the accuracy interval
    */
    public Date getMinDate() {
	Date d = null;
	if ( date != null ) {
	    d = new Date( date.getTime() - (long) (accuracy*MILLIS_IN_DAY) );
	}
	return d;
    }
    
    /**
       Returns the latest time that fits into the accuracy interval
    */
    public Date getMaxDate() {
	Date d = null;
	if ( date != null ) {
	    d = new Date( date.getTime() + (long) (accuracy*MILLIS_IN_DAY) );
	}
	return d;
    }
    
    
    static public FuzzyDate parse( String str ) {
	log.warn( "Parsing " + str );
	FuzzyDate fd = null;

	// First check whether the string contains a range with start and end dates
	String rangeSeparator = " - ";
	int separatorIdx = str.indexOf( rangeSeparator );
	if ( separatorIdx > 0 ) {
	    // Yes, it is. Parse both sides separately and set the fuzzy date to cover
	    // the whole range

	    if ( str.lastIndexOf( rangeSeparator ) != separatorIdx ||
		 separatorIdx > str.length() - rangeSeparator.length() ) {
		// Only 1 range separator is allowed, return null
		return null;
	    }

	    FuzzyDate date1 = FuzzyDate.parse( str.substring( 0, separatorIdx ) );
	    FuzzyDate date2 = FuzzyDate.parse( str.substring( separatorIdx + rangeSeparator.length() ) );
	    if ( date1 == null || date2 == null ) {
		return null;
	    }

	    // Set the date as average of begin and end
	    long t1 = date1.getMinDate().getTime();
	    long t2 = date2.getMaxDate().getTime();
	    if ( t1 > t2 ) {
		// The range is not valid since end time is smaller than start time!!!
		return null;
	    }
	    Date avgDate = new Date( (t1+t2)/2 );
	    long accuracy = (t2-t1)/2;
	    
	    fd = new FuzzyDate( avgDate, ((double)accuracy) / MILLIS_IN_DAY );
	} else {
	    // No, just one date given
	
	    // attempt to parse the date using all format strings

	    for ( int i = 0; i < accuracyFormatLimits.length; i++ ) {
		log.warn( "Trying format " + accuracyFormatStrings[i] );
		DateFormat df = new SimpleDateFormat( accuracyFormatStrings[i] );
		try {
		    Date d = df.parse( str );
		    if ( d != null ) {
			// Succeeded!!!
			log.warn( "Success with " + accuracyFormatStrings[i] );
			d = new Date( d.getTime() + (long)(accuracyFormatLimits[i] *24*3600*1000) );
			fd = new FuzzyDate( d, accuracyFormatLimits[i] );
			break;
		    }
		} catch ( ParseException e ) {
		    log.warn( "ParseException: " + e.getMessage() );
		}
	    }
	}
	return fd;
    }

    public String format() {
	long lAccuracy = (long) (accuracy  * 24 * 3600 * 1000);
	String dateStr = "";
	if ( date == null ) {
	    return "";
	}
	
	if ( accuracy > 0 ) {
	    
	    // Find the correct format to use
	    String formatStr =accuracyFormatStrings[0];
	    for ( int i = 1; i < accuracyFormatLimits.length; i++ ) {
		if ( accuracy < accuracyFormatLimits[i] ) {
		    break;
		}
		formatStr =accuracyFormatStrings[i];
	    }
	    
	    // Show the limits of the accuracy range
	    DateFormat df = new SimpleDateFormat( formatStr );
	    Date lower = new Date( date.getTime() - lAccuracy );
	    Date upper = new Date( date.getTime() + lAccuracy );
	    String lowerStr = df.format( lower );
	    String upperStr = df.format( upper );
	    dateStr = lowerStr;
	    if ( !lowerStr.equals( upperStr ) ) {
		dateStr += " - " + upperStr;
	    }
	} else {
	    DateFormat df = new SimpleDateFormat( "dd.MM.yyyy k:mm" );
	    dateStr = df.format( date );
	}
	return dateStr;
    }

    public boolean equals( Object obj ) {
	boolean isEqual = false;
	if ( obj instanceof FuzzyDate ) {
	    FuzzyDate fd = (FuzzyDate) obj;
	    if ( date != null ) {
		isEqual = date.equals( fd.date ) && (accuracy == fd.accuracy );
	    } else {
		isEqual = (fd.date == null ) && ( accuracy == fd.accuracy );
	    }
	}
	return isEqual;
    }
}
