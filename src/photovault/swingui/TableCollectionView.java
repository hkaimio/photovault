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
public class TableCollectionView extends JPanel {

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
    class CollectionTableModel extends AbstractTableModel {
	
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
	public boolean isCellEditable(int row, int col) {
	    return false;
	}
	public void setValueAt(Object value, int row, int col) {
	}


	public void setCollection( PhotoCollection c ) {
	    collection = c;
	}

	public PhotoCollection getCollection() {
	    return collection;
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

    JTable table = null;

    void createUI() {
	setLayout( new BorderLayout() );
	table = new JTable( model );
	JScrollPane scroll = new JScrollPane( table );
	add( scroll, BorderLayout.CENTER );
    }

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
    
