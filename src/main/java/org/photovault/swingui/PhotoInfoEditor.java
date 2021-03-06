/*
  Copyright (c) 2006-2011 Harri Kaimio
  
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


import com.jgoodies.binding.adapter.Bindings;
import com.jgoodies.binding.beans.PropertyAdapter;
import com.jgoodies.binding.beans.PropertyConnector;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListModel;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.text.NumberFormatter;
import javax.swing.tree.TreePath;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.image.ChannelMapOperation;
import org.photovault.image.ColorCurve;

import javax.swing.tree.TreeModel;
import org.photovault.imginfo.FuzzyDate;
import org.photovault.imginfo.License;
import org.photovault.imginfo.PhotoInfoFields;
import org.photovault.swingui.folderpane.FolderTreePane;
import org.photovault.swingui.selection.PhotoSelectionController;
import org.photovault.swingui.selection.PhotoSelectionView;
import org.photovault.swingui.selection.FieldController;
import org.photovault.swingui.tag.TagList2;

/** PhotoInfoEditor provides a GUI interface for creating of modifying PhotoInfo records in the database.
    Use can either edit an existing record or create a completely new record.
*/

public class PhotoInfoEditor extends JPanel implements PhotoSelectionView, ActionListener, DocumentListener, PropertyChangeListener {

    static Log log = LogFactory.getLog( PhotoInfoEditor.class );

    static Color multiValueColor = Color.LIGHT_GRAY;
    static Color singleValueColor = Color.WHITE;

    public PhotoInfoEditor( PhotoSelectionController ctrl ) {
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
	photographerField = createMvTextField( "photographer", 30 );
	photographerDoc = photographerField.getDocument();

	// "Fuzzy time" field
	JLabel fuzzyDateLabel = new JLabel( "Shooting date" );
	fuzzyDateField = new JTextField( 30 );
	fuzzyDateDoc = fuzzyDateField.getDocument();
	fuzzyDateDoc.putProperty( FIELD, PhotoInfoFields.FUZZY_SHOOT_TIME );
	fuzzyDateDoc.addDocumentListener( this );


	JLabel qualityLabel = new JLabel( "Quality" );
	qualityField = new JComboBox( qualityStrings );
	qualityField.addActionListener( this );

        StarRating qualityStars = new StarRating( 5 );
        FieldController<Integer> qfCtrl = ctrl.getFieldController( "quality" );
        ValueModel qualityFieldValue = new PropertyAdapter( qfCtrl, "value", true );
        ValueModel starCount = new PropertyAdapter(  qualityStars, "selection", true );
        PropertyConnector.connect(
                qualityFieldValue, "value",
                starCount, "value" );
	
	// Shooting place field
	JLabel shootingPlaceLabel =  new JLabel( "Shooting place" );
	shootingPlaceField = createMvTextField( "shotLocation.description", 30 );
	shootingPlaceDoc = shootingPlaceField.getDocument();
        JLabel shotLocRoadLabel = new JLabel( "Road" );
        JTextField shotLocRoadField = createMvTextField( "shotLocation.road", 30 );
        JLabel shotLocSuburbLabel = new JLabel( "Suburb" );
        JTextField shotLocSuburbField = createMvTextField( "shotLocation.suburb", 30 );
        JLabel shotLocCityLabel = new JLabel( "City" );
        JTextField shotLocCityField = createMvTextField( "shotLocation.city", 30 );
        JLabel shotLocCountryLabel = new JLabel( "Country" );
        JTextField shotLocCountryField = createMvTextField( "shotLocation.country", 30 );
        JLabel shotLocGeohashLabel = new JLabel( "Geohash code" );
        JTextField shotLocGeohashField = createMvTextField( "shotLocation.geoHashString", 30 );
        shotLocGeohashField.getInputMap().put( 
                KeyStroke.getKeyStroke( KeyEvent.VK_F2, 0 ), "fillAddress" );
        shotLocGeohashField.getActionMap().put(  
                "fillAddress", new FindAddressAction( ctrl ) );



        // Tags
        JLabel tagLabel = new JLabel( "Tags" );
        tagList = new TagList2( ctrl.getTagController() );
        tagList.setBackground( generalPane.getBackground() );

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
	descriptionDoc.putProperty( FIELD, PhotoInfoFields.DESCRIPTION );
	descriptionDoc.addDocumentListener( this );
		
	// Lay out the created controls
	GridBagLayout layout = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	generalPane.setLayout( layout );
	JLabel[] labels     = { 
            photographerLabel, fuzzyDateLabel, shootingPlaceLabel,
            shotLocGeohashLabel, shotLocRoadLabel, shotLocSuburbLabel, shotLocCityLabel,
            shotLocCountryLabel,
            qualityLabel, tagLabel };
	JComponent[] fields = { 
            photographerField, fuzzyDateField, shootingPlaceField,
            shotLocGeohashField, shotLocRoadField, shotLocSuburbField,
            shotLocCityField, shotLocCountryField,
            qualityStars, tagList };
	addLabelTextRows( labels, fields, layout, generalPane );
        c = layout.getConstraints( tagList );
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 0.0;
        c.weighty = 1.0;
        layout.setConstraints( tagList, c);

	
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
        createRightsUI();
    }

