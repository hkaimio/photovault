// Thumbnail.java

package imginfo;


import javax.imageio.*;
import java.awt.image.*;
import java.io.*;


/**
   Thumbnail class represents an image thumbnail. It encapsulates the image data as well as info
   about the PhotoInfo object it represents
*/

public class Thumbnail {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Thumbnail.class.getName() );


    /**
       Constructor. It is not expected that Thumbnails are created independently but
       only via PhotoInfo.getThumbnail()
    */

    private Thumbnail() {
    }

    /**
       Returns a BufferedImage that contains the thumbnail image data
    */
    public BufferedImage getImage() {
	return image;
    }

    /**
       Returns the photo that the thumbnail presents
    */

    public PhotoInfo getPhotoInfo() {
	return photo;
    }

    BufferedImage image = null;
    PhotoInfo photo = null;
    
    /**
       Creates a thumbnail from a given image instance
       @param photo The photoInfo which the thumbnail presents
       @param thumbnailFile File to be used as a thumbnail
       @return A new Thumbnail object if the image instance represents a thumbnail. Otherwise
       returns null
    */

    protected static Thumbnail createThumbnail( PhotoInfo photo, File thumbnailFile ) {
	Thumbnail thumb = new Thumbnail();
	thumb.photo = photo;
	
	try {
	    thumb.image = ImageIO.read( thumbnailFile );
	} catch ( IOException e ) {
	    log.warn( "Error reading thumbnail image: " + e.getMessage() );
	    return null;
	}
	return thumb;
    }

    /**
       Returns the thumbnail image that is used if thumbnail for a specific photo cannot
       be loaded for any reason.
    */
    public static Thumbnail getDefaultThumbnail() {
	if ( defaultThumbnail == null ) {
	    defaultThumbnail = new Thumbnail();
	    defaultThumbnail.image = new BufferedImage( 100, 75, BufferedImage.TYPE_INT_RGB );
	}
	return defaultThumbnail;
    }

    static Thumbnail defaultThumbnail = null;
}
