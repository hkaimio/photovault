// PhotoInfoEditor.java


package photovault.swingui;


import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import imginfo.*;
import dbhelper.*;
import javax.swing.event.*;

/** PhotoInfoEditor provides a GUI interface for creating of modifying PhotoInfo records in the database.
    Use can either edit an existing record or create a completely new record.
*/

public class PhotoInfoEditor extends JPanel implements PhotoInfoView, ActionListener, DocumentListener {

    public PhotoInfoEditor( PhotoInfoController ctrl ) {
	super();
	createUI();
	this.ctrl = ctrl;
    }
    
    protected void createUI() {
	
	// Create the fields & their labels

	// Photographer field
	JLabel photographerLabel = new JLabel( "Photographer" );
	photographerField = new JTextField( 30 );
	photographerDoc = photographerField.getDocument();
	photographerDoc.addDocumentListener( this );

	// Shooting date field
	JLabel shootingDayLabel = new JLabel( "Shooting date" );
	DateFormat df = DateFormat.getDateInstance();
	DateFormatter shootingDayFormatter = new DateFormatter( df );
	shootingDayField = new JFormattedTextField( df );
	shootingDayField.setColumns( 10 );
	shootingDayField.setValue( new Date( ));
	
	// Shooting place field
	JLabel shootingPlaceLabel =  new JLabel( "Shooting place" );
	shootingPlaceField = new JTextField( 30 );
	shootingPlaceDoc = shootingPlaceField.getDocument();
	shootingPlaceDoc.addDocumentListener( this );
	
	// Descrription text
	JLabel descLabel = new JLabel( "Description" );
	descriptionTextArea = new JTextArea( 5, 40 );
	descriptionTextArea.setLineWrap( true );
	descriptionTextArea.setWrapStyleWord( true );
	JScrollPane descScrollPane = new JScrollPane( descriptionTextArea );
	descScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
	Border descBorder = BorderFactory.createEtchedBorder( EtchedBorder.LOWERED );
	descBorder = BorderFactory.createTitledBorder( descBorder, "Description" );
        descScrollPane.setBorder( descBorder );

	// Save button
	JButton saveBtn = new JButton( "Save" );
	saveBtn.setActionCommand( "save" );
	saveBtn.addActionListener( this );

	// Discard button
	JButton discardBtn = new JButton( "Discard" );
	discardBtn.setActionCommand( "discard" );
	discardBtn.addActionListener( this );
	
	// Lay out the created controls
	GridBagLayout layout = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	setLayout( layout );
	JLabel[] labels     = { photographerLabel, shootingDayLabel, shootingPlaceLabel };
	JTextField[] fields = { photographerField, shootingDayField, shootingPlaceField };
	addLabelTextRows( labels, fields, layout, this );

	
	add( descScrollPane );
	c.gridwidth = GridBagConstraints.REMAINDER;
	c.weighty = 0.5;
	c.fill = GridBagConstraints.NONE;
	layout.setConstraints( descScrollPane, c );

	c = new GridBagConstraints();
	c.gridwidth = 1;
	c.weighty = 0;
	c.fill = GridBagConstraints.NONE;
	c.gridy = GridBagConstraints.RELATIVE;
	
	add( saveBtn );
	layout.setConstraints( saveBtn, c );

	c.gridy = GridBagConstraints.RELATIVE;
	
	add( discardBtn );
	layout.setConstraints( discardBtn, c );

    }

    public void setPhotographer( String newValue ) {
	photographerField.setText( newValue );
    }
    
    public void setShootTime( Date newValue ) {
	shootingDayField.setValue( newValue );
    }
    public void setShootPlace( String newValue ) {
	shootingPlaceField.setText( newValue );
    }
    
    
    // Important UI components
    JTextField photographerField = null;
    Document photographerDoc = null;
    JFormattedTextField shootingDayField = null;
    Document shootingDayDoc = null;
    JTextField shootingPlaceField = null;
    Document shootingPlaceDoc = null;
    JTextArea descriptionTextArea = null;
    Document descriptionDoc = null;

    public void actionPerformed( ActionEvent evt ) {
	if ( evt.getActionCommand().equals( "save" ) ) {
	    System.out.println( "Saving data" );
	    try {
		ctrl.save();
	    } catch ( Exception e ) {
	    }
	} else if ( evt.getActionCommand().equals( "discard" ) ) {
	    System.out.println( "Discarding data" );
	    ctrl.discard();
	}
    }

    // DocumentListener interface implementation
    public void changedUpdate( DocumentEvent ev ) {
    }

    public void insertUpdate( DocumentEvent ev ) {
	Document changedDoc = ev.getDocument();
	String changedField = null;
	Object newValue = null;
	if ( changedDoc == photographerDoc ) {
	    changedField = PhotoInfoController.PHOTOGRAPHER;
	    newValue = photographerField.getText();
	    System.err.println( "New photographer: " + newValue );
	} else if ( changedDoc == shootingDayDoc ) {
  	    changedField = PhotoInfoController.SHOOTING_DATE;
	    newValue = shootingDayField.getValue();
	} else if ( changedDoc == shootingPlaceDoc ) {
  	    changedField = PhotoInfoController.SHOOTING_PLACE;
	    newValue = shootingPlaceField.getText();
	} else {
	    System.err.println( "insertUpdate from unknown event!!!" );
	}
	ctrl.setField( changedField, newValue );
    }

    public void removeUpdate( DocumentEvent ev ) {
	insertUpdate( ev );
    }
    
    private void addLabelTextRows(JLabel[] labels,
                                  JTextField[] textFields,
                                  GridBagLayout gridbag,
                                  Container container) {
        GridBagConstraints c = new GridBagConstraints();
        c.anchor = GridBagConstraints.EAST;
	c.insets = new Insets( 2, 2, 2, 2 );
	int numLabels = labels.length;
	
        for (int i = 0; i < numLabels; i++) {
	    c.anchor = GridBagConstraints.EAST;
            c.gridwidth = GridBagConstraints.RELATIVE; //next-to-last
            c.fill = GridBagConstraints.NONE;      //reset to default
            c.weightx = 0.0;                       //reset to default
            gridbag.setConstraints(labels[i], c);
            container.add(labels[i]);

	    c.anchor = GridBagConstraints.WEST;
            c.gridwidth = GridBagConstraints.REMAINDER;     //end row
            c.weightx = 1.0;
            gridbag.setConstraints(textFields[i], c);
            container.add(textFields[i]);
        }
    }
    
    /** Main method to aid in testing this component
     */
    public static void main( String args[] ) {
	JFrame frame = new JFrame( "PhotoInfoEditorTest" );
	PhotoInfoController ctrl = new PhotoInfoController();
	PhotoInfoEditor editor = new PhotoInfoEditor( ctrl );
	frame.getContentPane().add( editor, BorderLayout.CENTER );
	frame.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    } );
	frame.pack();
	frame.setVisible( true );
    }

    private PhotoInfoController ctrl = null;
}
