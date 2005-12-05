package imginfo;

import org.apache.ojb.broker.query.Criteria;
import imginfo.FuzzyDate;
import org.apache.ojb.broker.query.Criteria;

public class QueryFuzzyTimeCriteria implements QueryFieldCriteria {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( QueryFuzzyTimeCriteria.class.getName() );

    QueryField dateField;
    QueryField accuracyField;
    FuzzyDate date;

    public static final int INCLUDE_CERTAIN = 1;
    public static final int INCLUDE_PROBABLE = 2;
    public static final int INCLUDE_POSSIBLE = 3;
    int strictness;
    
    QueryFuzzyTimeCriteria( QueryField dateField, QueryField accuracyField ) {
	this.dateField = dateField;
	this.accuracyField = accuracyField;
	strictness = INCLUDE_PROBABLE;
    }

    public void setStrictness( int value ) {
	strictness = value;
    }

    public int getStrictness() {
	return strictness;
    }

    public void setDate( FuzzyDate date ) {
	this.date = date;
    }

    public FuzzyDate getDate() {
	return date;
    }

    
    
    // Implementation of imginfo.QueryFieldCriteria

    /**
     * Describe <code>setupQuery</code> method here.
     *
     * @param crit a <code>Criteria</code> value
     */
    public final void setupQuery(final Criteria crit) {
	log.debug( "Entry: SetupQuery" );
	switch ( strictness ) {
	case INCLUDE_CERTAIN:
	    log.debug( "INCLUDE_CERTAIN" );
	    /* Only certain results must be included, so the whole
	    possible time period of object must be inside the accuracy
	    limits.  */
	    crit.addGreaterOrEqualThan( "subdate("+dateField.getName()
					+ ", " + accuracyField.getName() + ")",
					date.getMinDate() );
	    crit.addLessOrEqualThan( "adddate("+dateField.getName()
				     +", " + accuracyField.getName() + ")",
				     date.getMaxDate() );
	    break;
	case INCLUDE_PROBABLE:
	    log.debug( "INCLUDE_PROBABLE" );
	    crit.addBetween( dateField.getName(),
			     date.getMinDate(),
			     date.getMaxDate() );
	    crit.addLessOrEqualThan( accuracyField.getName(),
				     date.getAccuracy() );
	    break;

	case INCLUDE_POSSIBLE:
	    log.debug( "INCLUDE_POSSIBLE" );
	    String gtclause = "adddate("+dateField.getName()
		+", " + accuracyField.getName() + ")";
	    crit.addGreaterOrEqualThan( gtclause,
					date.getMinDate() );
	    log.debug( gtclause + " >= " + date.getMinDate() );
	    String ltclause = "subdate("+dateField.getName()
		+", " + accuracyField.getName() + ")";
	    crit.addLessOrEqualThan( ltclause,
				     date.getMaxDate() );
	    log.debug( ltclause + " <= " + date.getMaxDate() );
	    break;
	default:
	    log.error( "Illegal value for strictness: " + strictness );
	    

	}

	log.debug( "Exit: SetupQuery" );
    }



}