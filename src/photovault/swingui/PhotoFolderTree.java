// $Id


package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import imginfo.*;
import photovault.folder.*;

/**
   PhotoFolderTree is a control for displaying a tree structure of a PhotoFolder and its subfolders.
*/

public class PhotoFolderTree extends JPanel {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoFolderTree.class.getName() );

    JTree tree = null;
    PhotoFolderTreeModel model = null;
    JScrollPane scrollPane = null;
    
    public PhotoFolderTree() {
	super();
	model = new PhotoFolderTreeModel();
	model.setRoot( PhotoFolder.getRoot() );
	createUI();
    }

    private void createUI() {
	setLayout( new BorderLayout() );
	tree = new JTree( model );
	scrollPane = new JScrollPane( tree );
	scrollPane.setPreferredSize( new Dimension( 200, 500 ) );
	add( scrollPane, BorderLayout.CENTER );
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

