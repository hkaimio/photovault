// Browserwindow.java

package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import imginfo.*;
import photovault.folder.*;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;

public class BrowserWindow extends JFrame {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( BrowserWindow.class.getName() );
    
    /**
       Constructor
    */
    public BrowserWindow() {
	super( "Photovault Browser");
	createUI();
	addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    } );
    }

    protected void createUI() {
	tabPane = new JTabbedPane();
	queryPane = new QueryPane();
	treePane = new PhotoFolderTree();
	tabPane.addTab( "Query", queryPane );
	tabPane.addTab( "Folders", treePane );
	//	viewPane = new TableCollectionView();
	viewPane = new PhotoCollectionThumbView();

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
		    PhotoFolder f = e.getSelected();
		    if ( f != null ) {
			viewPane.setCollection( f );
		    }
		}
	    } );
	
	// Create the split pane to display both of these components
	JScrollPane viewScroll = new JScrollPane( viewPane );
	JSplitPane split = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, tabPane, viewScroll );
	Container cp = getContentPane();
	cp.setLayout( new BorderLayout() );
	cp.add( split, BorderLayout.CENTER );

	// Create the menu bar & menus
	JMenuBar menuBar = new JMenuBar();
	setJMenuBar( menuBar );
	JMenu fileMenu = new JMenu( "File" );
	fileMenu.setMnemonic(KeyEvent.VK_F);
	menuBar.add( fileMenu );
	
	JMenuItem importItem = new JMenuItem( "Import image...", KeyEvent.VK_I );
	importItem.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    importFile();
		}
	    });
	fileMenu.add( importItem );

	JMenuItem exitItem = new JMenuItem( "Exit", KeyEvent.VK_X );
	exitItem.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    System.exit( 0 );
		}
	    });	
	fileMenu.add( exitItem );
	pack();
    }

    /**
       Shows an file selection dialog that allows user to select a file to import. After
       that shows the PhotoInfo dialog to allow the user to edit the eriginal information
       about the file.
    */
    protected void importFile() {
	// Show the file chooser dialog
	JFileChooser fc = new JFileChooser();
	fc.addChoosableFileFilter( new ImageFilter() );
	fc.setAccessory( new ImagePreview( fc ) );
	fc.setMultiSelectionEnabled( true );
	
	int retval = fc.showDialog( this, "Import" );
	if ( retval == JFileChooser.APPROVE_OPTION ) {
	    // Add the selected file to the database and allow user to edit its attributes
	    final File[] files = fc.getSelectedFiles();

	    final ProgressDlg pdlg = new ProgressDlg( this, true );
	    
	    // Add all the selected files to DB
	    final PhotoInfo[] photos = new PhotoInfo[files.length];
	    Thread importThread = new Thread() {
		    public void run() {

			for ( int n = 0; n < files.length; n++ ) {
			    try {
				photos[n] = PhotoInfo.addToDB( files[n] );
				pdlg.setProgressPercent( (n*100) / files.length );
				pdlg.setStatus( "" + (n+1) + " of " + files.length + " files imported." );
			    } catch ( Exception e ) {
				log.error( "Unexpected exception: " + e.getMessage() );
			    }
			}
			pdlg.completed();
		    }
		};
	    
	    importThread.start();
	    pdlg.setVisible( true );
	    
	    // Show editor dialog for the added photos
	    PhotoInfoDlg dlg = new PhotoInfoDlg( this, false, photos );
	    dlg.showDialog();
	}
    }

    
    protected JTabbedPane tabPane = null;
    protected QueryPane queryPane = null;
    protected PhotoFolderTree treePane = null;
    
    
  //    protected TableCollectionView viewPane = null;
  protected PhotoCollectionThumbView viewPane = null;
  
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
	
	BrowserWindow br = new BrowserWindow();
	br.setVisible( true );
    }

    
}
