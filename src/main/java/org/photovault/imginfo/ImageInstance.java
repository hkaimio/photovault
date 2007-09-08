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

package org.photovault.imginfo;

import java.awt.geom.Rectangle2D;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.media.jai.PlanarImage;
import org.odmg.*;
import java.sql.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.io.*;
import org.photovault.common.PhotovaultException;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.dcraw.RawImage;
import org.photovault.image.ChannelMapOperation;
import javax.persistence.*;
import org.photovault.image.ChannelMapOperationFactory;

/**
 This class abstracts a single instance of a image that is stored in a file.
 */

@Entity
@Table( name="image_instances" )
@IdClass( ImageInstance.InstanceId.class )
public class ImageInstance implements ImageInstanceModifier {
    
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( ImageInstance.class.getName() );
    
    /**
     ImageInstance constructor must not be called outside persistence layer,
     otherwise ImageInstances must be created by ImageInstance.create().
     Reason for this is that creation can fail if e.g. the given file is not a
     proper image file.
     */
    protected ImageInstance() {
        uuid = UUID.randomUUID();
    }
    
    /**
     Wrapper for composite primapry key for this class
     */
    public static class InstanceId implements Serializable {
        String volumeId;
        protected String getVolume_id() {
            return volumeId;
        }
        
        protected void setVolume_id( String newId ) {
            this.volumeId = newId;
        }
        String fname;
        protected String getFname() {
            return fname;
        }
        
        protected void setFname( String newFname ) {
            this.fname = newFname;
        }
    }
    
    /**
     ID of the volume where this isntance is stored
     */
    String volumeId;
    
    @Id
//    @AttributeOverride( name="volumeId", column = @Column( name = "volume_id" ) )
    @Column( name = "volume_id" )
    protected String getVolume_id() {
        return volumeId;
    }
    
    protected void setVolume_id( String newId ) {
        this.volumeId = newId;
    }
    
    /**
     Name of the file in volume
     */
    String fname;
    
    @Id
    @Column( name = "fname" )
    protected String getFname() {
        return fname;
    }
    
    protected void setFname( String newFname ) {
        this.fname = newFname;
    }
    
    /**
     The the Hibernate ID for this instance.
     */
    @Transient
    public InstanceId getHibernateId() {
        InstanceId id = new InstanceId();
        id.setFname( this.getFname() );
        id.setVolume_id( this.getVolume_id() );
        return id;
    }
    
    
    /**
     Creates a new ImageInstance for a certain file in a volume. The instance is not
     yet assigned to any PhotoInfo, this must be done later by PhotoInfo.addInstance().
     @param vol The volume
     @param imgFile The file
     @return The ImageInstance created or <code>null</code> if creation was unsuccesfull
     (e.g. if imgFile does not exist or is not of a recognized file type). In this case
     the method also aborts ongoing ODMG transaction.
     */
    public static ImageInstance create( VolumeBase vol, File imgFile ) {
        log.debug( "Creating instance " + imgFile.getAbsolutePath() );
        ImageInstance i = new ImageInstance();
        i.uuid = UUID.randomUUID();
        i.volume = vol;
        i.volumeId = vol.getName();
        i.imageFile = imgFile;
        i.fileSize = imgFile.length();
        i.mtime = imgFile.lastModified();
        i.fname = vol.mapFileToVolumeRelativeName( imgFile );
        // Read the rest of fields from the image file
        try {
            i.readImageFile();
        } catch (Exception e ) {
            log.warn( "Error opening image file: " + e.getMessage() );
            // The image does not exist, so it cannot be read!!!
            return null;
        }
        i.calcHash();
        i.checkTime = new java.util.Date();
        return i;
    }
    
    
    /**
     Creates a new image file object.
     @param volume Volume in which the instance is stored
     @param imageFile File object pointing to the image instance
     file
     @param photo PhotoInfo object that represents the content of
     the image file
     @param instanceType Type of the instance (original, copy, thumbnail, ...)
     @return A ImageInstance object or <code>null</code> if the instance could
     not been created (e.g. if the file did not exist)
     
     */
    public static ImageInstance create( VolumeBase volume, File imageFile,
            PhotoInfo photo, int instanceType ) {
        
        log.debug( "Creating instance, volume = " + volume.getName() + " photo = " + photo.getId()
        + " image file = " + imageFile.getName() );
        // Initialize transaction context
        ImageInstance f = new ImageInstance();
        f.uuid = UUID.randomUUID();
        // Set up the primary key fields before locking the object
        f.volume = volume;
        f.volumeId = volume.getName();
        f.imageFile = imageFile;
        f.fileSize = imageFile.length();
        f.mtime = imageFile.lastModified();
        f.fname = volume.mapFileToVolumeRelativeName( imageFile );
        f.instanceType = instanceType;
        log.debug( "locked instance" );
        
        f.photo = photo;
        // Read the rest of fields from the image file
        try {
            f.readImageFile();
        } catch (Exception  e ) {
            log.warn( "Error opening image file: " + e.getMessage() );
            // The image does not exist, so it cannot be read!!!
            return null;
        }
        f.calcHash();
        f.checkTime = new java.util.Date();
        return f;
    }
    
