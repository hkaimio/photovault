package photovault.swingui.folderpane;


import javax.swing.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;
import java.awt.*;

public class FolderTreePane extends JPanel implements TreeModelListener  {

    public FolderTreePane( FolderController ctrl ) {
	super();
	createUI();
	this.ctrl = ctrl;
    }


    void createUI() {
	setLayout(new GridBagLayout());
	folderTree = new JTree(  );
 	folderTree.setRootVisible( false );
	folderTree.setShowsRootHandles( true );
	folderTree.setEditable( true );
	JScrollPane scrollPane = new JScrollPane( folderTree );
 	scrollPane.setPreferredSize( new Dimension( 300, 300 ) );

	GridBagConstraints c = new GridBagConstraints();
	c.gridx = 0;
	c.gridy = 0;
	c.gridheight = GridBagConstraints.REMAINDER;
	c.fill = GridBagConstraints.BOTH;
	
	add( scrollPane, c );
	
	JButton addFolderBtn = new JButton( "Add to folder..." );
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	add( addFolderBtn, c );
	
    }

    public void setFolderTreeModel( TreeModel model ) {
	TreeModel oldModel = folderTree.getModel();
	if ( oldModel != null ) {
	    oldModel.removeTreeModelListener( this );
	}
	folderTree.setModel( model );
	model.addTreeModelListener( this );
    }

    public void treeNodesInserted( TreeModelEvent e ) {
	TreePath parentPath = e.getTreePath();
	folderTree.expandPath( parentPath );
    }
    
    public void treeNodesRemoved( TreeModelEvent e ) {

    }

    public void treeNodesChanged( TreeModelEvent e ) {

    }

    public void treeStructureChanged( TreeModelEvent e ) {

    }

    
    JTree folderTree = null;
    FolderController ctrl = null;

}