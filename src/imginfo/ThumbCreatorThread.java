package photovault.swingui;

import java.lang.*;
import javax.swing.*;
import imginfo.*;


class ThumbCreatorThread extends Thread {
    public ThumbCreatorThread( PhotoCollectionThumbView view ) {
	this.view = view;
    }

    PhotoCollectionThumbView view;
    PhotoInfo photo;
    
    synchronized public void createThumbnail( PhotoInfo photo ) {
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
		    System.out.println( "Waiting..." );
		    wait();
		    System.out.println( "Waited..." );
		    if ( photo != null ) {
			System.out.println( "Creating thumbnail..." );
			Thumbnail thumb = photo.getThumbnail();
			System.out.println( "Done!" );
			final PhotoInfo lastPhoto = photo;
			photo = null;
			SwingUtilities.invokeLater( new Runnable() {
				public void run() {
				    view.thumbnailCreated( lastPhoto );
			    }
			    });
		    }
		} catch ( InterruptedException e ) {}
	    }
	}
    }
}
					    
					    
	