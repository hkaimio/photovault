// $Id

package photovault.swingui;


/**
   PhotoFolderTreeListener defines an interface via which an object can be nodified
   that a selection in a tree has changed
*/

public interface PhotoFolderTreeListener {

    /**
       This method is called when the tree selection is changed.
    */
    public void photoFolderTreeSelectionChanged( PhotoFolderTreeEvent evt );
}
