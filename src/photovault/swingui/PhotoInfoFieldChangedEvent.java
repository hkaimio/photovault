// PhotoInfoFieldChangedEvent.java

package photovault.swingui;

import java.util.*;


public class PhotoInfoFieldChangedEvent extends EventObject {

    /**
       Constructor.
       @param source Source of the event @see java.util.EventObject documentation.
       @param field The modified field, from constants defined in PhotoInfoController
    */
    public PhotoInfoFieldChangedEvent( PhotoInfoController source, String field ) {
	super( source );
	modifiedField = field;
    }

    /**
       Returns the field that was modified
    */
    public String getModifiedField() {
	return modifiedField;
    }

    private String modifiedField;
}
