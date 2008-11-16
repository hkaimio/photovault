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

import org.hibernate.Session;
import org.photovault.imginfo.*;
import org.photovault.replication.HibernateDTOResolver;

/**
 Utility class used to merge image files described by {@link ImageFileDTO} to 
 local database.
 
 @author Harri Kaimio
 @since 0.6.0
 @see ImageFileDTO
 */
public class ImageFileDtoResolver extends HibernateDTOResolver<ImageFile, ImageFileDTO> {


    /**
     Creates a new resolver.
     */
    public ImageFileDtoResolver() {
        super();
    }
        
    /**
     Constructor for creating resolver as part of {@link OrigImageRefResolver}.
     @param s
     */
    ImageFileDtoResolver( Session s ) {
        super();
        setSession( s );
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
                (ImageFile) getSession().get(  ImageFile.class, dto.getUuid() );
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
            getSession().save( file ); 
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
