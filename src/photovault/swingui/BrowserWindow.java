// Browserwindow.java

package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;

public class BrowserWindow extends JPanel {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( BrowserWindow.class.getName() );
    
    /**
       Constructor
    */
    public BrowserWindow() {
	super();
	createUI();
    }

    protected void createUI() {
	tabPane = new JTabbedPane();
	queryPane = new QueryPane();
	treePane = new PhotoFolderTree();
	tabPane.addTab( "Query", queryPane );
	tabPane.addTab( "Folders", treePane );
	viewPane = new TableCollectionView();

	viewPane.setCollection( queryPane.getResultCollection() );

	// Set listeners to both query and folder tree panes

	/*
	  If an actionEvent comes from queryPane & the viewed folder is
	  no the query resouts, swich to it (the result folder will be nodified of
	  changes to quert parameters directly
	*/
	queryPane.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    if ( viewPane.getCollection() != queryPane.getResultCollection() ) {
			viewPane.setCollection( queryPane.getResultCollection() );
		    }
		}
	    } );

	/*
	  If the selected folder is changed in treePane, switch to that immediately
	*/
	treePane.addPhotoFolderTreeListener( new PhotoFolderTreeListener() {
		public void photoFolderTreeSelectionChanged( PhotoFolderTreeEvent e ) {
		    viewPane.setCollection( e.getSelected() );
		}
	    } );
	
	// Create the split pane to display both of these components
	JSplitPane split = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, tabPane, viewPane );
	setLayout( new BorderLayout() );
	add( split, BorderLayout.CENTER );
    }

    protected JTabbedPane tabPane = null;
    protected QueryPane queryPane = null;
    protected PhotoFolderTree treePane = null;
    
    
    protected TableCollectionView viewPane = null;

    /**
       Simple main program for testing the compnent
    */
    public static void main( String [] args ) {
	// Configure logging
	//	BasicConfigurator.configure();
	try {
	    org.apache.log4j.lf5.DefaultLF5Configurator.configure();
	} catch ( Exception e ) {}
	log.info( "Starting application" );
	
	JFrame frame = new JFrame( "BrowserWindow test" );
	BrowserWindow br = new BrowserWindow();
	frame.getContentPane().add( br, BorderLayout.CENTER );
	frame.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    } );
	frame.pack();
	frame.setVisible( true );
    }

    
}
