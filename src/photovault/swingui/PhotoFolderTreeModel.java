// $Id: PhotoFolderTreeModel.java,v 1.3 2004/01/12 21:10:25 kaimio Exp $

package photovault.swingui;

import photovault.folder.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import imginfo.*;
import java.util.*;


/**
   This class implements a TreeModel for PhotoFolders.
*/

public class PhotoFolderTreeModel implements TreeModel, PhotoFolderChangeListener {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoFolderTreeModel.class.getName() );

    // implementation of javax.swing.tree.TreeModel interface

    /**
     * Returns a child of a given node
     * @param obj <description>
     * @param childNum <description>
     * @return <description>
     */
    public Object getChild(Object obj, int childNum)
    {
	PhotoFolder folder = (PhotoFolder) obj;
	return folder.getSubfolder( childNum );
    }

    PhotoFolder rootFolder = null;
    
    /**'
       Sets the root folder for the tree
       @param root the new root folder
    */
    public void setRoot( PhotoFolder root ) {
	if ( rootFolder != null ) {
	    rootFolder.removePhotoCollectionChangeListener( this );
	}
	rootFolder = root;
	rootFolder.addPhotoCollectionChangeListener( this );
	log.warn( "New root " + root.getName() + " - subfolderCount: " + getChildCount( root ) );
    }
    
    /**
     *
     * @return <description>
     */
    public Object getRoot()
    {
	return rootFolder;
    }

    /**
     * Returns the number of subfolders a given node has
     * @param obj The PhotoFolder we are interested in
     * @return Number of subfolders <code>obj</code> has.
     */
    public int getChildCount(Object obj)
    {
	PhotoFolder folder = (PhotoFolder) obj;
	log.debug( "childCount for " + folder.getName() + ": " + folder.getSubfolderCount() );
	return folder.getSubfolderCount();
    }

    /**
     * Returns true if the given subfolder has no subfolders
     * @param obj The PhotoFolder to inspect
     */
    public boolean isLeaf(Object obj)
    {
	PhotoFolder folder = (PhotoFolder) obj;
	int subfolderCount = folder.getSubfolderCount();
	log.debug( "subfolderCount for " + folder.getName() + ": " + subfolderCount );
	return (subfolderCount == 0);
    }

    /**
     *
     * @param param1 <description>
     * @param param2 <description>
     */
    public void valueForPathChanged(TreePath param1, Object param2)
    {
	// TODO: implement this javax.swing.tree.TreeModel method
    }

    /**
     * Get the index of a given child
     * @param parent The parent folder
     * @param child the child folder
     * @return index of the child folder inside the parent. If parent or child in <code>null</code> returns -1.
     */
    public int getIndexOfChild(Object parent, Object child)
    {
	PhotoFolder parentFolder = (PhotoFolder) parent;
	int childIndex = -1;
	if ( parent != null && child != null ) {
	    int childCount = parentFolder.getSubfolderCount();
	    for ( int n = 0; n < childCount; n++ ) {
		if ( parentFolder.getSubfolder( n ) == child ) {
		    childIndex = n;
		    break;
		}
	    }
	}
	return childIndex;
    }


    private Vector treeModelListeners = new Vector();
    
    /**
     * Adds a listener for the TreeModelEvent posted after the tree changes.
     */
    public void addTreeModelListener(TreeModelListener l) {
        treeModelListeners.addElement(l);
    }
    
    /**
     * Removes a listener previously added with addTreeModelListener().
     */
    public void removeTreeModelListener(TreeModelListener l) {
        treeModelListeners.removeElement(l);
    }

    /**
       Sends a strucure changed event to all listeners of this object. Currently only this type of
       event is supported, in future also other types of tree model events should be considered.
    */
      
    protected void fireTreeModelEvent( TreeModelEvent e ) {
	Iterator iter = treeModelListeners.iterator();
	while ( iter.hasNext() ) {
	    TreeModelListener l = (TreeModelListener) iter.next();
	    log.warn( "Sending treeModelEvent" );
	    l.treeStructureChanged( e );
	}
    }
    
    // implementation of imginfo.PhotoCollectionChangeListener interface

    /**
       This method is called when some of the tree nodes has changed. Currently it constructs TreePath to
       the changed object and sends a strucure changed event to listeners of the model.

     */
    public void photoCollectionChanged(PhotoCollectionChangeEvent e)
    {
	PhotoFolder changedFolder = (PhotoFolder)e.getSource();
	if ( e instanceof PhotoFolderEvent  ){
	    changedFolder = ((PhotoFolderEvent)e).getSubfolder();
	}
	PhotoFolder[] path = findFolderPath( changedFolder );
	
	// Construct the correct event
	TreeModelEvent treeEvent = new TreeModelEvent( changedFolder, path );
	log.warn( "collectionChanged " + path.length );
	fireTreeModelEvent( treeEvent );
    }

    public void subfolderChanged( PhotoFolderEvent e ) {
	PhotoFolder changedFolder = (PhotoFolder)e.getSource();
	if ( e instanceof PhotoFolderEvent ) {
	    changedFolder = ((PhotoFolderEvent)e).getSubfolder();
	}
	PhotoFolder[] path = findFolderPath( changedFolder );
	
	// Construct the correct event
	TreeModelEvent treeEvent = new TreeModelEvent( changedFolder, path );
	log.warn( "subfolderChanged " + path.length );
	fireTreeModelEvent( treeEvent );
	
    }

    public void structureChanged( PhotoFolderEvent e ) {
	PhotoFolder changedFolder = (PhotoFolder)e.getSource();
	if ( e instanceof PhotoFolderEvent ) {
	    changedFolder = ((PhotoFolderEvent)e).getSubfolder();
	}
	PhotoFolder[] path = findFolderPath( changedFolder );
	
	// Construct the correct event
	TreeModelEvent treeEvent = new TreeModelEvent( changedFolder, path );
	log.warn( "structureChanged " + path.length );
	fireTreeModelEvent( treeEvent );
    }

    protected PhotoFolder[] findFolderPath( PhotoFolder folder ) {
	// Construct a TreePath for this object

	// Number of ancestors between this 
	Vector ancestors = new Vector();
	// Add first the final folder
	ancestors.add( folder );
	log.warn( "starting finding path for " + folder.getName() );
	PhotoFolder ancestor = folder.getParentFolder();
	while ( ancestor != rootFolder && ancestor != null ) {
	    log.warn( "ancestor " + ancestor.getName() );
	    ancestors.add( ancestor );
	    ancestor = ancestor.getParentFolder();
	}
	// Add the root folder to the ancestors
	if ( ancestor == rootFolder ) {
	    ancestors.add( ancestor );
	}

	// Now ancestors has the path but in reversed order.
	PhotoFolder[] path = new PhotoFolder[ ancestors.size() ];
	for ( int m = 0; m < ancestors.size(); m++ ) {
	    path[m] = (PhotoFolder) ancestors.get( ancestors.size()-1-m );
	}
	return path;
    }	

}
  
