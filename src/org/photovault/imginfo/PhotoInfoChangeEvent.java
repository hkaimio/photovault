// $Id: PhotoInfoChangeEvent.java,v 1.1 2003/02/08 21:19:34 kaimio Exp $

package org.photovault.imginfo;

import java.util.*;

/**
   Event that is send to PhotoInfoChangeListeners when the PhotoInfo object controlled by
   PhotoInfoController object is changed
*/
public class PhotoInfoChangeEvent extends EventObject {
    /**
       Constructor
    */
    public PhotoInfoChangeEvent( PhotoInfo source ) {
	super (source );
    }
}
