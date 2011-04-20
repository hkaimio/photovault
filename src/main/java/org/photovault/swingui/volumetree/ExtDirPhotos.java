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

package org.photovault.swingui.volumetree;

import java.util.List;
import java.util.UUID;
import org.hibernate.Query;
import org.hibernate.Session;
import org.photovault.imginfo.ExternalVolume;
import org.photovault.imginfo.PhotoCollection;
import org.photovault.imginfo.PhotoCollectionChangeListener;
import org.photovault.imginfo.PhotoInfo;

/**
 * Collection to query photos that are stored in external directory
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class ExtDirPhotos implements PhotoCollection {
    private final UUID volId;
    private final String dirName;

    /**
     * Constructor
     * @param volId UUID of the volume to query
     * @param dirName path to the directory
     */
    ExtDirPhotos( UUID volId, String dirName ) {
        this.volId = volId;
        this.dirName = dirName;
    }
    public int getPhotoCount() {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public PhotoInfo getPhoto( int numPhoto ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void addPhotoCollectionChangeListener( PhotoCollectionChangeListener l ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public void removePhotoCollectionChangeListener( PhotoCollectionChangeListener l ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    public List<PhotoInfo> queryPhotos( Session session ) {
        ExternalVolume vol = 
                (ExternalVolume) session.get(  ExternalVolume.class, volId );
        Query q = session.createQuery( "from PhotoInfo p join p.original.file.locations loc where loc.volume = :vol and loc.dirName = :dir" ).setParameter( "vol", vol ).setString( "dir", dirName );
        return q.list();
    }
    
    public UUID getVolId() {
        return volId;
    }
    
    public String getDirPath() {
        return dirName;
    }
    
}
