// $Id: PhotoInfoChangeListener.java,v 1.1 2003/02/08 21:19:34 kaimio Exp $

package org.photovault.imginfo;


/** Interface for classes that want to receive events from modifications to the PhotoInfo. Note that 
    this is different to the (wrongly named) PhotoInfoListener which is notified of changes to <b> 
    PhotoInfoController</b> changes.
 */

public interface PhotoInfoChangeListener {
    /**
       This method gets called when a field of the PhotoInfo is changed.
       @param e
    */
    public void photoInfoChanged( PhotoInfoChangeEvent e );
}
