package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Collection;
import java.util.Iterator;
import org.photovault.imginfo.*;
import org.photovault.imginfo.PhotoInfo;

class EditSelectionPropsAction extends AbstractAction implements SelectionChangeListener {

    /**
       Constructor.
       @param view The view this action object is associated with. 
    */
    public EditSelectionPropsAction( PhotoCollectionThumbView view, String text, ImageIcon icon,
                      String desc, Integer mnemonic) {
	super( text, icon );
	this.view = view;
	putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, mnemonic);
	//	putValue( ACCELERATOR_KEY, KeyStroke.getKeyStroke( KeyEvent.VK_S, ActionEvent.CTRL_MASK ) );
	view.addSelectionChangeListener( this );
	setEnabled( view.getSelectedCount() > 0 );
    }

    public void selectionChanged( SelectionChangeEvent e ) {
	setEnabled( view.getSelectedCount() > 0 );
    }
    
    public void actionPerformed( ActionEvent ev ) {
        if ( view.getSelectedCount() == 0 ) {
            return;
        }
	Collection selection = view.getSelection();
        Iterator iter = selection.iterator();
        PhotoInfo[] selectedPhotos = new PhotoInfo[selection.size()];
        int i = 0;
        while ( iter.hasNext() && i < selectedPhotos.length ) {
            selectedPhotos[i++] = (PhotoInfo) iter.next();
        }
	
        // Try to find the frame in which this component is in
        Frame frame = null;
        Container c = view.getTopLevelAncestor();
        if ( c instanceof Frame ) {
            frame = (Frame) c;
        }

        if (propertyDlg == null ) {
            propertyDlg = new PhotoInfoDlg( frame, true, selectedPhotos );
        } else {
            propertyDlg.setPhotos( selectedPhotos );
        }

        propertyDlg.showDialog();
    }

    PhotoCollectionThumbView view;
    PhotoInfoDlg propertyDlg = null;


}