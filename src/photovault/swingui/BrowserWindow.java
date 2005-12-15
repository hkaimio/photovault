// Browserwindow.java

package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import imginfo.*;
import photovault.folder.*;
import org.apache.log4j.Logger;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.PropertyConfigurator;

public class BrowserWindow extends JFrame implements SelectionChangeListener {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( BrowserWindow.class.getName() );
    
    /**
       Constructor
    */
    public BrowserWindow() {
	super( "Photovault Browser");
	SwingUtilities.invokeLater(new java.lang.Runnable() {
		public void run() {
		    createUI();
		}
	    });
	
    }

    protected void createUI() {
	setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );
	tabPane = new JTabbedPane();
	queryPane = new QueryPane();
	treePane = new PhotoFolderTree();
	tabPane.addTab( "Query", queryPane );
	tabPane.addTab( "Folders", treePane );
	//	viewPane = new TableCollectionView();
	viewPane = new PhotoCollectionThumbView();
        viewPane.addSelectionChangeListener( this );
        previewPane = new JAIPhotoViewer();

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
	viewScroll = new JScrollPane( viewPane );
        
        collectionPane = new JPanel();
        collectionPane.add( viewScroll );
        collectionPane.add( previewPane );
        
        setupLayoutPreviewWithHorizontalIcons();
        
