// ShowSelectedPhotoAction

package photovault.swingui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import imginfo.*;

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
        try {
            Collection selectedPhotos = view.getSelection();
            Iterator iter = selectedPhotos.iterator();
            while ( iter.hasNext() ) {
                PhotoInfo photo = (PhotoInfo) iter.next();
                JFrame frame = new JFrame( "Photo" );
                final PhotoViewer viewer = new PhotoViewer();
                frame.getContentPane().add( viewer, BorderLayout.CENTER );
                frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

                // This is a WAR for a memory management problem. For
                // some reason the frame and objects owned by it seem
                // not to be garbage collected. So we free the large
                // image buffers to avoid massive memory leak.
                
                frame.addWindowListener( new WindowAdapter() {
                        public void windowClosing( WindowEvent e ) {
                            viewer.setPhoto( null );
                        }
                    } );
                viewer.setPhoto( photo );
                frame.pack();
                frame.setVisible( true );
            }
        } catch ( Throwable e ) {
            System.err.println( "Out of memory error" );
            log.warn( e );
            e.printStackTrace();
        }
    }

    PhotoCollectionThumbView view;
}