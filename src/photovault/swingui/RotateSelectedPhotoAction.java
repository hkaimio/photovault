// RotateSelectedPhotoAction

package photovault.swingui;


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
class RotateSelectedPhotoAction extends AbstractAction implements SelectionChangeListener {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( RotateSelectedPhotoAction.class.getName() );

    /**
       Constructor.
       @param view The view this action object is associated with. 
    */
    public RotateSelectedPhotoAction( PhotoCollectionThumbView view, 
				      double r,
				      String text, ImageIcon icon,
				      String desc, Integer mnemonic) {
	super( text, icon );
	this.view = view;
	putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, mnemonic);
	view.addSelectionChangeListener( this );
	setEnabled( view.getSelectedCount() > 0 );
	rot = r;
    }

    public void selectionChanged( SelectionChangeEvent e ) {
	setEnabled( view.getSelectedCount() > 0 );
    }
    
    public void actionPerformed( ActionEvent ev ) {
        Collection selectedPhotos = view.getSelection();
        Iterator iter = selectedPhotos.iterator();
        while ( iter.hasNext() ) {
            PhotoInfo photo = (PhotoInfo) iter.next();
            if ( photo != null ) {
                double curRot = photo.getPrefRotation();
                photo.setPrefRotation( curRot + rot );
            }
        }
    }

    PhotoCollectionThumbView view;
    double rot;
}