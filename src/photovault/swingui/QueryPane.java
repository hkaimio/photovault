// QueryPane.java

package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import imginfo.*;
import java.util.*;

/**
   QueryPane implements the container inlcudes the UI componets used to edit the queiry parameters
*/

public class QueryPane extends JPanel implements ActionListener {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( QueryPane.class.getName() );

    public QueryPane() {
	super();
	createUI();
	query = new PhotoQuery();
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
	    fireActionEvent( SEARCH_CMD );
	}
    }

    public void addActionListener( ActionListener l ) {
	actionListeners.add( l );
    }

    public void remoceActionListener( ActionListener l ) {
	actionListeners.remove( l );
    }

    Vector actionListeners = new Vector();

    protected void fireActionEvent( String action ) {
	Iterator iter = actionListeners.iterator();
	while ( iter.hasNext() ) {
	    ActionListener l = (ActionListener) iter.next();
	    l.actionPerformed( new ActionEvent( this, ActionEvent.ACTION_PERFORMED, action ) );
	}
    }
	
    

    static final String SEARCH_CMD = "search";

    public PhotoCollection getResultCollection() {
	return query;
    }

    
    protected void updateQuery() {
	query.setFieldCriteriaRange( PhotoQuery.FIELD_SHOOTING_TIME,
				     shootingDateRange.getStartDate(), shootingDateRange.getEndDate() );
	String photographer = shootingDateRange.getPhotographer();
	if( photographer.length() > 0 ) {
	    query.setLikeCriteria( PhotoQuery.FIELD_PHOTOGRAPHER,
				   photographer );
	}
    }

    DateRangeQueryEditor shootingDateRange = null;
    PhotoQuery query = null;
    
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
