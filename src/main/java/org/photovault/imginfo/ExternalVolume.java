/*
  Copyright (c) 2006-2007 Harri Kaimio
  
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
import java.io.IOException;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 Extenal volume is a volume that resides outside Photovault repository, i.e. a
 normal user-created subdirectory of photos. Since it is not under control of 
 Photovault some of its characterestics differ from normal volumes:
 <ul>
 <li> Photovault will not create new instances it external volume
 <li> Photovault does not set or expect restrictions on how the directories
 and files are organized.
 </ul>
 @author Harri Kaimio
 */
@Entity
@DiscriminatorValue( "external_volume")
public class ExternalVolume extends VolumeBase {
    
    /**
     Default constructor
     */
    public ExternalVolume() {
        super();
    }
    
    /** Creates a new instance of. The ExternalVolume 
     @param volName Name of the volume
     @param baseDir The base directory of the external volume
     */
    public ExternalVolume( String volName, String baseDir ) {
        super( volName, baseDir );
    }
    
    /**
     *     getFilingFname should return that can be used for an instance of a certain file.
     *     However, Photovault cannot store anything in external volume so return null.
     * @param imageFile File whose filing name is requested
     * @return returns always <CODE>null</CODE>
     */
     
    public File getFilingFname(File imageFile) {
        return null;
    }

    /**
     *     Photovault cannot create new instances in external volume so return null
     * @param photo The photo
     * @param strExtension File type extension
     * @return always <CODE>null</CODE>
     */
    public File getInstanceName(PhotoInfo photo, String strExtension) {
        return null;
    }
    

    /**
     * Returns the {@link FileLocation} object that describes a file in this 
     * volume
     * @param f The file whose location is requested
     * @return FileLocation object describing the location, or <code>null</code>
     * if f points to a file that is not under the volume's root directory.
     */
    @Override
    public FileLocation getFileLocation( File f ) {
        FileLocation ret = null;
        File directory = f.getParentFile().getAbsoluteFile();
        String dirPath = directory.getPath();
        // Get the relative path from volume root
        String basedir = getBaseDir().getPath();
        if ( dirPath.startsWith( basedir ) ) {
            String relPath= dirPath.equals( basedir) ?
                "" : dirPath.substring( basedir.length()+1 );
            String relFilePath = f.getAbsolutePath().substring( basedir.length()+1 );
            int level = 0;
            while ( !directory.equals( getBaseDir() ) ) {
                level++;
                directory = directory.getParentFile();
            }
            ret = new FileLocation( this, relFilePath );
            ret.setDirName( relPath );
            ret.setDirLevel( level );
        }
        return ret;
    }

    /**
     * Get a FileLocation object based on given volume relative path
     * @param volPath
     * @return 
     */
    public FileLocation getFileLocation( String volPath ) {
        File f = new File( volPath );
        File dir = f.getParentFile();
        String dirPath = dir == null ? "" : dir.getPath();
        int level = 0;
        while ( dir != null ) {
            level++;
            dir = dir.getParentFile();
        }
        FileLocation l = new FileLocation( this, volPath );
        l.setDirLevel( level );
        l.setDirName( dirPath );
        return l;
    }


    /**
     * Write the object as XML
     * @param outputWriter The writer into which the object is written
     * @param indent Number of spaces to indent the outermost element.
     * @throws java.io.IOException If writing fails.
     */
    public void writeXml(BufferedWriter outputWriter, int indent ) throws IOException {
        String s = "                                ".substring( 0, indent );
        outputWriter.write( s+ "<external-volume name=\"" + getName() +
                "\" basedir=\"" + getBaseDir() );
    }

}
