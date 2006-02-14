// PhotoInfoModelChangedEvent.java

package org.photovault.swingui;

import java.util.*;

/**
   Event that is send to PhotoInfoListeners when the model (i.e. PhotoInfo obejct) controlled by
   PhotoInfoController object is changed
*/
public class PhotoInfoModelChangedEvent extends EventObject {
    /**
       Constructor
    */
    public PhotoInfoModelChangedEvent( PhotoInfoController source ) {
	super ( source );
    }
}
