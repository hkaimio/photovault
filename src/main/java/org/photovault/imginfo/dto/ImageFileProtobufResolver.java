/*
  Copyright (c) 2011 Harri Kaimio

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

package org.photovault.imginfo.dto;

import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.Log;
import org.hibernate.Session;
import org.photovault.imginfo.Volume;
import org.photovault.imginfo.FileLocation;
import org.photovault.imginfo.VolumeBase;
import org.photovault.imginfo.VolumeDAO;
import org.photovault.imginfo.VolumeManager;
import java.util.List;
import org.photovault.imginfo.ImageDescriptorBase;
import java.util.UUID;
import org.photovault.imginfo.CopyImageDescriptor;
import org.photovault.imginfo.ExternalVolume;
import org.photovault.imginfo.ImageFile;
import org.photovault.imginfo.OriginalImageDescriptor;
import org.photovault.imginfo.dto.ImageProtos.ImageRef;
import org.photovault.replication.HibernateDTOResolver;
import static org.photovault.common.ProtobufHelper.*;
/**
 * Resolver for persisting ImageFile messages.
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class ImageFileProtobufResolver extends HibernateDTOResolver<ImageFile, ImageProtos.ImageFile>{

    private static Log log = LogFactory.getLog( ImageFileProtobufResolver.class );

    public ImageFileProtobufResolver() {
        super();
    }
    
    public ImageFileProtobufResolver( Session s ) {
        super();
        setSession( s );
    }
    
    public ImageFile getObjectFromDto( ImageProtos.ImageFile dto ) {
        UUID fileId = uuid( dto.getUuid() );
        ImageFile file =
                (ImageFile) getSession().get( ImageFile.class, fileId );
        if ( file == null ) {
            file = new ImageFile();
            file.setId( fileId );
            file.setHash( dto.getMd5Hash().toByteArray() );
            file.setFileSize( dto.getSize() );
            for ( ImageProtos.Image imgdto : dto.getImagesList() ) {
                ImageDescriptorBase img = null;
                switch( imgdto.getType() ) {
                    case ORIGINAL:
                        img = new OriginalImageDescriptor( file, imgdto );
                        break;
                    case COPY:
                        ImageProtos.ImageRef origRef = imgdto.getOriginal();
                        OriginalImageDescriptor orig = findImage( origRef );
                        img = new CopyImageDescriptor( file, imgdto, orig );
                }
            }
            List<ImageProtos.FileLocation> locations = dto.getLocationsList();
            VolumeManager vm = VolumeManager.instance();
            VolumeDAO volDao = getDAOFactory().getVolumeDAO();
            for ( ImageProtos.FileLocation l : locations ) {
                ImageProtos.Volume vp = l.getVolume();
                UUID volId = uuid( vp.getUuid() );
                VolumeBase v = volDao.getVolume( volId );
                if ( v == null ) {
                    switch( vp.getType() ) {
                        case TRAD:
                            v = new Volume();
                            break;
                        case EXTERNAL:
                            v = new ExternalVolume();
                            break;
                        default:
                            log.error( "Unknown volume type " + vp.getType() );
                    }
                    v.setId( volId );
                    volDao.makePersistent( v );
                    volDao.flush();
                }
                FileLocation loc = v.getFileLocation( l.getPath() );
                if ( l.hasLastModifiedTime() ) {
                    loc.setLastModified( l.getLastModifiedTime() );
                }
                if ( !file.getLocations().contains( loc ) ) {
                    file.addLocation( loc );
                }
            }
            getSession().save( file );
        }
        return file;
    }



    public ImageProtos.ImageFile getDtoFromObject( ImageFile object ) {
        throw new UnsupportedOperationException( "Not supported yet." );
    }

    private OriginalImageDescriptor findImage( ImageRef origRef ) {
        OriginalImageDescriptor orig = null;
        if ( origRef.hasFileUuid() && origRef.hasLocator() ) {
            UUID fileId = uuid( origRef.getFileUuid() );
            ImageFile file =
                    (ImageFile) getSession().get( ImageFile.class, fileId );
            if ( file != null ) {
                orig = (OriginalImageDescriptor) file.getImage( origRef.getLocator() );
            }
        }
        if ( orig == null && origRef.hasOriginalFile() && origRef.hasLocator() ) {
            ImageFile file = getObjectFromDto( origRef.getOriginalFile() );
            orig = (OriginalImageDescriptor) file.getImage( origRef.getLocator() );
        }
        return orig;
    }
}
