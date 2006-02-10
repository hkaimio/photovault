// PhotoCollection.java

package org.photovault.imginfo;

/**
   Interface to access a collection of photos, like folder or query result set
*/
public interface PhotoCollection {
    /**
       returns the number of photos in this collection
    */
    public int getPhotoCount();
    /**
       Get a single hpto from the collection
       @param numPhoto Number of the photo to retrieve. This must be >= 0 and < than
       the number of photos in collection.
    */
    public PhotoInfo getPhoto( int numPhoto );

    /**
       Adds a new listener that will be notified of changes to the collection
    */
    public void addPhotoCollectionChangeListener( PhotoCollectionChangeListener l );


    /** Remove a listener
     */
    public void removePhotoCollectionChangeListener( PhotoCollectionChangeListener l );
}
