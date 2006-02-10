// TableCollectionView.java

package photovault.swingui;

import org.photovault.imginfo.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.table.*;
import org.photovault.imginfo.DateRangeQuery;
import org.photovault.imginfo.PhotoCollection;
import org.photovault.imginfo.PhotoCollectionChangeEvent;
import org.photovault.imginfo.PhotoCollectionChangeListener;
import org.photovault.imginfo.PhotoInfo;
import photovault.folder.*;

/**
   TableCollectionView implements a simple table based interface for viewing PhotoCollections.
   It is primarily intended for testing purposes.
*/
public class TableCollectionView extends JPanel implements ActionListener {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( TableCollectionView.class.getName() );

    /**
       Constructor
    */
    public TableCollectionView() {
	super();
	model = new CollectionTableModel();
	createUI();
    }

    /**
       Table model for PhotoCollection
    */
    class CollectionTableModel extends AbstractTableModel implements PhotoCollectionChangeListener {
	
	String[]  columnNames = {
	    "Photo",
	    "Photographer",
	    "Shooting time",
	    "Sooting place",
	    "Description"
	};
	PhotoCollection collection;

	public String getColumnName(int col) { 
	    return columnNames[col]; 
	}
	public int getRowCount() {
	    if ( collection == null ) {
		return 0;
	    }
	    int photoCount = collection.getPhotoCount();
	    int rowCount = photoCount/5;
	    if ( rowCount*5 < photoCount ) {
		rowCount++;
	    }
	    return rowCount;
	}

	public int getColumnCount() {
	    return columnNames.length;
	}
	
	public Object getValueAt(int row, int col) { 
	    return getPhoto( row, col );
// 	    switch( col ) {
// 	    case 0:
// 		return photo;
// 	    case 1:
// 		return photo.getPhotographer();
// 	    case 2:
// 		return photo.getShootTime();
// 	    case 3:
// 		return photo.getShootingPlace();
// 	    case 4:
// 		return photo.getDescription();
// 	    }
// 	    return null;
	}

	public Class getColumnClass( int col ) {
	    return PhotoInfo.class;
// 	    switch (col) {
// 	    case 0:
// 		return PhotoInfo.class;
// 	    case 1:
// 	    case 3:
// 	    case 4:
// 		return String.class;
// 	    case 2:
// 		return Date.class;
// 	    }
// 	    return Object.class;
	}
	
	public PhotoInfo getPhoto( int row ) {
	    return collection.getPhoto( row );
	}

	public PhotoInfo getPhoto( int row, int col ) {
	    int photoNum = row*5 + col;
	    if ( photoNum < collection.getPhotoCount() ) {
		return collection.getPhoto( photoNum );
	    }
	    return null;
	}
	    
	public boolean isCellEditable(int row, int col) {
	    return false;
	}
	public void setValueAt(Object value, int row, int col) {
	}


	public void setCollection( PhotoCollection c ) {
	    if ( collection != null ) {
		collection.removePhotoCollectionChangeListener( this );
	    }
	    collection = c;
	    collection.addPhotoCollectionChangeListener( this );
	    fireTableDataChanged();
	}

	public PhotoCollection getCollection() {
	    return collection;
	}

	public void photoCollectionChanged( PhotoCollectionChangeEvent e ) {
	    fireTableDataChanged();
	}
    }

    /**
       Reference to the currently used table model
    */
    CollectionTableModel model = null;
    
    /**
     * Get the collection that is currently viewed
     * @return value of collection.
     */
    public PhotoCollection getCollection() {
	return model.getCollection();
    }
    
    /**
     * Set the collection that should be viewed
     * @param v  Value to assign to collection.
     */
    public void setCollection(PhotoCollection  v) {
	model.setCollection( v );
    }

    // implementation of java.awt.event.ActionListener interface

    /**
     * ActionListener implementation, is called when a popup menu item is selected
     * @param e The event object
     */
    public void actionPerformed(ActionEvent e)
    {
	String cmd = e.getActionCommand();
	if ( cmd == PHOTO_PROPS_CMD ) {
	    showSelectionPropsDialog();
	} else if ( cmd == PHOTO_SHOW_CMD ) {
	    showSelectedPhoto();
	} else if ( cmd == PHOTO_ROTATE_CW_CMD ) {
	    rotateSelectedPhoto( 90 );
	} else if ( cmd == PHOTO_ROTATE_CCW_CMD ) {
	    rotateSelectedPhoto( -90 );
	} else if ( cmd == PHOTO_ROTATE_180_CMD ) {
	    rotateSelectedPhoto( 180 );
	} else if ( cmd == PHOTO_ADD_TO_FOLDER_CMD ) {
	    queryForNewFolder();
	}
    }

    PhotoInfoDlg propertyDlg = null;
    
    /**
       Show the PhotoInfoEditor dialog for the selected photo
    */
    public void showSelectionPropsDialog() {
	PhotoInfo photo = getSelected();
	
	// Try to find the frame in which this component is in
	Frame frame = null;
	Container c = getTopLevelAncestor();
	if ( c instanceof Frame ) {
	    frame = (Frame) c;
	}

	if ( photo != null ) {
	    if (propertyDlg == null ) {
		propertyDlg = new PhotoInfoDlg( frame, true, photo );
	    } else {
		propertyDlg.setPhoto( photo );
	    }

	    propertyDlg.showDialog();
	}
    }
    
    JFrame frame = null;
    PhotoViewer viewer = null;

