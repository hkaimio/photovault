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

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;
import java.util.Map.Entry;
import java.util.UUID;
import org.photovault.imginfo.xml.Base64;

/**
 * (Un)marshal {@link ImageFileDTO} objects from/to XML representation
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class ImageFileXmlConverter implements Converter {
    private Mapper mapper;

    public ImageFileXmlConverter( Mapper mapper ) {
        this.mapper = mapper;
    }

    public boolean canConvert( Class clazz ) {
        return ImageFileDTO.class.equals( clazz );
    }

    public void marshal( Object obj, HierarchicalStreamWriter writer, MarshallingContext ctx ) {
        ImageFileDTO dto = (ImageFileDTO) obj;
        writer.addAttribute( "uuid", dto.getUuid().toString() );
        writer.addAttribute( "size", String.valueOf( dto.getSize() ) );
        writer.addAttribute( "md5", Base64.encodeBytes( dto.getHash() ) );
        for ( Entry<String, ImageDescriptorDTO> e : dto.getImages().entrySet()) {
            ImageDescriptorDTO img = e.getValue();
            String clazz = mapper.serializedClass( img.getClass() );
            writer.startNode( clazz );
            ctx.convertAnother( img );
            writer.endNode();
        }
    }

    public Object unmarshal( HierarchicalStreamReader reader, UnmarshallingContext ctx ) {
        ImageFileDTO dto = new ImageFileDTO();
        UUID uuid = UUID.fromString( reader.getAttribute( "uuid" ) );
        long size = Long.valueOf( reader.getAttribute( "size" ) );
        byte[] hash = Base64.decode( reader.getAttribute( "md5" ) );
        dto.setUuid( uuid );
        dto.setSize( size );
        dto.setHash( hash );

        while( reader.hasMoreChildren() ) {
            reader.moveDown();
            Class clazz = mapper.realClass( reader.getNodeName() );
            ImageDescriptorDTO img = (ImageDescriptorDTO) ctx.convertAnother( dto, clazz );
            dto.addImage( img.getLocator(), img );
            reader.moveUp();
        }
        return dto;
    }


}
