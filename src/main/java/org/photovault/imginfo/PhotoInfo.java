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

package org.photovault.imginfo;
import java.util.*;
import java.sql.*;
import java.io.*;
import java.text.*;
import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.*;
import com.sun.media.jai.codec.*;
import java.awt.Transparency;
import java.awt.image.*;
import java.awt.geom.*;
import com.drew.metadata.*;
import com.drew.metadata.exif.*;
import com.drew.imaging.jpeg.*;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.query.Criteria;
import org.apache.ojb.broker.query.QueryByCriteria;
import org.apache.ojb.odmg.*;
import org.odmg.*;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.folder.*;

/**
 PhotoInfo represents information about a single photograph
 TODO: write a decent doc!
 */
public class PhotoInfo {
    
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoInfo.class.getName() );
    
    public PhotoInfo() {
        changeListeners = new HashSet();
        instances = new Vector();
    }
    
    /**
     Static method to load photo info from database by photo id.
     @param photoId ID of the photo to be retrieved
     */
    public static PhotoInfo retrievePhotoInfo( int photoId ) throws PhotoNotFoundException {
        log.debug( "Fetching PhotoInfo with ID " + photoId );
        String oql = "select photos from " + PhotoInfo.class.getName() + " where uid=" + photoId;
        List photos = null;
        
        // Get transaction context
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Implementation odmg = ODMG.getODMGImplementation();
        
        try {
            OQLQuery query = odmg.newOQLQuery();
            query.create( oql );
            photos = (List) query.execute();
            txw.commit();
        } catch (Exception e ) {
            log.warn( "Error fetching record: " + e.getMessage() );
            txw.abort();
            throw new PhotoNotFoundException();
        }
        if ( photos.size() == 0 ) {
            throw new PhotoNotFoundException();
        }
        PhotoInfo photo = (PhotoInfo) photos.get(0);
        
        // For some reason when seeking for e.g. photoId -1, a photo with ID = 1 is returned
        // This sounds like a bug in OJB?????
        if ( photo.getUid() != photoId ) {
            log.warn( "Found photo with ID = " + photo.getUid() + " while looking for ID " + photoId );
            throw new PhotoNotFoundException();
        }
        return photo;
    }

    /**
     Retrieves the PhotoInfo objects whose original instance has a specific hash code
     @param hash The hash code we are looking for
     @return An array of matching PhotoInfo objects or <code>null</code>
     if none found.
     */
    static public PhotoInfo[] retrieveByOrigHash(byte[] hash) {
 	ODMGXAWrapper txw = new ODMGXAWrapper();
	Implementation odmg = ODMG.getODMGImplementation();

	Transaction tx = odmg.currentTransaction();

       PhotoInfo photos[] = null;
	try {
	    PersistenceBroker broker = ((HasBroker) tx).getBroker();
	    
	    Criteria crit = new Criteria();
            crit.addEqualTo( "hash", hash );
	    
	    QueryByCriteria q = new QueryByCriteria( PhotoInfo.class, crit );
	    Collection result = broker.getCollectionByQuery( q );
            if ( result.size() > 0 ) {
                photos = (PhotoInfo[]) result.toArray( new PhotoInfo[0] );
            }
            txw.commit();
	} catch ( Exception e ) {
	    log.warn( "Error executing query: " + e.getMessage() );
	    e.printStackTrace( System.out );
	    txw.abort();
	}
       return photos;
    }
    
    /**
     Creates a new persistent PhotoInfo object and stores it in database
     (just a dummy onject with no meaningful field values)
     @return A new PhotoInfo object
     */
    public static PhotoInfo create() {
        PhotoInfo photo = new PhotoInfo();
        
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( photo, Transaction.WRITE );
        txw.commit();
        return photo;
    }
    
    
    /**
     Add a new image to the database. Unless the image resides in an external 
     volume this method first copies a given image file to the default database 
     volume. It then extracts the information it can from the image file and 
     stores a corresponding entry in DB.
     
     @param imgFile File object that describes the image file that is to be added 
     to the database
     @return The PhotoInfo object describing the new file.
     @throws PhotoNotFoundException if the file given as imgFile argument does
     not exist or is unaccessible. This includes a case in which imgFile is part
     if normal Volume.
     */
    public static PhotoInfo addToDB( File imgFile )  throws PhotoNotFoundException {
        VolumeBase vol = null;
        try {
            vol = VolumeBase.getVolumeOfFile( imgFile );
        } catch (IOException ex) {
            throw new PhotoNotFoundException();
        }
        
        // Determine the fle that will be added as an instance
        File instanceFile = null;
        if ( vol == null ) {
            /* 
             The "normal" case: we are adding a photo that is not part of any 
             volume. Copy the file to the archive.
             */
            vol = VolumeBase.getDefaultVolume();
            instanceFile = vol.getFilingFname( imgFile );

            // 
            try {
                FileInputStream in = new FileInputStream( imgFile );
                FileOutputStream out = new FileOutputStream( instanceFile );
                byte buf[] = new byte[1024];
                int nRead = 0;
                int offset = 0;
                while ( (nRead = in.read( buf )) > 0 ) {
                    out.write( buf, 0, nRead );
                    offset += nRead;
                }
                out.close();
                in.close();
            } catch ( Exception e ) {
                log.warn( "Error copying file: " + e.getMessage() );
                throw new PhotoNotFoundException();
            }
        } else if ( vol instanceof ExternalVolume ) {
            // Thisfile is in an external volume so we do not need a copy
            instanceFile = imgFile;
        } else if ( vol instanceof Volume ) {
            // Adding file from normal volume is not permitted
            throw new PhotoNotFoundException();
        } else {
            throw new java.lang.Error( "Unknown subclass of VolumeBase: " 
                    + vol.getClass().getName() );
        }
        
        // Create the image
        ODMGXAWrapper txw = new ODMGXAWrapper();
        PhotoInfo photo = PhotoInfo.create();
        txw.lock( photo, Transaction.WRITE );
        photo.addInstance( vol, instanceFile, ImageInstance.INSTANCE_TYPE_ORIGINAL );
        photo.setOrigFname( imgFile.getName() );
        java.util.Date shootTime = new java.util.Date( imgFile.lastModified() );
        photo.setShootTime( shootTime );
        photo.setCropBounds( new Rectangle2D.Float( 0.0F, 0.0F, 1.0F, 1.0F ) );
        photo.updateFromFileMetadata( instanceFile );
        txw.commit();
        return photo;
    }
    
    /**
     Reads field values from original file EXIF values
     @return true if successfull, false otherwise
     */
     
    public boolean updateFromOriginalFile() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        ImageInstance instance = null;
        for ( int n = 0; n < instances.size(); n++ ) {
            ImageInstance candidate = (ImageInstance) instances.get( n );
            if ( candidate.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL ) {
                instance = candidate;
                File f = instance.getImageFile();
                if ( f != null ) {
                    updateFromFileMetadata( f );
                    txw.commit();                    
                    return true;
                }
            }
        }
        txw.commit();
        return false;
    }
    
    /**
     This method reads the metadata from image file and updates the PhotoInfo record from it
     @param f The file to read
     */
    void updateFromFileMetadata( File f ) {
        ExifDirectory exif = null; 
        try {
            Metadata metadata = JpegMetadataReader.readMetadata(f);
            if ( metadata.containsDirectory( ExifDirectory.class ) ) {
                try {
                    exif = (ExifDirectory) metadata.getDirectory( ExifDirectory.class );
                } catch ( MetadataException e ) {
                }
            } else {
                // No known directory was found no reason to continue
                return;
            }
        } catch (FileNotFoundException e) {
            // If error, just return - this is just an additional 'nice-if-succesful' operation.
            // If there is no metadata this will happen...
            return;
        }
        // Shooting date
        try {
            java.util.Date origDate = exif.getDate( exif.TAG_DATETIME_ORIGINAL );   
            setShootTime( origDate );
            log.debug( "TAG_DATETIME_ORIGINAL: " + origDate.toString() );
        } catch ( MetadataException e ) {
            log.info( "Error reading origDate: " + e.getMessage() );
        }
        
        // Exposure
        try {
            double fstop = exif.getDouble( exif.TAG_FNUMBER );
            log.debug( "TAG_FNUMBER: " + fstop );
            setFStop( fstop );
        } catch ( MetadataException e ) {
            log.info( "Error reading origDate: " + e.getMessage() );
        }
        try {
            double sspeed = exif.getDouble( exif.TAG_EXPOSURE_TIME );
            setShutterSpeed( sspeed );
            log.debug( "TAG_EXPOSURE_TIME: " + sspeed );
        } catch ( MetadataException e ) {
            log.info( "Error reading origDate: " + e.getMessage() );
        }
        try {
            double flen = exif.getDouble( exif.TAG_FOCAL_LENGTH );
            setFocalLength( flen );
        } catch ( MetadataException e ) {
            log.info( "Error reading origDate: " + e.getMessage() );
        }
        try {
            int filmSpeed = exif.getInt( exif.TAG_ISO_EQUIVALENT );
            setFilmSpeed( filmSpeed );
        } catch ( MetadataException e ) {
            log.info( "Error reading origDate: " + e.getMessage() );
        }
        
        // Camera name. Put here both camera manufacturer and model
        String maker = exif.getString( exif.TAG_MAKE );
        String model = exif.getString( exif.TAG_MODEL );
        setCamera( maker + " " + model );
        
    }
    
    
    /**
     Deletes the PhotoInfo and all related instances from database
     */
    public void delete() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Database db = ODMG.getODMGDatabase();
        
        // First delete all instances
        for ( int i = 0; i < instances.size(); i++ ) {
            ImageInstance f = (ImageInstance) instances.get( i );
            f.delete();
        }
        
        // Then delete the photo from all folders it belongs to
        if ( folders != null ) {
            Object[] foldersArray = folders.toArray();
            for ( int n = 0; n < foldersArray.length; n++ ) {
                ((PhotoFolder)foldersArray[n]).removePhoto( this );
            }
        }
        
        // Then delete the PhotoInfo itself
        db.deletePersistent( this );
        txw.commit();
    }
    
    
    
    /**
     Adds a new listener to the list that will be notified of modifications to this object
     @param l reference to the listener
     */
    public void addChangeListener( PhotoInfoChangeListener l ) {
        changeListeners.add( l );
    }
    
    /**
     Removes a listenre
     */
    public void removeChangeListener( PhotoInfoChangeListener l ) {
        changeListeners.remove( l );
    }
    
    private void notifyListeners( PhotoInfoChangeEvent e ) {
        Iterator iter = changeListeners.iterator();
        while ( iter.hasNext() ) {
            PhotoInfoChangeListener l = (PhotoInfoChangeListener) iter.next();
            l.photoInfoChanged( e );
        }
    }
    
    protected void modified() {
        lastModified = new java.util.Date();
        notifyListeners( new PhotoInfoChangeEvent( this ) );
    }
    
    /**
     set of the listeners that should be notified of any changes to this object
     */
    HashSet changeListeners = null;
    
    
    private int uid;
    
    /**
     * Describe timeAccuracy here.
     */
    private double timeAccuracy;
    
    /**
     * Describe quality here.
     */
    private int quality;
    
    /**
     * Describe lastModified here.
     */
    private java.util.Date lastModified;
    
    /**
     * Describe techNotes here.
     */
    private String techNotes;
    
    /**
     * Describe origFname here.
     */
    private String origFname;
    
    /**
     Returns the uid of the object
     */
    public int getUid() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return uid;
    }
    
    /**
     Adds a new image instance for this photo
     @param i The new instance
     */
    public void addInstance( ImageInstance i ) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        txw.lock( i, Transaction.WRITE );
        instances.add( i );
        i.setPhotoUid(  uid );
        if ( i.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL ) {
            origInstanceHash = i.getHash();
        }
        txw.commit();
        
    }
    
    /**
     Adds a new instance of the photo into the database.
     @param volume Volume in which the instance is stored
     @param instanceFile File name of the instance
     @param instanceType Type of the instance - original, modified or thumbnail.
     @return The new instance
     @see ImageInstance class documentation for details.
     */
    public ImageInstance addInstance( VolumeBase volume, File instanceFile, int instanceType ) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        // Vector origInstances = getInstances();
        ImageInstance instance = ImageInstance.create( volume, instanceFile, this );
        instance.setInstanceType( instanceType );
        instances.add( instance );
        
        // If this is the first instance or we are adding original image we need to invalidate
        // thumbnail
        if ( instances.size() == 1 || instanceType == ImageInstance.INSTANCE_TYPE_ORIGINAL ) {
            thumbnail = null;
        }
        
        if ( instanceType == ImageInstance.INSTANCE_TYPE_ORIGINAL ) {
            // Store the hash code of original (even if this original instance is later deleted
            // we can identify later that another file is an instance of this photo)
            origInstanceHash = instance.getHash();
            // If an original instance is added notify listeners since some of
            // them may be displaying the default thumbnail
            modified();
        }
        txw.commit();
        return instance;
    }
    
    public void removeInstance( int instanceNum )  throws IndexOutOfBoundsException {
        ODMGXAWrapper txw = new ODMGXAWrapper();        
        ImageInstance instance = null;
        try {
            instance = (ImageInstance) getInstances().get(instanceNum );
        } catch ( IndexOutOfBoundsException e ) {
            txw.abort();
            throw e;
        }
        txw.lock( this, Transaction.WRITE );
        txw.lock( instance, Transaction.WRITE );
        instances.remove( instance );
        instance.delete();
        txw.commit();
    }
    
    /**
     Returns the number of instances of this photo that are stored in database
     */
    public int getNumInstances() {
        return instances.size();
    }
    
    
    /**
     Returns the arrayList that contains all instances of this file.
     */
    public Vector getInstances() {
        return instances;
    }
    
    Vector instances = null;
    
    /**
     Return a single image instance based on its order number
     @param instanceNum Number of the instance to return
     @throws IndexOutOfBoundsException if instanceNum is < 0 or >= the number of instances
     */
    public ImageInstance getInstance( int instanceNum ) throws IndexOutOfBoundsException {
        ImageInstance instance =  (ImageInstance) getInstances().get(instanceNum );
        return instance;
    }
    
    /**
     Returns a thumbnail of this image. If no thumbnail instance is yetavailable, creates a
     new instance on the default volume. Otherwise loads an existing thumbnail instance. <p>
     
     If thumbnail creation fails of if there is no image instances available at all, returns
     a default thumbnail image.
     @return Thumbnail of this photo or default thumbnail if no photo instances available
     */
    public Thumbnail getThumbnail() {
        log.debug( "getThumbnail: entry, Finding thumbnail for " + uid );
        if ( thumbnail == null ) {
            thumbnail = getExistingThumbnail();
            if ( thumbnail == null ) {
                // Next try to create a new thumbnail instance
                log.debug( "No thumbnail found, creating" );
                createThumbnail();
            }
        }
        if ( thumbnail == null ) {
            // Thumbnail was not successful created, most probably because there 
            // is no available instance
            thumbnail = Thumbnail.getDefaultThumbnail();
        }
        
        log.debug( "getThumbnail: exit" );
        return thumbnail;
    }
    
    /**
     Returns an existing thumbnail for this photo but do not try to contruct a new 
     one if there is no thumbnail already created
     @return Thumbnail for this photo or null if none created
     */
    
    public Thumbnail getExistingThumbnail() {
        if ( thumbnail == null ) {
            log.debug( "Finding thumbnail from database" );
            // First try to find an instance from existing instances
            ImageInstance original = null;
            for ( int n = 0; n < instances.size(); n++ ) {
                ImageInstance instance = (ImageInstance) instances.get( n );
                if ( instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_THUMBNAIL
                        && Math.abs(instance.getRotated() - prefRotation) < 0.0001
                        && instance.getCropBounds().equals( getCropBounds() ) ) {
                    log.debug( "Found thumbnail from database" );
                    thumbnail = Thumbnail.createThumbnail( this, instance.getImageFile() );
                    break;
                }
            }
        }
        return thumbnail;
    }
    
    /**
     Returns true if the photo has a Thumbnail already created,
     false otherwise
     */
    public boolean hasThumbnail() {
        log.debug( "hasThumbnail: entry, Finding thumbnail for " + uid );
        if ( thumbnail == null ) {
            thumbnail = getExistingThumbnail();
        }
        log.debug( "hasThumbnail: exit" );
        return ( thumbnail != null && thumbnail != Thumbnail.getDefaultThumbnail() );
    }
    
    Thumbnail thumbnail = null;
    
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
    
    /** Creates a new thumbnail for this image on specific volume
     @param volume The volume in which the instance is to be created
     */
    protected void createThumbnail( VolumeBase volume ) {
        
        log.debug( "Creating thumbnail for " + uid );
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        
        // Maximum size of the thumbnail
        int maxThumbWidth = 100;
        int maxThumbHeight = 100;

        /*
         Determine the minimum size for the instance used for thumbnail creation 
         to get decent image quality.
         The cropped portion of the image must be roughly the same
         resolution as the intended thumbnail.
         */
        double cropWidth = cropMaxX - cropMinX;
        cropWidth = ( cropWidth > 0.000001 ) ? cropWidth : 1.0;
        double cropHeight = cropMaxY - cropMinY;
        cropHeight = ( cropHeight > 0.000001 ) ? cropHeight : 1.0;        
        int minInstanceWidth = (int)(((double)maxThumbWidth)/cropWidth);
        int minInstanceHeight = (int)(((double)maxThumbHeight)/cropHeight);
        int minInstanceSide = Math.max( minInstanceWidth, minInstanceHeight );
        
        
        // Find the original image to use as a staring point
        ImageInstance original = null;
        for ( int n = 0; n < instances.size(); n++ ) {
            ImageInstance instance = (ImageInstance) instances.get( n );
            if ( instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL ) {
                original = instance;
                txw.lock( original, Transaction.READ );
                break;
            }
        }
        if ( original == null || original.getImageFile() == null || !original.getImageFile().exists() ) {
            // If there are uncorrupted instances, no thumbnail can be created
            log.warn( "Error - no original image was found!!!" );
            txw.commit();
            return;
        }
        log.debug( "Found original, reading it..." );
        
        /*
         We try to ensure that the thumbnail is actually from the original image
         by comparing aspect ratio of it to original. This is not a perfect check 
         but it will usually catch the most typical errors (like having a the original
         rotated by RAW conversion SW but still the original EXIF thumbnail.
         */
        double origAspect = this.getAspect( 
                original.getWidth(), 
                original.getHeight(), 1.0 );
        double aspectAccuracy = 0.01;
        
        // First, check if there is a thumbnail in image header
        BufferedImage origImage = readExifThumbnail( original.getImageFile() );
        
        if ( origImage == null 
                || !isOkForThumbCreation( origImage.getWidth(),
                        origImage.getHeight(), minInstanceWidth, minInstanceHeight, origAspect, aspectAccuracy ) ) {
            // Read the image
            try {
                Iterator readers = ImageIO.getImageReadersByFormatName( "jpg" );
                if ( readers.hasNext() ) {
                    ImageReader reader = (ImageReader)readers.next();
                    log.debug( "Creating stream" );
                    ImageInputStream iis = ImageIO.createImageInputStream( original.getImageFile() );
                    reader.setInput( iis, false, false );
                    int numThumbs = 0;
                    try {
                        int numImages = reader.getNumImages( true );
                        numThumbs = reader.getNumThumbnails(0);
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    if ( numThumbs > 0 
                            && isOkForThumbCreation( reader.getThumbnailWidth( 0, 0 ),
                                    reader.getThumbnailHeight( 0, 0 ) , minInstanceWidth, minInstanceHeight, origAspect, aspectAccuracy )   ) {
                        // There is a thumbanil that is big enough - use it
                        
                        log.debug( "Original has thumbnail, size "
                                + reader.getThumbnailWidth( 0, 0 ) + " x "
                                + reader.getThumbnailHeight( 0, 0 ) );
                        origImage = reader.readThumbnail( 0, 0 );
                        log.debug( "Read thumbnail" );
                    } else {
                        log.debug( "No thumbnail in original" );
                        ImageReadParam param = reader.getDefaultReadParam();

                        // Find the maximum subsampling rate we can still use for creating
                        // a quality thumbnail
                        int subsampling = 1;
                        int minDim = Math.min( reader.getWidth( 0 ),reader.getHeight( 0 ) );
                        while ( 2 * minInstanceSide * subsampling < minDim ) {
                            subsampling *= 2;
                        }
                        param.setSourceSubsampling( subsampling, subsampling, 0, 0 );

                        origImage = reader.read( 0, param );
                        log.debug( "Read original" );
                    }
                }
            } catch ( IOException e ) {
                log.warn( "Error reading image: " + e.getMessage() );
                txw.abort();
                return;
            }
        }
        log.debug( "Done, finding name" );

        // Find where to store the file in the target volume
        File thumbnailFile = volume.getInstanceName( this, "jpg" );
        log.debug( "name = " + thumbnailFile.getName() );
        
        // Shrink the image to desired state and save it
        // Find first the correct transformation for doing this

        int origWidth = origImage.getWidth();
        int origHeight = origImage.getHeight();
        
        AffineTransform xform = org.photovault.image.ImageXform.getRotateXform(
                prefRotation -original.getRotated(), origWidth, origHeight );
        
        ParameterBlockJAI rotParams = new ParameterBlockJAI( "affine" );
        rotParams.addSource( origImage );
        rotParams.setParameter( "transform", xform );
        rotParams.setParameter( "interpolation",
                Interpolation.getInstance( Interpolation.INTERP_NEAREST ) );
        RenderedOp rotatedImage = JAI.create( "affine", rotParams );

        ParameterBlockJAI cropParams = new ParameterBlockJAI( "crop" );
        cropParams.addSource( rotatedImage );
        cropParams.setParameter( "x", 
                (float)( rotatedImage.getMinX() + cropMinX * rotatedImage.getWidth() ) );
        cropParams.setParameter( "y", 
                (float)( rotatedImage.getMinY() + cropMinY * rotatedImage.getHeight() ) );
        cropParams.setParameter( "width", 
                (float)( (cropWidth) * rotatedImage.getWidth() ) );
        cropParams.setParameter( "height", 
                (float) ( (cropHeight) * rotatedImage.getHeight() ) );
	RenderedOp cropped = JAI.create("crop", cropParams, null);
        // Translate the image so that it begins in origo
        ParameterBlockJAI pbXlate = new ParameterBlockJAI( "translate" );
        pbXlate.addSource( cropped );
        pbXlate.setParameter( "xTrans", (float) (-cropped.getMinX() ) );
        pbXlate.setParameter( "yTrans", (float) (-cropped.getMinY() ) );        
        RenderedOp xformImage = JAI.create( "translate", pbXlate );
        // Finally, scale this to thumbnail
        AffineTransform thumbScale = org.photovault.image.ImageXform.getFittingXform( maxThumbWidth, maxThumbHeight,
                0,
                xformImage.getWidth(), xformImage.getHeight() );
        ParameterBlockJAI thumbScaleParams = new ParameterBlockJAI( "affine" );
        thumbScaleParams.addSource( xformImage );
        thumbScaleParams.setParameter( "transform", thumbScale );
        thumbScaleParams.setParameter( "interpolation",
                Interpolation.getInstance( Interpolation.INTERP_NEAREST ) );
        
        PlanarImage thumbImage = JAI.create( "affine", thumbScaleParams );
        
        // Save it
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(thumbnailFile.getAbsolutePath());
        } catch(IOException e) {
            log.error( "Error writing thumbnail: " + e.getMessage() );
            txw.abort();
            return;
        }
        
        JPEGEncodeParam encodeParam = new JPEGEncodeParam();
        ImageEncoder encoder = ImageCodec.createImageEncoder("JPEG", out,
                encodeParam);
        try {
            encoder.encode( thumbImage );
            out.close();
            // origImage.dispose();
            thumbImage.dispose();
        } catch (IOException e) {
            log.error( "Error writing thumbnail: " + e.getMessage() );
            txw.abort();
            return;
        }
        
        // add the created instance to this persistent object
        ImageInstance thumbInstance = addInstance( volume, thumbnailFile,
                ImageInstance.INSTANCE_TYPE_THUMBNAIL );
        thumbInstance.setRotated( prefRotation -original.getRotated() );
        thumbInstance.setCropBounds( getCropBounds() );
        log.debug( "Loading thumbnail..." );
        
        thumbnail = Thumbnail.createThumbnail( this, thumbnailFile );
        
        log.debug( "Thumbnail loaded" );
        txw.commit();
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
    
    /** Creates a new thumbnail on the default volume
     */
    protected void createThumbnail() {
        VolumeBase vol = VolumeBase.getDefaultVolume();
        createThumbnail( vol );
    }
    
    /**
     Exports an image from database to a specified file with given resolution. 
     The image aspect ratio is preserved and the image is scaled so that it fits 
     to the given maximum resolution.
     @param file File in which the image will be saved
     @param width Width of the exported image in pixels. If negative the image is
     exported in its "natural" resolution (i.e. not scaled)
     @param height Height of the exported image in pixels
     */
    public void exportPhoto( File file, int width, int height ) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        
        // Find the original image to use as a staring point
        ImageInstance original = null;
        for ( int n = 0; n < instances.size(); n++ ) {
            ImageInstance instance = (ImageInstance) instances.get( n );
            if ( instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL ) {
                original = instance;
                txw.lock( original, Transaction.READ );
                break;
            }
        }
        if ( original == null || original.getImageFile() == null || !original.getImageFile().exists() ) {
            // If there are no instances, nothing can be exported
            log.warn( "Error - no original image was found!!!" );
            txw.commit();
            return;
        }
        
        // Read the image
        BufferedImage origImage = null;
        try {
            log.warn( "Export: reading image " + original.getImageFile() );
            origImage = ImageIO.read( original.getImageFile() );
        } catch ( IOException e ) {
            log.warn( "Error reading image: " + e.getMessage() );
            txw.abort();
            return;
        }
        
        
        // Shrink the image to desired state and save it
        // Find first the correct transformation for doing this
        int origWidth = origImage.getWidth();
        int origHeight = origImage.getHeight();
        
        AffineTransform xform = org.photovault.image.ImageXform.getRotateXform(
                prefRotation -original.getRotated(), origWidth, origHeight );
        
        ParameterBlockJAI rotParams = new ParameterBlockJAI( "affine" );
        rotParams.addSource( origImage );
        rotParams.setParameter( "transform", xform );
        rotParams.setParameter( "interpolation",
                Interpolation.getInstance( Interpolation.INTERP_BICUBIC ) );
        RenderedOp rotatedImage = JAI.create( "affine", rotParams );

        ParameterBlockJAI cropParams = new ParameterBlockJAI( "crop" );
        cropParams.addSource( rotatedImage );
        cropParams.setParameter( "x", 
                (float)( rotatedImage.getMinX() + cropMinX * rotatedImage.getWidth() ) );
        cropParams.setParameter( "y", 
                (float)( rotatedImage.getMinY() + cropMinY * rotatedImage.getHeight() ) );
        cropParams.setParameter( "width", 
                (float)( (cropMaxX - cropMinX) * rotatedImage.getWidth() ) );
        cropParams.setParameter( "height", 
                (float) ( (cropMaxY - cropMinY) * rotatedImage.getHeight() ) );
	RenderedOp cropped = JAI.create("crop", cropParams, null);
        // Translate the image so that it begins in origo
        ParameterBlockJAI pbXlate = new ParameterBlockJAI( "translate" );
        pbXlate.addSource( cropped );
        pbXlate.setParameter( "xTrans", (float) (-cropped.getMinX() ) );
        pbXlate.setParameter( "yTrans", (float) (-cropped.getMinY() ) );        
        RenderedOp xformImage = JAI.create( "translate", pbXlate );
        // Finally, scale this to thumbnail        
        PlanarImage exportImage = xformImage;
        if ( width > 0 ) {
            AffineTransform scale = org.photovault.image.ImageXform.getFittingXform( width, height,
                    0,
                    xformImage.getWidth(), xformImage.getHeight() );
            ParameterBlockJAI scaleParams = new ParameterBlockJAI( "affine" );
            scaleParams.addSource( xformImage );
            scaleParams.setParameter( "transform", scale );
            scaleParams.setParameter( "interpolation",
                    Interpolation.getInstance( Interpolation.INTERP_BICUBIC ) );
            
            exportImage = JAI.create( "affine", scaleParams );
        }
        
        // Try to determine the file type based on extension
        String ftype = "jpg";
        String imageFname = file.getName();
        int extIndex = imageFname.lastIndexOf( "." ) + 1;
        if ( extIndex > 0 ) {
            ftype = imageFname.substring( extIndex );
        }
        
        try {
            // Find a writer for that file extensions
            ImageWriter writer = null;
            Iterator iter = ImageIO.getImageWritersByFormatName( ftype );
            if (iter.hasNext()) writer = (ImageWriter)iter.next();
            if (writer != null) {
                ImageOutputStream ios = null;
                try {
                    // Prepare output file
                    ios = ImageIO.createImageOutputStream( file );
                    writer.setOutput(ios);
                    // Set some parameters
                    ImageWriteParam param = writer.getDefaultWriteParam();
                    // if bi has type ARGB and alpha is false, we have
                    // to tell the writer to not use the alpha
                    // channel: this is especially needed for jpeg
                    // files where imageio seems to produce wrong jpeg
                    // files right now...
//                    if (exportImage.getType() == BufferedImage.TYPE_INT_ARGB ) {
//                        // this is not so obvious: create a new
//                        // ColorModel without OPAQUE transparency and
//                        // no alpha channel.
//                        ColorModel cm = new ComponentColorModel(exportImage.getColorModel().getColorSpace(),
//                                false, false,
//                                Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
//                        // tell the writer to only use the first 3 bands (skip alpha)
//                        int[] bands = {0, 1, 2};
//                        param.setSourceBands(bands);
//                        // although the java documentation says that
//                        // SampleModel can be null, an exception is
//                        // thrown in that case therefore a 1*1
//                        // SampleModel that is compatible to cm is
//                        // created:
//                        param.setDestinationType(new ImageTypeSpecifier(cm,
//                                cm.createCompatibleSampleModel(1, 1)));
//                    }
                    // Write the image
                    writer.write(null, new IIOImage(exportImage, null, null), param);
                    
                    // Cleanup
                    ios.flush();
                } finally {
                    if (ios != null) ios.close();
                    writer.dispose();
                }
            }
            
        } catch ( IOException e ) {
            log.warn( "Error writing exported image: " + e.getMessage() );
            txw.abort();
            return;
        }
        
        txw.commit();
    }
    
    
    /**
     MD5 hash code of the original instance of this PhotoInfo. It must is stored also
     as part of PhotoInfo object since the original instance might be deleted from the 
     database (or we might synchronize just metadata without originals into other database!).
     With the hash code we are still able to detect that an image file is actually the 
     original.
     */
    byte origInstanceHash[] = null;
    
    public byte[] getOrigInstanceHash() {
        return (origInstanceHash != null) ? ((byte[])origInstanceHash.clone()) : null;
    }
    
    /**
     Sets the original instance hash. This is intended for only internal use
     @param hash MD5 hash value for original instance
     */
    protected void setOrigInstanceHash( byte[] hash ) {
       ODMGXAWrapper txw = new ODMGXAWrapper();
       txw.lock( this, Transaction.WRITE );
       origInstanceHash = (byte[]) hash.clone();
       txw.commit();
    }
    
    java.util.Date shootTime;
    
    /**
     * Get the value of shootTime. Note that shoot time can also be
     null (to mean that the time is unspecified)1
     @return value of shootTime.
     */
    public java.util.Date getShootTime() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return shootTime != null ? (java.util.Date) shootTime.clone() : null;
    }
    
    /**
     * Set the value of shootTime.
     * @param v  Value to assign to shootTime.
     */
    public void setShootTime(java.util.Date  v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.shootTime = (v != null) ? (java.util.Date) v.clone()  : null;
        modified();
        txw.commit();
    }
    
    
    /**
     
     @return The timeAccuracty value
     */
    public final double getTimeAccuracy() {
        return timeAccuracy;
    }
    
    /**
     
     Set the shooting time accuracy. The value is a +/- range from shootingTime
     parameter (i.e. shootingTime April 15 2000, timeAccuracy 15 means that the
     photo is taken in April 2000.
     
     * @param newTimeAccuracy The new TimeAccuracy value.
     */
    public final void setTimeAccuracy(final double newTimeAccuracy) {
        this.timeAccuracy = newTimeAccuracy;
    }
    
    
    String desc;
    
    /**
     * Get the value of desc.
     * @return value of desc.
     */
    public String getDesc() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return desc;
    }
    
    /**
     * Set the value of desc.
     * @param v  Value to assign to desc.
     */
    public void setDesc(String  v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.desc = v;
        modified();
        txw.commit();
    }
    double FStop;
    
    /**
     * Get the value of FStop.
     * @return value of FStop.
     */
    public double getFStop() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return FStop;
    }
    
    /**
     * Set the value of FStop.
     * @param v  Value to assign to FStop.
     */
    public void setFStop(double  v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.FStop = v;
        modified();
        txw.commit();
    }
    double focalLength;
    
    /**
     * Get the value of focalLength.
     * @return value of focalLength.
     */
    public double getFocalLength() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return focalLength;
    }
    
    /**
     * Set the value of focalLength.
     * @param v  Value to assign to focalLength.
     */
    public void setFocalLength(double  v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.focalLength = v;
        modified();
        txw.commit();
    }
    String shootingPlace;
    
    /**
     * Get the value of shootingPlace.
     * @return value of shootingPlace.
     */
    public String getShootingPlace() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return shootingPlace;
    }
    
    /**
     * Set the value of shootingPlace.
     * @param v  Value to assign to shootingPlace.
     */
    public void setShootingPlace(String  v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.shootingPlace = v;
        modified();
        txw.commit();
    }
    String photographer;
    
    /**
     * Get the value of photographer.
     * @return value of photographer.
     */
    public String getPhotographer() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return photographer;
    }
    
    /**
     * Set the value of photographer.
     * @param v  Value to assign to photographer.
     */
    public void setPhotographer(String  v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.photographer = v;
        modified();
        txw.commit();
    }
    double shutterSpeed;
    
    /**
     * Get the value of shutterSpeed.
     * @return value of shutterSpeed.
     */
    public double getShutterSpeed() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return shutterSpeed;
    }
    
    /**
     * Set the value of shutterSpeed.
     * @param v  Value to assign to shutterSpeed.
     */
    public void setShutterSpeed(double  v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.shutterSpeed = v;
        modified();
        txw.commit();
    }
    String camera;
    
    /**
     * Get the value of camera.
     * @return value of camera.
     */
    public String getCamera() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return camera;
    }
    
    /**
     * Set the value of camera.
     * @param v  Value to assign to camera.
     */
    public void setCamera(String  v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.camera = v;
        modified();
        txw.commit();
    }
    String lens;
    
    /**
     * Get the value of lens.
     * @return value of lens.
     */
    public String getLens() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return lens;
    }
    
    /**
     * Set the value of lens.
     * @param v  Value to assign to lens.
     */
    public void setLens(String  v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.lens = v;
        modified();
        txw.commit();
    }
    String film;
    
    /**
     * Get the value of film.
     * @return value of film.
     */
    public String getFilm() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return film;
    }
    
    /**
     * Set the value of film.
     * @param v  Value to assign to film.
     */
    public void setFilm(String  v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.film = v;
        modified();
        txw.commit();
    }
    int filmSpeed;
    
    /**
     * Get the value of filmSpeed.
     * @return value of filmSpeed.
     */
    public int getFilmSpeed() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return filmSpeed;
    }
    
    /**
     * Set the value of filmSpeed.
     * @param v  Value to assign to filmSpeed.
     */
    public void setFilmSpeed(int  v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.filmSpeed = v;
        modified();
        txw.commit();
    }
    
    double prefRotation;
    
    /**
     Get the preferred rotation for this image in degrees. Positive values indicate that the image should be
     rotated clockwise.
     @return value of prefRotation.
     */
    public double getPrefRotation() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return prefRotation;
    }
    
    /**
     * Set the value of prefRotation.
     * @param v  Value to assign to prefRotation.
     */
    public void setPrefRotation(double  v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        if ( v != prefRotation ) {
            // Rotation changes, invalidate the thumbnail
            thumbnail = null;
        }
        this.prefRotation = v;
        modified();
        txw.commit();
    }
    
    /**
     Check that the e crop bounds are defined in consistent manner. This is needed
     since in old installations the max parameters can be larger thatn min ones.
     */
    
    private void checkCropBounds() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        if ( cropMaxX - cropMinX <= 0) {
            txw.lock( this, Transaction.WRITE );
            cropMaxX = 1.0 - cropMinX;
        }
        if ( cropMaxY - cropMinY <= 0) {
            txw.lock( this, Transaction.WRITE );
            cropMaxY = 1.0 - cropMinY;
        }
        txw.commit();
    }
    
    /**
     Get the preferred crop bounds of the original image
     */
    public Rectangle2D getCropBounds() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        checkCropBounds();
        txw.commit();
        return new Rectangle2D.Double( cropMinX, cropMinY, 
                cropMaxX-cropMinX, cropMaxY-cropMinY );        
    }

    
    /**
     Set the preferred cropping operation
     @param cropBounds New crop bounds
     */
    public void setCropBounds( Rectangle2D cropBounds ) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        if ( !cropBounds.equals( getCropBounds() ) ) {
            // Rotation changes, invalidate the thumbnail
            thumbnail = null;
        }
        cropMinX = cropBounds.getMinX();
        cropMinY = cropBounds.getMinY();
        cropMaxX = cropBounds.getMaxX();
        cropMaxY = cropBounds.getMaxY();
        modified();
        txw.commit();
    }
    

    
    /**
     CropBounds describes the desired crop rectange from original image. It is 
     defined as proportional coordinates that are applied after rotating the
     original image so that top left corner is (0.0, 0.0) and bottong right
     (1.0, 1.0)
     */
    
    double cropMinX;
    double cropMaxX;
    double cropMinY;
    double cropMaxY;
    
    String description;
    
    /**
     * Get the value of description.
     * @return value of description.
     */
    public String getDescription() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return description;
    }
    
    /**
     * Set the value of description.
     * @param v  Value to assign to description.
     */
    public void setDescription(String  v) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.description = v;
        modified();
        txw.commit();
    }
    
    public static final int QUALITY_UNDEFINED = 0;
    public static final int QUALITY_TOP = 1;
    public static final int QUALITY_GOOD = 2;
    public static final int QUALITY_FAIR = 3;
    public static final int QUALITY_POOR = 4;
    public static final int QUALITY_UNUSABLE = 5;
    
    /**
     * Get the value of value attribute.
     *
     * @return an <code>int</code> value
     */
    public final int getQuality() {
        return quality;
    }
    
    /**
     * Set the "value attribute for the photo which tries to describe
     How good the pohot is. Possible values:
     <ul>
     <li>QUALITY_UNDEFINED - value of the photo has not been evaluated</li>
     <li>QUALITY_TOP - This frame is a top quality photo</li>
     <li>QUALITY_GOOD - This frame is good, one of the best available from the session</li>
     <li>QUALITY_FAIR - This frame is OK but probably not the 1st choice for use</li>
     <li>QUALITY_POOR - Unsuccesful picture</li>
     <li>QUALITY_UNUSABLE - Technical failure</li>
     </ul>
     
     *
     * @param newQuality The new Quality value.
     */
    public final void setQuality(final int newQuality) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.quality = newQuality;
        modified();
        txw.commit();
    }
    
    /**
     Returns the time when this photo (=metadata of it) was last modified
     * @return a <code>Date</code> value
     */
    public final java.util.Date getLastModified() {
        return lastModified != null ? (java.util.Date) lastModified.clone() : null;
    }
    
    public  void setLastModified(final java.util.Date newDate) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.lastModified = (newDate != null) ? (java.util.Date) newDate.clone()  : null;
        modified();
        txw.commit();
    }
    
    /**
     * Get the <code>TechNotes</code> value.
     *
     * @return a <code>String</code> value
     */
    public final String getTechNotes() {
        return techNotes;
    }
    
    /**
     * Set the <code>TechNotes</code> value.
     *
     * @param newTechNotes The new TechNotes value.
     */
    public final void setTechNotes(final String newTechNotes) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.techNotes = newTechNotes;
        modified();
        txw.commit();
    }
    
    /**
     Get the original file name of this photo
     
     * @return a <code>String</code> value
     */
    public final String getOrigFname() {
        return origFname;
    }
    
    /**
     Set the original file name of this photo. This is set also by addToDB which is the
     preferred way of creating a new photo into the DB.
     */
    public final void setOrigFname(final String newFname) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.origFname = newFname;
        modified();
        txw.commit();
    }
    
    
    /**
     List of folders this photo belongs to
     */
    Collection folders = null;
    
    /**
     This is called by PhotoFolder when the photo is added to a folder
     */
    public void addedToFolder( PhotoFolder folder ) {
        if ( folders == null ) {
            folders = new Vector();
        }
        
        folders.add( folder );
    }
    
    /**
     This is called by PhotoFolder when the photo is removed from a folder
     */
    public void removedFromFolder( PhotoFolder folder ) {
        if ( folders == null ) {
            folders = new Vector();
        }
        
        folders.remove( folder );
    }
    
    
    /**
     Returns a collection that contains all folders the photo belongs to
     */
    public Collection getFolders() {
        Vector foldersCopy = new Vector();
        if ( folders != null ) {
            foldersCopy = new Vector( folders );
        }
        return foldersCopy;
    }
    
    static private boolean isEqual( Object o1, Object o2 ) {
        if ( o1 == null ) {
            if ( o2 == null ) {
                return true;
            } else {
                return false;
            }
        }
        return o1.equals( o2 );
    }
    
    public boolean equals( Object obj ) {
        if ( obj == null || obj.getClass() != this.getClass() ) {
            return false;
        }
        PhotoInfo p = (PhotoInfo)obj;
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.lock( p, Transaction.READ );
        txw.commit();
        
        return ( isEqual( p.photographer, this.photographer )
        && isEqual( p.shootingPlace, this.shootingPlace )
        && isEqual( p.shootTime, this.shootTime )
        && isEqual(p.description, this.description )
        && isEqual( p.camera, this.camera )
        && isEqual( p.lens, this.lens )
        && isEqual( p.film, this.film )
        && isEqual( p.techNotes, this.techNotes )
        && isEqual( p.origFname, this.origFname )
        && p.shutterSpeed == this.shutterSpeed
                && p.filmSpeed == this.filmSpeed
                && p.focalLength == this.focalLength
                && p.FStop == this.FStop
                && p.uid == this.uid
                && p.quality == this.quality );
    }

    public int hashCode() {
        return uid;
    }
}
