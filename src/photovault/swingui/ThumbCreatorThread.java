package photovault.swingui;

import java.lang.*;
import javax.swing.*;
import imginfo.*;


class ThumbCreatorThread extends Thread {
    static org.apache.log4j.Logger log 
	= org.apache.log4j.Logger.getLogger( ThumbCreatorThread.class.getName() );

    public ThumbCreatorThread( PhotoCollectionThumbView view ) {
	this.view = view;
    }

    PhotoCollectionThumbView view;
    PhotoInfo photo;
    
    synchronized public void createThumbnail( PhotoInfo photo ) {
	log.debug( "createThumbnail for " + photo.getUid() );
	this.photo = photo;
	notify();
    }

    public boolean isBusy() {
	return ( photo != null );
    }

    public void run() {
	synchronized ( this ) {
	    while ( true ) {
		try {
		    log.debug( "Waiting..." );
		    wait();
		    log.debug( "Waited..." );
		    if ( photo != null ) {
			log.debug( "Creating thumbnail for " + photo.getUid() );
			Thumbnail thumb = photo.getThumbnail();
			log.debug( "Done!" );
			final PhotoInfo lastPhoto = photo;
			photo = null;
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
				    log.debug( "drawing new thumbnail for " + lastPhoto.getUid() );
				    view.thumbnailCreated( lastPhoto );
			    }
			    });
		    }
		} catch ( InterruptedException e ) {}
	    }
	}
    }
}
					    
					    
	