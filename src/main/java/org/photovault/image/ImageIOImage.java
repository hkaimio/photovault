/*
  Copyright (c) 2006 Harri Kaimio
  
  This file is part of Photovault.

  Photovault is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  Photovault is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even therrore implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.image;

import com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.EXIFParentTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.EXIFTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.TIFFDirectory;
import com.sun.media.imageio.plugins.tiff.TIFFField;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.PlanarImage;
import org.photovault.imginfo.ImageInstance;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Wrapper class for imaging pipeline for images that are read using JAI ImageIO
 */
public class ImageIOImage extends PhotovaultImage {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( ImageIOImage.class.getName() );
    
    /**
     * Creates a new instance of ImageIOImage. Note that you should not normally use
     * this constructor directly but use {@link PhotovaultImageFactory} instead.
     */
    public ImageIOImage( File f ) {
        this.f = f;
    }
    
    public static ImageIOImage getImage( File f, boolean loadImage, boolean loadMetadata ) {
        if ( getImageReader( f ) == null ) {
            return null;
        }
        ImageIOImage i = new ImageIOImage( f );
        i.load( loadImage, loadMetadata );
        return i;
    }
    
    RenderedImage image = null;
    
    /**
     * Get the image pixel data. If the iamge has not been read earlier, this method
     * reads it from disk.
     * @return The image data as an RenderedImage.
     */
    public RenderedImage getCorrectedImage() {
        if ( image == null ) {
            load( true, (metadata == null) );
        }
        return image;
    }
    
    /**
     * Get the shooting time of the image
     * @return Shooting time as reported by dcraw or <CODE>null</CODE> if
     * not available
     */
    public Date getTimestamp() {
        Date ret = null;
        String origDateStr = getEXIFTagAsString( EXIFTIFFTagSet.TAG_DATE_TIME_ORIGINAL );
        if ( origDateStr == null ) {
            origDateStr = getEXIFTagAsString( BaselineTIFFTagSet.TAG_DATE_TIME );
        }
        if ( origDateStr != null ) {
            SimpleDateFormat df = new SimpleDateFormat( "yyyy:MM:dd HH:mm:ss");
            try {
                ret = df.parse( origDateStr );
            } catch (ParseException ex) {
                ex.printStackTrace();
            }
        }
        return ret;        
    }
    
    /**
     * Get the camera mode used to shoot the image
     * @return Camera model reported by dcraw
     */
    public String getCamera() {
        // Put here both camera manufacturer and model
        String maker = getEXIFTagAsString( BaselineTIFFTagSet.TAG_MAKE );        
        String model = getEXIFTagAsString( BaselineTIFFTagSet.TAG_MODEL );
        StringBuffer cameraBuf = new StringBuffer( maker != null ? maker : "" );
        if ( model != null ) {
            cameraBuf.append( " "). append( model );
        }
        return cameraBuf.toString();
    }
    
    /**
     * Get the film speed setting used when shooting the image
     * @return Film speed (in ISO) as reported by dcraw
     */
    public int getFilmSpeed() {
        return getEXIFTagAsInt( EXIFTIFFTagSet.TAG_ISO_SPEED_RATINGS );
    }
    
    /**
     * Get the shutter speed used when shooting the image
     * @return Exposure time (in seconds) as reported by dcraw
     */
    public double getShutterSpeed() {
        return getEXIFTagAsDouble( EXIFTIFFTagSet.TAG_EXPOSURE_TIME );
    }
    
    /**
     * Get aperture (f-stop) used when shooting the image
     * @return F-stop number reported by dcraw
     */
    public double getAperture() {
        return getEXIFTagAsDouble( EXIFTIFFTagSet.TAG_F_NUMBER );
    }
    
    /**
     * Get the focal length from image file meta data.
     * @return Focal length used when taking the picture (in millimetres)
     */
    public double getFocalLength() {
        return getEXIFTagAsDouble( EXIFTIFFTagSet.TAG_FOCAL_LENGTH );
    }
    
    
    TIFFDirectory metadata = null;
    TIFFDirectory exifData = null;

    /**
     * Get a TIFF metadata field
     * @param tag Numeric ID of the tag
     * @return TIFFField object describing the tag or <CODE>null</CODE> if the tag does not 
     * exist in the image.
     */
    private TIFFField getMetadataField( int tag ) {
        if ( metadata == null ) {
            load( false, true );
        }
        
        TIFFField ret = exifData.getTIFFField( tag );
        if ( ret == null ) {
            ret = metadata.getTIFFField( tag );
        }
        return ret;        
    }
    
