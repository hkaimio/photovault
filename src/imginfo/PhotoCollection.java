// PhotoCollection.java

package imginfo;

/**
   Interface to access a collection of photos, like folder or query result set
*/
interface PhotoCollection {
    /**
       returns the number of photos in this collection
    */
    public int getPhotoCount();
    /**
       Get a single hpto from the collection
       @param photoNum Number of the photo to retrieve. This must be >= 0 and < than the number of photos in collection.
    */
    public PhotoInfo getPhoto( int numPhoto );
}
