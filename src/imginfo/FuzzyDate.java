package imginfo;

import java.util.Date;
import java.util.GregorianCalendar;
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
    static final double MILLIS_IN_MINUTE = 60000;
    static final double MILLIS_IN_HOUR = 3600 * 1000;
    static final double MILLIS_IN_DAY = 24 * MILLIS_IN_HOUR;

    static class FuzzyDateParser {
        
        public FuzzyDateParser( String formatStr, long fuzzyPeriodLength ) {
            dateFormatStr = formatStr;
            this.fuzzyPeriodLength = fuzzyPeriodLength;
        }
        
        String dateFormatStr;
        long fuzzyPeriodLength;
        DateFormat df = null;
        
        protected long getFuzzyPeriodLength( Date startDate ) {
            return fuzzyPeriodLength;
        }

        /**
         * Returns the number of days in the fuzziness period.
         */ 
        protected double getFloatFuzzyPeriodLength( Date startDate ) {
            return ((double)(getFuzzyPeriodLength( startDate ) ) ) / FuzzyDate.MILLIS_IN_DAY; 
        }
        
        protected DateFormat getDateFormat() {
            if ( df == null ) {
                df = new SimpleDateFormat( dateFormatStr );
            }
            return df;
        }
        
        public FuzzyDate parse(String strDate) {
            DateFormat df = getDateFormat();
            FuzzyDate fd = null;
            try {
                Date d = df.parse( strDate );
                if ( d != null ) {
                    Date midpoint = new Date( d.getTime() + getFuzzyPeriodLength(d) / 2 );
                    fd = new FuzzyDate( midpoint, 0.5 * getFloatFuzzyPeriodLength( d ) );
                }
            } catch ( ParseException e ) {
                log.warn( "ParseException: " + e.getMessage() );

            }
            return fd;
        }
        
        /**
         * Formats a date using this formatter
         */
        public String format( Date date ) {
            DateFormat df = getDateFormat();
            return df.format( date );
        }
    }
    
    static class FuzzyDateMonthParser extends FuzzyDateParser {
        public FuzzyDateMonthParser( String formatStr ) {
            super( formatStr, 0 );
        }
        
        protected long getFuzzyPeriodLength( Date startDate ) {
            GregorianCalendar c = new GregorianCalendar();
            c.setTime( startDate );
            c.add( GregorianCalendar.MONTH, 1 );
            return c.getTimeInMillis() - startDate.getTime();
        }        
    }

    static class FuzzyDateYearParser extends FuzzyDateParser {
        public FuzzyDateYearParser( String formatStr ) {
            super( formatStr, 0 );
        }
        
        protected long getFuzzyPeriodLength( Date startDate ) {
            GregorianCalendar c = new GregorianCalendar();
            c.setTime( startDate );
            c.add( GregorianCalendar.YEAR, 1 );
            return c.getTimeInMillis() - startDate.getTime();
        }        
        
//        protected double getFloatFuzzyPeriodLength( Date startDate ) {
//            double fuzzyPeriodLength = ((double)(getFuzzyPeriodLength( startDate ) ) ) / FuzzyDate.MILLIS_IN_DAY; 
//            fuzzyPeriodLength = ((double)Math.round(  fuzzyPeriodLength*100 )) * 0.01;
//            return fuzzyPeriodLength;
//        }
    }
    
    static double accuracyFormatLimits[] = {0.5/(24.0*60), .5, 3, 14, 180};
    static String accuracyFormatStrings[] = {
	"dd.MM.yyyy k:mm",
	"dd.MM.yyyy",
	"'wk' w yyyy",
	"MMMM yyyy",
	"yyyy"
    };

    static FuzzyDateParser fdParsers[] = null;
    
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
    
    static private void createParsers() {
        fdParsers = new FuzzyDateParser[5];
        fdParsers[0] = new FuzzyDateParser( "dd.MM.yyyy k:mm", (long) 60000 );
        fdParsers[1] = new FuzzyDateParser( "dd.MM.yyyy", (long)24 * 3600 * 1000 );
        fdParsers[2] = new FuzzyDateParser( "'wk' w yyyy", (long)7 * 24 * 3600 * 1000 );
        fdParsers[3] = new FuzzyDateMonthParser( "MMMM yyyy" );
        fdParsers[4] = new FuzzyDateYearParser( "yyyy" );
    }
    
    static public FuzzyDate parse( String str ) {
        if ( fdParsers == null ) {
            createParsers();
        }
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

	    for ( int i = 0; i < fdParsers.length; i++ ) {
                fd = fdParsers[i].parse( str );
		if ( fd != null ) {
                    break;
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
        
        Date lower = new Date( date.getTime() - lAccuracy );
        Date upper = new Date( date.getTime() + (lAccuracy-1) );

	if ( fdParsers == null ) {
            createParsers();
        }
	if ( accuracy > 0 ) {
	    // Find the correct format to use
            FuzzyDateParser parser = fdParsers[0];
            for ( int i = 0; i < fdParsers.length; i++ ) {
		if ( (2 * lAccuracy) < fdParsers[i].getFuzzyPeriodLength( lower ) ) {
		    break;
		}
		parser = fdParsers[i];
	    }
	    
	    // Show the limits of the accuracy range
	    String lowerStr = parser.format( lower );
	    String upperStr = parser.format( upper );
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
		isEqual = date.equals( fd.date ) && (Math.abs(accuracy - fd.accuracy) < 0.0001);
	    } else {
		isEqual = (fd.date == null ) && ( accuracy == fd.accuracy );
	    }
	}
	return isEqual;
    }
}
