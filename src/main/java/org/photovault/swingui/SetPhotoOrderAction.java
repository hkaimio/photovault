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
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.swingui;

import java.awt.event.ActionEvent;
import java.util.Comparator;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import org.photovault.imginfo.PhotoInfo;

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
     * @param text Test to display in menus
     * @param icon Icon that is displayed in menus etc.
    */
    public SetPhotoOrderAction( PhotoCollectionThumbView view, Comparator c, String text, ImageIcon icon,
                      String desc, Integer mnemonic) {
	super( text, icon );
	this.view = view;
        this.c =  new FullOrderPhotoComparator( c );
	putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, mnemonic);
    }

    Comparator c;
    
    /**
     Helper class that forces an absolute order on the photos, i.e. it gurantees
     that even photos that would be compared as equal will always be sorted to
     same order.
     */
    static private class FullOrderPhotoComparator implements Comparator {
        public FullOrderPhotoComparator( Comparator comp ) {
            c = comp;
        }

        Comparator c;
        
        public int compare(Object o1, Object o2 ) {
            int res = c.compare( o1, o2 );
            if ( res == 0 ) {
                PhotoInfo p1 = (PhotoInfo) o1;
                PhotoInfo p2 = (PhotoInfo) o2;
                int id1 = p1.getUid();
                int id2 = p2.getUid();
                if ( id1 < id2 ) res = -1;
                if ( id1 > id2 ) res = 1;
            }
            return res;
        }
    };
    
    public void actionPerformed( ActionEvent ev ) {
	// Show the file chooser dialog
        // TODO: change to do this in controller
        // view.setPhotoOrderComparator( c );
        // throw new UnsupportedOperationException( "Not migrated to Hibernate" ); 
    }

    PhotoCollectionThumbView view;
}