// $Id: PhotoFolderTreeModel.java,v 1.1 2003/02/22 13:10:55 kaimio Exp $

package photovault.swingui;

import photovault.folder.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import imginfo.*;
import java.util.*;


/**
   This class implements a TreeModel for PhotoFolders.
*/

public class PhotoFolderTreeModel implements TreeModel {
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
    
    /**
       Sets the root folder for the tree
       @param root the new root folder
    */
    public void setRoot( PhotoFolder root ) {
	rootFolder = root;
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

}
  