    protected void createTechDataUI() {
	JPanel pane = new JPanel();
	tabPane.addTab( "Tech data", pane );

	JLabel cameraMakerLabel =  new JLabel( "Camera make" );
	JTextField cameraMakerField = createMvTextField( "cameraMaker", 20 );
	// cameraDoc = cameraField.getDocument();

        JLabel cameraLabel =  new JLabel( "Camera model" );
	cameraField = createMvTextField( "camera", 20 );
	cameraDoc = cameraField.getDocument();
	
	JLabel lensLabel =  new JLabel( "Lens" );
	lensField = createMvTextField( "lens", 20 );
	lensDoc = lensField.getDocument();

	JLabel filmLabel =  new JLabel( "Film" );
	filmField = createMvTextField( "film", 20 );
	filmDoc = filmField.getDocument();

	JLabel filmSpeedLabel =  new JLabel( "Film speed" );
	NumberFormatter filmSpeedFormatter = new NumberFormatter( new DecimalFormat( "#########0" ) );	
        filmSpeedFormatter.setValueClass( PhotoInfoFields.FILM_SPEED.getType() );
        filmSpeedField = new JFormattedTextField( filmSpeedFormatter );

	filmSpeedField.setColumns( 5 );
	filmSpeedField.addPropertyChangeListener( this );
	filmSpeedField.putClientProperty( FIELD, PhotoInfoFields.FILM_SPEED );

	JLabel shutterSpeedLabel =  new JLabel( "Shutter speed" );
	DecimalFormat shutterSpeedFormat = new DecimalFormat( "###0.####" );
        NumberFormatter shutterSpeedFormatter = new NumberFormatter( shutterSpeedFormat );
        shutterSpeedFormatter.setValueClass( PhotoInfoFields.SHUTTER_SPEED.getType() );
	shutterSpeedField = new JFormattedTextField( shutterSpeedFormatter );
	shutterSpeedField.setColumns( 5 );
	shutterSpeedField.addPropertyChangeListener( this );
	shutterSpeedField.putClientProperty( FIELD, PhotoInfoFields.SHUTTER_SPEED );

	JLabel fStopLabel =  new JLabel( "F-stop" );
	DecimalFormat fStopFormat = new DecimalFormat( "#0.#" );
        NumberFormatter fStopFormatter = new NumberFormatter( fStopFormat );
        fStopFormatter.setValueClass( PhotoInfoFields.FSTOP.getType() );
	fStopField = new JFormattedTextField( fStopFormatter );
	fStopField.setColumns( 5 );
	fStopField.addPropertyChangeListener( this );
	fStopField.putClientProperty( FIELD, PhotoInfoFields.FSTOP );
	
	JLabel focalLengthLabel =  new JLabel( "Focal length" );
	NumberFormatter focalLengthFormatter = new NumberFormatter( new DecimalFormat( "#######0.#" ) );
        focalLengthFormatter.setValueClass( PhotoInfoFields.FSTOP.getType() );
	focalLengthField = new JFormattedTextField( focalLengthFormatter );
	focalLengthField.setColumns( 5 );
	focalLengthField.addPropertyChangeListener( this );
	focalLengthField.putClientProperty( FIELD, PhotoInfoFields.FOCAL_LENGTH );

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
	technoteDoc.putProperty( FIELD, PhotoInfoFields.TECH_NOTES );
	technoteDoc.addDocumentListener( this );
	
	// Lay out the created controls
	GridBagLayout layout = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	pane.setLayout( layout );
	JLabel[] labels     = { cameraMakerLabel, cameraLabel, lensLabel, focalLengthLabel, filmLabel, filmSpeedLabel, shutterSpeedLabel, fStopLabel };
	JTextField[] fields = { cameraMakerField, cameraField, lensField, focalLengthField, filmField, filmSpeedField, shutterSpeedField, fStopField };
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

    private void createRightsUI() {
	JPanel pane = new JPanel();
	tabPane.addTab( "Rights", pane );

        JLabel copyrightLabel =  new JLabel( "Copyright" );
	JTextField copyrightField = createMvTextField( "usageRights.copyright", 30 );
        JLabel attributionLabel =  new JLabel( "Name for attribution" );
	JTextField attributionField = createMvTextField( "usageRights.attributionName", 30 );
        JLabel attributionUrlLabel =  new JLabel( "Url for attribution" );
	JTextField attributionUrlField = createMvTextField( "usageRights.attributionUrl", 30 );

        JLabel licenseLabel =  new JLabel( "License" );
        List<License> licenses = new ArrayList();
        licenses.addAll( EnumSet.allOf( License.class ) );
        JComboBox licenseCombo = createMvComboBox( "usageRights.license", licenses );

        // Tech note text
	JLabel termsLabel = new JLabel( "Usage terms" );
	JTextArea termsTextArea = createMvTextArea( "usageRights.usageTerms", 5, 40 );
	termsTextArea.setLineWrap( true );
	termsTextArea.setWrapStyleWord( true );
	JScrollPane termsScrollPane = new JScrollPane( termsTextArea );
	termsScrollPane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_ALWAYS );
	Border termsBorder = BorderFactory.createEtchedBorder( EtchedBorder.LOWERED );
	termsBorder = BorderFactory.createTitledBorder( termsBorder, "Usage terms" );
        termsScrollPane.setBorder( termsBorder );

	GridBagLayout layout = new GridBagLayout();
	GridBagConstraints c = new GridBagConstraints();
	pane.setLayout( layout );
	JLabel[] labels     = { copyrightLabel, licenseLabel, attributionLabel, attributionUrlLabel};
	JComponent[] fields = { copyrightField, licenseCombo, attributionField, attributionUrlField };
	addLabelTextRows( labels, fields, layout, pane );
	pane.add( termsScrollPane );
	c.gridwidth = GridBagConstraints.REMAINDER;
	c.weighty = 0.5;
	c.fill = GridBagConstraints.BOTH;
	layout.setConstraints( termsScrollPane, c );
    }

