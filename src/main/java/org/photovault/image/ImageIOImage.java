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

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifDirectory;
import com.sun.media.imageio.plugins.tiff.BaselineTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.EXIFParentTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.EXIFTIFFTagSet;
import com.sun.media.imageio.plugins.tiff.TIFFDirectory;
import com.sun.media.imageio.plugins.tiff.TIFFField;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedImageAdapter;
import javax.media.jai.TiledImage;
import javax.media.jai.operator.RenderableDescriptor;
import org.photovault.imginfo.ImageInstance;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Wrapper class for imaging pipeline for images that are read using JAI ImageIO
 */
public class ImageIOImage extends PhotovaultImage {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( ImageIOImage.class.getName() );

    private int width = 0;

    private int height = 0;
    
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
        i.load( loadImage, loadMetadata, Integer.MAX_VALUE, Integer.MAX_VALUE, false );
        return i;
    }
    
    /**
     The loaded image file
     */
    PlanarImage image = null;
    RenderableOp renderableImage = null;
    
    /**
     If true, the loaded image stored in {@see image} is loaded with low quality.
     */
    boolean imageIsLowQuality = false;
    
    /**
     Get the image pixel data. If the iamge has not been read earlier, this method
     reads it from disk.
     @param minWidth The minimum size for the iamge to be loaded
     @param minHeight The minimum height for the loaded image
     @param isLowQualityAllowed if <code>true</code> the method may use shortcuts 
     that make tradeoff in image quality for improved performance or memory consumption
     (like increase subsampling)
     @return The image data as an RenderedImage.
     */
    public RenderableOp getCorrectedImage( int minWidth, int minHeight, boolean isLowQualityAllowed ) {
        if ( image == null ||
                (minWidth > image.getWidth() || minHeight > image.getHeight() ) ||
                ( imageIsLowQuality && !isLowQualityAllowed ) ) {
            load( true, (metadata == null), minWidth, minHeight, isLowQualityAllowed );
        }
        return renderableImage;
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
            load( false, true, Integer.MAX_VALUE, Integer.MAX_VALUE, false );
        }
        
        TIFFField ret =  null;
        if ( exifData != null ) {
            ret = exifData.getTIFFField( tag );
        }
        if ( ret == null && metadata != null ) {
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
    private void load( boolean loadImage, boolean loadMetadata, 
            int minWidth, int minHeight, boolean isLowQualityAllowed ) {
        if ( f != null && f.canRead() ) {
            ImageReader reader = getImageReader( f );
            if ( reader != null ) {
                log.debug( "Creating stream" );
                ImageInputStream iis = null;
                try {
                    iis = ImageIO.createImageInputStream( f );
                    reader.setInput( iis, false, false );
                    width = reader.getWidth( 0 );
                    height = reader.getHeight( 0 );
                    if ( loadImage ) {
                        RenderedImage ri = null;
                        if ( isLowQualityAllowed ) {
                            ri = readExifThumbnail( f );
                            if ( ri == null || !isOkForThumbCreation( ri.getWidth(),
                                ri.getHeight(), minWidth, minHeight,
                                    reader.getAspectRatio( 0 ), 0.01 ) ) {
                                /*
                                 EXIF thumbnail either didi not exist or was unusable,
                                 tru to read subsampled version of original
                                 */
                                ri = readSubsampled( reader, minWidth, minHeight );
                            }
                        } else {
                            ri = reader.read( 0, null );
                            
                            /*
                             TODO: JAI seems to have problems in doing convolutions
                             for large image tiles. Split image to reasonably sized
                             tiles as a workaround for this.
                             */
                            ri = new TiledImage( ri, 1024, 1024 );
                        }
                        if ( ri != null ) {                            
                            image =  new RenderedImageAdapter( ri );
                            renderableImage =
                                    RenderableDescriptor.createRenderable(
                                    image, null, null, null, null, null, null );
                        } else {
                            image = null;
                            renderableImage = null;
                        }
                        imageIsLowQuality = isLowQualityAllowed;
                    }
                    if (loadMetadata ) {
                        Set<String> nodes = new HashSet<String>();
                        nodes.add( "unknown" );
                        IIOMetadata metadata = reader.getImageMetadata( 0,
                                "javax_imageio_jpeg_image_1.0", nodes );
                        if ( metadata != null ) {
                            Node tree = metadata.getAsTree( "javax_imageio_jpeg_image_1.0" );
                            log.debug( "read metadata: " + metadata.toString() );
                            this.parseJPEGMetadata( (IIOMetadataNode) tree);
                        }
                    }
                } catch (Exception ex) {
                    log.warn( ex.getMessage() );
                    ex.printStackTrace();
                    return;
                }
            }
        }
    }
    
    /**
     Read the image (either original or proper thumbnail in the same file and subsample 
     it to save memory & time. The image is subsampled so that its reasolution is the
     smallest possible that is bigger than given limits. 
     
     @param reader The image reader that is used for reading the image
     @param minWidth Minimum width of the subsampled image
     @param minHeight Minimum height of the subsampled iamge
     
     @return Subsampled image.
     */
    
    private RenderedImage readSubsampled( ImageReader reader, int minWidth, 
            int minHeight )
            throws IOException {
        /*
         We try to ensure that the thumbnail is actually from the original image
         by comparing aspect ratio of it to original. This is not a perfect check
         but it will usually catch the most typical errors (like having a the original
         rotated by RAW conversion SW but still the original EXIF thumbnail.
         */
        double origAspect = reader.getAspectRatio( 0 );        
        double aspectAccuracy = 0.01;
        int minInstanceSide = Math.max( minWidth, minHeight );
        
        int numThumbs = 0;
        RenderedImage image = null;
        try {
            int numImages = reader.getNumImages( true );
            if ( numImages > 0 ) {
                numThumbs = reader.getNumThumbnails(0);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        if ( numThumbs > 0
                && isOkForThumbCreation( reader.getThumbnailWidth( 0, 0 ),
                reader.getThumbnailHeight( 0, 0 ) , minWidth, minHeight, origAspect, aspectAccuracy )   ) {
            // There is a thumbanil that is big enough - use it
            
            log.debug( "Original has thumbnail, size "
                    + reader.getThumbnailWidth( 0, 0 ) + " x "
                    + reader.getThumbnailHeight( 0, 0 ) );
            image = reader.readThumbnail( 0, 0 );
            log.debug( "Read thumbnail" );
        } else {
            log.debug( "No thumbnail in original" );
            ImageReadParam param = reader.getDefaultReadParam();
            
            // Find the maximum subsampling rate we can still use for creating
            // a quality thumbnail. Some image format readers seem to have
            // problems with subsampling values (e.g. PNG sometimes crashed
            // the whole virtual machine, to for now let's do this only
            // with JPG.
            int subsampling = 1;
            if ( reader.getFormatName().equals( "JPEG" ) ) {
                int minDim = Math.min( reader.getWidth( 0 ),reader.getHeight( 0 ) );
                while ( 2 * minInstanceSide * subsampling < minDim ) {
                    subsampling *= 2;
                }
            }
            param.setSourceSubsampling( subsampling, subsampling, 0, 0 );            
            image = reader.read( 0 );
        }
        return image;
    }   
    
    
    /**
     Attemps to read a thumbnail from EXIF headers
     @return The thumbnail image or null if none available
     */
    private BufferedImage readExifThumbnail( File f ) {
        BufferedImage bi = null;
        Metadata metadata = null;
        try {
            metadata = JpegMetadataReader.readMetadata( f );
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        }
        ExifDirectory exif = null;
        if ( metadata != null && metadata.containsDirectory( ExifDirectory.class ) ) {
            try {
                exif = (ExifDirectory) metadata.getDirectory( ExifDirectory.class );
                byte[] thumbData = exif.getThumbnailData();
                if ( thumbData != null ) {
                    ByteArrayInputStream bis = new ByteArrayInputStream( thumbData );
                    try {
                        bi = ImageIO.read( bis );
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    } finally {
                        try {
                            bis.close();
                        } catch (IOException ex) {
                            log.error( "Cannot close image instance after creating thumbnail." );
                        }
                    }
                }
            } catch ( MetadataException e ) {
            }
        }
        return bi;
    }
    
    /**
     Helper method to check if a image is ok for thumbnail creation, i.e. that
     it is large enough and that its aspect ration is same as the original has
     @param width width of the image to test
     @param height Height of the image to test
     @param minWidth Minimun width needed for creating a thumbnail
     @param minHeight Minimum height needed for creating a thumbnail
     @param origAspect Aspect ratio of the original image
     */
    private boolean isOkForThumbCreation( int width, int height,
            int minWidth, int minHeight, double origAspect, double aspectAccuracy ) {
        if ( width < minWidth ) return false;
        if ( height < minHeight ) return false;
        double aspect = getAspect( width, height, 1.0 );
        if ( Math.abs( aspect - origAspect) / origAspect > aspectAccuracy )  {
            return false;
        }
        return true;
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

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
        
}