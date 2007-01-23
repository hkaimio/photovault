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

import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.RenderedOp;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.dcraw.RawImage;
import org.photovault.dcraw.RawImageChangeEvent;
import org.photovault.dcraw.RawImageChangeListener;
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
import org.photovault.swingui.color.ColorSettingsDlg;
import org.photovault.swingui.color.RawSettingsPreviewEvent;

public class JAIPhotoViewer extends JPanel implements 
        PhotoInfoChangeListener, ComponentListener, CropAreaChangeListener,
        RawImageChangeListener {
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
    
    float rawConvScaling = 1.0f;
    
    public void setScale( float scale ) {
        if ( rawImage != null ) {
            // Check if the raw image needs to be refiltered
            int minWidth = (int) (rawImage.getWidth() * scale);
            int minHeight = (int) (rawImage.getHeight() * scale);
            boolean needsReload = rawImage.setMinimumPreferredSize( 
                    minWidth, minHeight ); 
            PlanarImage img = rawImage.getCorrectedImage();
            rawConvScaling = img.getWidth() / (float) rawImage.getWidth();
            imageView.setScale( scale/rawConvScaling );
            if ( needsReload ) {
                setImage( img );
            }
        } else {
            imageView.setScale(scale);
        }
    }

    public float getScale() {
	return imageView.getScale() * rawConvScaling;
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
     TODO: This function is currently messy, refactor it at the same time as 
     RawImage/image handling is refactored.
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
            fireViewChangeEvent();
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
                File imageFile = original.getImageFile();
                if ( imageFile != null && imageFile.canRead() ) {
                    String fname = imageFile.getName();
                    int lastDotPos = fname.lastIndexOf( "." );
                    if ( lastDotPos <= 0 || lastDotPos >= fname.length()-1 ) {
                        // TODO: error handling needs thinking!!!!
                        // throw new IOException( "Cannot determine file type extension of " + imageFile.getAbsolutePath() );
                        fireViewChangeEvent();
                        return;
                    }
                    String suffix = fname.substring( lastDotPos+1 );
                    Iterator readers = ImageIO.getImageReadersBySuffix( suffix );
                    RenderedImage origImage = null;
                    if ( readers.hasNext() ) {
                        ImageReader reader = (ImageReader)readers.next();
                        log.debug( "Creating stream" );
                        ImageInputStream iis = null;
                        try {
                            iis = ImageIO.createImageInputStream(original.getImageFile());
                            reader.setInput( iis, false, false );
                            origImage = reader.readAsRenderedImage( 0, null );
                        } catch (IOException ex) {
                            log.warn( ex.getMessage() );
                            ex.printStackTrace();
                            fireViewChangeEvent();
                            return;
                        }
                        rawImage = null;
                        rawConvScaling = 1.0f;
                    } else {
                        // JAI could not read the image, check if it is a raw file
                        RawImage tmpRaw = new RawImage( original.getImageFile() );
                        if ( tmpRaw.isValidRawFile() ) {
                            if ( rawImage != null ) {
                                rawImage.removeChangeListener( this );
                            }
                            rawImage = tmpRaw;
                            rawImage.setRawSettings( photo.getRawSettings() );
                            rawImage.addChangeListener( this );
                            // Check the correct resolution for this image
                            if ( !isFit ) {
                                setScale( getScale() );
                            }                            
                            origImage = rawImage.getCorrectedImage();
                        }
                    }
                    final String imageFilePath = original.getImageFile().getAbsolutePath();
                    setImage( origImage );
                    instanceRotation = original.getRotated();
                    double rot = photo.getPrefRotation() - instanceRotation;
                    imageView.setRotation( rot );
                    imageView.setCrop( photo.getCropBounds() );
                    fireViewChangeEvent();
                    return;
                }
            }
        }
        // if we get this far no instance of the original image has been found
        setImage( null );
        throw new FileNotFoundException( "No original instance of photo " 
                + photo.getUid() + " found" );
    }

    // Rotation of the currently displayed instance (compared to the original)
    double instanceRotation = 0;
    
    public PhotoInfo  getPhoto() {
	return photo;
    }
    
    /**
     Get the lookup table used in raw conversion
     @return The lookup table if current image in a raw image, <code>null</code>
     otherwise.
     @deprecated This function will be replaced with a more sphisticated histogram 
     data access API in next Photovault version.
     */
    public byte[] getRawConversionLut() {
        return ( rawImage != null ) ? rawImage.getGammaLut() : null;
    }

    /**
     Get the raw image histogram data
     @deprecated This function will be replaced with a more sphisticated histogram 
     data access API in next Photovault version.
     */
    public int[][] getRawImageHistogram() {
        return (rawImage != null ) ? rawImage.getHistogramBins() : null;
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
     {@lookup RawImage} of the currently displayed instance or <code>null</code>
     if current instance is not a raw image.
     */
    RawImage rawImage = null;
    
    ColorSettingsDlg colorDlg = null;

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

    public void rawImageSettingsChanged(RawImageChangeEvent ev) {
        /*
         TODO: This may recreate the converted image unnecessarily. It would 
         probably be faster to just modify the parameters in image chain & 
         redraw.
         */
        PlanarImage origImage = rawImage.getCorrectedImage();
        setImage( origImage );
    }

    /**
     List of current preview listener. These will be notified when the actual image
     displayed changes.
     */
    
    Vector viewChangeListeners = new Vector();
    
    /**
     Adds a listener that will be notified when the image in this view changes.
     @param l The new listener.
     */
    public void addViewChangeListener( PhotoViewChangeListener l ) {
        viewChangeListeners.add( l );
    }
    
    /**
     Removes a listener from list of liteners that will be notified of changes 
     of this view.
     @param l The listener to remove
     */
    public void removeViewChangeListener( PhotoViewChangeListener l ) {
        viewChangeListeners.remove( l );
    }
    
    /**
     Send info to all registered listeners that this view has been changed.
     */
    void fireViewChangeEvent() {
        Iterator iter = viewChangeListeners.iterator();
        PhotoViewChangeEvent e = new PhotoViewChangeEvent( this, photo );
        while ( iter.hasNext() ) {
            PhotoViewChangeListener l = (PhotoViewChangeListener) iter.next();
            l.photoViewChanged( e );
        }
    }
    
    /**
     Called when raw settings in another control that user this control as 
     a preview image are changed.
     @param e Event that describes the change.
     */
    public void previewRawSettingsChanged(RawSettingsPreviewEvent e) {
        PhotoInfo[] model = e.getModel();
        if ( model != null && model.length == 1 && model[0] == photo ) {
            RawConversionSettings r = e.getNewSettings();
            if ( rawImage != null ) {
                rawImage.setRawSettings( r );
                
            }
        }
    }
    
    JAIPhotoView imageView = null;
    JScrollPane scrollPane = null;
}    
