// ExportSelectedAction

package photovault.swingui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import imginfo.*;

/**
   This action class implements exporting of all the selected images from a certain thumbnail
   view.
*/
class ExportSelectedAction extends AbstractAction implements SelectionChangeListener {

    /**
       Constructor.
       @param view The view this action object is associated with. The action gets
       the selection to export from this view.
    */
    public ExportSelectedAction( PhotoCollectionThumbView view, String text, ImageIcon icon,
                      String desc, Integer mnemonic) {
	super( text, icon );
	this.view = view;
	putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, mnemonic);
	putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_S, ActionEvent.CTRL_MASK ) );
	view.addSelectionChangeListener( this );
	setEnabled( view.getSelectedCount() > 0 );
    }

    public void selectionChanged( SelectionChangeEvent e ) {
	setEnabled( view.getSelectedCount() > 0 );
    }
    
    public void actionPerformed( ActionEvent ev ) {
	// Show the file chooser dialog
	JFileChooser fc = new JFileChooser();
	fc.addChoosableFileFilter( new ImageFilter() );
	fc.setAccessory( new ImagePreview( fc ) );
	
	int retval = fc.showDialog( view, "Export image" );
	if ( retval == JFileChooser.APPROVE_OPTION ) {
	    File exportFile = fc.getSelectedFile();
	    Collection selection = view.getSelection();
	    if ( selection != null ) {
		Iterator iter = selection.iterator();
		if ( iter.hasNext() ) {
		    PhotoInfo photo = (PhotoInfo) iter.next();
		    photo.exportPhoto( exportFile, 400, 400 );
		}
	    }
	}
    }

    PhotoCollectionThumbView view;
}