// QueryFieldCriteria.java

package org.photovault.imginfo;

import org.apache.ojb.broker.query.Criteria;

interface QueryFieldCriteria {

    public void setupQuery( Criteria q );

}