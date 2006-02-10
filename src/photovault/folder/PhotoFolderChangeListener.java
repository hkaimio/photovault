// $Id: PhotoFolderChangeListener.java,v 1.1 2003/02/23 21:43:41 kaimio Exp $

package photovault.folder;


import org.photovault.imginfo.*;
import org.photovault.imginfo.PhotoCollectionChangeListener;


/**
   PhotoFolderChangeListener is an extension of PhotoCollectionChangeListener that
   contains methods to informa also about changes to subfolders or subfolder hierarchy
*/

public interface PhotoFolderChangeListener extends PhotoCollectionChangeListener {

    /**
       This method is called when attributes of a subfolder are changed
    */
    public void subfolderChanged( PhotoFolderEvent e );

    /**
       This method is called when the sutructure under the folder is radically changed
    */
    public void structureChanged( PhotoFolderEvent e );
}
