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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.Tag;
import org.photovault.swingui.selection.PhotoSelectionController;
import org.photovault.swingui.selection.PhotoSelectionView;

/**
 * Controller for handling state changes for tag field. As tag field contains
 * actually a list of items, some of which might apply to all photos in model
 * and some for just part of them, the normal single/multivalued logic is not
 * applicable. Therefore a special controller is needed
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class TagController {

    ListModel listModel;
    private final PhotoSelectionController parentCtrl;
    Set<Tag> addedTags = new HashSet();
    Set<Tag> removedTags = new HashSet();

    /**
     * Structure for storing needed information about a tag that is present in
     * some of the photos in model.
     */
    static class TagInfo {
        /**
         * The tag
         */
        Tag tag;
        /**
         * Is tag assigned to all photos in model
         */
        boolean isForAll = false;
        public String toString() {
            return tag.toString() + (isForAll ? "*" : "" );
        }
    }

    /**
     * Constructor
     * @param parentCtrl
     */
    public TagController( PhotoSelectionController parentCtrl ) {
        this.parentCtrl = parentCtrl;
        listModel = new DefaultListModel();

    }

    Collection<PhotoSelectionView> views = null;

    /**
     * Add a tag to all photos in model
     * @param tag
     */
    public void addTag( Tag tag ) {
        removedTags.remove( tag );
        addedTags.add( tag );
        parentCtrl.getChangeCommand().addToSet( "tags", tag );
        boolean didExist = false;
        for ( int n = 0 ; n < listModel.getSize() ; n++ ) {
            TagInfo ti = (TagInfo) listModel.getElementAt( n );
            if ( tag.equals( ti.tag ) ) {
                ti.isForAll = true;
                didExist = true;
            }
        }
        if ( !didExist ) {
            TagInfo ti = new TagInfo();
            ti.tag = tag;
            ti.isForAll = true;
        }
        updateAllViews();
    }

    /**
     * Remove a tag from all photos in the model
     * @param tag
     */
    public void removeTag( Tag tag ) {
        addedTags.remove( tag );
        removedTags.add(  tag );
        parentCtrl.getChangeCommand().removeFromSet( "tags", tag );
        for ( int n = 0 ; n < listModel.getSize() ; n++ ) {
            TagInfo ti = (TagInfo) listModel.getElementAt( n );
            if ( tag.equals( ti.tag ) ) {
                ((DefaultListModel) listModel).remove( n );
                break;
            }
        }
    }

    public void setPhotos( PhotoInfo[] photos ) {
        listModel = new DefaultListModel();
        if ( photos != null ) {
            Map<Tag, Integer> tags = new HashMap();

            for ( PhotoInfo p : photos ) {
                for ( Tag tag : p.getTags() ) {
                    int count = tags.containsKey( tag ) ? tags.get( tag ) : 0;
                    tags.put( tag, count + 1 );
                }
            }
            for ( Map.Entry<Tag, Integer> e : tags.entrySet() ) {
                TagInfo ti = new TagInfo();
                ti.tag = e.getKey();
                ti.isForAll = (e.getValue().equals( photos.length ));
                ((DefaultListModel) listModel).addElement( ti );
            }
        }
        if ( views != null ) {
            updateAllViews();
        }
    }

    public void setViews( Collection views ) {
	this.views = views;
	updateAllViews();
    }

    void updateAllViews() {
        for( PhotoSelectionView view : views ) {
            view.setTagListModel( listModel );
        }
    }


}
