// QueryPane.java

package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import imginfo.*;

/**
   QueryPane implements the container inlcudes the UI componets used to edit the queiry parameters
*/

public class QueryPane extends JPanel implements ActionListener {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( QueryPane.class.getName() );

    public QueryPane() {
	super();
	createUI();
	query = new DateRangeQuery();
    }

    private void createUI() {
	setLayout( new BoxLayout( this, BoxLayout.Y_AXIS ) );
	shootingDateRange = new DateRangeQueryEditor();
	add( shootingDateRange );

	// Add search button to the bottom
	JButton searchButton = new JButton( "Serach" );
	searchButton.setActionCommand( SEARCH_CMD );
	searchButton.addActionListener( this );
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout( new BoxLayout( buttonPane, BoxLayout.X_AXIS ) );
	buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
	buttonPane.add(Box.createHorizontalGlue());
	buttonPane.add( searchButton );
	add( buttonPane );

	// Add glue so that the empty space will go to the bottom
	add( Box.createVerticalGlue() );
    }

    public void actionPerformed( ActionEvent e ) {
	if ( e.getActionCommand() == SEARCH_CMD ) {
	    log.debug( "Action performed" );
	    updateQuery();
	}
    }

    static final String SEARCH_CMD = "search";

    public PhotoCollection getResultCollection() {
	return query;
    }

    
    protected void updateQuery() {
	query.setStartDate( shootingDateRange.getStartDate() );
	query.setEndDate( shootingDateRange.getEndDate() );
    }

    DateRangeQueryEditor shootingDateRange = null;
    DateRangeQuery query = null;
    
    public static void main( String [] args ) {
	JFrame frame = new JFrame( "PhotoInfoEditorTest" );
	QueryPane qp = new QueryPane();
	frame.getContentPane().add( qp, BorderLayout.CENTER );
	frame.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    } );
	frame.pack();
	frame.setVisible( true );
    }

}
