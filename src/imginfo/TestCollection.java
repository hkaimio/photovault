// TestCollection.java

package imginfo;

import java.util.*;
/**
   Simple colledction of photos for testing purposes
*/
public class TestCollection implements PhotoCollection {

    Vector photos = null;
    
    public TestCollection() {
	photos = new Vector();
    }

    public void addPhoto( PhotoInfo photo ) {
	photos.add( photo );
    }

    public PhotoInfo getPhoto( int photoNum ) {
	return (PhotoInfo) photos.get(photoNum );
    }

    public int getPhotoCount() {
	return photos.size();
    }
}
	
