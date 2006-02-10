// QueryRangeCriteria.java
  
package org.photovault.imginfo;
import org.apache.ojb.broker.query.Criteria;

public class QueryLikeCriteria implements QueryFieldCriteria {

    public QueryLikeCriteria( QueryField field ) {
	this.field = field;
    }

    public QueryLikeCriteria( QueryField field, String text ) {
	this.field = field;
	setSearchText( text );
    }

    public void setSearchText( String text ) {
	this.searchText = text;
    }

    public void setupQuery( Criteria crit ) {
	if ( searchText != null ) {
	    crit.addLike( field.getName(), searchText );
	}
    }

    String searchText = null;
    QueryField field;
}