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

package org.photovault.imginfo;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.odmg.*;
import java.sql.*;
import java.util.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import java.io.*;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;

/**
   This class abstracts a single instance of a image that is stored in a file.
*/

public class ImageInstance {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( ImageInstance.class.getName() );

    /**
     ImageInstance constructor is private and they must be created by ImageInstance.create().
     Reason for this is that creation can fail if e.g. the given file is not a
     proper image file.
     */
    private ImageInstance() {
        // Empty
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
    public static ImageInstance create ( VolumeBase vol, File imgFile ) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
        ImageInstance i = new ImageInstance();
	i.volume = vol;
	i.volumeId = vol.getName();
	i.imageFile = imgFile;
        i.fileSize = imgFile.length();
        i.mtime = imgFile.lastModified();
        i.fname = vol.mapFileToVolumeRelativeName( imgFile );
        txw.lock( i, Transaction.WRITE );
        i.calcHash();
        // Read the rest of fields from the image file
	try {
	    i.readImageFile();
	} catch (IOException e ) {
	    txw.abort();
	    log.warn( "Error opening image file: " + e.getMessage() );
	    // The image does not exist, so it cannot be read!!!
	    return null;
	}
        i.checkTime = new java.util.Date();
        txw.commit();
        return i;
    }
    
    
    /**
       Creates a new image file object. The object is persistent,
       i.e. it is stored in database
       @param volume Volume in which the instance is stored
       @param imageFile File object pointing to the image instance
       file
       @param photo PhotoInfo object that represents the content of
       the image file
       @return A ImageInstance object

    */
    public static ImageInstance create( VolumeBase volume, File imageFile, PhotoInfo photo ) {

	log.debug( "Creating instance, volume = " + volume.getName() + " photo = " + photo.getUid()
		   + " image file = " + imageFile.getName() );
	// Initialize transaction context
	ODMGXAWrapper txw = new ODMGXAWrapper();
	
	ImageInstance f = new ImageInstance();
	// Set up the primary key fields before locking the object
	f.volume = volume;
	f.volumeId = volume.getName();
	f.imageFile = imageFile;
        f.fileSize = imageFile.length();
        f.mtime = imageFile.lastModified();
        f.fname = volume.mapFileToVolumeRelativeName( imageFile );
	txw.lock( f, Transaction.WRITE );
	log.debug( "locked instance" );
	
	f.photoUid = photo.getUid();
	// Read the rest of fields from the image file
	try {
	    f.readImageFile();
	} catch (IOException e ) {
	    txw.abort();
	    log.warn( "Error opening image file: " + e.getMessage() );
	    // The image does not exist, so it cannot be read!!!
	    return null;
	}
        f.calcHash();
	f.checkTime = new java.util.Date();
        txw.commit();
	return f;
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
    */
    public void delete() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	Database db = ODMG.getODMGDatabase();	
	db.deletePersistent( this );
        // Delete the file unless it is stored in external volume
        if ( !(volume instanceof ExternalVolume) ) {
            if ( imageFile != null && !imageFile.delete() ) {
                log.error( "File " + imageFile.getAbsolutePath() + " could not be deleted" );
            }
        }
	txw.commit();
    }

    /**
       Opens the image file specified by fname & dirname properties and reads the rest of fields from that
       @throws IOException if the image cannot be read.
    */	    
    protected void readImageFile() throws IOException {

	// Find the JPEG image reader
	// TODO: THis shoud decode also other readers from fname
	Iterator readers = ImageIO.getImageReadersByFormatName("jpg");
	ImageReader reader = (ImageReader)readers.next();
	ImageInputStream iis = ImageIO.createImageInputStream( imageFile );
        if ( iis != null ) {
            reader.setInput( iis, true );
            
            width = reader.getWidth( 0 );
            height = reader.getHeight( 0 );
            
            iis.close();
        }
    }

