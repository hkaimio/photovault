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

public class JAIPhotoViewer extends JPanel implements PhotoInfoChangeListener, ComponentListener {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( JAIPhotoViewer.class.getName() );

    public JAIPhotoViewer() {
	super();
	createUI();
	
    }

    public void createUI() {
	setLayout( new BorderLayout() );
	imageView = new JAIPhotoView();
	scrollPane = new JScrollPane( imageView );
	scrollPane.setPreferredSize( new Dimension( 500, 500 ) );
	add( scrollPane, BorderLayout.CENTER );
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
			float newScale = getScale();
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
	// 	int origWidth = imageView.getOrigWidth();
	// 	int origHeight = imageView.getOrigHeight();

	// 	if ( origWidth > 0 && origHeight > 0 ) {
	// 	    float widthScale = ((float)displaySize.getWidth())/(float)origWidth;
	// 	    float heightScale = ((float)displaySize.getHeight())/(float)origHeight;

	// 	    float scale = heightScale;
	// 	    if ( widthScale < heightScale ) {
	// 		scale = widthScale;
	// 	    }
	// 	    setScale( scale );
	// 	}
    }

    public void setPhoto( PhotoInfo photo ) {
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
	    final String imageFilePath = original.getImageFile().getAbsolutePath();
            log.debug( "loading image " + imageFilePath );
            PlanarImage origImage = JAI.create( "fileload", imageFilePath );
	    log.debug( "image " + imageFilePath + " loaded");
            setImage( origImage );
	    instanceRotation = original.getRotated();
	    double rot = photo.getPrefRotation() - instanceRotation;
	    imageView.setRotation( rot );
	}
    }

    // Rotation of the currently displayed instance (compared to the original)
    double instanceRotation = 0;
    
    public PhotoInfo  getPhoto() {
	return photo;
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
    }
    
    void setImage( RenderedImage bi ) {
	imageView.setImage( bi );
    }

    
	
    
	    
    JAIPhotoView imageView = null;
    JScrollPane scrollPane = null;
}    