    /**
     Creates a new ImageInstance with a given UUID. This method should only be
     used when importing data from other database, since the attributes of the
     image are not set to legal (even those that form primary key in database!)
     @param uuid UUID of the created instance
     @return New ImageInstance object
     */
    
    public static ImageInstance create( UUID uuid ) {
        ImageInstance i = new ImageInstance();
        i.uuid = uuid;
        i.volume = null;
        /*
         volumeId & fname form the primary key in persistent DB so we must set
         them to some value. These values indicate that the instance is just a
         "placeholder" and no file is available.
         */
        i.volumeId = "##nonexistingfiles##";
        i.fname = uuid.toString();
        Database db = ODMG.getODMGDatabase();
        return i;
    }
    
    /**
     Retrieves the info record for a image file based on its name and path.
     @param volume Volume in which the instance is stored
     @param fname Finle name for the image
     @return ImageFile object representing the image
     @throws PhotoNotFoundException exception if the object can not be retrieved.
     */
    
    public static ImageInstance retrieve( VolumeBase volume, String fname ) throws PhotoNotFoundException {
        String oql = "select instance from " + ImageInstance.class.getName()
        + " where volumeId = \"" + volume.getName() + "\" and fname = \"" + fname + "\"";
        
        List instances = null;
        
        // Get transaction context
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Implementation odmg = ODMG.getODMGImplementation();
        try {
            OQLQuery query = odmg.newOQLQuery();
            query.create( oql );
            instances = (List) query.execute();
            txw.commit();
        } catch ( Exception e ) {
            txw.abort();
            return null;
        }
        
        ImageInstance instance = null;
        if ( instances != null && instances.size() > 0 ) {
            instance = (ImageInstance) instances.get( 0 );
        }
        return instance;
    }
    
    
    public static ImageInstance retrieveByUuid(UUID uuid) {
        String oql = "select instance from " + ImageInstance.class.getName()
        + " where instance_uuid = \"" + uuid.toString() + "\"";
        
        List instances = null;
        
        // Get transaction context
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Implementation odmg = ODMG.getODMGImplementation();
        try {
            OQLQuery query = odmg.newOQLQuery();
            query.create( oql );
            instances = (List) query.execute();
            txw.commit();
        } catch ( Exception e ) {
            txw.abort();
            return null;
        }
        
        ImageInstance instance = null;
        if ( instances != null && instances.size() > 0 ) {
            if ( instances.size() > 1 ) {
                log.error( "ERROR: " + instances.size() +
                        " records for ImageInstance with uuid=" + uuid.toString() );
            }
            instance = (ImageInstance) instances.get( 0 );
        }
        return instance;
    }
    
    
    UUID uuid = null;
    
    /**
     Get the globally unique ID for this photo;
     */
    @Column( name = "instance_uuid" )
    @org.hibernate.annotations.Type( type = "org.photovault.persistence.UUIDUserType" )
    public UUID getUUID() {
        if ( uuid == null ) {
            setUUID( UUID.randomUUID() );
        }
        return uuid;
    }
    