 //       JSplitPane collectionSplitPane = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, viewScroll, previewPane );
	JSplitPane split = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, tabPane, collectionPane );
	Container cp = getContentPane();
	cp.setLayout( new BorderLayout() );
	cp.add( split, BorderLayout.CENTER );

	// Create the menu bar & menus
	JMenuBar menuBar = new JMenuBar();
	setJMenuBar( menuBar );
	// File menu
	JMenu fileMenu = new JMenu( "File" );
	fileMenu.setMnemonic(KeyEvent.VK_F);
	menuBar.add( fileMenu );
	
	JMenuItem newWindowItem = new JMenuItem( "New window", KeyEvent.VK_N );
	newWindowItem.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    BrowserWindow br = new BrowserWindow();
		    //		    br.setVisible( true );
		}
	    });
	fileMenu.add( newWindowItem );

	JMenuItem importItem = new JMenuItem( "Import image...", KeyEvent.VK_I );
	importItem.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    importFile();
		}
	    });
	fileMenu.add( importItem );

	JMenuItem exportItem = new JMenuItem( viewPane.getExportSelectedAction() );
	fileMenu.add( exportItem );
        
	JMenuItem exitItem = new JMenuItem( "Exit", KeyEvent.VK_X );
	exitItem.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    System.exit( 0 );
		}
	    });	
	fileMenu.add( exitItem );

        JMenu viewMenu = new JMenu( "View" );
        viewMenu.setMnemonic( KeyEvent.VK_V );
        menuBar.add( viewMenu );
        
	JMenuItem vertIconsItem = new JMenuItem( "Preview on Right", KeyEvent.VK_R );
	vertIconsItem.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    setupLayoutPreviewWithVerticalIcons();
		}
	    });
	viewMenu.add( vertIconsItem );
        
	JMenuItem horzIconsItem = new JMenuItem( "Preview on Top", KeyEvent.VK_T );
	horzIconsItem.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    setupLayoutPreviewWithHorizontalIcons();
		}
	    });
	viewMenu.add( horzIconsItem );
        
	JMenuItem noPreviewItem = new JMenuItem( "No preview", KeyEvent.VK_T );
	noPreviewItem.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    setupLayoutNoPreview();
		}
	    });
	viewMenu.add( noPreviewItem );
        
        JMenuItem nextPhotoItem = new JMenuItem( "Next photo", KeyEvent.VK_N );
	nextPhotoItem.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    viewPane.selectNextPhoto();
		}
	    });
	viewMenu.add( nextPhotoItem );
        
        JMenuItem prevPhotoItem = new JMenuItem( "Previous photo", KeyEvent.VK_P );
	prevPhotoItem.addActionListener( new ActionListener() {
		public void actionPerformed( ActionEvent e ) {
		    viewPane.selectPreviousPhoto();
		}
	    });
	viewMenu.add( prevPhotoItem );
        
        
	JMenu imageMenu = new JMenu( "Image" );
	imageMenu.setMnemonic(KeyEvent.VK_I);
	menuBar.add( imageMenu );

	imageMenu.add( new JMenuItem( viewPane.getEditSelectionPropsAction() ) );
	imageMenu.add( new JMenuItem( viewPane.getShowSelectedPhotoAction() ) );
	imageMenu.add( new JMenuItem( viewPane.getRotateCWActionAction() ) );
	imageMenu.add( new JMenuItem( viewPane.getRotateCCWActionAction() ) );
	imageMenu.add( new JMenuItem( viewPane.getRotate180degActionAction() ) );
        
        JMenu aboutMenu = new JMenu( "About" );
        aboutMenu.setMnemonic( KeyEvent.VK_A );
        aboutMenu.add( new JMenuItem( new ShowAboutDlgAction( "About Photovault...", null, "", null ) ) );
        
        menuBar.add( Box.createHorizontalGlue() );
        menuBar.add( aboutMenu );
	pack();
	setVisible( true );
    }

    /**
     * Sets up the window layout so that the collection is displayed as one vertical
     * column with preview image on right
     */
    protected void setupLayoutPreviewWithVerticalIcons() {
        GridBagLayout layout = new GridBagLayout();
        collectionPane.setLayout( layout );

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.weightx = 0.0;
        c.gridx = 0;

        // Minimum size is the size of one thumbnail
        viewScroll.setMinimumSize( new Dimension( 150, 150 ));
        viewScroll.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        viewScroll.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED );
        layout.setConstraints( viewScroll, c );
        viewPane.setColumnCount( 1 );
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        // c.gridheight = GridBagConstraints.REMAINDER;
        c.gridx = 1;
        c.weightx = 1.0;
        layout.setConstraints( previewPane, c );
        previewPane.setVisible( true );
        validate();
    }
    
     /**
     * Sets up the window layout so that the collection is displayed as one horizontal
     * row with preview image above it.
     */
    protected void setupLayoutPreviewWithHorizontalIcons() {
        GridBagLayout layout = new GridBagLayout();
        collectionPane.setLayout( layout );

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 0.0;
        c.weightx = 1.0;
        c.gridy = 1;

        // Minimum size is the size of one thumbnail
        viewScroll.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED );
        viewScroll.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER );
        viewScroll.setMinimumSize( new Dimension( 150, 200 ));
        layout.setConstraints( viewScroll, c );
        viewPane.setRowCount( 1 );
        
        c.gridwidth = GridBagConstraints.REMAINDER;
        // c.gridheight = GridBagConstraints.REMAINDER;
        c.gridy = 0;
        c.weighty = 1.0;
        layout.setConstraints( previewPane, c );
        previewPane.setVisible( true );
        validate();
    }   
    
    public void setupLayoutNoPreview() {
        GridBagLayout layout = new GridBagLayout();
        collectionPane.setLayout( layout );

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weighty = 1.0;
        c.weightx = 1.0;
        c.gridy = 0;

        // Minimum size is the size of one thumbnail
        viewScroll.setHorizontalScrollBarPolicy( ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER );
        viewScroll.setVerticalScrollBarPolicy( ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED );
        viewScroll.setMinimumSize( new Dimension( 150, 200 ));
        layout.setConstraints( viewScroll, c );
        viewPane.setRowCount( -1 );
        viewPane.setColumnCount( -1 );
        
        previewPane.setVisible( false );
        validate();        
    }
    
    /** 
	Shows an file selection dialog that allows user to select a
	file to import. After that shows the PhotoInfo dialog to allow the
	user to edit the eriginal information about the file. 
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


    /**
       Exports the selected images to folder outside the database.
    */
    protected void exportSelected() {
	// Show the file chooser dialog
	JFileChooser fc = new JFileChooser();
	fc.addChoosableFileFilter( new ImageFilter() );
	fc.setAccessory( new ImagePreview( fc ) );
	
	int retval = fc.showDialog( this, "Export image" );
	if ( retval == JFileChooser.APPROVE_OPTION ) {
	    File exportFile = fc.getSelectedFile();
	    Collection selection = viewPane.getSelection();
	    if ( selection != null ) {
		Iterator iter = selection.iterator();
		if ( iter.hasNext() ) {
		    PhotoInfo photo = (PhotoInfo) iter.next();
		    photo.exportPhoto( exportFile, 400, 400 );
		}
	    }
	}
    }
    
    /**
     *Selection in Thumb view has changed. Is a single photo is selected, show 
     * it in preview pane, otherwise set preview empty.
     */
    public void selectionChanged( SelectionChangeEvent e ) {
       Collection selection = viewPane.getSelection();
       if ( selection.size() == 1 ) {
           Cursor oldCursor = getCursor();
           setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
           PhotoInfo selected = (PhotoInfo) (selection.toArray())[0];
           previewPane.setPhoto( selected );
           setCursor( oldCursor );
       } else {
           previewPane.setPhoto( null );
       }
    }
    protected JTabbedPane tabPane = null;
    protected JTabbedPane collectionTabPane = null;
    protected JScrollPane viewScroll = null;
    protected JPanel collectionPane = null;
    protected QueryPane queryPane = null;
    protected PhotoFolderTree treePane = null;
    
    
    
    //    protected TableCollectionView viewPane = null;
    protected PhotoCollectionThumbView viewPane = null;
    protected JAIPhotoViewer previewPane = null;

    
    /**
       Simple main program for testing the compnent
    */
    public static void main( String [] args ) {
	// Configure logging
	//	BasicConfigurator.configure();
	PropertyConfigurator.configure( "conf/log4j.properties" );
	// 	try {
// 	    org.apache.log4j.lf5.DefaultLF5Configurator.configure();
// 	} catch ( Exception e ) {}
// 	log.info( "Starting application" );

	
	BrowserWindow br = new BrowserWindow();
    }

    
}
