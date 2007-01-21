/*
  Copyright (c) 2006 Harri Kaimio
  
  This file is part of Photovault.

  Photovault is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  Photovault is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/


package org.photovault.swingui;


import javax.swing.tree.TreePath;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.imginfo.FuzzyDate;
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

import org.photovault.imginfo.*;
import javax.swing.event.*;
import javax.swing.tree.TreeModel;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.swingui.folderpane.FolderTreePane;

/** PhotoInfoEditor provides a GUI interface for creating of modifying PhotoInfo records in the database.
    Use can either edit an existing record or create a completely new record.
*/

public class PhotoInfoEditor extends JPanel implements PhotoInfoView, ActionListener, DocumentListener, PropertyChangeListener {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoInfoEditor.class.getName() );

    static Color multiValueColor = Color.LIGHT_GRAY;
    static Color singleValueColor = Color.WHITE;
    
    public PhotoInfoEditor( PhotoInfoController ctrl ) {
	super();
	this.ctrl = ctrl;
	createUI();
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

	// "Fuzzy time" field
	JLabel fuzzyDateLabel = new JLabel( "Shooting date" );
	fuzzyDateField = new JTextField( 30 );
	fuzzyDateDoc = fuzzyDateField.getDocument();
	fuzzyDateDoc.putProperty( FIELD_NAME, PhotoInfoController.FUZZY_DATE );
	fuzzyDateDoc.addDocumentListener( this );


	JLabel qualityLabel = new JLabel( "Quality" );
	qualityField = new JComboBox( qualityStrings );
	qualityField.addActionListener( this );
	
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
	JLabel[] labels     = { photographerLabel, fuzzyDateLabel, shootingPlaceLabel, qualityLabel };
	JComponent[] fields = { photographerField, fuzzyDateField, shootingPlaceField, qualityField };
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
	createFolderPaneUI();

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

	// Tech note text
	JLabel notesLabel = new JLabel( "Tech. notes" );
	technoteTextArea = new JTextArea( 5, 40 );
	technoteTextArea.setLineWrap( true );
	technoteTextArea.setWrapStyleWord( true );
	JScrollPane technoteScrollPane = new JScrollPane( technoteTextArea );
	technoteScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
	Border technoteBorder = BorderFactory.createEtchedBorder( EtchedBorder.LOWERED );
	technoteBorder = BorderFactory.createTitledBorder( technoteBorder, "Description" );
        technoteScrollPane.setBorder( technoteBorder );
	technoteDoc = technoteTextArea.getDocument();
	technoteDoc.putProperty( FIELD_NAME, PhotoInfoController.TECHNOTE );
	technoteDoc.addDocumentListener( this );
	
	// Lay out the created controls
	GridBagLayout layout = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	pane.setLayout( layout );
	JLabel[] labels     = { cameraLabel, lensLabel, focalLengthLabel, filmLabel, filmSpeedLabel, shutterSpeedLabel, fStopLabel };
	JTextField[] fields = { cameraField, lensField, focalLengthField, filmField, filmSpeedField, shutterSpeedField, fStopField };
	addLabelTextRows( labels, fields, layout, pane );
	pane.add( technoteScrollPane );
	c.gridwidth = GridBagConstraints.REMAINDER;
	c.weighty = 0.5;
	c.fill = GridBagConstraints.BOTH;
	layout.setConstraints( technoteScrollPane, c );


    }
	

    protected void createFolderPaneUI() {
	JPanel pane = new JPanel();
        pane.setLayout( new GridBagLayout() );
	folderTreePane = new FolderTreePane( ctrl.getFolderController() );
	tabPane.addTab( "Folders", pane );
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
	pane.add( folderTreePane, c );
    }

    public void setPhotographer( String newValue ) {
	photographerField.setText( newValue );
    }
    
    public String getPhotographer( ) {
	String str = photographerField.getText( );
	return str;
    }

    public void setPhotographerMultivalued( boolean mv ) {
	photographerField.setBackground( mv ? multiValueColor : singleValueColor );
    }

    public void setFuzzyDate( FuzzyDate d ) {
	if ( d != null ) {
	    fuzzyDateField.setText( d.format() );
	} else {
	    fuzzyDateField.setText( "" );
	}
    }
    
    public FuzzyDate getFuzzyDate( ) {
	String str = fuzzyDateField.getText( );
	log.debug( "fuzzyDate = " + str );
	FuzzyDate d = FuzzyDate.parse( str );
	return d;
    }
    
    public void setFuzzyDateMultivalued( boolean mv ) {
	fuzzyDateField.setBackground( mv ? multiValueColor : singleValueColor );
    }
    
    public void setShootTime( Date newValue ) {
	log.warn( "setShootingTime: " + newValue );
	shootingDayField.setValue( newValue );
    }

    public Date getShootTime( ) {
	log.warn( "getShootingTime" );
	return (Date) shootingDayField.getValue();
    }

    public void setShootTimeMultivalued( boolean mv ) {
	shootingDayField.setBackground( mv ? multiValueColor : singleValueColor );
    }

    public void setTimeAccuracy( Number newValue ) {
	timeAccuracyField.setValue( newValue );
    }

    public Number getTimeAccuracy() {
	return (Number) timeAccuracyField.getValue();
    }

    public void setShootPlace( String newValue ) {
	shootingPlaceField.setText( newValue );
    }
    
    public String getShootPlace( ) {
	return shootingPlaceField.getText( );
    }
    
    public void setShootPlaceMultivalued( boolean mv ) {
	shootingPlaceField.setBackground( mv ? multiValueColor : singleValueColor );
    }

    public void setFStop( Number newValue ) {
	fStopField.setValue( newValue  );
    }
    
    public Number getFStop( ) {
	Number value = (Number) fStopField.getValue( );
	return value;
    }

    public void setFStopMultivalued( boolean mv ) {
	fStopField.setBackground( mv ? multiValueColor : singleValueColor );
    }
    
    public void setShutterSpeed( Number newValue ) {
	shutterSpeedField.setValue( newValue );
    }
    
    public Number getShutterSpeed( ) {
	Number value = (Number) shutterSpeedField.getValue( );
	return value;
    }
    
    public void setShutterSpeedMultivalued( boolean mv ) {
	shutterSpeedField.setBackground( mv ? multiValueColor : singleValueColor );
    }

    public void setFocalLength( Number newValue ) {
	focalLengthField.setValue( newValue  );
    }
    
    public Number getFocalLength( ) {
	Number value = ((Number) focalLengthField.getValue( ));
	return value;
    }
    
    public void setFocalLengthMultivalued( boolean mv ) {
	focalLengthField.setBackground( mv ? multiValueColor : singleValueColor );
    }

    public void setFilmSpeed( Number newValue ) {
	filmSpeedField.setValue( newValue );
    }
    
    public Number getFilmSpeed( ) {
	Number value = (Number) filmSpeedField.getValue( );
	return value;
    } 
    public void setFilmSpeedMultivalued( boolean mv ) {
	filmSpeedField.setBackground( mv ? multiValueColor : singleValueColor );
    }

    public void setCamera( String newValue ) {
	cameraField.setText( newValue );
    }
    
    public String getCamera( ) {
	return cameraField.getText( );
    }
    public void setCameraMultivalued( boolean mv ) {
	cameraField.setBackground( mv ? multiValueColor : singleValueColor );
    }
    
    public void setLens( String newValue ) {
	lensField.setText( newValue );
    }
    
    public String getLens( ) {
	return lensField.getText( );
    }

    public void setLensMultivalued( boolean mv ) {
	lensField.setBackground( mv ? multiValueColor : singleValueColor );
    }
    
    public void setFilm( String newValue ) {
	filmField.setText( newValue );
    }
    
    public String getFilm( ) {
	return filmField.getText( );
    }

    public void setFilmMultivalued( boolean mv ) {
	filmField.setBackground( mv ? multiValueColor : singleValueColor );
    }
    
    public void setDescription( String newValue ) {
	descriptionTextArea.setText( newValue );
    }
    
    public String getDescription( ) {
	return descriptionTextArea.getText( );
    }

    public void setDescriptionMultivalued( boolean mv ) {
	descriptionTextArea.setBackground( mv ? multiValueColor : singleValueColor );
    }

    
    public void setTechNote( String newValue ) {
	technoteTextArea.setText( newValue );
    }
    
    public String getTechNote( ) {
	return technoteTextArea.getText( );
    }
    
    public void setTechNoteMultivalued( boolean mv ) {
	technoteTextArea.setBackground( mv ? multiValueColor : singleValueColor );
    }

    public Number getQuality() {
	int q = qualityField.getSelectedIndex();
	Integer retval = null;
	if ( q >= 0 ) {
	    retval = new Integer( q );
	}
	return retval;
    }

    public void setQuality( Number quality ) {
	if ( quality != null ) {
	    qualityField.setSelectedIndex( quality.intValue() );
	} else {
	    qualityField.setSelectedIndex( -1 );
	}
	    
    }

    public void setQualityMultivalued( boolean mv ) {
    }

    public void setFolderTreeModel( TreeModel model ) {
	folderTreePane.setFolderTreeModel( model );
    }

    public void expandFolderTreePath(TreePath path) {
        folderTreePane.expandPath( path );
    }
    
    // Important UI components
    JTextField photographerField = null;
    Document photographerDoc = null;
    JTextField fuzzyDateField = null;
    Document fuzzyDateDoc = null;
    JFormattedTextField shootingDayField = null;
    JFormattedTextField timeAccuracyField = null;
    Document shootingDayDoc = null;
    JTextField shootingPlaceField = null;
    Document shootingPlaceDoc = null;
    JTextArea descriptionTextArea = null;
    Document descriptionDoc = null;

    String qualityStrings[] = { "Unevaluated", "Top", "Good", "OK", "Poor", "Unusable" };
    JComboBox qualityField = null;

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
    JTextArea technoteTextArea = null;
    Document technoteDoc = null;

    JTabbedPane tabPane = null;

    FolderTreePane folderTreePane = null;
    
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
	} else if ( evt.getSource() == qualityField ) {
	    log.debug( "quality changed"  );
	    // If getQuality returns null this action event is generated
	    // by the controller that is setting up quality field to display
	    // model with multiple quality values.
	    if ( getQuality() != null ) {
		ctrl.viewChanged( this, PhotoInfoController.QUALITY );
	    }
	}
    }

    // DocumentListener interface implementation
    public void changedUpdate( DocumentEvent ev ) {
    }

    public void insertUpdate( DocumentEvent ev ) {
	log.debug( "insertUpdate,  " );
	Document changedDoc = ev.getDocument();
	String changedField = (String) changedDoc.getProperty( FIELD_NAME );
	Object fieldValue = ctrl.getField( changedField );
	/* Avoid emptying model when the field has multiple values
	   in the model.
	*/
	if ( fieldValue != null || changedDoc.getLength() > 0 ) {
	    ctrl.viewChanged( this, changedField );
	}

// 	// Handle fuzzy time
// 	if ( changedDoc == fuzzyDateDoc ) {
// 	    log.warn( "Fuzzy date entered" );
// 	    String fdStr = fuzzyDateField.getText();
// 	    FuzzyDate fd = FuzzyDate.parse( fdStr );
// // 	    if ( fd != null ) {
// // 		log.warn( "FuzzyDate parsed succesfully!!!" );
// // 		shootingDayField.setValue( fd.getDate() );
// // 		timeAccuracyField.setValue( new Double( fd.getAccuracy() ) );
// // 	    }
	    
// 	}	
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
		    log.debug( "Property changed: " + (String) field );
		    ctrl.viewChanged( this, (String) field );
		}
	    } 
	}
    }
    
    private void addLabelTextRows(JLabel[] labels,
                                  JComponent[] textFields,
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

    RawConversionSettings rawSettings = null;
    public void setRawSettings(RawConversionSettings rs ) {
        rawSettings = rs;
    }

    public void setRawSettingsMultivalued(boolean mv) {
    }

    public RawConversionSettings getRawSettings() {
        return rawSettings;
    }

    private PhotoInfoController ctrl = null;
    private static final String FIELD_NAME = "FIELD_NAME";
}
