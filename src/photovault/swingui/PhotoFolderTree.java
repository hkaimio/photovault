// $Id: PhotoFolderTree.java,v 1.7 2004/01/12 21:10:25 kaimio Exp $


package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.dnd.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import org.photovault.imginfo.*;
import photovault.folder.*;
import java.util.*;

/**
   PhotoFolderTree is a control for displaying a tree structure of a PhotoFolder and its subfolders.
*/

public class PhotoFolderTree extends JPanel implements TreeSelectionListener, ActionListener {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoFolderTree.class.getName() );

    JTree tree = null;
    PhotoFolderTreeModel model = null;
    JScrollPane scrollPane = null;
    JPopupMenu popup = null;
    PhotoFolder selected = null;
    
    public PhotoFolderTree() {
	super();
	model = new PhotoFolderTreeModel();
	PhotoFolder root = PhotoFolder.getRoot();
	log.warn( "Root folder" + root );
	model.setRoot( root );
	createUI();
    }

    /**
       Returns the currently selected PhotoFolder or <code>null</code> if none is selected.
    */
    public PhotoFolder getSelected() {
	return selected;
    }

    /**
       Adds a listener to listen for selection changes
    */
    public void addPhotoFolderTreeListener( PhotoFolderTreeListener l ) {
	folderTreeListeners.add( l );
    }

    /**
       removes a listener for selection changes
    */
    public void removePhotoFolderTreeListener( PhotoFolderTreeListener l ) {
	folderTreeListeners.remove( l );
    }

    protected void fireSelectionChangeEvent() {
	log.debug( "fireselectionChangedEvent " + selected );
	Iterator iter = folderTreeListeners.iterator();
	while ( iter.hasNext() ) {
	    PhotoFolderTreeListener l = (PhotoFolderTreeListener) iter.next();
	    l.photoFolderTreeSelectionChanged( new PhotoFolderTreeEvent( this, selected ) );
	}
    }
    
    Vector folderTreeListeners = new Vector();
    
    private void createUI() {
	setLayout( new BorderLayout() );
	tree = new JTree( model );
	tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );
	tree.addTreeSelectionListener( this );
	DropTarget dropTarget = new DropTarget( tree, new PhotoTreeDropTargetListener( tree ) );
	// 	tree.setTransferHandler( new PhotoCollectionTransferHandler(null) );
	scrollPane = new JScrollPane( tree );
	scrollPane.setPreferredSize( new Dimension( 200, 500 ) );
	add( scrollPane, BorderLayout.CENTER );

	// Set up the popup menu
	popup = new JPopupMenu();
	JMenuItem propsItem = new JMenuItem( "Properties" );
	propsItem.addActionListener( this );
	propsItem.setActionCommand( FOLDER_PROPS_CMD );
	JMenuItem renameItem = new JMenuItem( "Rename" );
	renameItem.addActionListener( this );
	renameItem.setActionCommand( FOLDER_RENAME_CMD );
	JMenuItem deleteItem = new JMenuItem( "Delete" );
	deleteItem.addActionListener( this );
	deleteItem.setActionCommand( FOLDER_DELETE_CMD );
	JMenuItem newFolderItem = new JMenuItem( "New folder..." );
	newFolderItem.addActionListener( this );
	newFolderItem.setActionCommand( FOLDER_NEW_CMD );
	popup.add( newFolderItem );
	popup.add( renameItem );
	popup.add( deleteItem );
	popup.add( propsItem );

	MouseListener popupListener = new PopupListener();
	tree.addMouseListener( popupListener );
	
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
    
    static final String FOLDER_PROPS_CMD = "FOLDER_PROPS_CMD";
    static final String FOLDER_RENAME_CMD = "FOLDER_RENAME_CMD";
    static final String FOLDER_DELETE_CMD = "FOLDER_DELETE_CMD";
    static final String FOLDER_NEW_CMD = "FOLDER_NEW_CMD";


    /**
       Implementation of TreeSelectionListener interface. This method is called when tree selection changes
    */

    public void valueChanged( TreeSelectionEvent e ) {
	selected = (PhotoFolder)tree.getLastSelectedPathComponent();
	fireSelectionChangeEvent();
	if ( selected != null ) {
	    log.debug( "Tree selection changed, new selction = " + selected.toString() );
	} else {
	    log.debug( "Selected nothing!!" );
	}
    }
    
    /**
     * ActionListener implementation, is called when a popup menu item is selected
     * @param  e The event object
     */
    public void actionPerformed(ActionEvent e)
    {
	String cmd = e.getActionCommand();
	if ( cmd == FOLDER_PROPS_CMD ) {
	    showSelectionPropsDialog();
	} else if ( cmd == FOLDER_NEW_CMD ) {
	    createNewFolder();
	} else if ( cmd == FOLDER_RENAME_CMD ) {
	    renameSelectedFolder();
	} else if ( cmd == FOLDER_DELETE_CMD ) {
	    deleteSelectedFolder();
	} else {
	    log.warn( "Unknown action: " + cmd );
	}
    }

    void showSelectionPropsDialog() {
	log.warn( "Not implemented: showSelectionPropsDialog()" );
    }

    /**
       Creates a new subfolder for the selected folder
    */
    void createNewFolder() {
	if ( selected != null ) {
	    String newName = (String) JOptionPane.showInputDialog( this, "Enter name for new folder",
								   "New folder", JOptionPane.PLAIN_MESSAGE,
								   null, null, "New folder" );
	    if ( newName != null ) {
	    
		PhotoFolder newFolder = PhotoFolder.create( newName, selected );
	    }
	}
    }
    
    /**
       Deletes the selected folder
    */
    void deleteSelectedFolder() {
	if ( selected != null ) {
	    // Ask for confirmation
	    if ( JOptionPane.showConfirmDialog( this, "Delete folder " + selected.getName() + "?",
						"Delete folder", JOptionPane.YES_NO_OPTION,
						JOptionPane.WARNING_MESSAGE, null )
		 == JOptionPane.YES_OPTION ) {
		selected.delete();
		selected = null;
		fireSelectionChangeEvent();
	    }
	}
    }
    void renameSelectedFolder() {
	if ( selected != null ) {
	    String origName = selected.getName();
	    String newName = (String) JOptionPane.showInputDialog( this, "Enter new name",
								   "Rename folder", JOptionPane.PLAIN_MESSAGE,
								   null, null, origName );
	    if ( newName != null ) {
		PhotoFolder f = selected;
		f.setName( newName );
		log.debug( "Changed name to " + newName );
	    }
	}
    }
    
    
    public static void main( String[] args ) {
	org.apache.log4j.BasicConfigurator.configure();
	log.setLevel( org.apache.log4j.Level.DEBUG );
	org.apache.log4j.Logger folderLog = org.apache.log4j.Logger.getLogger( PhotoFolder.class.getName() );
	folderLog.setLevel( org.apache.log4j.Level.DEBUG );
	
	JFrame frame = new JFrame( "PhotoFoldertree test" );
	PhotoFolderTree view = new PhotoFolderTree();
	frame.getContentPane().add( view, BorderLayout.CENTER );
	frame.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    } );

		
	frame.pack();
	frame.setVisible( true );

    }
}

