package photovault.swingui.folderpane;


import javax.swing.*;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeModelEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.*;
import photovault.swingui.PhotoFolderSelectionDlg;
import photovault.folder.PhotoFolder;

public class FolderTreePane extends JPanel implements TreeModelListener, ActionListener  {

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
	addFolderBtn.setActionCommand( ADD_ALL_TO_FOLDER_CMD );
	addFolderBtn.addActionListener( this );
	c = new GridBagConstraints();
	c.gridx = 1;
	c.gridy = 0;
	add( addFolderBtn, c );


	popup = new JPopupMenu();
	JMenuItem addAllItem = new JMenuItem( "Add photos" );
	addAllItem.addActionListener( this );
	addAllItem.setActionCommand( ADD_ALL_TO_THIS_FOLDER_CMD );
	JMenuItem removeAllItem = new JMenuItem( "Remove photos" );
	removeAllItem.addActionListener( this );
	removeAllItem.setActionCommand( REMOVE_ALL_FROM_THIS_FOLDER_CMD );
	popup.add( addAllItem );
	popup.add( removeAllItem );

	MouseListener popupListener = new PopupListener();
	folderTree.addMouseListener( popupListener );
	
    }

    /**
       This helper class from Java Tutorial handles displaying of popup menu on correct mouse events
    */
    class PopupListener extends MouseAdapter {
	public void mousePressed(MouseEvent e) {
	    maybeShowPopup(e);
	}
	
	public void mouseReleased(MouseEvent e) {
	    maybeShowPopup(e);
	}
	
	private void maybeShowPopup(MouseEvent e) {
	    if (e.isPopupTrigger()) {
		popup.show(e.getComponent(),
			   e.getX(), e.getY());
	    }
	}
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

    public void actionPerformed(ActionEvent e) {
        String cmd = e.getActionCommand();
	if ( cmd == ADD_ALL_TO_FOLDER_CMD ) {
            queryForNewFolder();
        } else if ( cmd == ADD_ALL_TO_THIS_FOLDER_CMD ) {
	    addAllToSelectedFolder();
	} else if ( cmd == REMOVE_ALL_FROM_THIS_FOLDER_CMD ) {
	    removeAllFromSelectedFolder();
	    
	}
    }


    /**
       Queries the user for a new folder into which the photo will be added.
    */
    public void queryForNewFolder() {
        // Try to find the frame in which this component is in
        Frame frame = null;
        Container c = getTopLevelAncestor();
        if ( c instanceof Frame ) {
            frame = (Frame) c;
        }

        PhotoFolderSelectionDlg dlg = new PhotoFolderSelectionDlg( frame, true );
        if ( dlg.showDialog() ) {
            PhotoFolder folder = dlg.getSelectedFolder();
            // A folder was selected, so add the selected photo to this folder
	    ctrl.addAllToFolder( folder );
        }
    }

    /** returns the selected folder or null if none selected
     */
    PhotoFolder getSelectedFolder() {
	PhotoFolder selected = null;
	TreePath path = folderTree.getSelectionPath();
	if ( path != null ) {
	    DefaultMutableTreeNode treeNode =
		(DefaultMutableTreeNode) path.getLastPathComponent();
	    FolderNode node = (FolderNode) treeNode.getUserObject();
	    selected = node.getFolder();
	}
	return selected;
    }

    void addAllToSelectedFolder() {
	PhotoFolder selected = getSelectedFolder();
	if ( selected != null ) {
	    ctrl.addAllToFolder( selected );
	}
    }

    void removeAllFromSelectedFolder() {
	PhotoFolder selected = getSelectedFolder();
	if ( selected != null ) {
	    ctrl.removeAllFromFolder( selected );
	}
    }


    
    JTree folderTree = null;
    JPopupMenu popup;
    FolderController ctrl = null;

    private static final String ADD_ALL_TO_FOLDER_CMD = "addAllToFolder";
    private static final String ADD_ALL_TO_THIS_FOLDER_CMD = "addAllToThisFolder";
    private static final String REMOVE_ALL_FROM_THIS_FOLDER_CMD = "removeAllFromThisFolder";
}