    public void setUUID( UUID uuid ) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.uuid = uuid;
        txw.commit();
    }
        
    ImageFile file;
    
    @ManyToOne( cascade = CascadeType.ALL )
    @org.hibernate.annotations.Cascade({
        org.hibernate.annotations.CascadeType.SAVE_UPDATE })
    @JoinColumn( name = "file_id" )
        
    public ImageFile getFile() {
            return file;
    }
    
    public void setFile( ImageFile f ) {
        file = f;
    } 
    
    /**
     Inits the complex attributes volume and imageFile. Since these
     are not mapped directly to database columns, this function will
     be called by OJB Rowreader to initialize these correctly after
     the object has been read from database.
     */
    protected void initFileAttrs() {
        volume = VolumeBase.getVolume( volumeId );
        try {
            imageFile = volume.mapFileName( fname );
        } catch ( Exception e ) {
            log.warn( "Error while initializing imageFile: " + e.getMessage() );
        }
    }
    
    /**
     Deletes the ImageInstance object from database.
     @deprecated Use delete( boolean deleteFromExtVol ) instead.
     */
    public void delete() {
        // Delete the file unless it is stored in external volume
        if ( !(volume instanceof ExternalVolume) ) {
            if ( imageFile != null && !imageFile.delete() ) {
                log.error( "File " + imageFile.getAbsolutePath() + " could not be deleted" );
            }
        }
    }
    
    /**
     Attempts to delete the instance
     @param deleteFromExtVol If true, the instance is deleted even if it resides
     on external volume.
     @return True if deletion was successful
     */
    public boolean delete( boolean deleteFromExtVol ) {
        boolean success = false;
        ODMGXAWrapper txw = new ODMGXAWrapper();
        Database db = ODMG.getODMGDatabase();
        if ( !(volume instanceof ExternalVolume) || deleteFromExtVol ) {
            if ( imageFile ==  null || imageFile.delete() ) {
                db.deletePersistent( this );
                success = true;
            }
        }
        txw.commit();
        return success;
    }
    
    /**
     Opens the image file specified by fname & dirname properties and reads
     the rest of fields from that
     @throws IOException if the image cannot be read.
     @deprecated use {@link ImageFile} for access to image files.
     */
    protected void readImageFile() throws PhotovaultException, IOException {
        
        String fname = getImageFile().getName();
        int lastDotPos = fname.lastIndexOf( "." );
        if ( lastDotPos <= 0 || lastDotPos >= fname.length()-1 ) {
            throw new PhotovaultException( "Cannot determine file type extension of " + imageFile.getAbsolutePath() );
        }
        String suffix = fname.substring( lastDotPos+1 );
        Iterator readers = ImageIO.getImageReadersBySuffix( suffix );
        if ( readers.hasNext() ) {
            ImageReader reader = (ImageReader)readers.next();
            ImageInputStream iis = null;
            try {
                iis = ImageIO.createImageInputStream( imageFile );
                if ( iis != null ) {
                    reader.setInput( iis, true );
                    
                    width = reader.getWidth( 0 );
                    height = reader.getHeight( 0 );
                    reader.dispose();
                }
            } catch (IOException ex) {
                log.debug( "Exception in readImageFile: " + ex.getMessage() );
                throw ex;
            } finally {
                if ( iis != null ) {
                    try {
                        iis.close();
                    } catch (IOException ex) {
                        log.warn( "Cannot close image stream: " + ex.getMessage() );
                    }
                }
            }
        } else {
            RawImage ri = new RawImage( imageFile );
            if ( ri.isValidRawFile() ) {
                // PlanarImage img = ri.getCorrectedImage();
                width = ri.getWidth();
                height = ri.getHeight();
            } else {
                throw new PhotovaultException( "Unknown image file extension " + suffix +
                        "\nwhile reading " + imageFile.getAbsolutePath() );
            }
        }
        
    }
    
    /**
     Calculates MD5 hash of the image file
     */
    protected void calcHash() {
        hash = calcHash( imageFile );
        
//        if ( instanceType == INSTANCE_TYPE_ORIGINAL && photoUid > 0 ) {
//            try {
//                // TODO: Fix this!!!
////                PhotoInfo p = PhotoInfo.findPhotoInfo( photoUid );
//                if ( p != null ) {
//                    p.setOrigInstanceHash( hash );
//                }
//            } catch (PhotoNotFoundException ex) {
//                // No action taken
//            }
//        }
    }
    
    /**
     Utility function to calculate the hash of a specific file
     @param f The file
     @return Hash of f
     */
    public static byte[] calcHash( File f ) {
        FileInputStream is = null;
        byte hash[] = null;
        try {
            is = new FileInputStream( f );
            byte readBuffer[] = new byte[4096];
            MessageDigest md = MessageDigest.getInstance("MD5");
            int bytesRead = -1;
            while( ( bytesRead = is.read( readBuffer ) ) > 0 ) {
                md.update( readBuffer, 0, bytesRead );
            }
            hash = md.digest();
        } catch (NoSuchAlgorithmException ex) {
            log.error( "MD5 algorithm not found" );
        } catch (FileNotFoundException ex) {
            log.error( f.getAbsolutePath() + "not found" );
        } catch (IOException ex) {
            log.error( "IOException while calculating hash: " + ex.getMessage() );
        }  finally {
            try {
                if ( is != null ) {
                    is.close();
                }
            } catch (IOException ex) {
                log.error( "Cannot close stream after calculating hash" );
            }
        }
        return hash;
    }
    
    byte[] hash = null;
    
    /**
     Returns the MD5 hash
     */
    public byte[] getHash() {
        if ( hash == null && imageFile != null ) {
            calcHash();
        }
        return  (hash != null) ? (byte[]) hash.clone() : null;
    }
    
    public void setHash( byte[] hash ) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        this.hash = hash;
        txw.commit();
    }
    
    /**
     reference to the volume where the image file is located
     */
    VolumeBase volume;
    
    /**
     * Get the value of volume.
     * @return value of volume.
     */
    @Transient
    public VolumeBase getVolume() {
        if ( volume == null && volumeId != null ) {
            volume = VolumeBase.getVolume( volumeId );
        }
        return volume;
    }
    
    /**
     * Set the value of volume.
     * @param v  Value to assign to volume.
     */
    public void setVolume(VolumeBase  v) {
        this.volume = v;
        volumeId = volume.getName();
    }
    
    /**
     The image file
     */
    File imageFile;
    
    
    /**
     * Get the value of imageFile.
     * @return value of imageFile.
     */
    @Transient
    public File getImageFile() {
        if ( imageFile == null && (fname != null && getVolume() != null ) ) {
            try {
                imageFile = getVolume().mapFileName( fname );
            } catch (FileNotFoundException ex) {
                log.warn( "file" + fname + "not found in ImageInstance#getImageFile");
            }
        }
        return imageFile;
    }
    
    /**
     * Set the value of imageFile.
     * @param v  Value to assign to imageFile.
     */
    public void setImageFile(File  v) {
        this.imageFile = v;
        fname = volume.mapFileToVolumeRelativeName( v );
    }
    
    /**
     Returns the file path relative to the volume base directory
     */
    @Transient
    public String getRelativePath() {
        String baseDir = null;
        try {
            baseDir = volume.getBaseDir().getCanonicalPath();
        } catch ( IOException e ) {}
        
        String filePath = null;
        try {
            filePath = imageFile.getCanonicalPath();
        } catch ( IOException e ) {}
        
        // Make a sanity check that the image file is uder the base directory
        if ( !filePath.substring( 0, baseDir.length() ).equals( baseDir ) ) {
            log.warn( "ERROR: " + filePath + " not under " + baseDir );
            return "";
        }
        
        String relPath = filePath.substring( baseDir.length() );
        return relPath;
    }
    
    private long fileSize;
    
    /**
     Get the size of the image file <b>as stored in database</b>
     */
    @Column( name = "file_size" )
    public long getFileSize() {
        return fileSize;
    }
    
    /**
     Set the file size. NOTE!!! This method should only be used by XmlImporter.
     */
    public void setFileSize( long s ) {
        this.fileSize = s;
    }
    
    private long mtime;
    
    /**
     Get the last modification time of the actual image file <b>as stored in
     database</b>. Measured as milliseconds since epoc(Jan 1, 1970 midnight)
     */
    @Column( name = "mtime" )
    public long getMtime() {
        return mtime;
    }
    
    /**
     Set the time the file was last modified.
     @param newMtime new last modified time as milliseconds sence epoc (Jan 1, 1970
     midnight UTC)
     */
    protected void setMtime( long newMtime ) {
        this.mtime = newMtime;
    }
    private java.util.Date checkTime;
    
    /**
     Returns the time when consistency of the instance information was last checked
     (i.e. that the image file really exists and is still unchanged after creating
     the instance.
     */
    @Column( name = "check_time" )
    @Temporal(value = TemporalType.TIMESTAMP)
    public java.util.Date getCheckTime() {
        return checkTime != null ? (java.util.Date) checkTime.clone() : null;
    }
    
    /**
     Set the time this instance was last checked for consistency
     @param newCheckTime The last check time.
     */
    public void setCheckTime( java.util.Date newCheckTime ) {
        this.checkTime = newCheckTime;
    }
    
    /**
     Check that the information in database and associated volume are consistent.
     In praticular, this method checks that
     <ul>
     <li>The file exists</li>
     <li>That the actual file size matches information in database unless
     size in database is 0.</li>
     <li>That the last modification time matches that in the database. If the
     modification times differ the hash is recalculated and that is used
     to determine consistency.</li>
     <li>If file hash matches the hash stored in database the information is
     assumed to be consistent. If mtimes or file sizes differed thee are updated
     into database.
     </ul>
     @return true if information was consistent, false otherwise
     */
    public boolean doConsistencyCheck() {
        boolean isConsistent = true;
        boolean needsHashCheck = false;
        File f = this.getImageFile();
        if ( f.exists() ) {
            long size = f.length();
            if ( size != this.fileSize ) {
                isConsistent = false;
                if ( this.fileSize == 0 ) {
                    needsHashCheck = true;
                }
            }
            
            long mtime = f.lastModified();
            if ( mtime != this.mtime ) {
                needsHashCheck = true;
            }
            
            if ( needsHashCheck ) {
                byte[] dbHash = (byte[]) hash.clone();
                calcHash();
                byte[] realHash = (byte[])hash.clone();
                isConsistent = Arrays.equals( dbHash, realHash );
                if ( isConsistent ) {
                    this.mtime = mtime;
                    this.fileSize = size;
                }
            }
        }
        
        /* Update the database with check result if it was positive */
        
        if ( isConsistent ) {
            this.checkTime = new java.util.Date();
        }
        return isConsistent;
    }
    
    private int width;
    
    /**
     * Get the value of width.
     * @return value of width.
     */
    @Column( name = "width" )
    public int getWidth() {
        return width;
    }
    
    /**
     * Set the value of width.
     * @param v  Value to assign to width.
     */
    public void setWidth(int  v) {
        this.width = v;
    }
    
    int height;
    
    /**
     * Get the value of height.
     * @return value of height.
     */
    @Column( name = "height" )
    public int getHeight() {
        return height;
    }
    
    /**
     * Set the value of height.
     * @param v  Value to assign to height.
     */
    public void setHeight(int  v) {
        this.height = v;
    }
    
    // File history
    /**
     The file is a original
     */
    final public static int INSTANCE_TYPE_ORIGINAL = 1;
    /**
     The file has been created from original image by e.g. changing resolution, file format,
     by doing some image procesing...
     */
    final public static int INSTANCE_TYPE_MODIFIED = 2;
    /**
     The file is intended to be used only as a thumbnail, not by other appilcations
     */
    final public static int INSTANCE_TYPE_THUMBNAIL = 3;
    
    int instanceType = INSTANCE_TYPE_ORIGINAL;
    
    /**
     * Get the value of instanceType.
     * @return value of instanceType.
     */
    @Transient
    public int getInstanceType() {
        return instanceType;
    }
    
    /**
     * Set the value of instanceType.
     * @param v  Value to assign to instanceType.
     */
    public void setInstanceType(int  v) {
        this.instanceType = v;
    }
    
    @Column( name = "instance_type" )
    protected String getInstanceTypeStr() {
        switch( instanceType ) {
            case ImageInstance.INSTANCE_TYPE_ORIGINAL:
                return "original";
            case ImageInstance.INSTANCE_TYPE_MODIFIED:
                return "modified";
            case ImageInstance.INSTANCE_TYPE_THUMBNAIL:
                return "thumbnail";
            default:
                return null;
        }
    }
    
    protected void setInstanceTypeStr( String strInstanceType ) {
        if ( strInstanceType.equals( "original" ) ) {
            instanceType = ImageInstance.INSTANCE_TYPE_ORIGINAL;
        } else if ( strInstanceType.equals( "modified" ) ) {
            instanceType = ImageInstance.INSTANCE_TYPE_MODIFIED;
        } else if ( strInstanceType.equals( "thumbnail" ) ) {
            instanceType = ImageInstance.INSTANCE_TYPE_THUMBNAIL;
        }
    }
    
    double rotated;
    
    /**
     * Get the amount this instance is rotated compared to the original image.
     * @return value of rotated.
     */
    @Column( name = "rotated" )
    public double getRotated() {
        return rotated;
    }
    
    /**
     * Set the amount this image is rotated when compared to the original image
     * @param v  Value to assign to rotated.
     */
    public void setRotated(double  v) {
        this.rotated = v;
    }
    
    /**
 CropBounds describes the how this instance is cropped from original image. It is
 defined as proportional coordinates that are applied after rotating the
 original image so that top left corner is (0.0, 0.0) and bottom right
 (1.0, 1.0)
     */
    
    double cropMinX;
    double cropMaxX;
    double cropMinY;
    double cropMaxY;
    
    /**
 Check that the e crop bounds are defined in consistent manner. This is needed
 since in old installations the max parameters can be larger than min ones.
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
    @org.hibernate.annotations.Type( type = "org.photovault.persistence.CropRectUserType" )
    @org.hibernate.annotations.Columns(
    columns = {
        @Column( name = "crop_xmin" ),
@Column( name = "crop_xmax" ),
@Column( name = "crop_ymin" ),
@Column( name = "crop_ymax", length = 4 )
    }
    )
    public Rectangle2D getCropBounds() {
        checkCropBounds();
        return new Rectangle2D.Double( cropMinX, cropMinY,
                cropMaxX-cropMinX, cropMaxY-cropMinY );
    }
    
    
    /**
 Set the preferred cropping operation
 @param cropBounds New crop bounds
     */
    public void setCropBounds( Rectangle2D cropBounds ) {
        cropMinX = cropBounds.getMinX();
        cropMinY = cropBounds.getMinY();
        cropMaxX = cropBounds.getMaxX();
        cropMaxY = cropBounds.getMaxY();
    }
    
    /**
     Raw conversion settings that were used when creating this instance or
     <code>null</code> if it was not created from raw image.
     */
    RawConversionSettings rawSettings = null;
    
    
    /**
     Set the raw conversion settings for this photo
     @param s The new raw conversion settings used to create this instance.
     The method makes a clone of the object.
     */
    public void setRawSettings( RawConversionSettings s ) {
        RawConversionSettings settings = null;
        if ( s != null ) {
            settings = s.clone();
        }
        rawSettings = settings;
    }
    
    /**
     Get the current raw conversion settings.
     @return Current settings or <code>null</code> if instance was not created
     from a raw image.
     */
    @ManyToOne( cascade = CascadeType.ALL )
    @org.hibernate.annotations.Cascade({
        org.hibernate.annotations.CascadeType.SAVE_UPDATE })
    @JoinColumn( name = "rawconv_id" )
    // In old databases (created by OJB rawconv_id can be 0 if no settings are present
    @org.hibernate.annotations.NotFound( action = org.hibernate.annotations.NotFoundAction.IGNORE )        
    public RawConversionSettings getRawSettings() {
        return rawSettings;
    }
        
        
    /**
     Mapping from original iamge color channels to those used in this instance.
     */
    ChannelMapOperation channelMap = null;
    
    /**
     Set the color channel mapping from original to this instance
     @param cm the new color channel mapping
     */
    public void setColorChannelMapping( ChannelMapOperation cm ) {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.WRITE );
        channelMap = cm;
        txw.commit();
    }
    
    /**
     Get color channel mapping from original to this instance.
     @return The current color channel mapping
     */
    // TOOD: Implement ChannelMap persistence
    @Transient
    public ChannelMapOperation getColorChannelMapping() {
        ODMGXAWrapper txw = new ODMGXAWrapper();
        txw.lock( this, Transaction.READ );
        txw.commit();
        return channelMap;
    }
    
    /**
     Get the XML data for color channel mapping that is stored into database field.
     */    
    @Column( name = "channel_map" )
    protected byte[] getColorChannelMappingXmlData() {
        byte[] data = null;
        if ( channelMap != null ) {
            String xmlStr = this.channelMap.getAsXml();
            data = xmlStr.getBytes();
        }
        return data;
    }
    
    /**
     Set the color channel mapping based on XML data read from database field. For
     Hibernate use.
     @param data The data read from database.
     */
    protected void setColorChannelMappingXmlData( byte[] data ) {
        channelMap = ChannelMapOperationFactory.createFromXmlData( data );
    }
    
    
    PhotoInfo photo = null;
    
    @ManyToOne( targetEntity = org.photovault.imginfo.PhotoInfo.class )
    @JoinColumn( name = "photo_id", nullable = false )
    public PhotoInfo getPhoto() {
        return photo;
    }
    
    public void setPhoto( PhotoInfo photo ) {
        this.photo = photo;
    }
    
    /**
     @deprecated Was needed by OJB, replaced by getPhoto().
     TODO: Remove dependencies!!!
     */
    @Transient
    public int getPhotoUid() {
        return photo != null ? photo.getUid() : -1;
    }
    
    /**
     Test for eqaulity. Since ImageInstance is immutable we will compare only 
     primary keys.
     @param o The objetc to compare with
     @return true if o & this are equal.
     */
    public boolean equals( Object o ) {
        if ( this == o ) return true;
        if ( !(o instanceof ImageInstance) ) return false;
        ImageInstance that = (ImageInstance) o;
        return this.uuid.equals( that.uuid );
    }
    
    public int hashCode() {
        return uuid.hashCode();
    }
}
