/*
 * SortedPhotoCollection.java
 *
 * Created on 7. toukokuuta 2005, 6:58
 */

package imginfo;
import java.util.SortedSet;
import java.util.Comparator;
import java.util.TreeSet;
import java.util.Vector;
import java.util.Iterator;

/**
 * This class implements a filter for sorting a photo collection by specific criteria. 
 * I practice, it creates a copy of the collection, sorts it and listens to change events
 * from the original colelction. If it is changed, the sorted copy is also recreated.
 * <p> The sorting order can be defined using a Comparator object that is capable of 
 * Comparing PhotoInfo objects. The default is to order photos according to their ID
 * @author Harri Kaimio
 */
public class SortedPhotoCollection implements PhotoCollection, PhotoCollectionChangeListener {
    
     static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( SortedPhotoCollection.class.getName() );
     
    /**
     * Creates a new SortedPhotoCollection that is based on c.
     */
    public SortedPhotoCollection( PhotoCollection c ) {
        origCollection = c;
        comparator = new PhotoIdComparator();
        sortCollection();
        c.addPhotoCollectionChangeListener( this );
    }
    
    protected PhotoCollection origCollection;
    protected Vector sortedPhotos = null;
    protected Comparator comparator = null;
    protected Vector changeListeners = new Vector();
    
    class PhotoIdComparator implements Comparator {
        public int compare( Object o1, Object o2 ) {
            PhotoInfo p1 = (PhotoInfo) o1;
            PhotoInfo p2 = (PhotoInfo) o2;
            int id1 = p1.getUid();
            int id2 = p2.getUid();
            if ( id1 == id2 ) {
                return 0;
            } else if ( id1 < id2 ) {
                return -1;
            } else {
                return 1;
            }
        }
    }
        
   
    /** Creates a new instance of SortedPhotoCollection */
    public SortedPhotoCollection() {
    }

    /**
     * Remove a listener
     */
    public void removePhotoCollectionChangeListener(PhotoCollectionChangeListener l) {
        changeListeners.remove( l );
    }

    /**
     *       Adds a new listener that will be notified of changes to the collection
     */
    public void addPhotoCollectionChangeListener(PhotoCollectionChangeListener l) {
        changeListeners.add( l );
    }

    /**
     *       Get a single hpto from the collection
     *       @param photoNum Number of the photo to retrieve. This must be >= 0 and < than
     *       the number of photos in collection.
     */
    public PhotoInfo getPhoto(int numPhoto) {
        if ( sortedPhotos == null || numPhoto >= sortedPhotos.size() ) {
	    throw new ArrayIndexOutOfBoundsException();
	}
    	return (PhotoInfo) sortedPhotos.get( numPhoto );
    }

    /**
     *       returns the number of photos in this collection
     */
    public int getPhotoCount() {
        if ( sortedPhotos == null ) {
	    return 0;
	}
	return sortedPhotos.size();
    }

    /**
     *       This method will be changed when the photo collection has changed for some reason
     */
    public void photoCollectionChanged(PhotoCollectionChangeEvent ev) {
        sortCollection();
    }
    
    
    
    protected void sortCollection() {
        TreeSet sortedTree = new TreeSet( comparator );
        for ( int n = 0; n < origCollection.getPhotoCount(); n++ ) {
            PhotoInfo p = origCollection.getPhoto( n );
            sortedTree.add( p );
        }
        sortedPhotos = new Vector( sortedTree );
        
        notifyListeners();
    }
    
     protected void notifyListeners() {
	PhotoCollectionChangeEvent ev = new PhotoCollectionChangeEvent( this );
	Iterator iter = changeListeners.iterator();
	while ( iter.hasNext() ) {
	    PhotoCollectionChangeListener l = (PhotoCollectionChangeListener) iter.next();
	    l.photoCollectionChanged( ev );
	}
    }
}
