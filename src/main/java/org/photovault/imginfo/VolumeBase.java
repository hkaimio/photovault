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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.Vector;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.common.PVDatabase;
import org.photovault.common.PhotovaultSettings;


/**
 This is an abstract base class for all volume types.
 <p>
 All image files in Photovault are stored in <strong>volumes</strong>,
 i.e. a directory hierarchy managed by an object derived from VolumeBase.
  
 @author Harri Kaimio
 */
@Entity
@Table( name = "pv_volumes" )
@Inheritance( strategy = InheritanceType.SINGLE_TABLE )
@DiscriminatorColumn( name = "volume_type", discriminatorType = DiscriminatorType.STRING )
public abstract class VolumeBase implements java.io.Serializable {
    
    Log log = LogFactory.getLog( VolumeBase.class.getName() );

    static VolumeBase defaultVolume = null;
    private static HashMap volumes = null;
    
    private UUID id;
    
    /**
     * Get UUID of this volume
     * @return UUID
     */
    @Id
    @Column( name = "volume_id" )
    @org.hibernate.annotations.Type( type = "org.photovault.persistence.UUIDUserType" )    
    public UUID getId() {
        return id;
    }
    
    /**
     * Set the UUID of this volume. USed only by persistence layer.
     * @param id New uuid
     */
    public void setId( UUID id ) {
        this.id = id;
    }
    
    /**
     *       Returns the current default volume object
     * @return The default volume
     @deprecated Use VolumeDAO#getDefaultVolume instead
     */
    public static VolumeBase getDefaultVolume() {
	if ( defaultVolume == null ) {
            PhotovaultSettings settings = PhotovaultSettings.getSettings();
            PVDatabase db = settings.getCurrentDatabase();
            defaultVolume = db.getDefaultVolume();
	}
	return defaultVolume;
    }

    /**
     *       Returns the volume with a given name or null if such volume does not exist
     * @param volName The name to look for
     * @return The volume of <CODE>null</CODE>
     */
    public static VolumeBase getVolume( String volName ) {
	VolumeBase vol = null;
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        PVDatabase db = settings.getCurrentDatabase();
	vol = db.getVolume( volName );
	return vol;
    }
     
    /**
     Tries to find the volume into which a fiel belongs, i.e. whether it is under 
     the base directory of some volume of current database.
     @param f The file whose volume is of interest
     @return The volume the file belongs to or <code>null</code> if it does not 
     belong to any volume
     @throws IOException if there is an error constructing canonical form of the file
     @deprecated Use PVDatabase#getVolumeOfFile instead.
     */
    public static VolumeBase getVolumeOfFile( File f ) throws IOException {
        VolumeBase v = null;
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        PVDatabase db = settings.getCurrentDatabase();
	v = db.getVolumeOfFile( f );
        return v;
    }

    /**
     * Base constructor.
     */
    public VolumeBase() {
        id = UUID.randomUUID();
    }
    
    /**
     Constructor.
     @param volName Name of the new volume
     @param volBaseDir Tob directory of the volume's directory hierarchy
     */
     
    public VolumeBase( String volName, String volBaseDir ) {
        id = UUID.randomUUID();
	volumeName = volName;
	volumeBaseDir = new File( volBaseDir );
	if ( !volumeBaseDir.exists() ) {
	    volumeBaseDir.mkdir();
	}
	registerVolume();
    }

       
    /**
     This private method adds the volume into volumes collection
     */
    private void registerVolume() {
        log.debug( "registering volume " + volumeName + ", basedir " + volumeBaseDir );
	if ( volumes == null ) {
	    volumes = new HashMap();
	}
	volumes.put( volumeName, this );
    }
    
    /**
     This private method removes the volume from the volumes collection
     */    
    private void unregisterVolume() {
        if ( volumes != null ) {
            volumes.remove( volumeName );
        }
    }
    
    /**
     *     This abstract method must be overloaded by each VolumeBase implementation so
     *     that it returns the correct file path in which a given image should be stored 
     *     in the volume.
     * @param imageFile File whose name to look for
     * @return Volume internal name for that file or <code>null</code> if it is not inside 
     * volume directory hierarchy.
     */
    abstract public File getFilingFname( File imageFile );
    
    /**
     Tis abstract method must be overloaded so that it returns a file path in which a
     new instanc of a certain PhotoInfo object can be stored.
     @param photo The PhotoInfo objec which instance will be stored into the volume
     @param strExtension Filename extension for the photo
     @return A unique file name in the directory hierarchy controlled by the 
     volume or <code>null</code> if the volume does not allow storing on the image
     */
    abstract public File getInstanceName( PhotoInfo photo, String strExtension );
    
