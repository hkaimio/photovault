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
			Thumbnail thumb = null;
			while ( thumb == null ) {
			    try {
				thumb = photo.getThumbnail();
			    } catch ( Throwable e ) {
				// Most likely out of memory. Sleep for a while (to allow for other
				// tasks to release memory) and try again
				log.warn( "Out of memory while creating thumbnail" );
				try {
				    sleep( 5 * 1000 );
				} catch ( InterruptedException e1 ) {
				    // Interrupted while sleeping, no action required
				}
			    }
			}
			log.debug( "Done!" );

			// Inform the view that the thumbnail is now created
			final PhotoInfo lastPhoto = photo;
			photo = null;
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
				    log.debug( "drawing new thumbnail for " + lastPhoto.getUid() );
				    view.thumbnailCreated( lastPhoto );
			    }
			    });
		    }
		} catch ( InterruptedException e ) {
		    // Interrupt while waiting for mutex, just continute...
		} 
	    }
	}
    }
}
					    
					    
	