// QueryRangeCriteria.java
  
package imginfo;
import org.apache.ojb.broker.query.Criteria;

public class QueryRangeCriteria implements QueryFieldCriteria {

    public QueryRangeCriteria( QueryField field ) {
	this.field = field;
    }

    public QueryRangeCriteria( QueryField field, Object lower, Object upper ) {
	this.field = field;
	setRange( lower, upper );
    }

    public void setRange( Object lower, Object upper ) {
	this.lower = lower;
	this.upper = upper;
    }

    public void setupQuery( Criteria crit ) {
	if ( lower != null && upper != null ) {
	    crit.addBetween( field.getName(), lower, upper );
	} else if ( lower != null ) {
	    crit.addGreaterOrEqualThan( field.getName(), lower );
	} else if ( upper != null ) {
	    crit.addLessOrEqualThan( field.getName(), upper );
	}
    }

    Object lower = null;
    Object upper = null;
    QueryField field;
}