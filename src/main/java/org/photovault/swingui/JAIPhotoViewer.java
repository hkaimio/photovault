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

import org.photovault.command.CommandException;
import org.photovault.common.PhotovaultException;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.dcraw.RawImage;
import org.photovault.dcraw.RawImageChangeEvent;
import org.photovault.dcraw.RawImageChangeListener;
import org.photovault.image.ColorCurve;
import org.photovault.image.PhotovaultImage;
import org.photovault.image.PhotovaultImageFactory;
import org.photovault.image.ChannelMapOperation;
import org.photovault.imginfo.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoChangeEvent;
import org.photovault.imginfo.PhotoInfoChangeListener;
import org.photovault.swingui.color.ColorSettingsDlg;
import org.photovault.swingui.color.ColorSettingsPreview;
import org.photovault.swingui.color.RawSettingsPreviewEvent;

public class JAIPhotoViewer extends JPanel implements 
        PhotoInfoChangeListener, ComponentListener, CropAreaChangeListener,
        RawImageChangeListener, ColorSettingsPreview {
    static Log log = LogFactory.getLog( JAIPhotoViewer.class.getName() );

    public JAIPhotoViewer( PhotoViewController ctrl ) {
	super();
        this.ctrl = ctrl;
	createUI();
	
    }

    PhotoViewController ctrl;
    
    public void createUI() {
	setLayout( new BorderLayout() );
        addComponentListener( this );
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
                KeyEvent.VK_O, null );
        
    }
    
    float rawConvScaling = 1.0f;
    
    public void setScale( float scale ) {
        imageView.setScale(scale);
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
     Operations that must be applied to the image dynamically, i.e. instances in 
     which these are already applied cannot be used.
     */
    private EnumSet<ImageOperations> dynOps = EnumSet.noneOf( ImageOperations.class );
    
    /**
     Operations that are applied to the currently displayed instance.
     */
    private EnumSet<ImageOperations> appliedOps = EnumSet.noneOf( ImageOperations.class );
    
    /**
     Local raw settings that override those in database or <code>null</code> 
     if none.
     */
    private RawConversionSettings localRawSettings = null;
    
    private ChannelMapOperation localChanMap = null;
    
    
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
        dynOps = EnumSet.noneOf( ImageOperations.class );
        localRawSettings = null;
        localChanMap = null;

        log.debug( "JAIPhotoViewer.setPhoto() photo="  + photo.getUid() );

	photo.addChangeListener( this );
        try {
            showBestInstance();
        } catch (PhotovaultException ex) {
            throw new FileNotFoundException( ex.getMessage() );
        }

    }

    /**
     Finds the best instance of the current photo and shows it. The instance is 
     selected so that it needs as little postprocessing as possible. Hovewer, the 
     operations in {@link dynOps} must not be preapplied in the instance since 
     these may be changing during viewing of the image.
     
     */
    private void showBestInstance() throws PhotovaultException {
        EnumSet<ImageOperations> allowedOps = EnumSet.allOf( ImageOperations.class );
        allowedOps.removeAll( dynOps );
        ImageDescriptorBase image = photo.getPreferredImage( 
                EnumSet.noneOf( ImageOperations.class ),
                allowedOps,
                imageView.getWidth(), imageView.getHeight(),
                Integer.MAX_VALUE, Integer.MAX_VALUE );
        if ( image != null && image.getLocator().equals( "image#0" ) ) {
            File imageFile = image.getFile().findAvailableCopy();
            if ( imageFile != null && imageFile.canRead() ) {
                // TODO: is this really needed?
                String fname = imageFile.getName();
                int lastDotPos = fname.lastIndexOf( "." );
                if ( lastDotPos <= 0 || lastDotPos >= fname.length()-1 ) {
                    // TODO: error handling needs thinking!!!!
                    // throw new IOException( "Cannot determine file type extension of " + imageFile.getAbsolutePath() );
                    fireViewChangeEvent();
                    return;
                }
                PhotovaultImageFactory imageFactory = new PhotovaultImageFactory();
                PhotovaultImage img = null;
                try {
                        /*
                         Do not read the image yet since setting raw conversion
                         parameters later may force a re-read.
                         */
                    img = imageFactory.create(imageFile, false, false);
                } catch (PhotovaultException ex) {
                    final JAIPhotoViewer component = this;
                    final String msg = ex.getMessage();
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            JOptionPane.showMessageDialog( component,
                                    msg, "Error loading file",
                                    JOptionPane.ERROR_MESSAGE );
                        }
                    } );
                }
                if ( img != null ) {
                    appliedOps = EnumSet.noneOf( ImageOperations.class );
                    if ( rawImage != null ) {
                        rawImage.removeChangeListener( this );
                    }
                    if ( img instanceof RawImage ) {
                        rawImage = (RawImage) img;
                        rawImage.setRawSettings( localRawSettings != null ? localRawSettings : photo.getRawSettings(  ) );
                        rawImage.addChangeListener( this );
                        // Check the correct resolution for this image
                        if ( isFit ) {
                            fit(  );
                        } else {
                            setScale( getScale(  ) );
                        }
                    } else {
                        rawImage = null;
                        rawConvScaling = 1.0f;
                    }
                }
                if ( image instanceof CopyImageDescriptor ) {
                    // This is a copy, so it may be cropped already
                    appliedOps = ((CopyImageDescriptor) image).getAppliedOperations(  );
                    if ( !appliedOps.contains( ImageOperations.COLOR_MAP ) ) {
                        img.setColorAdjustment( localChanMap != null ? localChanMap : photo.getColorChannelMapping(  ) );
                    }
                    if ( !appliedOps.contains( ImageOperations.CROP ) ) {
                        instanceRotation = ((CopyImageDescriptor) image).getRotation(  );
                        double rot = photo.getPrefRotation(  ) - instanceRotation;
                        imageView.setRotation( rot );
                        imageView.setCrop( photo.getCropBounds(  ) );
                    }
                } else {
                    // This is original so we must apply corrections
                    img.setColorAdjustment( localChanMap != null ? localChanMap : photo.getColorChannelMapping(  ) );
                    imageView.setRotation( photo.getPrefRotation(  )  );
                    imageView.setCrop( photo.getCropBounds(  ) );
                }
                setImage( img );
                fireViewChangeEvent(  );
                return;
            }
        }
        // if we get this far no instance of the original image has been found
        setImage( null );
        throw new PhotovaultException( "No suitable instance of photo " 
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
     {@link RawImage} of the currently displayed instance or <code>null</code>
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
    
    void setImage( PhotovaultImage img ) {
	imageView.setImage( img );
    }
    
    /**
     Get the actual image currently displayed in the control
     */
    public PhotovaultImage getImage() {
        return imageView.getImage();
    }

    public void cropAreaChanged(CropAreaChangeEvent evt) {
        ChangePhotoInfoCommand changeCmd = new ChangePhotoInfoCommand( photo.getId() );
        changeCmd.setPrefRotation( evt.getRotation() );
        changeCmd.setCropBounds( evt.getCropArea() );
        try {
            ctrl.getCommandHandler().executeCommand( changeCmd );
        } catch (CommandException ex) {
            log.error( ex.getMessage() );
            JOptionPane.showMessageDialog( this, 
                    "Error while storing new crop settings:\n" + ex.getMessage(), 
                    "Crop error", JOptionPane.ERROR_MESSAGE );
        }
        imageView.setDrawCropped( true );
    }

    public void rawImageSettingsChanged(RawImageChangeEvent ev) {
        /*
         TODO: This may recreate the converted image unnecessarily. It would 
         probably be faster to just modify the parameters in image chain & 
         redraw.
         */
        // RenderedImage origImage = rawImage.getCorrectedImage();
        setImage( imageView.getImage() );
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
     Called when raw settings in another control that uses this control as 
     a preview image are changed.
     @param e Event that describes the change.
     */
    public void previewRawSettingsChanged(RawSettingsPreviewEvent e) {
        PhotoInfo[] model = e.getModel();
        if ( model != null && model.length == 1 && model[0] == photo ) {
            localRawSettings = e.getNewSettings();
            // We must use the original raw image, not instaqnce which has already been converted.
            dynOps.add( ImageOperations.RAW_CONVERSION );
            if ( appliedOps.contains( ImageOperations.RAW_CONVERSION ) ) {
                try {
                    showBestInstance();
                } catch (PhotovaultException ex) {
                    final JAIPhotoViewer component = this;
                    SwingUtilities.invokeLater( new Runnable() {
                        public void run() {
                            JOptionPane.showMessageDialog( component,                                    
                                    "Cannot change raw settings, original raw image not found", 
                                    "Cannot load raw image",
                                    JOptionPane.ERROR_MESSAGE );
                        }
                    });
                }
            } else if ( rawImage != null ) {
                rawImage.setRawSettings( localRawSettings );
                
            }
        }
    }
    
    /**
     Called when color settings in another control that uses this control as 
     a preview image are changed.
     @param v the {@link PhotoInfoView} that initiated the changes
     @param name Name of the color curve that was changed
     @param c The changed color curve
     */
    public void setColorCurve( String name, ColorCurve c ) {
        dynOps.add( ImageOperations.COLOR_MAP );
        if ( appliedOps.contains( ImageOperations.COLOR_MAP ) ) {
            try {
                // Current instance has color mapping preapplied so we cannot use it.
                showBestInstance();
            } catch (PhotovaultException ex) {
                ex.printStackTrace();
            }
        }
        imageView.setColorCurve( name, c );
        // Save the channel mapping so that it can be reapplied if we must again
        // cahnge the used instance for some other reason.
        if ( imageView.getImage() != null ) {
            localChanMap = imageView.getImage().getColorAdjustment();
        }
    }

    public void setSaturation(double newSat) {
        imageView.setSaturation( newSat );
    }

    public void setField(PhotoInfoFields field, Object value, java.util.List refValues) {
        
        switch ( field ) {
            case COLOR_CURVE_VALUE:
                setColorCurve( "value", (ColorCurve) value );
                break;
            case COLOR_CURVE_RED:
                setColorCurve( "red", (ColorCurve) value );
                break;
            case COLOR_CURVE_GREEN:
                setColorCurve( "green", (ColorCurve) value );
                break;
            case COLOR_CURVE_BLUE:
                setColorCurve( "blue", (ColorCurve) value );
                break;
            case COLOR_CURVE_SATURATION:
                setColorCurve( "saturation", (ColorCurve) value );
                break;
        }
    }
    
    JAIPhotoView imageView = null;
    JScrollPane scrollPane = null;
}    
