package photovault.swingui.folderpane;

import javax.swing.tree.*;
import javax.swing.*;
import javax.swing.event.TreeModelListener;
import photovault.swingui.*;
import imginfo.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Collection;
import java.util.Iterator;
import photovault.folder.*;

public class FolderController extends FieldController {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( FolderController.class.getName() );

    DefaultTreeModel treeModel;
    
    public FolderController( PhotoInfo[] model ) {
	super( model );
	addedToFolders = new HashSet();
	removedFromFolders = new HashSet();
	treeModel = new DefaultTreeModel( new DefaultMutableTreeNode() );
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
	DefaultMutableTreeNode node = addFolder( f );
	FolderNode fn = (FolderNode) node.getUserObject();
	fn.addAllPhotos();
	
	// Notify the tree model that representation of this node may
	// be changed
	treeModel.nodeChanged( node );
    }
    /**
       Mark all photos in model so that they will be removed from specified folder
       when committing the changes.
    */

    public void removeAllFromFolder( PhotoFolder f ) {
	addedToFolders.remove( f );
	removedFromFolders.add( f );
	
	// Remove the folder from the tree
	DefaultMutableTreeNode treeNode =
	    (DefaultMutableTreeNode) folderNodes.get( f );
	DefaultMutableTreeNode parentNode
	    = (DefaultMutableTreeNode) treeNode.getParent();

	int idx = parentNode.getIndex( treeNode );
	parentNode.remove( treeNode );
	int[] idxs = new int[1];
	idxs[0] = idx;
	Object[] nodes = new Object[1];
	nodes[0] = treeNode;
	treeModel.nodesWereRemoved( parentNode, idxs, nodes );
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
       Hash table that maps PhotoFolders into nodes in trees.
    */
    HashMap folderNodes = new HashMap();

    /** Initializes the JTree object's model based on folders 
	the objects in model belong to
    */
    protected void initTree() {
	folderNodes = new HashMap();
	topNode = null;
	// 	topNode = new DefaultMutableTreeNode( PhotoFolder.getRoot() );
// 	treeModel.setRoot( topNode );
	if ( model == null ) {
	    return;
	}
	
	// Add all photos in the model to folder tree
	for ( int n = 0; n < model.length; n++ ) {
	    addPhotoToTree( (PhotoInfo) model[n] );
	}
    }

    void addPhotoToTree( PhotoInfo photo ) {
	Collection folders = photo.getFolders();
	Iterator iter = folders.iterator();
	while ( iter.hasNext() ) {
	    PhotoFolder folder = (PhotoFolder) iter.next();
	    DefaultMutableTreeNode node = addFolder( folder );
	    FolderNode fn = (FolderNode) node.getUserObject();
	    fn.addPhoto( photo );
	}
    }

    void expandPath( TreePath path ) {
	TreeModelListener listeners[] = treeModel.getTreeModelListeners();
	for ( int n = 0; n < listeners.length; n++ ) {
	    if (listeners[n] instanceof JTree ) {
		((JTree) listeners[n]).expandPath( path );
	    }
	}
    }
	

    /**
       Recursively walk through all parents of a folder until such a folder is found
       that is already part of the model (or we find the root folder. Then add this folder
       to the tree.
    */
    DefaultMutableTreeNode addFolder( PhotoFolder folder ) {
	if ( folderNodes.containsKey( folder ) ) {
	    // This folder is already added to tree
	    return (DefaultMutableTreeNode) folderNodes.get( folder );
	}

	FolderNode fn = new FolderNode( model, folder );
	DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode( fn );
	folderNodes.put( folder, folderNode );

	PhotoFolder parent = folder.getParentFolder();
	if ( parent == null ) {
	    // this is the root folder
	    if ( topNode != null ) {
		log.error( "Found a 2nd top folder" );
		return null;
	    }
	    topNode = folderNode;
	    treeModel.setRoot( topNode );
	} else {
	    DefaultMutableTreeNode parentNode = addFolder( parent );
	    treeModel.insertNodeInto( folderNode, parentNode, parentNode.getChildCount() );
// 	    expandPath( new TreePath( folderNode.getPath() ) );
	}

	return folderNode;
    }
}