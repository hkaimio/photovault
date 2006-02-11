package org.photovault.swingui;

import java.lang.*;
import javax.swing.*;
import org.photovault.imginfo.*;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.Thumbnail;

/**
 ThumbCreatorThread is used for deferred creation of thumbnails by PhotoCollectionThumbView.
 It starts a new thread that waits for thumbnail creation requests and notifies 
 the PhotoCollectionThumbVew object when the requested thumbnail is ready.
 <p>
 This class implements a simple producer-consumer model for passing photos between the 2 
 threads.
 
 */

class ThumbCreatorThread extends Thread {
    static org.apache.log4j.Logger log 
	= org.apache.log4j.Logger.getLogger( ThumbCreatorThread.class.getName() );

    /**
     Constructor
     @param view The view that will be notified after the thumbnail has been drawn
     */
    public ThumbCreatorThread( PhotoCollectionThumbView view ) {
        super( "ThumbCreator" );
	this.view = view;
    }

    PhotoCollectionThumbView view;
    PhotoInfo photo;
    
    /**
     Submits a request for creation of a new thumbnail. This method returns immediately
     after notifying the thumbnail creation thread. However, it blocks on a monitor
     until the thread is free to take another job. So in performance critical situations
     it is safest to call isBusy() before claling this.
     @param photo the photo for which the thumbnail will be created.
     */
    synchronized public void createThumbnail( PhotoInfo photo ) {
	log.debug( "createThumbnail for " + photo.getUid() );
	this.photo = photo;
	notify();
    }

    /**
     @return true if the thread is currently working on a new thumbnail, false otherwise.
     */
    public boolean isBusy() {
	return ( photo != null );
    }

    /**
     Actual code for the thread.
     */
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
					    
					    
	