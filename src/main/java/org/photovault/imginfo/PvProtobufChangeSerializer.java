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

package org.photovault.imginfo;

import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.Message;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.image.ChanMapOp;
import org.photovault.image.ColorCurve;
import org.photovault.image.CropOp;
import org.photovault.image.DCRawMapOp;
import org.photovault.image.DCRawOp;
import org.photovault.image.ImageOpChain;
import org.photovault.imginfo.dto.FileLocationDTO;
import org.photovault.imginfo.dto.FolderRefDTO;
import org.photovault.imginfo.dto.ImageFileDTO;
import org.photovault.imginfo.dto.ImageProtos;
import org.photovault.imginfo.dto.OrigImageRefDTO;
import org.photovault.replication.ChangeProtos;
import org.photovault.replication.ChangeProtos.ValueChange;
import org.photovault.replication.ProtobufChangeSerializer;

/**
 * Change serializer for custom types used by Photovault
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class PvProtobufChangeSerializer extends ProtobufChangeSerializer {

    private static Log log = LogFactory.getLog( PvProtobufChangeSerializer.class );
    public PvProtobufChangeSerializer() {
        super();
        ImageProtos.registerAllExtensions( extensions );
        registerConverter( ImageProtos.chanMapOp, ChanMapOp.class,
                new ChanMapOp.ProtobufConv() );
        registerConverter( ImageProtos.colorCurve, ColorCurve.class,
                new ColorCurve.ProtobufConv() );
        registerConverter( ImageProtos.cropOp, CropOp.class,
                new CropOp.ProtobufConv() );
        registerConverter( ImageProtos.dcrawOp, DCRawOp.class,
                new DCRawOp.ProtobufConv() );
        registerConverter( ImageProtos.fileLocation, FileLocationDTO.class,
                new FileLocationDTO.ProtobufConv() );
        registerConverter( ImageProtos.imafeFile, ImageFileDTO.class,
                new ImageFileDTO.ProtobufConv() );
        registerConverter( ImageProtos.imageOpChain, ImageOpChain.class,
                new ImageOpChain.ProtobufConv() );
        registerConverter( ImageProtos.imageRef, OrigImageRefDTO.class,
                new OrigImageRefDTO.ProtobufConv() );
        registerConverter( ImageProtos.rawMapOp, DCRawMapOp.class,
                new DCRawMapOp.ProtobufConv() );
        registerConverter( ImageProtos.timeRange, FuzzyDate.class,
                new FuzzyDate.ProtobufConv() );
        registerConverter( ImageProtos.folderRef, FolderRefDTO.class,
                new FolderRefDTO.ProtobufConv() );
        registerConverter( ImageProtos.tag, Tag.class, new Tag.ProtobufConv() );
    }

    /**
     * Simple strucure to hold information about capabilities of a converter
     */
    private static class ConverterEntry {
        ConverterEntry( GeneratedMessage.GeneratedExtension ext, Class clazz,
                ProtobufConverter conv ) {
            id = ext.getDescriptor().getNumber();
            this.ext = ext;
            this.clazz = clazz;
            this.conv = conv;
        }

        int id;
        GeneratedMessage.GeneratedExtension ext;
        Class clazz;
        ProtobufConverter conv;
    }

    /**
     * Type converters based on the Protobuf extension number used for the type
     */
    private Map<Integer, ConverterEntry> convertersById = new HashMap();
    /**
     * Type convertes indexed by the class they can convert
     */
    private Map<Class, ConverterEntry> convertersByClass = new HashMap();

    /**
     * Register a converter for certain type
     * @param ext Protobuf extension used for storing the type in ValueChange.
     * @param clazz Class that can be converted by this converter
     * @param conv The converter to use
     */
    private void registerConverter( GeneratedMessage.GeneratedExtension ext, Class clazz, ProtobufConverter conv ){
        ConverterEntry e = new ConverterEntry( ext, clazz, conv );
        convertersById.put( e.id, e );
        convertersByClass.put( clazz, e );
    }

    @Override
    protected void initValueChangeBuilderExt( ChangeProtos.ValueChange.Builder vb, Object value ) {
        Class clazz = value.getClass();
        ConverterEntry e = convertersByClass.get(  clazz );
        if ( e != null ) {
            Message msg = e.conv.createMessage( value );
            vb.setType( e.id );
            vb.setExtension( e.ext, msg );
        } else {
            log.error( "Cannot find converter for class " + value.getClass() );
        }
    }

    @Override
    protected Object msgToValueChangeExt( ValueChange ch ) {
        Object ret = null;
        int id = ch.getType();
        ConverterEntry e = convertersById.get( id );
        if ( e != null ) {
            Message msg = (Message) ch.getExtension( e.ext );
            ret = e.conv.createObject( msg );
        } else {
            log.error(  "Cannot find converter for value type " + id );
        }
        return ret;
    }
}
