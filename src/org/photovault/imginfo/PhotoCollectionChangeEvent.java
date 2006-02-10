// PhotoCollectionChangeEvent.java

package org.photovault.imginfo;
import java.util.*;

/**
   PhotoCollectionChangeEvent will be sent to PhotoCollectionChangeListeners when the photo colledction
   is changed.
*/
public class PhotoCollectionChangeEvent extends EventObject {
    public PhotoCollectionChangeEvent ( PhotoCollection source ) {
	super( source );
    }
}


