// $Id: PhotoFolderTree.java,v 1.3 2003/02/23 21:44:44 kaimio Exp $


package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.swing.tree.*;
import imginfo.*;
import photovault.folder.*;

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
	model.setRoot( PhotoFolder.getRoot() );
	createUI();
    }

    private void createUI() {
	setLayout( new BorderLayout() );
	tree = new JTree( model );
	tree.getSelectionModel().setSelectionMode( TreeSelectionModel.SINGLE_TREE_SELECTION );
	tree.addTreeSelectionListener( this );
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
	JMenuItem newFolderItem = new JMenuItem( "New folder..." );
	newFolderItem.addActionListener( this );
	newFolderItem.setActionCommand( FOLDER_NEW_CMD );
	popup.add( newFolderItem );
	popup.add( renameItem );
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
    static final String FOLDER_NEW_CMD = "FOLDER_NEW_CMD";


    /**
       Implementation of TreeSelectionListener interface. This method is called when tree selection changes
    */

    public void valueChanged( TreeSelectionEvent e ) {
	selected = (PhotoFolder)tree.getLastSelectedPathComponent();
	if ( selected != null ) {
	    log.debug( "Tree selection changed, new selction = " + selected.toString() );
	} else {
	    log.debug( "Selected nothing!!" );
	}
    }
    
    /**
     * ActionListener implementation, is called when a popup menu item is selected
     * @param  <description>
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
	    PhotoFolder newFolder = PhotoFolder.create( "New folder", selected );
	}
    }
    
    void renameSelectedFolder() {
	log.warn( "Not implemented: renameSelectedFolder()" );
    }
    
    
    
    public static void main( String[] args ) {
	org.apache.log4j.BasicConfigurator.configure();
	log.setLevel( org.apache.log4j.Level.DEBUG );

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

