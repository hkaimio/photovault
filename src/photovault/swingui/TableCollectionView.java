// TableCollectionView.java

package photovault.swingui;

import imginfo.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.io.*;
import java.text.*;
import java.util.*;
import javax.swing.table.*;

/**
   TableColelctionView implements a simple table based interface for viewing PhotoCollections.
   It is primarily intended for testing purposes.
*/
public class TableCollectionView extends JPanel implements ActionListener {

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
	    return collection.getPhotoCount();
	}

	public int getColumnCount() { return columnNames.length; }
	public Object getValueAt(int row, int col) { 
	    PhotoInfo photo = collection.getPhoto( row );
	    switch( col ) {
	    case 0:
		return photo.getPhotographer();
	    case 1:
		return photo.getShootTime();
	    case 2:
		return photo.getShootingPlace();
	    case 3:
		return photo.getDescription();
	    }
	    return null;
	}

	public PhotoInfo getPhoto( int row ) {
	    return collection.getPhoto( row );
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
     * @param  <description>
     */
    public void actionPerformed(ActionEvent e)
    {
	String cmd = e.getActionCommand();
	if ( cmd == PHOTO_PROPS_CMD ) {
	    showSelectionPropsDialog();
	} else if ( cmd == PHOTO_SHOW_CMD ) {
	    showSelectedPhoto();
	}
    }

    /**
       Show the PhotoInfoEditor dialog for the selected photo
    */
    public void showSelectionPropsDialog() {
	PhotoInfo photo = getSelected();

	if ( photo != null ) {
	    // TODO: Change PhotoInfoEditor to a dialog!!!
	    final JFrame frame = new JFrame( "Photo properties" );
	    PhotoInfoController ctrl = new PhotoInfoController();
	    PhotoInfoEditor editor = new PhotoInfoEditor( ctrl );
	    ctrl.setPhoto( photo );
	    frame.getContentPane().add( editor, BorderLayout.CENTER );
	    frame.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
			frame.dispose( );
		    }
		} );
	    frame.pack();
	    frame.setVisible( true );
	}
    }

    /**
       Shows the selected photo in a popup window
    */
    public void showSelectedPhoto() {
	PhotoInfo photo = getSelected();
	if ( photo != null ) {
	    final JFrame frame = new JFrame( "Photo" );
	    PhotoViewer viewer = new PhotoViewer();
	    frame.getContentPane().add( viewer, BorderLayout.CENTER );
	    frame.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
			frame.dispose();
		    }
		} );
	    viewer.setPhoto( photo );
	    frame.pack();
	    frame.setVisible( true );
	}
    }
    
    /**
       Returns the PhotoInfo object that is described on the currently selected line or null
       if nothing is selected
    */
    public PhotoInfo getSelected() {
	ListSelectionModel selection = table.getSelectionModel();
	int selectedRow = selection.getMinSelectionIndex();
	PhotoInfo photo = null;
	if ( selectedRow >= 0 ) {
	    photo = model.getPhoto( selectedRow );
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
	setLayout( new BorderLayout() );
	table = new JTable( model );
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
	popup.add( showItem );
	popup.add( propsItem );
	MouseListener popupListener = new PopupListener();
	table.addMouseListener( popupListener );

    }

    // Popup menu actions
    private static final String PHOTO_PROPS_CMD = "photoProps";
    private static final String PHOTO_SHOW_CMD = "photoShow";
    
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
    
