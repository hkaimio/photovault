// QueryFieldCriteria.java

package imginfo;

import org.apache.ojb.broker.query.Criteria;

interface QueryFieldCriteria {

    public void setupQuery( Criteria q );

}