    /**
     * Helper function to create a text field for editing a property and
     * connecting it to selection model.
     * @param propName Property to be edited
     * @param length Length of the text field
     * @return The created text field
     */
    private JTextField createMvTextField( String propName, int length ) {
	JTextField fld = new JTextField( length );
        FieldController tfc = ctrl.getFieldController( propName );

        Bindings.bind( fld, new PropertyAdapter( tfc, "value", true ), true );
        ValueModel mvValueModel = new PropertyAdapter( tfc, "multivalued", true );
        ValueModel fieldColorModel = new MultivalueColorConverter( mvValueModel );
        PropertyConnector.connect(
                fieldColorModel, "value",
                fld, "background" );
        return fld;
    }

    /**
     * Helper function to create a text area for editing a property and
     * connecting it to selection model.
     * @param propName Property to be edited
     * @param rows Number of rows in the text area
     * @param cols Number of columns in the text area
     * @return The created text area
     */
    private JTextArea createMvTextArea( String propName, int rows, int cols ) {
	JTextArea fld = new JTextArea( rows, cols );
        FieldController tfc = ctrl.getFieldController( propName );

        Bindings.bind( fld, new PropertyAdapter( tfc, "value", true ), true );
        ValueModel mvValueModel = new PropertyAdapter( tfc, "multivalued", true );
        ValueModel fieldColorModel = new MultivalueColorConverter( mvValueModel );
        PropertyConnector.connect(
                fieldColorModel, "value",
                fld, "background" );
        return fld;
    }

