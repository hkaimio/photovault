package org.photovault.swingui;

import java.util.EventObject;

/**
   This event is send by PhotoCollectionThumbView when the selection is changed
*/
public class SelectionChangeEvent extends EventObject {

    public  SelectionChangeEvent( PhotoCollectionThumbView src ) {
	super( src );
    }

}