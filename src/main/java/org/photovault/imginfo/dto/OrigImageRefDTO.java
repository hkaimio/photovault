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

import com.google.protobuf.Message;
import java.io.Serializable;
import java.util.UUID;
import org.photovault.imginfo.ProtobufConverter;
import org.photovault.common.ProtobufHelper;
import org.photovault.common.ProtobufSupport;
import org.photovault.common.Types;
import org.photovault.imginfo.ImageFile;
import org.photovault.imginfo.OriginalImageDescriptor;
import org.photovault.imginfo.dto.ImageProtos.ImageRef.Builder;

/**
 Data transfer object for serializing reference to {@link OriginalImageDescriptor}.
 
 This class is used to serialize the reference to original in {@link PhotoInfo}
 
 @since 0.6.0
 @author Harri Kaimio
 @see ImageFileDTO
 @see OrigImageDescriptorDTO
 */
public class OrigImageRefDTO implements Serializable,
        ProtobufSupport<OrigImageRefDTO, ImageProtos.ImageRef, ImageProtos.ImageRef.Builder>{

    /**
     DTO describing the {@link ImageFile} that contains the referenced image
     @serialField 
     */
    private ImageFileDTO fileDto;
    
    /**
     Location string of th eimage inside the containing file
     @serialField 
     */
    private String locator;
    
    /**
     * UUID of the file (used of fileDto is not stored in this reference
     */
    private UUID fileUuid;
    
    /**
     Constructs a reference DTO for image
     @param orig The image
     */
    OrigImageRefDTO( OriginalImageDescriptor orig ) {
        fileDto = new ImageFileDTO( orig.getFile() );
        locator = orig.getLocator();
        fileUuid = fileDto.getUuid();
    }

    public OrigImageRefDTO( ImageProtos.ImageRef proto ) {
        locator = proto.getLocator();
        if ( proto.hasOriginalFile() ) {
            fileDto = new ImageFileDTO( proto.getOriginalFile() );

        } else if ( proto.hasFileUuid() ) {
            fileUuid = ProtobufHelper.uuid( proto.getFileUuid() );
        } else {
            throw new IllegalArgumentException( 
                    "ImageRef proto must have either file UUID or file itself defined" );
        }
        
    }
    
    /**
     Get the DTO of the image file that contains the image
     @return DTO for the file
     */
    public ImageFileDTO getFileDto() {
        return fileDto;
    }

    /**
     Returns location of the image inside the file
     @return Locator string for the image
     */
    public String getLocator() {
        return locator;
    }

    /**
     * Returns UUID of the file.
     */
    public UUID getFileUuid() {
        return fileUuid;
    }
    
    public Builder getBuilder() {
        return ImageProtos.ImageRef.newBuilder()
                .setFileUuid( ProtobufHelper.uuidBuf( this.fileDto.getUuid() ) )
                .setLocator( locator )
                .setOriginalFile( fileDto.getBuilder() )
                .setFileUuid( ProtobufHelper.uuidBuf( fileUuid ) );
    }

   public static class ProtobufConv implements ProtobufConverter<OrigImageRefDTO> {

        public Message createMessage( OrigImageRefDTO obj ) {
            return obj.getBuilder().build();
        }

        public OrigImageRefDTO createObject( Message msg ) {
            return new OrigImageRefDTO( (ImageProtos.ImageRef) msg );
        }

    }
}