    /**
     * Get an EXIF tag as string
     * @param tag ID of the tag.
     * @return The tag value as String or <CODE>null</CODE> if the tag does not exist in the image
     */
    public String getEXIFTagAsString( int tag ) {
        String ret = null;
        TIFFField fld = getMetadataField( tag );
        if ( fld != null ) {
            ret = fld.getAsString( 0 );
        }
        return ret;
    }
    
    public double getEXIFTagAsDouble( int tag ) {
        double ret = 0.0;
        TIFFField fld = getMetadataField( tag );
        if ( fld != null ) {
            ret = fld.getAsDouble( 0 );
        }
        return ret;
    }

    public int getEXIFTagAsInt( int tag ) {
        int ret = 0;
        TIFFField fld = getMetadataField( tag );
        if ( fld != null ) {
            ret = fld.getAsInt( 0 );
        }
        return ret;
    }
    
    
    Rectangle2D cropBounds = null;
    
    public void setCropBounds( Rectangle2D newCrop ) {
        cropBounds = newCrop;
    }
    
    public Rectangle2D getCropBounds() {
        return cropBounds;
    }
    
    double rot = 0.0;
    
    public void setRotation( double newRot ) {
        rot = newRot;
    }
    
    public double getRotation() {
        return rot;
    }
    
    public RenderedImage getImage() {
        return null;
    }
    
    public static interface ScalingOp {
        
    };
    
    public static class MaxResolutionScalingOp implements ScalingOp {
        int width;
        int height;
        
        public MaxResolutionScalingOp( int width, int height ) {
            this.width = width;
            this.height = height;
        }
        
        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }        
    };
    
    public static class RelativeScalingOp implements ScalingOp {
        double scale = 1.0;
        
        public RelativeScalingOp( double scale ) {
            this.scale = scale;
        }
        
        public double getScale() {
            return scale;
        }
    };
    
    ScalingOp scalingOp = null;
    
    public void setScale( ScalingOp scalingOp ) {
        this.scalingOp = scalingOp;
    }
    
    public ScalingOp getScalingOp() {
        return scalingOp;
    }

    
