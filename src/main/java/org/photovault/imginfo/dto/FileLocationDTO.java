/*
  Copyright (c) 2010 Harri Kaimio

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

import com.google.protobuf.Message;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;
import org.photovault.imginfo.ProtobufConverter;
import org.photovault.common.ProtobufHelper;
import org.photovault.common.ProtobufSupport;
import org.photovault.imginfo.ExternalVolume;
import org.photovault.imginfo.FileLocation;
import org.photovault.imginfo.VolumeBase;
import org.photovault.imginfo.dto.ImageProtos.Volume;

/**
 * Data transfer object that describes {@link FileLocation}
 * @author Harri Kaimio
 * @since 0.6.0
 */
@XStreamAlias( "location" )
public class FileLocationDTO
        implements ProtobufSupport<FileLocationDTO, ImageProtos.FileLocation, ImageProtos.FileLocation.Builder>,
        DebugStringBuilder {
    @XStreamAsAttribute
    private UUID volumeId;
    @XStreamAsAttribute
    private String volumeType;
    @XStreamAsAttribute
    private String location;
    @XStreamAsAttribute
    private String lastModified;
    static private DateFormat df = new SimpleDateFormat( "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );
    @XStreamOmitField
    private long lastModifiedMillis;

    public FileLocationDTO( FileLocation l ) {
        VolumeBase v = l.getVolume();
        volumeId = v.getId();
        volumeType= v.getClass().getName();
        location = l.getFname();
        lastModifiedMillis = l.getLastModified();
        lastModified = df.format( new Date( lastModifiedMillis ) );
    }

    public FileLocationDTO( ImageProtos.FileLocation l ) {
        switch ( l.getVolume().getType() ) {
            case EXTERNAL:
                volumeType = ExternalVolume.class.getName();
                break;
            case TRAD:
                volumeType = Volume.class.getName();
                break;
            default:
                throw new IllegalStateException(
                        "Unknown volume type " + l.getVolume().getType() );
        }
        volumeId = ProtobufHelper.uuid( l.getVolume().getUuid() );
        location = l.getPath();
        lastModifiedMillis = l.getLastModifiedTime();
        lastModified = df.format( new Date( lastModifiedMillis ) );
    }

    public FileLocationDTO( Class<VolumeBase> volClass,
            UUID volId, String path, long lastMod ) {
        volumeType = volClass.getName();
        volumeId = volId;
        location = path;
        lastModifiedMillis = lastMod;
        lastModified = df.format( new Date( lastModifiedMillis ) );
    }

    /**
     * @return the volumeId
     */
    public UUID getVolumeId() {
        return volumeId;
    }

    /**
     * @return the volumeType
     */
    public String getVolumeType() {
        return volumeType;
    }

    /**
     * @return the location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @return the lastModified
     */
    public String getLastModified() {
        return lastModified;
    }

    public ImageProtos.FileLocation.Builder getBuilder() {
        Volume.Builder vb = ImageProtos.Volume.newBuilder();
        vb.setUuid( ProtobufHelper.uuidBuf( volumeId ) );
        if ( volumeType.equals( Volume.class.getName() ) ) {
            vb.setType( ImageProtos.VolumeType.TRAD );
        } else if ( volumeType.equals( ExternalVolume.class.getName() ) ) {
            vb.setType( ImageProtos.VolumeType.EXTERNAL );
        }

        ImageProtos.FileLocation.Builder b = ImageProtos.FileLocation.newBuilder()
                .setLastModifiedTime( lastModifiedMillis )
                .setPath( location )
                .setVolume( vb )
                .setPath( location );
        return b;
    }

    public void buildDebugString( StringBuilder b, String prefix ) {
        b.append( "Location volume: " ).append( volumeType ).append( "-" );
        b.append( volumeId ).append(  ", location: ").append( location );
        b.append( ", last modified: " ).append( lastModified );
    }
   public static class ProtobufConv implements ProtobufConverter<FileLocationDTO> {

        public Message createMessage( FileLocationDTO obj ) {
            return obj.getBuilder().build();
        }

        public FileLocationDTO createObject( Message msg ) {
            return new FileLocationDTO( (ImageProtos.FileLocation) msg );
        }

    }
}
