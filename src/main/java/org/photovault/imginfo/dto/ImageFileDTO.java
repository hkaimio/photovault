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
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 Data transfer object that is used to transfer information about {@link ImageFile}
 between Photovault instances. It contains only information that is relevant in 
 any context - for example, known locations of the file are omitted as these are 
 specific to certain Photovault installation. On the other hand, if the file 
 contains a {@link CopyImageDescriptor} based on original in different file, 
 information about that file is included.
 <p>
 Use {@link ImageFileDtoResolver} to convert ImageFileDTO into a persistent 
 {@link ImageFile} object in local context.
 
 @since 0.6.0
 @author Harri Kaimio
 */
public class ImageFileDTO {
    
    /**
     Constructs an ImageFileDTO based on ImageFile object
     @param f The ImageFile used.
     */
    public ImageFileDTO( ImageFile f ) {
        this(f, new HashMap<UUID, ImageFileDTO>());
    }
    
    /**
     Constructor that is used internally while constructing ImageFile graph.
     @param f The file used
     @param createdFiles Collection of ImageFile objects that are already 
     included in the graph being processed.
     */
    ImageFileDTO( ImageFile f, Map<UUID, ImageFileDTO> createdFiles ) {
        uuid = f.getId();
        hash = f.getHash();
        size = f.getFileSize();        
        createdFiles.put( uuid, this );
        images = new HashMap<String, ImageDescriptorDTO>( f.getImages().size() );
        for ( ImageDescriptorBase img : f.getImages().values() ) {
            ImageDescriptorDTO idto = null;
            if ( img instanceof OriginalImageDescriptor ) {
                idto = new OrigImageDescriptorDTO( img, createdFiles );
            } else {
                idto = new CopyImageDescriptorDTO( (CopyImageDescriptor) img, createdFiles );
            }
            images.put( img.getLocator(), idto );
        }
    }
    /**
     UUID of the image file
     */
    private UUID uuid;
    
    /**
     MD5 hash of the image file
     */
    private byte[] hash;
    
    /**
     Size of the image file (in bytes)
     */
    private long size;
    
    /**
     Images belonging to the file
     */
    private Map<String, ImageDescriptorDTO> images;

    public UUID getUuid() {
        return uuid;
    }

    public byte[] getHash() {
        return hash;
    }

    public long getSize() {
        return size;
    }

    public Map<String, ImageDescriptorDTO> getImages() {
        return Collections.unmodifiableMap( images );
    }

}
