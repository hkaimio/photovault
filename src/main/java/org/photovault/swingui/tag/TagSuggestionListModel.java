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

import java.util.ArrayList;
import java.util.List;
import javax.swing.ComboBoxModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import org.photovault.imginfo.Tag;

/**
 * List model for suggesting tag completions
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class TagSuggestionListModel implements ComboBoxModel {

    private List<Tag> allTags;
    private List<Tag> tags;
    private String filter;
    private List<ListDataListener> listeners;
    private Tag selectedItem = null;
    
    public TagSuggestionListModel() {
        allTags = new ArrayList<Tag>();
        tags = new ArrayList<Tag>();
        listeners = new ArrayList<ListDataListener>();
        
        filterTags();
    }
    
    /**
     * Add a new tag to the possible completions
     * @param newTag 
     */
    public void addTag( Tag newTag ) {
        for ( Tag tag : allTags ) {
            if ( tag.equals( newTag ) ) {
                return;
            }
        }
        allTags.add( newTag );
        filterTags();
    }
    
    public void setSelectedItem( Object anItem ) {
        selectedItem = (Tag) anItem;
    }

    public Object getSelectedItem() {
        return selectedItem;
    }

    public int getSize() {
        return tags.size();
    }

    public Object getElementAt( int index ) {
        return tags.get( index );
    }

    public void addListDataListener( ListDataListener l ) {
        listeners.add( l );
    }

    public void removeListDataListener( ListDataListener l ) {
        listeners.remove( l );
    }
    
    /**
     * Set the filter for possible completions
     * @param filter 
     */
    public void setFilter( String filter ) {
        this.filter = filter;
        filterTags();
    }
    
    /**
     * Filter tags using current filter
     */
    private void filterTags() {
        System.err.println( "filterTags(" + filter + ")" );
        int oldSize = tags.size();
        List<Tag> filtered = new ArrayList<Tag>();
        if ( filter == null || filter.length() == 0 ) {
            tags = allTags;
            fireContentsChanged( oldSize );
            return;
        } 
        for ( Tag t : allTags ) {
            if ( t.getName().startsWith( filter ) ) {
                filtered.add( t );
            }
        }
        tags = filtered;
        fireContentsChanged( oldSize );
    }

    private void fireContentsChanged( int oldSize ) {
        final ListDataEvent e = new ListDataEvent( 
                this, ListDataEvent.CONTENTS_CHANGED, 0, oldSize );
        SwingUtilities.invokeLater( new Runnable() {

            public void run() {
                for ( ListDataListener l : listeners ) {
                    l.contentsChanged( e );
                }
            }
        } );
    }
}