    /**
     * Helper function to create a combo box for editing a property and
     * connecting it to selection model.
     * @param propName Property to be edited
     * @param values List of values in the combo box
     * @return The created combo box
     */

    private JComboBox createMvComboBox( String propName, List values ) {
	JComboBox fld = new JComboBox();
        FieldController fc = ctrl.getFieldController( propName );
        ValueModel fcValue = new PropertyAdapter(  fc, "value", true );

        Bindings.bind( fld, new SelectionInList( values, fcValue ) );
//        ValueModel mvValueModel = new PropertyAdapter( fc, "multivalued", true );
//        ValueModel fieldColorModel = new MultivalueColorConverter( mvValueModel );
//        PropertyConnector.connect(
//                fieldColorModel, "value",
//                fld, "background" );
        return fld;
    }

    public void setPhotographer( String newValue ) {
    }
    
    public String getPhotographer( ) {
	String str = photographerField.getText( );
	return str;
    }

    public void setPhotographerMultivalued( boolean mv ) {
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
    
    public void setShootingPlace( String newValue ) {
    }
    
    public String getShootingPlace( ) {
	return shootingPlaceField.getText( );
    }
    
    public void setShootingPlaceMultivalued( boolean mv ) {
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
    }
    
    public String getCamera( ) {
	return cameraField.getText( );
    }
    public void setCameraMultivalued( boolean mv ) {
    }
    
    public void setLens( String newValue ) {
    }
    
    public String getLens( ) {
	return lensField.getText( );
    }

    public void setLensMultivalued( boolean mv ) {
    }
    
    public void setFilm( String newValue ) {
    }
    
    public String getFilm( ) {
	return filmField.getText( );
    }

    public void setFilmMultivalued( boolean mv ) {
    }
    
    public void setDescription( String newValue ) {
        descriptionTextArea.getDocument().removeDocumentListener( this );
	descriptionTextArea.setText( newValue );
        descriptionTextArea.getDocument().addDocumentListener( this );
    }
    
    public String getDescription( ) {
	return descriptionTextArea.getText( );
    }

    public void setDescriptionMultivalued( boolean mv ) {
	descriptionTextArea.setBackground( mv ? multiValueColor : singleValueColor );
    }

    
    public void setTechNotes( String newValue ) {
        technoteTextArea.getDocument().removeDocumentListener( this );
	technoteTextArea.setText( newValue );
        technoteTextArea.getDocument().addDocumentListener( this );
    }
    
    public String getTechNotes( ) {
	return technoteTextArea.getText( );
    }
    
    public void setTechNotesMultivalued( boolean mv ) {
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
    Document shootingDayDoc = null;
    JTextField shootingPlaceField = null;
    Document shootingPlaceDoc = null;
    JTextArea descriptionTextArea = null;
    Document descriptionDoc = null;

    String qualityStrings[] = { "Unevaluated", "Top", "Good", "OK", "Poor", "Unusable" };
    JComboBox qualityField = null;

    TagList2 tagList = null;

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
	if ( evt.getSource() == qualityField ) {
	    log.debug( "quality changed"  );
	    // If getQuality returns null this action event is generated
	    // by the controller that is setting up quality field to display
	    // model with multiple quality values.
	    if ( getQuality() != null ) {
		ctrl.viewChanged( this, PhotoInfoFields.QUALITY, getQuality() );
	    }
	}
    }

    // DocumentListener interface implementation
    public void changedUpdate( DocumentEvent ev ) {
    }

    /**
     DocumentEvent is generated when text field (or to be more exact, its 
     document) is modified.
     @param ev The DocumentEvent describing the change.
     */
    public void insertUpdate( DocumentEvent ev ) {
	Document changedDoc = ev.getDocument();
	PhotoInfoFields changedField = (PhotoInfoFields) changedDoc.getProperty( FIELD );	
        Set fieldValues = ctrl.getFieldValues( changedField );
	/* Avoid emptying model when the field has multiple values
	   in the model.
	*/
        Object value = getField( changedField );
        StringBuffer debugMsg = new StringBuffer();
        debugMsg.append( "insertUpdate " ).append( changedField ).append( ": ").append( value );
        debugMsg.append( "\nOld values: [");
        boolean first = true;
        for ( Object oldValue : fieldValues ) {
            if ( !first ) debugMsg.append( ", " );
            debugMsg.append( oldValue );
            first = false;
        }
        debugMsg.append( "]" );
        log.debug( debugMsg.toString() );
        
        
        if ( ( fieldValues.size() == 1 && !fieldValues.iterator().next().equals( value ) ) || 
                ( fieldValues.size() != 1 && changedDoc.getLength() > 0 ) ) {
            ctrl.viewChanged( this, changedField, value );
        }
    }

    public void removeUpdate( DocumentEvent ev ) {
	insertUpdate( ev );
    }

    /**
     PropertyChange method is called when a FormattedTextField is modified
     @param ev The event
     */
    public void propertyChange( PropertyChangeEvent ev ) {
	if ( ev.getPropertyName().equals( "value" ) ) {
	    Object src = ev.getSource();
	    if ( src.getClass() == JFormattedTextField.class ) {
		PhotoInfoFields field = 
                        (PhotoInfoFields) ((JFormattedTextField) src).getClientProperty( FIELD );
                Object value = ((JFormattedTextField) src).getValue();

                StringBuffer debugMsg = new StringBuffer();
                debugMsg.append( "valueChange " ).append( field ).append( ": ").append( value );
                
                /* Field value is set to null (as it is when ctrl is
                 controlling multiple photos which have differing
		 value for te field) this is called every time the
		 field is accessed, so we must not notify the
		 controller.  After the user has actually set the
		 value it is no longer null.
		*/
		if ( value != null ) {
		    log.debug( "Property changed: " +  field );
		    ctrl.viewChanged( this, field, value );
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

    RawConversionSettings rawSettings = null;
    public void setRawSettings(RawConversionSettings rs ) {
        rawSettings = rs;
    }

    public void setRawSettingsMultivalued(boolean mv) {
    }

    public RawConversionSettings getRawSettings() {
        return rawSettings;
    }

    public void setColorChannelCurve(String name, ColorCurve curve) {
    }

    public void setColorChannelMultivalued(String name, boolean isMultivalued, ColorCurve[] values ) {
    }

    public ColorCurve getColorChannelCurve(String name) {
        return null;
    }

    public void setColorChannelMapping(ChannelMapOperation cm) {
    }

    public ChannelMapOperation getColorChannelMapping() {
        return null;
    }

    public void setColorChannelMappingMultivalued(boolean mv) {
    }


    public void setField(PhotoInfoFields field, Object newValue) {
        StringBuffer debugMsg = new StringBuffer();
        debugMsg.append( "setField " ).append( field ).append( ": ").append( newValue );
        log.debug( debugMsg.toString() );
        String propertyName = field.getName();
        try {
            PropertyUtils.setProperty( this, propertyName, newValue );
        } catch (NoSuchMethodException ex) {
            log.error( "Cannot set property " + propertyName );
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            log.error( ex.getMessage() );
        } catch (InvocationTargetException ex) {
            log.error( ex.getMessage() );
        }
    }

    /**
     Get the current value of given field in the editor
     @param field The field to get
     @return Value of field
     */

    public Object getField(PhotoInfoFields field) {
        Object value = null;
        switch( field ) {
            case CAMERA:
                value = getCamera();
                break;
            case DESCRIPTION:
                value = getDescription();
                break;
            case FILM:
                value = getFilm();
                break;
            case FILM_SPEED:
                value = getFilmSpeed();
                break;
            case FOCAL_LENGTH:
                value = getFocalLength();
                break;
            case FSTOP:
                value = getFStop();
                break;
            case LENS:
                value = getLens();
                break;
            case PHOTOGRAPHER:
                value = getPhotographer();
                break;
            case QUALITY:
                value = getQuality();
                break;
            case SHOOTING_PLACE:
                value = getShootingPlace();
                break;
            case FUZZY_SHOOT_TIME:
                value = getFuzzyDate();
                break;
            case SHUTTER_SPEED:
                value = getShutterSpeed();
                break;
            case TECH_NOTES:
                value = getTechNotes();
                break;
            default:
                log.debug( "field " + field + " not available" );
        }
        return value;
    }

    /**     
     Set the multivalued state of given field
     @param field Field to set
     @param isMultivalued If <code>null, set the field to multivalued state. If not, set
     it to normal state 
     */
    public void setFieldMultivalued(PhotoInfoFields field, 
            boolean isMultivalued) {
        String propertyName = field.getName() + "Multivalued";
        try {
            PropertyUtils.setProperty( this, propertyName, isMultivalued ); 
        } catch (NoSuchMethodException ex) {
            log.error( "Cannot set property " + propertyName );
            ex.printStackTrace();
        } catch (IllegalAccessException ex) {
            log.error( ex.getMessage() );
        } catch (InvocationTargetException ex) {
            log.error( ex.getMessage() );
        }
    }

    /**
     Set a field in the UI to given value
     @param field The field to set
     @param newValue New value for the field
     @param refValues Reference values (i.e. other values that the selection has 
     for this field. If there are more than 1 reference value and newValue is 
     <code>null</code> the field ins interpreted to be in multivalued state.
     */
    public void setField(PhotoInfoFields field, Object newValue, java.util.List refValues) {
        StringBuffer debugMsg = new StringBuffer();
        debugMsg.append( "setField " ).append( field ).append( ": ").append( newValue );
        log.debug( debugMsg.toString() );

        boolean isMultivalued = false;
        if ( newValue == null && refValues != null && refValues.size() > 1 ) {
            isMultivalued = true;
        }
        switch( field ) {
            case CAMERA:
                setCamera( (String) newValue);
                setCameraMultivalued( isMultivalued );
                break;
            case DESCRIPTION:
                setDescription( (String) newValue);
                setDescriptionMultivalued( isMultivalued );
                break;
            case FILM:
                setFilm( (String) newValue);
                setFilmMultivalued( isMultivalued );
                break;
            case FILM_SPEED:
                setFilmSpeed( (Number) newValue);
                setFilmSpeedMultivalued( isMultivalued );
                break;
            case FOCAL_LENGTH:
                setFocalLength( (Number) newValue);
                setFocalLengthMultivalued( isMultivalued );
                break;
            case FSTOP:
                setFStop( (Number) newValue);
                setFStopMultivalued( isMultivalued );
                break;
            case LENS:
                setLens( (String) newValue);
                setLensMultivalued( isMultivalued );
                break;
            case PHOTOGRAPHER:
                setPhotographer( (String) newValue);
                setPhotographerMultivalued( isMultivalued );
                break;
            case QUALITY:
                setQuality( (Number) newValue);
                setQualityMultivalued( isMultivalued );
                break;
            case SHOOTING_PLACE:
                setShootingPlace( (String) newValue);
                setShootingPlaceMultivalued( isMultivalued );
                break;
            case FUZZY_SHOOT_TIME:
                setFuzzyDate( (FuzzyDate) newValue );
                setFuzzyDateMultivalued( isMultivalued );
                break;
            case SHUTTER_SPEED:
                setShutterSpeed( (Number) newValue);
                setShutterSpeedMultivalued( isMultivalued );
                break;
            case TECH_NOTES:
                setTechNotes( (String) newValue);
                setTechNotesMultivalued( isMultivalued );
                break;
            default:
                log.debug( "field " + field + " not used" );
        }
    }

    private PhotoSelectionController ctrl = null;
    private static final String FIELD = "FIELD";

    public void setHistogram( String channel, int[] histData ) {
    }

    public void setTagListModel( ListModel listModel ) {
        tagList.setModel( listModel );
    }
}
