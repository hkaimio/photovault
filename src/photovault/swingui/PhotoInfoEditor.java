// PhotoInfoEditor.java


package photovault.swingui;


import javax.swing.*;
import javax.swing.border.*;
import javax.swing.text.*;
import java.text.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.text.*;
import java.beans.*;
import java.io.File;

import imginfo.*;
import dbhelper.*;
import javax.swing.event.*;

/** PhotoInfoEditor provides a GUI interface for creating of modifying PhotoInfo records in the database.
    Use can either edit an existing record or create a completely new record.
*/

public class PhotoInfoEditor extends JPanel implements PhotoInfoView, ActionListener, DocumentListener, PropertyChangeListener {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoInfoEditor.class.getName() );

    public PhotoInfoEditor( PhotoInfoController ctrl ) {
	super();
	createUI();
	this.ctrl = ctrl;
	ctrl.setView( this );
    }
    
    protected void createUI() {
	setLayout(new BorderLayout());
	setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
	tabPane = new JTabbedPane();
	add( tabPane, BorderLayout.CENTER );

	// General pane
	JPanel generalPane = new JPanel();
	tabPane.addTab( "General", generalPane );
	
	// Create the fields & their labels

	// Photographer field
	JLabel photographerLabel = new JLabel( "Photographer" );
	photographerField = new JTextField( 30 );
	photographerDoc = photographerField.getDocument();
	photographerDoc.addDocumentListener( this );
	photographerDoc.putProperty( FIELD_NAME, PhotoInfoController.PHOTOGRAPHER );

	// Shooting date field
	JLabel shootingDayLabel = new JLabel( "Shooting date" );
	DateFormat df = DateFormat.getDateInstance();
	DateFormatter shootingDayFormatter = new DateFormatter( df );
	shootingDayField = new JFormattedTextField( df );
	shootingDayField.setColumns( 10 );
	shootingDayField.setValue( new Date( ));
	shootingDayField.addPropertyChangeListener( this );
	shootingDayField.putClientProperty( FIELD_NAME, PhotoInfoController.SHOOTING_DATE );

	
	// Shooting place field
	JLabel shootingPlaceLabel =  new JLabel( "Shooting place" );
	shootingPlaceField = new JTextField( 30 );
	shootingPlaceDoc = shootingPlaceField.getDocument();
	shootingPlaceDoc.addDocumentListener( this );
	shootingPlaceDoc.putProperty( FIELD_NAME, PhotoInfoController.SHOOTING_PLACE );
	
	// Description text
	JLabel descLabel = new JLabel( "Description" );
	descriptionTextArea = new JTextArea( 5, 40 );
	descriptionTextArea.setLineWrap( true );
	descriptionTextArea.setWrapStyleWord( true );
	JScrollPane descScrollPane = new JScrollPane( descriptionTextArea );
	descScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
	Border descBorder = BorderFactory.createEtchedBorder( EtchedBorder.LOWERED );
	descBorder = BorderFactory.createTitledBorder( descBorder, "Description" );
        descScrollPane.setBorder( descBorder );
	descriptionDoc = descriptionTextArea.getDocument();
	descriptionDoc.putProperty( FIELD_NAME, PhotoInfoController.DESCRIPTION );
	descriptionDoc.addDocumentListener( this );
	
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
	generalPane.setLayout( layout );
	JLabel[] labels     = { photographerLabel, shootingDayLabel, shootingPlaceLabel };
	JTextField[] fields = { photographerField, shootingDayField, shootingPlaceField };
	addLabelTextRows( labels, fields, layout, generalPane );

	
	generalPane.add( descScrollPane );
	c.gridwidth = GridBagConstraints.REMAINDER;
	c.weighty = 0.5;
	c.fill = GridBagConstraints.BOTH;
	layout.setConstraints( descScrollPane, c );

	c = new GridBagConstraints();
	c.gridwidth = 1;
	c.weighty = 0;
	c.fill = GridBagConstraints.NONE;
	c.gridy = GridBagConstraints.RELATIVE;
	

	c.gridy = GridBagConstraints.RELATIVE;
	

	createTechDataUI();

	// Create a pane for the buttols
	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.X_AXIS));
	buttonPane.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
	buttonPane.add(Box.createHorizontalGlue());
	buttonPane.add(discardBtn);
	buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
	buttonPane.add(saveBtn);
	//	add( buttonPane, BorderLayout.SOUTH );

    }

    protected void createTechDataUI() {
	JPanel pane = new JPanel();
	tabPane.addTab( "Tech data", pane );

	JLabel cameraLabel =  new JLabel( "Camera" );
	cameraField = new JTextField( 20 );
	cameraDoc = cameraField.getDocument();
	cameraDoc.addDocumentListener( this );
	cameraDoc.putProperty( FIELD_NAME, PhotoInfoController.CAMERA_MODEL );
	
	JLabel lensLabel =  new JLabel( "Lens" );
	lensField = new JTextField( 20 );
	lensDoc = lensField.getDocument();
	lensDoc.addDocumentListener( this );
	lensDoc.putProperty( FIELD_NAME, PhotoInfoController.LENS_TYPE );

	JLabel filmLabel =  new JLabel( "Film" );
	filmField = new JTextField( 20 );
	filmDoc = filmField.getDocument();
	filmDoc.addDocumentListener( this );
	filmDoc.putProperty( FIELD_NAME, PhotoInfoController.FILM_TYPE );

	JLabel filmSpeedLabel =  new JLabel( "Film speed" );
	DecimalFormat filmSpeedFormat = new DecimalFormat( "#########0" );	
	filmSpeedField = new JFormattedTextField( filmSpeedFormat );
	filmSpeedField.setColumns( 5 );
	filmSpeedField.addPropertyChangeListener( this );
	filmSpeedField.putClientProperty( FIELD_NAME, PhotoInfoController.FILM_SPEED );

	JLabel shutterSpeedLabel =  new JLabel( "Shutter speed" );
	DecimalFormat shutterSpeedFormat = new DecimalFormat( "###0.####" );
	shutterSpeedField = new JFormattedTextField( new NumberFormatter( shutterSpeedFormat ) );
	shutterSpeedField.setColumns( 5 );
	shutterSpeedField.addPropertyChangeListener( this );
	shutterSpeedField.putClientProperty( FIELD_NAME, PhotoInfoController.SHUTTER_SPEED );

	JLabel fStopLabel =  new JLabel( "F-stop" );
	DecimalFormat fStopFormat = new DecimalFormat( "#0.#" );
	fStopField = new JFormattedTextField( new NumberFormatter( fStopFormat ) );
	fStopField.setColumns( 5 );
	fStopField.addPropertyChangeListener( this );
	fStopField.putClientProperty( FIELD_NAME, PhotoInfoController.F_STOP );
	
	JLabel focalLengthLabel =  new JLabel( "Focal length" );
	DecimalFormat focalLengthFormat = new DecimalFormat( "#######0.#" );
	focalLengthField = new JFormattedTextField( new NumberFormatter( focalLengthFormat ));
	focalLengthField.setColumns( 5 );
	focalLengthField.addPropertyChangeListener( this );
	focalLengthField.putClientProperty( FIELD_NAME, PhotoInfoController.FOCAL_LENGTH );
	
	// Lay out the created controls
	GridBagLayout layout = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	pane.setLayout( layout );
	JLabel[] labels     = { cameraLabel, lensLabel, focalLengthLabel, filmLabel, filmSpeedLabel, shutterSpeedLabel, fStopLabel };
	JTextField[] fields = { cameraField, lensField, focalLengthField, filmField, filmSpeedField, shutterSpeedField, fStopField };
	addLabelTextRows( labels, fields, layout, pane );
    }
	
	

    public void setPhotographer( String newValue ) {
	photographerField.setText( newValue );
    }
    
    public String getPhotographer( ) {
	return photographerField.getText( );
    }
    
    public void setShootTime( Date newValue ) {
	log.warn( "setShootingTime: " + newValue );
	shootingDayField.setValue( newValue );
    }

    public Date getShootTime( ) {
	log.warn( "getShootingTime" );
	return (Date) shootingDayField.getValue();
    }

    public void setShootPlace( String newValue ) {
	shootingPlaceField.setText( newValue );
    }
    
    public String getShootPlace( ) {
	return shootingPlaceField.getText( );
    }
    
    public void setFStop( Number newValue ) {
	fStopField.setValue( newValue  );
    }
    
    public Number getFStop( ) {
	Number value = (Number) fStopField.getValue( );
	return value;
    }
    
    public void setShutterSpeed( Number newValue ) {
	shutterSpeedField.setValue( newValue );
    }
    
    public Number getShutterSpeed( ) {
	Number value = (Number) shutterSpeedField.getValue( );
	return value;
    }
    
    public void setFocalLength( Number newValue ) {
	focalLengthField.setValue( newValue  );
    }
    
    public Number getFocalLength( ) {
	Number value = ((Number) focalLengthField.getValue( ));
	return value;
    }
    
    public void setFilmSpeed( Number newValue ) {
	filmSpeedField.setValue( newValue );
    }
    
    public Number getFilmSpeed( ) {
	Number value = (Number) filmSpeedField.getValue( );
	return value;
    } 

    public void setCamera( String newValue ) {
	cameraField.setText( newValue );
    }
    
    public String getCamera( ) {
	return cameraField.getText( );
    }
    
    public void setLens( String newValue ) {
	lensField.setText( newValue );
    }
    
    public String getLens( ) {
	return lensField.getText( );
    }
    
    public void setFilm( String newValue ) {
	filmField.setText( newValue );
    }
    
    public String getFilm( ) {
	return filmField.getText( );
    }
    
    public void setDescription( String newValue ) {
	descriptionTextArea.setText( newValue );
    }
    
    public String getDescription( ) {
	return descriptionTextArea.getText( );
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

    JTextField cameraField = null;
    Document cameraDoc = null;
    JTextField lensField = null;
    Document lensDoc = null;

    JTextField filmField = null;
    Document filmDoc = null;
    JFormattedTextField filmSpeedField = null;
    JFormattedTextField shutterSpeedField = null;
    JFormattedTextField fStopField = null;     
    Document filmSpeedDoc = null;
    Document shutterSpeedDoc = null;
    Document fStopDoc = null;     
    
    JFormattedTextField focalLengthField = null;
    Document focalLengthDoc = null;

    JTabbedPane tabPane = null;
    
    public void actionPerformed( ActionEvent evt ) {
	if ( evt.getActionCommand().equals( "save" ) ) {
	    try {
		ctrl.save();
	    } catch ( Exception e ) {
		log.warn( "exception while saving" + e.getMessage() );
		e.printStackTrace();
	    }
	} else if ( evt.getActionCommand().equals( "discard" ) ) {
	    log.debug( "Discarding data" );
	    ctrl.discard();
	}
    }

    // DocumentListener interface implementation
    public void changedUpdate( DocumentEvent ev ) {
    }

    public void insertUpdate( DocumentEvent ev ) {
	Document changedDoc = ev.getDocument();
	String changedField = (String) changedDoc.getProperty( FIELD_NAME );
	ctrl.viewChanged( this, changedField );
    }

    public void removeUpdate( DocumentEvent ev ) {
	insertUpdate( ev );
    }

    // PropertyChangeListener implementation
    public void propertyChange( PropertyChangeEvent ev ) {
	if ( ev.getPropertyName().equals( "value" ) ) {
	    Object src = ev.getSource();
	    if ( src.getClass() == JFormattedTextField.class ) {
		Object field = ((JFormattedTextField) src).getClientProperty( FIELD_NAME );
		Object value = ((JFormattedTextField) src).getValue();

		/* Field value is set to null (as it is when ctrl is
		 controlling multiple photos which have differing
		 value for te field) this is called every time the
		 field is accessed, so we must not notify the
		 controller.  After the user has actually set the
		 value it is no longer null.
		*/
		if ( value != null ) {
		    log.warn( "Property changed: " + (String) field );
		    System.out.println( "Property changed: " + (String) field );
		    ctrl.viewChanged( this, (String) field );
		}
	    }
	}
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
	// Parse the arguments
	PhotoInfo photo = null;
	log.debug( "Number of args" + args.length );
	log.debug( args.toString() );
	if ( args.length == 2 ) {
	    if ( args[0].equals( "-f" ) ) {
		File f = new File( args[1] );
		try {
		    log.debug( "Getting file " + f.getPath() );
		    photo = PhotoInfo.addToDB( f );
		} catch ( Exception e ) {
		    log.warn( e.getMessage() );
		}
	    } else if ( args[0].equals( "-id" ) ) {
		try {
		    int id = Integer.parseInt( args[1] );
		    log.debug( "Getting photo " + id );
		    photo = PhotoInfo.retrievePhotoInfo( id );
		} catch ( Exception e ) {
		    log.warn( e.getMessage() );
		}
	    }
	}
	
	JFrame frame = new JFrame( "PhotoInfoEditorTest" );
	PhotoInfoController ctrl = new PhotoInfoController();
	PhotoInfoEditor editor = new PhotoInfoEditor( ctrl );
	if ( photo != null ) {
	    ctrl.setPhoto( photo );
	}
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
    private static final String FIELD_NAME = "FIELD_NAME";
}