    /**
     Maps a file name that is stored in an ImgeInstance to a absolute path in the Volume.
     For e.g. performance reasons the volume can internally store files in a different 
     directory structure that wht is shown to outside.<p>
     The default implementation just constructs an apsolute path by adding the file name
     to volume bas directory.
     @param fname File name to map
     @return An absolute path to the file
     @throws FileNotFoundException if there is no file with the given name
     */
    public File mapFileName( String fname ) throws FileNotFoundException {
        File f= new File( volumeBaseDir, fname );
        if ( !f.exists() ) {
            throw new FileNotFoundException( "File " + f.getAbsolutePath() + " does not exist" );
        }
        return f;
    }

    
    /**
     Maps a file path into a name that should be used for the file in the volume.
     <p>
     THis is the reverse operation of mapFileName(), i.e. for al volume 
     implementations it is required that f.equals( mapFileName( mapFileToVolmeRelativeName( f ) )
     is true for all possible files that are stored in the volume.
     @param f The file to map
     @return Volume relative name for the file or null if the file is outside 
     volume
     */
    public String mapFileToVolumeRelativeName( File f ) {
        File absVolBaseDir = getBaseDir().getAbsoluteFile();
        File absImageFile = f.getAbsoluteFile();
        Vector pathElems = new Vector();
        File p = absImageFile;
        boolean found = false;
        while ( p != null ) {
            if ( p.equals( absVolBaseDir ) ) {
                found = true;
                break;
            }
            pathElems.add( p.getName() );
            p = p.getParentFile();
        }
        String relPath = null;
        if ( found ) {
            File relFile = new File( "" );
            for ( int n = pathElems.size()-1; n >= 0; n-- ) {
                relFile = new File( relFile, (String) pathElems.get( n ) );
            }
            relPath = relFile.getPath();
        }
        return relPath;
    }
    
    
    /**
     *     Returns the base directory for the volume.
     * @return The base directory
     */
    @Transient
    public File getBaseDir() {
	return volumeBaseDir;
    }

    /**
     * Get the base directory name. Used by persistence layer.
     * @return absolute pathname of base directory
     */
    @Column( name = "base_dir" )    
    protected String getBaseDirName() {
        return (volumeBaseDir != null ) ? volumeBaseDir.getAbsolutePath() : null;
    }
    
    
    /**
     * Sets the base dir for the volume. If the directory does no exist it is 
     * created.
     * @param baseDirName absolute path to base directory
     */
    public void setBaseDir( String baseDirName ) {
        log.debug( "New basedir for " + volumeName + ": " + baseDirName );
        File baseDir = new File( baseDirName );
        setBaseDir( baseDir );
    }
    
    /**
     * Set the base directory. Used by persistence layer.
     * @param baseDirName Absolute path to base directory.
     */
    protected void setBaseDirName( String baseDirName ) {
        setBaseDir( baseDirName );
    }

    /**
     * Sets the base dir for the volume. If the directory does no exist it is 
     * created.
     * @param baseDir Base directory.
     */
    public void setBaseDir( File baseDir ) {
        log.debug( "New basedir for " + volumeName + ": " + baseDir );        
	volumeBaseDir = baseDir;
        if ( !volumeBaseDir.exists() ) {
	    volumeBaseDir.mkdir();
	}        
    }
    
    
    /**
     * Name of this volume
     */
    protected String volumeName = "";

    /**
     *       Returns the volume name
     * @return Name of the volume
     */
    @Column( name = "volume_name" )
    public String getName() {
	return volumeName;
    }

    /**
     * Sets the voume name
     * @param volName New name fot the volume
     */
    public void setName( String volName ) {
        unregisterVolume();
        volumeName = volName;
        registerVolume();
    }
    
    /**
     * returns true if the volume is available, false otherwise (if e.g. the volume is
     * 	stored on CD-ROM that is not mounted currently
     * @return Availablility status
     */
    @Transient
    public boolean isAvailable() {
	return true;
    }
    
    /**
     *     Checks whether a certain file is part  of the volume (i.e. in the directory 
     *     hierarchy under the base directory. Note that existence of the file is not 
     *     ckecked, nor whether the file is really an instance of a PhotoInfo.
     * @return true if the file belongs to the volume, false otherwise
     * @param f File 
     * @throws IOException if is an error when creating canonical form of f
     */
    @Transient
    public boolean isFileInVolume( File f ) throws IOException {
        boolean isInVolume = false;
        
        if ( f == null ) {
            return false;
        }
        
        // First get the canonical forms of both f and volumeBaseDir
        // After that check if f or some of its parents matches f
        File vbdCanon = volumeBaseDir.getCanonicalFile();
        File fCanon = f.getCanonicalFile();
        File p = fCanon;        
        while ( p != null ) {
            if ( p.equals( vbdCanon ) ){
                isInVolume = true;
                break;
            }
            p = p.getParentFile();
        }
        return isInVolume;
        
    }

    /**
     *     The derived classes must overload this method to write the value of the object as an 
     *     XML element.
     * @param outputWriter The writer into which the object is written
     * @param indent Number of spaces to indent each line
     * @throws java.io.IOException if writing fails.
     */
    public abstract void writeXml(BufferedWriter outputWriter, int indent ) throws IOException;
    /**
     * Base directory for the volume
     */
    protected File volumeBaseDir;    
}
