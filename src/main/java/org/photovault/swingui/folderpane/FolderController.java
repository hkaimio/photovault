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

package org.photovault.swingui.folderpane;

import javax.swing.tree.*;
import org.photovault.imginfo.PhotoCollectionChangeEvent;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.swingui.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;
import org.photovault.folder.*;

public class FolderController extends FieldController {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( FolderController.class.getName() );

    PhotoFolderTreeModel treeModel;
    FolderToFolderNodeMapper nodeMapper;
    
    public FolderController( PhotoInfo[] model ) {
	super( model );
	addedToFolders = new HashSet();
	removedFromFolders = new HashSet();
        initTree();
    }

    protected void setModelValue( Object model ) {
    }

    protected Object getModelValue( Object model ) {
	return null;
    }
    
    protected void updateView( Object view ) {
    }
    
    protected void updateViewMultivalueState( Object view ) {
    }
    
    protected void updateValue( Object view ) {
    }

    public void setViews( Collection views ) {
	this.views = views;
	updateAllViews();
    }

    public void updateAllViews() {
	Iterator iter = views.iterator();
	while ( iter.hasNext() ) {
	    PhotoInfoView view = (PhotoInfoView) iter.next();
	    view.setFolderTreeModel( treeModel );
	}
    }

    
    DefaultMutableTreeNode topNode = null;

    public DefaultMutableTreeNode getRootNode() {
	return topNode;
    }

    public void setModel( Object[] model, boolean preserveState ) {
	this.model = model;
	if ( !preserveState && addedToFolders != null  ) {
	    addedToFolders.clear();
	    removedFromFolders.clear();
	}
	initTree();
    }

    HashSet addedToFolders;
    HashSet removedFromFolders;
    
    /**
       Mark all photos in model so that they will be added to specified folder
       when committing the changes.
    */
    public void addAllToFolder( PhotoFolder f ) {
	addedToFolders.add( f );
	removedFromFolders.remove( f );
	FolderNode fn = (FolderNode)nodeMapper.mapFolderToNode( f );
	fn.addAllPhotos();
	
	// Notify the tree model that representation of this node may
	// be changed;
        PhotoCollectionChangeEvent e = new PhotoCollectionChangeEvent( f );
        treeModel.photoCollectionChanged( e );
    }
    /**
       Mark all photos in model so that they will be removed from specified folder
       when committing the changes.
    */

    public void removeAllFromFolder( PhotoFolder f ) {
	addedToFolders.remove( f );
	removedFromFolders.add( f );
	
        FolderNode fn = (FolderNode) nodeMapper.mapFolderToNode( f );
        fn.removeAllPhotos();
        PhotoCollectionChangeEvent e = new PhotoCollectionChangeEvent( f );
        treeModel.photoCollectionChanged( e );        
    }

    /**
       Saves all modifications to database
    */
    public void save() {
	Iterator iter = addedToFolders.iterator();
	while ( iter.hasNext() ) {
	    PhotoFolder folder = (PhotoFolder) iter.next();
	    for ( int n = 0; n < model.length; n++ ) {
		folder.addPhoto( (PhotoInfo) model[n] );
	    }
	}
	iter = removedFromFolders.iterator();
	while ( iter.hasNext() ) {
	    PhotoFolder folder = (PhotoFolder) iter.next();
	    for ( int n = 0; n < model.length; n++ ) {
		folder.removePhoto( (PhotoInfo) model[n] );
	    }
	}	
    }
    
    /**
       Hash table that maps PhotoFolders into nodes in the tree.
    */
    HashMap folderNodes = new HashMap();

    /** Initializes the JTree object's model based on folders 
	the objects in model belong to
    */
    protected void initTree() {
        /*
         TODO: Currently this is called several times while initializing the dialog.
         Optimize!!!
         */
        nodeMapper = new FolderToFolderNodeMapper( model );
        treeModel = new PhotoFolderTreeModel( nodeMapper );
        treeModel.setRoot( PhotoFolder.getRoot() );        
        if ( model != null ) {
            // Add all photos in the model to folder tree
            for ( int n = 0; n < model.length; n++ ) {
                // if the model is empty it can contain a null
                // TODO: this is IMHO a hack - passing null up to this point is certainly
                // not elegant and there might be even more error opportunities
                if ( model[n] != null ) {
                    addPhotoToTree( (PhotoInfo) model[n] );
                }
            }
        }
        if ( views != null ) {
            updateAllViews();
        }
    }

    void addPhotoToTree( PhotoInfo photo ) {
	Collection folders = photo.getFolders();
	Iterator iter = folders.iterator();
	while ( iter.hasNext() ) {
	    PhotoFolder folder = (PhotoFolder) iter.next();
	    FolderNode fn = (FolderNode) nodeMapper.mapFolderToNode( folder );
	    fn.addPhoto( photo );
	}
    }
	

//    /**
//     *Add a folder into the tree. If folder's parent has not yet been added, 
//       walk recursively through all parents until such a folder is found
//       that is already part of the model (or we find the root folder)
//     *@param folder The folder that will be added to the tree.
//     *@return Parent of the folder
//    */
//    DefaultMutableTreeNode addFolder( PhotoFolder folder ) {
//	if ( folderNodes.containsKey( folder ) ) {
//	    // This folder is already added to tree
//	    return (DefaultMutableTreeNode) folderNodes.get( folder );
//	}
//
//	FolderNode fn = new FolderNode( model, folder );
//	DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode( fn );
//	folderNodes.put( folder, folderNode );
//
//	PhotoFolder parent = folder.getParentFolder();
//	if ( parent == null ) {
//	    // this is the root folder
//	    if ( topNode != null ) {
//		log.error( "Found a 2nd top folder" );
//		return null;
//	    }
//	    topNode = folderNode;
//	    treeModel.setRoot( topNode );
//           
//	} else {
//	    DefaultMutableTreeNode parentNode = addFolder( parent );
//	    treeModel.insertNodeInto( folderNode, parentNode, parentNode.getChildCount() );
//	}
//
//	return folderNode;
//    }
}