/*
  Copyright (c) 2008 Harri Kaimio
  
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

package org.photovault.imginfo.dto;

import org.photovault.imginfo.*;
import org.hibernate.Session;
import org.photovault.replication.DTOResolver;

/**
 Utility class used to merge image files described by {@link ImageFileDTO} to 
 local database.
 
 @author Harri Kaimio
 @since 0.6.0
 @see ImageFileDTO
 */
public class ImageFileDtoResolver implements DTOResolver<ImageFile, ImageFileDTO> {

    /**
     Hibernate session used to do queries and persist created objects.
     */
    private Session session;

    /**
     Creates a new resolver.
     */
    public ImageFileDtoResolver() {
        
    }
    
    /**
     Set the Hibernate session used.
     @param s The session.
     */
    public void setSession( Session s ) {
        session = s;
    }
    
    /**
     Get a persistent object that corresponds tp given data transfer object. If
     a ImageFile with the same UUID exists in the database, that is returned. 
     Otherwise a new persistent object is created.
     @param dto The DTO being converted.
     @return The persistent object mathing the DTO
     */
    public ImageFile getObjectFromDto( ImageFileDTO dto ) {
        ImageFile file = 
                (ImageFile) session.get(  ImageFile.class,dto.getUuid() );
        if ( file == null ) {
            file = new ImageFile();
            file.setId( dto.getUuid() );
            file.setHash( dto.getHash() );
            file.setFileSize( dto.getSize() );
            for ( ImageDescriptorDTO imgdto : dto.getImages().values() ) {
                ImageDescriptorBase img = imgdto.getImageDescriptor( this );
                img.setFile( file );
                file.getImages().put( imgdto.getLocator(), img  );
            }
            session.save( file );
        }
        return file;
    }

    /**
     Creates DTO based on ImageFile object.
     @param f
     @return
     */
    public ImageFileDTO getDtoFromObject( ImageFile f ) {
        return new ImageFileDTO( f );
    }

}
