// Browserwindow.java

package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class BrowserWindow extends JPanel {

    /**
       Constructor
    */
    public BrowserWindow() {
	super();
	createUI();
    }

    protected void createUI() {
	queryPane = new QueryPane();
	viewPane = new TableCollectionView();

	viewPane.setCollection( queryPane.getResultCollection() );
	// Create the split pane to display both of these components
	JSplitPane split = new JSplitPane( JSplitPane.HORIZONTAL_SPLIT, queryPane, viewPane );
	setLayout( new BorderLayout() );
	add( split, BorderLayout.CENTER );
    }

    protected QueryPane queryPane = null;
    protected TableCollectionView viewPane = null;

    /**
       Simple main program for testing the compnent
    */
    public static void main( String [] args ) {
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