    /**
        Calculates MD5 hash of the image file
     */
    protected void calcHash() {
        hash = calcHash( imageFile );

        if ( instanceType == INSTANCE_TYPE_ORIGINAL && photoUid > 0 ) {
            try {
                PhotoInfo p = PhotoInfo.retrievePhotoInfo( photoUid );
                if ( p != null ) {
                    p.setOrigInstanceHash( hash );
                }
            } catch (PhotoNotFoundException ex) {
                // No action taken
            }
        }
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
    
    /**
       Id of the volume (for OJB)
    */
    String volumeId;
    /**
       reference to the volume where the image file is located
    */
    VolumeBase volume;
    
    /**
     * Get the value of volume.
     * @return value of volume.
     */
    public VolumeBase getVolume() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
	return volume;
    }
    
    /**
     * Set the value of volume.
     * @param v  Value to assign to volume.
     */
    public void setVolume(VolumeBase  v) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.volume = v;
	volumeId = volume.getName();
	txw.commit();
    }

    /**
       The image file
    */
    File imageFile;

    /**
       File name of the image file without directory (as returner by imageFile.getName()).
       Used as OJB reference.
    */
    String fname;

    
    /**
     * Get the value of imageFile.
     * @return value of imageFile.
     */
    public File getImageFile() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
        log.debug( "ImageFile = " + imageFile + " volume = " + volume.getName() + " base dir = " + volume.getBaseDir() );
	return imageFile;
    }
    
    /**
     * Set the value of imageFile.
     * @param v  Value to assign to imageFile.
     */
    public void setImageFile(File  v) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.imageFile = v;
	fname = volume.mapFileToVolumeRelativeName( v );
	txw.commit();
    }

    /**
       Returns the file path relative to the volume base directory
    */

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
    public long getFileSize() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
	return fileSize;        
    }
    
    private long mtime;
    
    /**
     Get the last modification time of the actual image file <b>as stored in 
     database</b>. Measured as milliseconds since epoc(Jan 1, 1970 midnight)
     */
    public long getMtime() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
	return mtime;                
    }
    
    private java.util.Date checkTime;
    
    /**
     Returns the time when consistency of the instance information was last checked 
     (i.e. that the image file really exists and is still unchanged after creating 
     the instance.
     */
    public java.util.Date getCheckTime() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
	return checkTime != null ? (java.util.Date) checkTime.clone() : null;                
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
        ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
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
                    txw.lock( this, Transaction.WRITE );
                    this.mtime = mtime;
                    this.fileSize = size;
                }
            }
        }    
        
        /* Update the database with check result if it was positive */
         
        if ( isConsistent ) {
            txw.lock( this, Transaction.WRITE );
            this.checkTime = new java.util.Date();
        }
        txw.commit();
        return isConsistent;
    }
    
    private int width;
    
    /**
     * Get the value of width.
     * @return value of width.
     */
    public int getWidth() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
	return width;
    }
    
    /**
     * Set the value of width.
     * @param v  Value to assign to width.
     */
    public void setWidth(int  v) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.width = v;
	txw.commit();
    }
    int height;
    
    /**
     * Get the value of height.
     * @return value of height.
     */
    public int getHeight() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
	return height;
    }
    
    /**
     * Set the value of height.
     * @param v  Value to assign to height.
     */
    public void setHeight(int  v) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.height = v;
	txw.commit();
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
    public int getInstanceType() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
	return instanceType;
    }
    
    /**
     * Set the value of instanceType.
     * @param v  Value to assign to instanceType.
     */
    public void setInstanceType(int  v) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.instanceType = v;
	txw.commit();
    }

    double rotated;
    
    /**
     * Get the amount this instance is rotated compared to the original image.
     * @return value of rotated.
     */
    public double getRotated() {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.READ );
	txw.commit();
	return rotated;
    }
    
    /**
     * Set the amount this image is rotated when compared to the original image
     * @param v  Value to assign to rotated.
     */
    public void setRotated(double  v) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	this.rotated = v;
	txw.commit();
    }

    /**
     Sets the photo UID of this instance. THis should only be called by 
     PhotoInfo.addInstance()
     @param uid UID of the photo
     */
    protected void setPhotoUid(int uid) {
	ODMGXAWrapper txw = new ODMGXAWrapper();
	txw.lock( this, Transaction.WRITE );
	photoUid = uid;
	txw.commit();
        
    }
    
    int photoUid;
    
    public int getPhotoUid() {
        return photoUid;
    }
}
