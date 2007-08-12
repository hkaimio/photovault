/*
  Copyright (c) 2007, Harri Kaimio
 
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

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageEncoder;
import com.sun.media.jai.codec.JPEGEncodeParam;
import java.awt.geom.Rectangle2D;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Set;
import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jdesktop.swingworker.SwingWorker;
import org.photovault.command.CommandException;
import org.photovault.command.PhotovaultCommandHandler;
import org.photovault.common.PhotovaultException;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.dcraw.RawImage;
import org.photovault.image.ChannelMapOperation;
import org.photovault.image.PhotovaultImage;
import org.photovault.image.PhotovaultImageFactory;
import org.photovault.imginfo.CreateImageInstanceCommand;
import org.photovault.imginfo.DeleteImageInstanceCommand;
import org.photovault.imginfo.ImageInstance;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.imginfo.Volume;

/**
 This utility class creates missing instances of a {@link PhotoInfo} in a
 background thread.
 
 */
public class PhotoInstanceCreator extends SwingWorker {
    private static Log log = LogFactory.getLog(PhotoInstanceCreator.class);
    
    final int maxThumbWidth = 100;
    final int maxThumbHeight = 100;
    
    /**
     Photo whose instances will be created
     */
    private PhotoInfo photo;
    /**
     volume in which the instances will be created
     */
    private Volume volume;
    /**
     Instance that will be used as a source for the instances
     */
    private ImageInstance srcInstance = null;
    
    CreateImageInstanceCommand createCmd = null;
    
    PhotoInfoDAO photoDAO;
    
    /**
     View that asked for the thumbnail.
     */
    PhotoCollectionThumbView view;
    
    /**
     Source image file that will be used when creating instances
     */
    File srcFile;
    
    /**
     Desired crop bounds
     */
    Rectangle2D cropBounds;
    RawConversionSettings rawSettings;
    double srcRotation;
    double prefRotation;
    ChannelMapOperation channelMap;
    
    /**
     Instances that are outdated and should be deleted
     */
    Set<ImageInstance> staleInstances = new HashSet<ImageInstance>();
    
    /** Creates a new instance of PhotoInstanceCreator */
    public PhotoInstanceCreator( PhotoCollectionThumbView view, PhotoInfoDAO photoDAO, PhotoInfo photo, Volume vol ) {
        this.view = view;
        this.photo = photo;
        this.photoDAO = photoDAO;
        volume = vol;
        // Find the original image to use as a staring point
        for ( ImageInstance instance : photo.getInstances() ) {
            if ( instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL
                    && instance.getImageFile() != null
                    && instance.getImageFile().exists() ) {
                srcInstance = instance;
            } else if ( instance.getInstanceType() != ImageInstance.INSTANCE_TYPE_ORIGINAL ) {
                /*
                 If the instance file does not exist or the instance in no longer
                 valid delete it.
                 */
                File f = instance.getImageFile();
                if ( f != null && ( !f.exists() ) || !matchesCurrentSettings( instance ) ) {
                    /*
                     We want to delete instances that do have associated file
                     but the file either has been deleted or is obsolete.
                     */
                    staleInstances.add( instance );
                }
            }
        }
        
        if ( srcInstance != null ) {
            srcFile = srcInstance.getImageFile();
            cropBounds = photo.getCropBounds();
            rawSettings = photo.getRawSettings();
            srcRotation = srcInstance.getRotated();
            prefRotation = photo.getPrefRotation();
            channelMap = photo.getColorChannelMapping();
        }
    }    
    
    /**
     The image processing operations that are executed asynchronously. This 
     method creates the instance file based on {@link srcInstance} and
     also instantiates createCmd but does not yet execute it.
     */
    
