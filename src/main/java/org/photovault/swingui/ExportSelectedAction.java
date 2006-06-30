/*
  Copyright (c) 2006 Harri Kaimio
  
  This file is part of Photovault.

  Photovault is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  Photovault is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Foobar; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

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
                      String desc, int mnemonic) {
	super( text, icon );
	this.view = view;
	putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, new Integer( mnemonic) );
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
       	    Container c = view.getTopLevelAncestor();
	    Cursor oldCursor = c.getCursor();
            c.setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
            File exportFile = fc.getSelectedFile();
	    Collection selection = view.getSelection();
	    if ( selection != null ) {
		Iterator iter = selection.iterator();
		if ( iter.hasNext() ) {
		    PhotoInfo photo = (PhotoInfo) iter.next();
		    photo.exportPhoto( exportFile, -1, -1 );
		}
	    }
            c.setCursor( oldCursor );
	}
    }

    PhotoCollectionThumbView view;
}