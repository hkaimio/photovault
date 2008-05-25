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
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.UUID;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.image.ChannelMapOperation;

/**
 Data transfer object of {@link CopyImageDescriptor} objects.
 
 @since 0.6.0
 @author Harri Kaimio
 @see CopyImageDescriptor
 @see ImageDescriptorDTO
 */
public class CopyImageDescriptorDTO extends ImageDescriptorDTO {
    
    CopyImageDescriptorDTO( CopyImageDescriptor img, Map<UUID, ImageFileDTO> createdFiles ) {
        super( img );
        colorChannelMapping = img.getColorChannelMapping();
        cropArea = img.getCropArea();
        rotation = img.getRotation();
        rawSettings = img.getRawSettings();
        
        // Make sure that also original instance is stored in this graph
        ImageFile origFile = img.getOriginal().getFile();
        UUID origFileId = origFile.getId();
        if ( createdFiles.containsKey( origFileId ) ) {
            origImageFile = createdFiles.get( origFileId );
        } else {
            origImageFile = new ImageFileDTO( origFile, createdFiles );
        }
        origLocator = img.getLocator();
    }
    private ChannelMapOperation colorChannelMapping;
    private Rectangle2D cropArea;
    private double rotation;
    private RawConversionSettings rawSettings;
    private ImageFileDTO origImageFile;
    private String origLocator;

    @Override
    protected ImageDescriptorBase createImageDescriptor() {
        return new CopyImageDescriptor();
    }
    
    @Override
    protected void updateDescriptor( ImageDescriptorBase img,
            ImageFileDtoResolver fileResolver ) {
        super.updateDescriptor( img, fileResolver );
        CopyImageDescriptor cimg = (CopyImageDescriptor) img;
        cimg.setColorChannelMapping( getColorChannelMapping() );
        cimg.setCropArea( getCropArea() );
        cimg.setRawSettings( getRawSettings() );
        ImageFile origFile = fileResolver.getObjectFromDto( getOrigImageFile() );
        OriginalImageDescriptor original = 
                (OriginalImageDescriptor) origFile.getImage( getLocator() );
        cimg.setOriginal( original );
    }

    public ChannelMapOperation getColorChannelMapping() {
        return colorChannelMapping;
    }

    public Rectangle2D getCropArea() {
        return cropArea;
    }

    public double getRotation() {
        return rotation;
    }

    public RawConversionSettings getRawSettings() {
        return rawSettings;
    }

    public ImageFileDTO getOrigImageFile() {
        return origImageFile;
    }

    public String getOrigLocator() {
        return origLocator;
    }
}