//    private void buildXformImage() {
//        try {
//            
//            float scale = 1.0f;
//            
//            if ( fitSize ) {
//                log.debug( "fitSize" );
//                float widthScale = ((float)maxWidth)/origImage.getWidth();
//                float heightScale = ((float)maxHeight)/origImage.getHeight();
//                
//                scale = widthScale;
//                if ( heightScale < scale ) {
//                    scale = heightScale;
//                }
//                log.debug( "scale: " + scale );
//            } else {
//                scale = (float) imgScale;
//                log.debug( "scale: " + scale );
//            }
//            
//            
//            // Create the zoom xform
//            AffineTransform at = null;
//            if ( fitSize ) {
//                at = org.photovault.image.ImageXform.getFittingXform(
//                        (int)maxWidth, (int)maxHeight, imgRot,
//                        (int)( origImage.getWidth() * cropUsed.getWidth() ),
//                        (int)(( cropUsed.getHeight()* origImage.getHeight() ) ) );
//            } else {
//                at = org.photovault.image.ImageXform.getScaleXform( imgScale, imgRot,
//                        (int)( origImage.getWidth() * cropUsed.getWidth() ),
//                        (int)(( cropUsed.getHeight()* origImage.getHeight() ) ) );
//            }
//            
//            
//            // Create a ParameterBlock and specify the source and
//            // parameters
//            ParameterBlockJAI scaleParams = new ParameterBlockJAI( "affine" );
//            scaleParams.addSource( origImage );
//            scaleParams.setParameter( "transform", at );
//            scaleParams.setParameter( "interpolation", new InterpolationBilinear());
//            
//            // Create the scale operation
//            RenderedOp tmp = JAI.create( "affine", scaleParams, null );
//            
//            ParameterBlockJAI cropParams = new ParameterBlockJAI( "crop" );
//            cropParams.addSource( tmp );
//            float cropX = (float)( Math.rint( tmp.getMinX() + cropUsed.getMinX() *  tmp.getWidth() ));
//            float cropY = (float)( Math.rint( tmp.getMinY() + cropUsed.getMinY() *  tmp.getHeight() ));
//            float cropW = (float)( Math.rint( cropUsed.getWidth() * tmp.getWidth() ));
//            float cropH = (float) ( Math.rint( cropUsed.getHeight() * tmp.getHeight() ));
//            cropParams.setParameter( "x", cropX );
//            cropParams.setParameter( "y", cropY );
//            cropParams.setParameter( "width", cropW );
//            cropParams.setParameter( "height", cropH );
//            RenderedOp cropped = JAI.create("crop", cropParams, null);
//            // Translate the image so that it begins in origo
//            ParameterBlockJAI pbXlate = new ParameterBlockJAI( "translate" );
//            pbXlate.addSource( cropped );
//            pbXlate.setParameter( "xTrans", (float) (-cropped.getMinX() ) );
//            pbXlate.setParameter( "yTrans", (float) (-cropped.getMinY() ) );
//            xformImage = JAI.create( "translate", pbXlate );
//            
//        } catch ( Exception ex ) {
//                /*
//                 There was some kind of error when constructing the image to show.
//                 Most likely the image file was corrupted.
//                 */
//            final String exMsg = ex.getMessage();
//            final JAIPhotoView staticThis = this;
//            staticThis.origImage = null;
//            SwingUtilities.invokeLater( new Runnable() {
//                public void run() {
//                    JOptionPane.showMessageDialog( staticThis,
//                            "Error while showing an image\n" +
//                            exMsg +
//                            "\nMost likely the image file in Photovault database is corrupted.",
//                            "Error displaying image",
//                            JOptionPane.ERROR_MESSAGE );
//                }
//            });
//            
//        }
//        setCursor( oldCursor );
//    }    
    
    
    /**
     * Parse JPEG metadata structure and store the data in metadata and exifData fields
     * @param top The metadata object tree in format "javax_imageio_jpeg_image_1.0"
     */
    private void parseJPEGMetadata( IIOMetadataNode top ) {
        NodeList candidates = top.getElementsByTagName( "unknown" );
        for ( int n = 0; n < candidates.getLength(); n++ ) {
            Node node = candidates.item( n );
            if ( node instanceof IIOMetadataNode ) {
                IIOMetadataNode m = (IIOMetadataNode) node;
                Object obj = m.getUserObject();
                if ( obj instanceof byte[] ) {
                    byte[] data = (byte[]) obj;
                    if ( data[0] == 'E' && data[1] == 'x' && data[2] == 'i' && data[3] == 'f' ) {
                        log.debug( "exif data found" );
                        InputStream is = new ByteArrayInputStream( data, 6, data.length - 6 );
                        try {
                            ImageInputStream metadataStream = ImageIO.createImageInputStream( is );
                            Iterator readers = ImageIO.getImageReadersByFormatName( "TIFF" );
                            if ( readers.hasNext() ) {
                                ImageReader reader = (ImageReader) readers.next();
                                reader.setInput( metadataStream );
                                IIOMetadata metadata = reader.getImageMetadata( 0 );
                                this.metadata = TIFFDirectory.createFromMetadata( metadata );
                                TIFFField exifField = this.metadata.getTIFFField( EXIFParentTIFFTagSet.TAG_EXIF_IFD_POINTER );
                                if ( exifField != null ) {
                                    exifData = (TIFFDirectory) exifField.getData();
                                }
                            }
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Get a proper image reader for a file based on file name extension.
     * @param f The file
     * @return Correct Reader or <CODE>null</CODE> if no proper reader is found.
     */
    static private ImageReader getImageReader( File f ) {
        ImageReader ret = null;
        if ( f != null ) {
            String fname = f.getName();
            int lastDotPos = fname.lastIndexOf( "." );
            if ( lastDotPos > 0 && lastDotPos < fname.length()-1 ) {
                String suffix = fname.substring( lastDotPos+1 );
                Iterator readers = ImageIO.getImageReadersBySuffix( suffix );
                if ( readers.hasNext() ) {
                    ret = (ImageReader)readers.next();
                }
            }
        }
        return ret;
    }
    
    /**
     * Load the image and/or metadata
     * @param loadImage Load the image pixel data if <CODE>true</CODE>
     * @param loadMetadata Load image metadata if <CODE>true</CODE>.
     */
    private void load( boolean loadImage, boolean loadMetadata) {
        if ( f != null && f.canRead() ) {
            ImageReader reader = getImageReader( f );
            if ( reader != null ) {
                log.debug( "Creating stream" );
                ImageInputStream iis = null;
                try {
                    iis = ImageIO.createImageInputStream( f );
                    reader.setInput( iis, false, false );
                    if ( loadImage ) {
                        image = reader.readAsRenderedImage( 0, null );
                    }
                    if ( loadMetadata ) {
                        Set<String> nodes = new HashSet<String>();
                        nodes.add( "unknown" );
                        IIOMetadata metadata = reader.getImageMetadata( 0,
                                "javax_imageio_jpeg_image_1.0", nodes );
                        Node tree = metadata.getAsTree( "javax_imageio_jpeg_image_1.0" );
                        log.debug( "read metadata: " + metadata.toString() );
                        this.parseJPEGMetadata( (IIOMetadataNode) tree);
                    }
                } catch (Exception ex) {
                    log.warn( ex.getMessage() );
                    ex.printStackTrace();
                    return;
                }
            }
        }
    }
}