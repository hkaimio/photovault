// PhotoCollectionChangeListener.java

package org.photovault.imginfo;


public interface PhotoCollectionChangeListener {
    /**
       This method will be changed when the photo collection has changed for some reason
    */
    public void photoCollectionChanged( PhotoCollectionChangeEvent ev );
}
