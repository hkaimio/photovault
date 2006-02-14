// ShowSelectedPhotoAction

package org.photovault.swingui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import org.photovault.imginfo.*;
import org.photovault.imginfo.PhotoInfo;

/**
   This action class implements displays the selected images
*/
class ShowSelectedPhotoAction extends AbstractAction implements SelectionChangeListener {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( ShowSelectedPhotoAction.class.getName() );

    /**
       Constructor.
       @param view The view this action object is associated with. 
    */
    public ShowSelectedPhotoAction( PhotoCollectionThumbView view, String text, ImageIcon icon,
                      String desc, Integer mnemonic) {
	super( text, icon );
	this.view = view;
	putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, mnemonic);
	view.addSelectionChangeListener( this );
	setEnabled( view.getSelectedCount() > 0 );
    }

    public void selectionChanged( SelectionChangeEvent e ) {
	setEnabled( view.getSelectedCount() > 0 );
    }
    
    public void actionPerformed( ActionEvent ev ) {
	Frame parentFrame = null;
	Container c = view.getTopLevelAncestor();
	if ( c instanceof Frame ) {
	    parentFrame = (Frame) c;
	}
	Collection selectedPhotos = view.getSelection();
	if ( selectedPhotos.size() > 1 ) {
	    // If user wants to open many photos at once ask for confirmation
	    // Try to find the frame in which this component is in
	    int n = JOptionPane.showConfirmDialog( parentFrame,
						   "You have selected " + selectedPhotos.size() + " photos\n" +
						   "Are you sure that you want to\ndisplay all of them at once?",
						   "Open multiple photos",
						   JOptionPane.YES_NO_OPTION );
	    if ( n != JOptionPane.YES_OPTION ) {
		return;
	    }
	}
	
	Cursor oldCursor = c.getCursor();
	c.setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
	Iterator iter = selectedPhotos.iterator();
	JAIPhotoViewer viewer = null;
	while ( iter.hasNext() ) {
	    try {
		viewer = null;
		PhotoInfo photo = (PhotoInfo) iter.next();
		JFrame frame = new JFrame( "Photo" );
		viewer = new JAIPhotoViewer();
		frame.getContentPane().add( viewer, BorderLayout.CENTER );
		frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
		
		// This is a WAR for a memory management problem. For
		// some reason the frame and objects owned by it seem
		// not to be garbage collected. So we free the large
		// image buffers to avoid massive memory leak.

		final JAIPhotoViewer pv = viewer;
		frame.addWindowListener( new WindowAdapter() {
			public void windowClosing( WindowEvent e ) {
			pv.setPhoto( null );
			}
		    } );
		viewer.setPhoto( photo );
		frame.pack();
		frame.setVisible( true );
	    } catch ( Throwable e ) {
		// Most likely we got an out of memory error
		log.warn( "Error while trying to show a photo: " + e );
		e.printStackTrace();
		// Clean up as much as we can
		if ( viewer != null ) {
		    viewer.setPhoto( null );
		}
		try {
		    JOptionPane.showMessageDialog( parentFrame,
						   "Out of memory while trying to show a photo\n"
						   + "Try closing some windows and try again",
						   "Out of memory",
						   JOptionPane.ERROR_MESSAGE );
		} catch (Throwable t ) {
		    // We are so screwed up that we cannot even show an error message
		    // Anyway, catch the error so that we can exit the action orderly
		}
		// Do not continue opeing new images
		break;
	    }
	}

	// Restore the old cursor
	c.setCursor( oldCursor );
    }

    PhotoCollectionThumbView view;
}