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
	String sql = "MATCH(" + field.getName() + ") AGAINST('" + text + "')";
	System.out.println( sql );
	crit.addSql( sql );
	System.out.println( "Added" );
    }

    String text = null;
    QueryField field;
}