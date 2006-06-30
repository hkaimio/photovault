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
  along with Foobar; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.swingui;

import org.photovault.imginfo.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.geom.AffineTransform;
import java.text.*;
import java.util.*;
import javax.media.jai.PlanarImage;
import javax.media.jai.JAI;
import org.photovault.imginfo.ImageInstance;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoChangeEvent;
import org.photovault.imginfo.PhotoInfoChangeListener;

public class JAIPhotoViewer extends JPanel implements 
        PhotoInfoChangeListener, ComponentListener, CropAreaChangeListener {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( JAIPhotoViewer.class.getName() );

    public JAIPhotoViewer() {
	super();
	createUI();
	
    }

    public void createUI() {
	setLayout( new BorderLayout() );
	imageView = new JAIPhotoView();
        imageView.addCropAreaChangeListener( this );
	scrollPane = new JScrollPane( imageView );
	scrollPane.setPreferredSize( new Dimension( 500, 500 ) );
        add( scrollPane, BorderLayout.CENTER );
        
        // Get the crop icon
        ImageIcon cropIcon = null;
        java.net.URL cropIconURL = JAIPhotoViewer.class.getClassLoader().getResource( "crop_icon.png" );
        if ( cropIconURL != null ) {
            cropIcon = new ImageIcon( cropIconURL );
        }
        
        cropPhotoAction = new CropPhotoAction( imageView,
                "Crop photo", cropIcon, "Crop or rotate the selected photo",
                KeyEvent.VK_C, null );
        
        // Listen fot resize events of the scroll area so that fitted image can
        // be resized as well.
        addComponentListener( this );

	JToolBar toolbar = new JToolBar();

	String[] defaultZooms = {
	    "12%",
	    "25%",
	    "50%",
	    "100%",
	    "Fit"
	};
	JLabel zoomLabel = new JLabel( "Zoom" );
	JComboBox zoomCombo = new JComboBox( defaultZooms );
	zoomCombo.setEditable( true );
	zoomCombo.setSelectedItem( "Fit" );

	final JAIPhotoViewer viewer = this;
	zoomCombo.addActionListener(  new ActionListener() {
		public void actionPerformed( ActionEvent ev ) {
		    JComboBox cb = (JComboBox) ev.getSource();
		    String selected = (String)cb.getSelectedItem();
		    log.debug( "Selected: " + selected );
		    isFit = false;

		    // Parse the pattern
		    DecimalFormat percentFormat = new DecimalFormat( "#####.#%" );
		    if ( selected == "Fit" ) {
			isFit = true;
			log.debug( "Fitting to window" );
			fit();
			String strNewScale = "Fit";
			cb.setSelectedItem( strNewScale );
		    } else {
			// Parse the number. First remove all white space to ease the parsing
			StringBuffer b = new StringBuffer( selected );
			int spaceIndex =  b.indexOf( " " );
			while ( spaceIndex >= 0  ) {
			    b.deleteCharAt( spaceIndex );
			    spaceIndex =  b.indexOf( " " );
			}
			selected = b.toString();
			boolean success = false;
			float newScale = 0;
			try {
			    newScale = Float.parseFloat( selected );
			    newScale /= 100.0;
			    success = true;
			} catch (NumberFormatException e ) {
			    // Not a float
			}
			if ( !success ) {
			    try {
				newScale = percentFormat.parse( selected ).floatValue();
				success = true;
			    } catch ( ParseException e ) {
			    }
			}
			if ( success ) {
			    log.debug( "New scale: " + newScale );
			    viewer.setScale( newScale );
			    String strNewScale = percentFormat.format( newScale );
			    cb.setSelectedItem( strNewScale );
			} 
		    }
		}
	    });

	toolbar.add( zoomLabel );
	toolbar.add( zoomCombo );

        // Create the crop checkbox
        JButton cropBtn = new JButton( cropPhotoAction  );
        cropBtn.setText( "" );
        toolbar.add( cropBtn );
        
	add( toolbar, BorderLayout.NORTH );
	    
    }

    public void setScale( float scale ) {
	imageView.setScale(scale);
    }

    public float getScale() {
	return imageView.getScale();
    }

    public void fit() {
	Dimension displaySize = scrollPane.getSize();
        log.debug( "fit to " + displaySize.getWidth() + ", " + displaySize.getHeight() );
	imageView.fitToRect( displaySize.getWidth()-4, displaySize.getHeight()-4 );
    }
    
    /**
     Select whether to show the image cropped according to preferred state stored 
     in database or as its original state.
     */
    public void setShowCroppedPhoto( boolean crop ) {
        imageView.setDrawCropped( crop );
    }

    
    public boolean getShowCroppedPhoto( ) {
        return imageView.getDrawCropped();
    }
    
    /**
     Set the photo displayed in the component.
     
     The function tries to search for a suitable instance of the photo to display.
     If none  are found the Photo is set to <code>null</code> and an exception 
     is reported to application.
     @param photo The photo to be displayed.
     @throws FileNotFoundException if the instance file cannot be found. This can
     happen if e.g. user has deleted an image file from directory indexed as an 
     external volume.
     */
    public void setPhoto( PhotoInfo photo ) throws FileNotFoundException {
	if ( this.photo != null ) {
	    this.photo.removeChangeListener( this );
	}
	this.photo = photo;
	if ( photo == null ) {
	    setImage( null );
	    return;
	}

        log.debug( "JAIPhotoViewer.setPhoto() photo="  + photo.getUid() );

	photo.addChangeListener( this );

	// Find the original file
	ImageInstance original = null;
	Vector instances = photo.getInstances();
	for ( int n = 0; n < instances.size(); n++ ) {
	    ImageInstance instance = (ImageInstance) instances.get( n );
	    if ( instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL ) {
		original = instance;
		break;
	    } 
	}
	if ( original == null ) {
	    log.debug( "Error - no original image was found!!!" );
	} else {
            File imageFile = original.getImageFile();
            if ( imageFile == null ) {
                throw new FileNotFoundException( );
            }
	    final String imageFilePath = original.getImageFile().getAbsolutePath();
            log.debug( "loading image " + imageFilePath );
            PlanarImage origImage = JAI.create( "fileload", imageFilePath );
	    log.debug( "image " + imageFilePath + " loaded");
            setImage( origImage );
	    instanceRotation = original.getRotated();
	    double rot = photo.getPrefRotation() - instanceRotation;
	    imageView.setRotation( rot );
            imageView.setCrop( photo.getCropBounds() );
	}
    }

    // Rotation of the currently displayed instance (compared to the original)
    double instanceRotation = 0;
    
    public PhotoInfo  getPhoto() {
	return photo;
    }

    CropPhotoAction cropPhotoAction;
    
    public CropPhotoAction getCropAction() {
        return cropPhotoAction;
    }


    public void componentHidden( ComponentEvent e) {
    }

    public void componentMoved( ComponentEvent e) {
    }

    public void componentResized( ComponentEvent e) {
	if ( isFit ) {
            fit();
	}
			 
    }

    public void componentShown( ComponentEvent e) {
    }
    
    
    
    
    PhotoInfo photo = null;
    boolean isFit = true;

    /**
       Implementation of the photoInfoChangeListener interface. Checks if the preferred rotation is changed
       and adjusts displayed image accordingly
    */
    public void photoInfoChanged( PhotoInfoChangeEvent e ) {
	double newRotation = photo.getPrefRotation() - instanceRotation;
	if ( Math.abs( newRotation - imageView.getRotation() ) > 0.1 ) {
	    imageView.setRotation( newRotation );
	}
        imageView.setCrop( photo.getCropBounds() );
    }
    
    void setImage( RenderedImage bi ) {
	imageView.setImage( bi );
    }

    public void cropAreaChanged(CropAreaChangeEvent evt) {
        photo.setPrefRotation( evt.getRotation() );
        photo.setCropBounds( evt.getCropArea() );
        imageView.setDrawCropped( true );
    }

    
    JAIPhotoView imageView = null;
    JScrollPane scrollPane = null;
}    
