// $Id: PhotoFolderEvent.java,v 1.1 2003/02/23 21:43:41 kaimio Exp $

package photovault.folder;

import imginfo.*;
import org.photovault.imginfo.PhotoCollectionChangeEvent;

/**
   PhotoFolderEvent describes an event that has changed a PhotoFolder. It extends @see PhotoCollectionChangeEvent
   by providing information about the subfolder that has changed it thas has been the case.
*/

public class PhotoFolderEvent extends PhotoCollectionChangeEvent {

    PhotoFolder subfolder = null;
    PhotoFolder[] path = null;

    /**
       Constructor
       @param source The PhotoFolder object that has initiateed the event
       @param subfolder If the evenbt has been created by a change to a subfolder, reference to it.
       Otherwise <code>null</code>.
       @param path Array of PhotoFolders that describes the hierarchy from source to subfolder.
    */
    public PhotoFolderEvent( PhotoFolder source, PhotoFolder subfolder, PhotoFolder[] path ) {
	super( source );
	this.subfolder = subfolder;
	this.path = path;
    }

    public PhotoFolder getSubfolder() {
	return subfolder;
    }

}
