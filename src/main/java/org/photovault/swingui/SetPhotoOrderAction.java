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

import java.awt.event.ActionEvent;
import java.util.Comparator;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

/**
 *
 * @author harri
 */
public class SetPhotoOrderAction extends AbstractAction  {

    /**
       Constructor.
       @param view The view this action object is associated with. The action gets
       the selection to export from this view.
     * @param c Comparator object that is used to sort the photos
     * @param texr Test to display in menus
     * @param icon Icon that is displayed in menus etc.
    */
    public SetPhotoOrderAction( PhotoCollectionThumbView view, Comparator c, String text, ImageIcon icon,
                      String desc, Integer mnemonic) {
	super( text, icon );
	this.view = view;
        this.c = c;
	putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, mnemonic);
    }

    Comparator c;
    
    public void actionPerformed( ActionEvent ev ) {
	// Show the file chooser dialog
        view.setPhotoOrderComparator( c );
    }

    PhotoCollectionThumbView view;
}