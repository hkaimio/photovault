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
import java.util.Iterator;
import java.util.Vector;
import org.photovault.common.PVDatabase;
import org.photovault.common.PhotovaultSettings;


/**
 This is an abstract base class for all volume types.
 <p>
 All image files in Photovault are stored in <strong>volumes</strong>,
 i.e. a directory hierarchy managed by an object derived from VolumeBase.
  
 @author Harri Kaimio
 */
public abstract class VolumeBase {
    
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( VolumeBase.class.getName() );

    static VolumeBase defaultVolume = null;
    private static HashMap volumes = null;
    
    /**
       Returns the current default volume object
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
       Returns the volume with a given name or null if such volume does not exist
       @param volName The name to look for
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

    public VolumeBase() {
        
    }
    
    /**
     Constructor.
     @param volName Name of the new volume
     @param volBaseDir Tob directory of the volume's directory hierarchy
     */
     
    public VolumeBase( String volName, String volBaseDir ) {
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
     This abstract method must be overloaded by each VolumeBase implementation so
     that it returns the correct file path in which a given image should be stored 
     in the volume.
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
     Returns the base directory for the volume.
    */
    
    public File getBaseDir() {
	return volumeBaseDir;
    }

    /**
     * Sets the base dir for the volume. If the directory does no exist it is 
     * created.
     */
    public void setBaseDir( String baseDirName ) {
        log.debug( "New basedir for " + volumeName + ": " + baseDirName );
        File baseDir = new File( baseDirName );
        setBaseDir( baseDir );
    }
    

    /**
     * Sets the base dir for the volume. If the directory does no exist it is 
     * created.
     */
    public void setBaseDir( File baseDir ) {
        log.debug( "New basedir for " + volumeName + ": " + baseDir );        
	volumeBaseDir = baseDir;
        if ( !volumeBaseDir.exists() ) {
	    volumeBaseDir.mkdir();
	}        
    }
    
    protected String volumeName = "";

    /**
       Returns the volume name
     */
    public String getName() {
	return volumeName;
    }

    /**
     * Sets the voume name
     */
    public void setName( String volName ) {
        unregisterVolume();
        volumeName = volName;
        registerVolume();
    }
    
    /** returns true if the volume is available, false otherwise (if e.g. the volume is
	stored on CD-ROM that is not mounted currently
    */
    
    public boolean isAvailable() {
	return true;
    }
    
    /**
     Checks whether a certain file is part  of the volume (i.e. in the directory 
     hierarchy under the base directory. Note that existence of the file is not 
     ckecked, nor whether the file is really an instance of a PhotoInfo.
     @return true if the file belongs to the volume, false otherwise
     @throws IOException if is an error when creating canonical form of f
     */
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
     The derived classes must overload this method to write the value of the object as an 
     XML element.
     @param outputWriter The writer into which the object is written
     @param indent Number of spaces to indent each line
     */
    public abstract void writeXml(BufferedWriter outputWriter, int indent ) throws IOException;
    protected File volumeBaseDir;    
}
