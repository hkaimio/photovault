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
	basicFields = new BasicQueryFieldEditor();
	add( basicFields );
	dateCB = new JCheckBox( "Shooting date", false );
	dateCB.setActionCommand( DATE_CB_CHANGED );
	dateCB.addActionListener( this );
	add( dateCB );
	shootingDateRange = new DateRangeQueryEditor();
	shootingDateRange.setVisible( false );
	add( shootingDateRange );

	// Add search button to the bottom
	JButton searchButton = new JButton( "Search" );
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
	} else if ( e.getActionCommand() == DATE_CB_CHANGED ) {
	    shootingDateRange.setVisible( dateCB.isSelected() );
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
    static final String DATE_CB_CHANGED = "date visibility changed";

    JCheckBox dateCB = null;
    
    public PhotoCollection getResultCollection() {
	return query;
    }

    
    protected void updateQuery() {

	query.clear();
	String photographer = basicFields.getPhotographer();
	if( photographer.length() > 0 ) {
	    query.setLikeCriteria( PhotoQuery.FIELD_PHOTOGRAPHER,
				   photographer );
	}
	String desc = basicFields.getDescription();
	if( desc.length() > 0 ) {
	    query.setLikeCriteria( PhotoQuery.FIELD_DESCRIPTION,
				   desc );
	}
	String shootingPlace = basicFields.getShootingPlace();
	if( shootingPlace.length() > 0 ) {
	    query.setLikeCriteria( PhotoQuery.FIELD_SHOOTING_PLACE,
				   shootingPlace );
	}
	if ( dateCB.isSelected() ) {
	    query.setFieldCriteriaRange( PhotoQuery.FIELD_SHOOTING_TIME,
					 shootingDateRange.getStartDate(), shootingDateRange.getEndDate() );
	}
    }

    DateRangeQueryEditor shootingDateRange = null;
    BasicQueryFieldEditor basicFields = null;
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