    /**
       Shows the selected photo in a popup window
    */
    public void showSelectedPhoto() {
	try {
	PhotoInfo photo = getSelected();
	if ( photo != null ) {
 	    JFrame frame = new JFrame( "Photo" );
 	    final PhotoViewer viewer = new PhotoViewer();
	    frame.getContentPane().add( viewer, BorderLayout.CENTER );
	    frame.setDefaultCloseOperation( JFrame.DISPOSE_ON_CLOSE );

	    // This is a WAR for a memory management problem. For some
	    // reason the frame and objects owned by it Seem not to be
	    // garbage collected. So we free the large image buffers
	    // to avoid massive memory leak.

	    frame.addWindowListener( new WindowAdapter() {
		    public void windowClosing( WindowEvent e ) {
			viewer.setPhoto( null );
		    }
		} );
	    log.warn( "Created frame" );
	    viewer.setPhoto( photo );
	    log.warn( "Photo set" );
	    frame.pack();
	    log.warn( "Frame packed" );
	    frame.setVisible( true );
	    log.warn( "Frame visible" );
	}
	} catch ( Throwable e ) {
	    System.err.println( "Out of memory error" );
	    log.warn( e );
	    e.printStackTrace();
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
	    // A folder was selected, so add the selected photo to this folder
	    PhotoInfo photo = getSelected();
	    PhotoFolder folder = dlg.getSelectedFolder();
	    if ( photo != null ) {
		folder.addPhoto ( photo );
	    }
	}

    }

    /**
       Rotate selected photo by specified amount
       @param rot Rotation in degrees, positive means clockwise
    */
    public void rotateSelectedPhoto( double rot ) {
	PhotoInfo photo = getSelected();
	if ( photo != null ) {
	    double curRot = photo.getPrefRotation();
	    photo.setPrefRotation( curRot + rot );
	}
    }
    
    /**
       Returns the PhotoInfo object that is described on the currently selected line or null
       if nothing is selected
    */
    public PhotoInfo getSelected() {
	int selectedRow = table.getSelectedRow();
	int selectedCol = table.getSelectedColumn();
	PhotoInfo photo = null;
	if ( selectedRow >= 0 ) {
	    photo = model.getPhoto( selectedRow, selectedCol );
	}
	return photo;
    }
	
    JTable table = null;
    JPopupMenu popup = null;

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
    
    
    void createUI() {
	// Create the table
	setLayout( new BorderLayout() );
	table = new JTable( model );
	ThumbnailTableRenderer thumbRenderer = new ThumbnailTableRenderer();
	table.setDefaultRenderer( PhotoInfo.class, thumbRenderer );
	table.setCellSelectionEnabled( true );
	table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION );
	int rowHeight = (int)thumbRenderer.getPreferredSize().getHeight();
	table.setRowHeight( rowHeight );
	JScrollPane scroll = new JScrollPane( table );
	add( scroll, BorderLayout.CENTER );

	// Create the popup menu
	popup = new JPopupMenu();
	JMenuItem propsItem = new JMenuItem( "Properties" );
	propsItem.addActionListener( this );
	propsItem.setActionCommand( PHOTO_PROPS_CMD );
	JMenuItem showItem = new JMenuItem( "Show image" );
	showItem.addActionListener( this );
	showItem.setActionCommand( PHOTO_SHOW_CMD );
	JMenuItem rotateCW = new JMenuItem( "Rotate 90 deg CW" );
	rotateCW.addActionListener( this );
	rotateCW.setActionCommand( PHOTO_ROTATE_CW_CMD );
	JMenuItem rotateCCW = new JMenuItem( "Rotate 90 deg CCW" );
	rotateCCW.addActionListener( this );
	rotateCCW.setActionCommand( PHOTO_ROTATE_CCW_CMD );
	JMenuItem rotate180deg = new JMenuItem( "Rotate 180 degrees" );
	rotate180deg.addActionListener( this );
	rotate180deg.setActionCommand( PHOTO_ROTATE_180_CMD );
	JMenuItem addToFolder = new JMenuItem( "Add to folder..." );
	addToFolder.addActionListener( this );
	addToFolder.setActionCommand( PHOTO_ADD_TO_FOLDER_CMD );
	popup.add( showItem );
	popup.add( propsItem );
	popup.add( rotateCW );
	popup.add( rotateCCW );
	popup.add( rotate180deg );
	popup.add( addToFolder );
	MouseListener popupListener = new PopupListener();
	table.addMouseListener( popupListener );

    }

    private void setupPhotoInfoRenderer() {
	table.setDefaultRenderer( PhotoInfo.class, new ThumbnailTableRenderer() );
    }
    
    // Popup menu actions
    private static final String PHOTO_PROPS_CMD = "photoProps";
    private static final String PHOTO_SHOW_CMD = "photoShow";
    private static final String PHOTO_ROTATE_CW_CMD = "rotateCW";
    private static final String PHOTO_ROTATE_CCW_CMD = "rotateCCW";
    private static final String PHOTO_ROTATE_180_CMD = "rotate180";
    private static final String PHOTO_ADD_TO_FOLDER_CMD = "addToFolder";
    
    
    /**
       Simple test program
    */
    public static void main( String[] args ) {
	JFrame frame = new JFrame( "TableCollectionView test" );
	TableCollectionView photoTbl = new TableCollectionView();
	frame.getContentPane().add( photoTbl, BorderLayout.CENTER );
	frame.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    } );

	// Create a query to view
	DateRangeQuery qry = new DateRangeQuery();
	Calendar cal = Calendar.getInstance();
	cal.set( 2002, 11, 24 );
	qry.setStartDate( cal.getTime() );
	photoTbl.setCollection( qry );
	
	frame.pack();
	frame.setVisible( true );
    }
    
}
    
