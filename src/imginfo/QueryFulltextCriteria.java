// QueryFulltextCriteria.java
  
package imginfo;
import org.apache.ojb.broker.query.Criteria;

public class QueryFulltextCriteria implements QueryFieldCriteria {

    public QueryFulltextCriteria( QueryField field ) {
	this.field = field;
    }

    public QueryFulltextCriteria( QueryField field, String text ) {
	this.field = field;
	this.text = text;
    }

    public void setText( String text ) {
	this.text = text;
    }

    public void setupQuery( Criteria crit ) {
	crit.addSql( "MATCH(" + field + ") AGAINST(\"" + text + "\")" ); 
    }

    String text = null;
    QueryField field;
}