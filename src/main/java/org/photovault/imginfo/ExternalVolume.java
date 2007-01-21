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
     getFilingFname should return that can be used for an instance of a certain file.
     However, Photovault cannot store anything in external volume so return null.
     */
     
    public File getFilingFname(File imageFile) {
        return null;
    }

    /**
     Photovault cannot create new instances in external volume so return null
     */
    public File getInstanceName(PhotoInfo photo, String strExtension) {
        return null;
    }
    
    int folderId = -1;
    
    /**
     Returns the ID of the folder that is used to represent this external volume. 
     If this external volume is not associated withny folder return <code>null</code>.
     */
    public int getFolderId() {
        return folderId;
    }
    
    /**
     Sets the id of the folder that is used to represent this volume.
     @param id The id of new folder of -1 to disassociate this volume from any 
     folder.
     */
    public void setFolderId( int id ) {
        folderId = id;
    }

    public void writeXml(BufferedWriter outputWriter, int indent ) throws IOException {
        String s = "                                ".substring( 0, indent );
        outputWriter.write( s+ "<external-volume name=\"" + getName() +
                "\" basedir=\"" + getBaseDir() +
                "\" folder=\"" + getFolderId() + "\"/>\n" );
    }
    
}
