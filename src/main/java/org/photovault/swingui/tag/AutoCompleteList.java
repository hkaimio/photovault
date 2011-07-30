/*
  Copyright (c) 2011 Harri Kaimio

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

package org.photovault.swingui.tag;

import javax.swing.JList;

/**
 * List used for autocomplete entries. This extends {@link JList} with a couple
 * of methods for selecting the previous or next element.
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class AutoCompleteList extends JList {
    
    public AutoCompleteList() {
        super();
    }
    
    /**
     * Select the next entry in the list. The selection rolls over so that after
     * last item is selected, the selection is cleared, after which the first 
     * element is selected.
     */ 
    public void selectNext() {
        int index = getSelectedIndex();
        if ( index < getModel().getSize() - 1 ) {
            setSelectedIndex( index + 1 );
        } else {
            clearSelection();
        }
    }

    /**
     * Select previous entry in the list. See {@link #selectNext() } for 
     * explanation of rolling over first/last item.
     */
    public void selectPrev() {
        int index = getSelectedIndex();
        if ( index > 0 ) {
            setSelectedIndex( index - 1 );
        } else if ( index == 0 ) {
            clearSelection();
        } else {
            setSelectedIndex( getModel().getSize()-1 );
        }
    }
}
