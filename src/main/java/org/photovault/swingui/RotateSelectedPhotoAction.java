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