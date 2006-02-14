// QueryField.java

package org.photovault.imginfo;

/**
   QueryField method represents the needed information about a class field for generic query mechanism.
*/
public class QueryField {

    public QueryField( String fieldName ) {
	this.fieldName = fieldName;
    }

    public String getName() {
	return fieldName;
    }
    
    String fieldName;
}
    