    protected Object doInBackground() throws Exception {
        if ( srcInstance == null ) {
            return null;
        }
        
        /*
         We try to ensure that the thumbnail is actually from the original image
         by comparing aspect ratio of it to original. This is not a perfect check
         but it will usually catch the most typical errors (like having a the original
         rotated by RAW conversion SW but still the original EXIF thumbnail.
         */
        double origAspect = this.getAspect(
                srcInstance.getWidth(),
                srcInstance.getHeight(), 1.0 );
        double aspectAccuracy = 0.01;
        
        // First, check if there is a thumbnail in image header
        RenderedImage origImage = null;
        
        // Read the image
        RenderedImage thumbImage = null;
        try {
            File imageFile = srcInstance.getImageFile();
            PhotovaultImageFactory imgFactory = new PhotovaultImageFactory();
            PhotovaultImage img = imgFactory.create( imageFile, false, false );
            img.setCropBounds( cropBounds );
            img.setRotation( prefRotation - srcRotation );
            if ( channelMap != null ) {
                img.setColorAdjustment( channelMap );
            }
            if ( img instanceof RawImage ) {
                RawImage ri = (RawImage) img;
                ri.setRawSettings( rawSettings );
            }
            thumbImage = img.getRenderedImage( maxThumbWidth, maxThumbHeight, true );
        } catch ( Exception e ) {
            log.warn( "Error reading image: " + e.getMessage() );
            throw e;
        }
        log.debug( "Done, finding name" );
        
        // Find where to store the file in the target volume
        File thumbnailFile = volume.getInstanceName( photo, "jpg" );
        log.debug( "name = " + thumbnailFile.getName() );
        
        try {
            saveInstance( thumbnailFile, thumbImage );
            if ( thumbImage instanceof PlanarImage ) {
                ((PlanarImage)thumbImage).dispose();
                System.gc();
            }
        } catch (PhotovaultException ex) {
            log.error( "error writing thumbnail for " + srcInstance.getImageFile().getAbsolutePath() +
                    ": " + ex.getMessage() );
            throw ex;
        }
        try {
            
            createCmd = new CreateImageInstanceCommand( volume, thumbnailFile, photo, ImageInstance.INSTANCE_TYPE_THUMBNAIL );
        } catch (PhotovaultException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        createCmd.setCropBounds( cropBounds );
        createCmd.setRotated( prefRotation );
        createCmd.setRawSettings( rawSettings );
        createCmd.setColorChannelMapping( channelMap );
        PhotovaultCommandHandler cmdHandler = view.ctrl.getCommandHandler();
        try {
            cmdHandler.executeCommand( createCmd );
            if ( this.staleInstances.size() > 0 ) {
                DeleteImageInstanceCommand deleteCmd = new DeleteImageInstanceCommand( staleInstances );
                cmdHandler.executeCommand( deleteCmd );
            }
        } catch (CommandException ex) {
            ex.printStackTrace();
        }
        return null;
    }
    
    /**
     This method is called in AWT event thread context after the image file has
     been saved. Persist the image instance in and merge changes to current context
     */
    public void done() {
        view.thumbnailCreated( photo );
    }
    
    private boolean matchesCurrentSettings( ImageInstance instance ) {
        PhotoInfo p = instance.getPhoto();
        ChannelMapOperation channelMap = p.getColorChannelMapping();
        RawConversionSettings rawSettings = p.getRawSettings();
        return Math.abs(instance.getRotated() - p.getPrefRotation() ) < 0.0001
                && instance.getCropBounds().equals( p.getCropBounds() )
                && (channelMap == null || channelMap.equals( instance.getColorChannelMapping()))
                && ( rawSettings == null || rawSettings.equals( instance.getRawSettings()));
    }
    
    
    /**
     Helper function to calculate aspect ratio of an image
     @param width width of the image
     @param height height of the image
     @param pixelAspect Aspect ratio of a single pixel (width/height)
     @return aspect ratio (width/height)
     */
    private double getAspect( int width, int height, double pixelAspect ) {
        return height > 0
                ? pixelAspect*(((double) width) / ((double) height )) : -1.0;
    }
    
    /**
     Helper function to save a rendered image to file
     @param instanceFile The file into which the image will be saved
     @param img Image that willb e saved
     @throws PhotovaultException if saving does not succeed
     */
    protected void saveInstance( File instanceFile, RenderedImage img ) throws PhotovaultException {
        OutputStream out = null;
        log.debug( "Entry: saveInstance, file = " + instanceFile.getAbsolutePath() );
        try {
            out = new FileOutputStream( instanceFile.getAbsolutePath());
        } catch(IOException e) {
            log.error( "Error writing thumbnail: " + e.getMessage() );
            throw new PhotovaultException( e.getMessage() );
        }
        if ( img.getSampleModel().getSampleSize( 0 ) == 16 ) {
            log.debug( "16 bit image, converting to 8 bits");
            double[] subtract = new double[1]; subtract[0] = 0;
            double[] divide   = new double[1]; divide[0]   = 1./256.;
            // Now we can rescale the pixels gray levels:
            ParameterBlock pbRescale = new ParameterBlock();
            pbRescale.add(divide);
            pbRescale.add(subtract);
            pbRescale.addSource( img );
            PlanarImage outputImage = (PlanarImage)JAI.create("rescale", pbRescale, null);
            // Make sure it is a byte image - force conversion.
            ParameterBlock pbConvert = new ParameterBlock();
            pbConvert.addSource(outputImage);
            pbConvert.add(DataBuffer.TYPE_BYTE);
            img = JAI.create("format", pbConvert);
        }
        JPEGEncodeParam encodeParam = new JPEGEncodeParam();
        ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG", out,
                encodeParam);
        try {
            log.debug( "starting JPEG enconde" );
            encoder.encode( img );
            log.debug( "done JPEG encode" );
            out.close();
            // origImage.dispose();
        } catch (Exception e) {
            log.warn( "Exception while encoding" + e.getMessage() );
            throw new PhotovaultException( "Error writing instance " +
                    instanceFile.getAbsolutePath()+ ": " +
                    e.getMessage() );
        }
        log.debug( "Exit: saveInstance" );
    }
}
