// PhotoInfoListener.java

package photovault.swingui;

import org.photovault.imginfo.*;

/** Interface for classes that want to receive events from modifications to PhotoInfoController state
 */

public interface PhotoInfoListener {
    /**
       This method gets called when a field of the PhotoInfoController is changed.
       @param e
    */
    public void fieldValueChanged( PhotoInfoFieldChangedEvent e );
    /**
       This method gets called when the model controlled by the PhotoInfoController object is changed
    */
    public void modelChanged( PhotoInfoModelChangedEvent e );
}
