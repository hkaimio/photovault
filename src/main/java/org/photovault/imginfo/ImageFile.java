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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * ImageFile represents info about a certain image file that may or may not exist 
 * in some of Photovault volumes. Image files are regarded as immutable objects -
 * in practice it is of course possible to modify an image file but after the
 * modification it is regarded as a new one. Therefore, an image file in file system 
 * can be linked to an ImageFile object by its MD5 hash code.
 * <p>
 * ImageFile contains one or more {@link ImageInstance}s. There can be several (or 
 * no) copies of an ImageFile available in different volumes. The known locations 
 * are described bu {@link FileLocation} objects.
 * </p>
 * @author Harri Kaimio
 * @since 0.6.0
 */

@Entity
@Table( name="pv_image_files" )
public class ImageFile implements java.io.Serializable {

    static Log log = LogFactory.getLog( ImageFile.class );
    
    /** Creates a new instance of ImageFile */
    protected ImageFile() {
    }
    
    /**
     * Creates a new ImageFile from given file
     * @param f The file from which the ImageFiel fields are calculated.
     */
    public ImageFile( File f ) {
        // Set lastChecked to a time that is certainly earlier than any data read 
        // from file system
        hash = calcHash( f );
        id = UUID.nameUUIDFromBytes( hash );
        size = f.length();
    }
    
    private UUID id;
        
    /**
     * Get the UUID of this ImageFile
     * @return UUID of this object
     */
    @Id
    @Column( name = "id" )
    @org.hibernate.annotations.Type( type = "org.photovault.persistence.UUIDUserType" )
    public UUID getId() {
        return id;
    }
    
    
    /**
     * Set UUID of this object. Note that this should be called by persistence layer 
     * only.
     * @param id UUID for the ImageFile object.
     */
    protected void setId( UUID id ) {
        this.id = id;
    }

    long size;
    
    /**
     * Get size of the associated image file
     * @return Size of the file in bytes.
     */
    @Column( name = "file_size" )
    public long getFileSize() {
        return size;
    }
    
    /**
     * Set size of the associated file. Tgis method should be called only by persistence 
     * layer.
     * @param size Size of the associated file in bytes.
     */
    public void setFileSize( long size ) {
        this.size = size;
    }
    
    private byte[] hash;
    
    /**
     * Get MD5 hash of the associated image file.
     * @return Hash of the file
     */
    @Column( name = "md5_hash" )
    // @org.hibernate.annotations.Type( type = "varbinary" )
    public byte[] getHash() {
        return hash;
    }

    /**
     * Set the MD5 hash of associated file
     * @param hash Value of hash.
     */
    public void setHash(byte[] hash) {
        this.hash = hash;
    }
    
    Set<FileLocation> locations = new HashSet<FileLocation>();
    
    
    /**
     * Get the known locations in which this file is stored
     * @return Set of known locations
     */
    @org.hibernate.annotations.CollectionOfElements
    @JoinTable(
    name = "pv_image_locations",
            joinColumns = @JoinColumn(name = "id")
    )            
    public Set<FileLocation> getLocations() {
        return locations;
    }
    
    /**
     * Set the known locations of the file. Used by persistence layer only.
     * @param locations New value for locations.
     */
    protected void setLocations( Set<FileLocation> locations ) {
        this.locations = locations;
    }
    
    /**
     * Add a new location for the image.
     * @param newLocation FileLocation object that describes the location.
     */
    public void addLocation( FileLocation newLocation ) {
        locations.add( newLocation );
    }
    
    /**
     * remove a location from the file.
     * @param location Location to remove.
     */
    public void removeLocation( FileLocation location ) {
        locations.remove( location );
    }
    
    /**
     Images contained in this file
     */
    Set<ImageInstance> images = new HashSet<ImageInstance>();
    
    /**
     * Get all images stored in this file
     * @return Set of images
     */
    @OneToMany(cascade  = { CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE },
               mappedBy = "file")
    @org.hibernate.annotations.Cascade({
               org.hibernate.annotations.CascadeType.SAVE_UPDATE })
    public Set<ImageInstance> getImages() {
        return images;
    }
    
    /**
     * Set the images stored in this file. USed by persistence layer only.
     * @param images New set of images.
     */
    protected void setImages( Set<ImageInstance> images ) {
        this.images = images;
    }
    
    /**
     *     Utility function to calculate the hash of a specific file
     * @param f The file
     * @return Hash of f
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
    
}
