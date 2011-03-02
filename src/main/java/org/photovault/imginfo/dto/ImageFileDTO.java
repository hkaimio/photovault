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

import com.google.protobuf.ByteString;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamConverter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import org.photovault.imginfo.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.media.jai.ImageLayout;
import org.photovault.common.ProtobufConverter;
import org.photovault.common.ProtobufHelper;
import org.photovault.common.ProtobufSupport;
import org.photovault.common.Types;

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
@XStreamAlias( "image-file" )
@XStreamConverter( ImageFileXmlConverter.class )
public class ImageFileDTO implements Serializable,
        ProtobufSupport<ImageFileDTO, ImageProtos.ImageFile, ImageProtos.ImageFile.Builder>{

    static final long serialVersionUID = -1744409450144341619L;

    ImageFileDTO() {
        images = new HashMap<String, ImageDescriptorDTO>();
        locations = new ArrayList<FileLocationDTO>();
    }

    /**
     Constructs an ImageFileDTO based on ImageFile object
     @param f The ImageFile used.
     */
    public ImageFileDTO( ImageFile f ) {
        this(f, new HashMap<UUID, ImageFileDTO>());
    }

    public ImageFileDTO( ImageProtos.ImageFile proto ) {
        this();
        hash = proto.getMd5Hash().toByteArray();
        uuid = new UUID( proto.getUuid().getMostSigBits(),
                proto.getUuid().getLeastSigBits() );
        size = proto.getSize();
        for ( ImageProtos.Image i : proto.getImagesList() ) {
            ImageDescriptorDTO idesc = null;
            switch( i.getType() ) {
                case ORIGINAL:
                    idesc = new OrigImageDescriptorDTO( i );
                    break;
                case COPY:
                    idesc = new CopyImageDescriptorDTO( i );
                    break;
                default:
                    break;
            }
            if ( idesc != null ) {
                images.put( idesc.getLocator(), idesc );
            }
        }
        for ( ImageProtos.FileLocation lp : proto.getLocationsList() ) {
            Class volType = null;
            switch ( lp.getVolume().getType() ) {
                case TRAD:
                    volType = Volume.class;
                    break;
                case EXTERNAL:
                    volType = ExternalVolume.class;
                    break;
            }
            UUID volId = ProtobufHelper.uuid( lp.getVolume().getUuid() );
            String path = lp.getPath();
            long lastMod = lp.getLastModifiedTime();
            FileLocationDTO l = new FileLocationDTO(
                    volType, volId, path, lastMod );
            addLocation( l );
        }
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
        locations = new ArrayList<FileLocationDTO>();
        for ( ImageDescriptorBase img : f.getImages().values() ) {
            ImageDescriptorDTO idto = null;
            if ( img instanceof OriginalImageDescriptor ) {
                idto = new OrigImageDescriptorDTO( img, createdFiles );
            } else {
                idto = new CopyImageDescriptorDTO( (CopyImageDescriptor) img, createdFiles );
            }
            images.put( img.getLocator(), idto );
        }
        for (FileLocation l : f.getLocations() ) {
            FileLocationDTO ldto = new FileLocationDTO( l );
            addLocation( ldto );
        }
    }
    /**
     UUID of the image file
     @serialField 
     */
    @XStreamAsAttribute
    private UUID uuid;
    
    /**
     MD5 hash of the image file
     @serialField 
     */
    @XStreamAsAttribute
    private byte[] hash;
    
    /**
     Size of the image file (in bytes)
     @serialField 
     */
    @XStreamAsAttribute
    private long size;
    
    /**
     Images belonging to the file
     */
    @XStreamAlias( "images" )
    transient private Map<String, ImageDescriptorDTO> images;

    transient private List<FileLocationDTO> locations;

    /**
     * Add a new image to the file. USer by {@link ImageFileXmlConverter}
     * @param locator Location of the image in file
     * @param img The image
     */
    void addImage( String locator, ImageDescriptorDTO img ) {
        images.put( locator, img );
    }

    /**
     Returns UUID of the described image file
     */
    public UUID getUuid() {
        return uuid;
    }


    void setUuid( UUID uuid ) {
        this.uuid = uuid;
    }

    /**
     Returns hash of the described image file
     */
    public byte[] getHash() {
        return hash != null ? Arrays.copyOf( hash, hash.length ) : null;
    }

    /**
     * @param hash the hash to set
     */
    void setHash( byte[] hash ) {
        this.hash = hash;
    }
    /**
     Returns size of the described image file in bytes
     */
    public long getSize() {
        return size;
    }

    /**
     * @param size the size to set
     */
    void setSize( long size ) {
        this.size = size;
    }

    /**
     Returns all images contained in the file
     */
    public Map<String, ImageDescriptorDTO> getImages() {
        return Collections.unmodifiableMap( images );
    }
    
    
    public List<FileLocationDTO> getLocations() {
        return Collections.unmodifiableList( locations );
    }

    void addLocation( FileLocationDTO l ) {
        locations.add( l );
    }

    /**
     Write the obejct to stream
     @param is
     */
    private void writeObject( ObjectOutputStream os ) throws IOException {
        os.defaultWriteObject();
        os.writeInt( images.size() );
        for ( Map.Entry<String,ImageDescriptorDTO> e : images.entrySet() ) {
            os.writeObject( e.getKey() );
            os.writeObject( e.getValue() );
        }
    }
    
    private void readObject( ObjectInputStream is ) 
            throws IOException, ClassNotFoundException {
        is.defaultReadObject();
        int imageCount = is.readInt();
        images = new HashMap<String, ImageDescriptorDTO>( imageCount );
        
        for ( int n = 0 ; n < imageCount ; n++ ) {
            String locator = (String) is.readObject();
            ImageDescriptorDTO dto = (ImageDescriptorDTO) is.readObject();
            images.put( locator, dto );
        }
    }

    public ImageProtos.ImageFile.Builder getBuilder() {
        return getBuilder( new HashSet<UUID>() );
    }

    ImageProtos.ImageFile.Builder getBuilder( Set<UUID> knownFiles ) {
        Types.UUID uuidProto = ProtobufHelper.uuidBuf( uuid );
        ImageProtos.ImageFile.Builder b = ImageProtos.ImageFile.newBuilder();
        b.setSize( size )
                .setUuid( uuidProto )
                .setMd5Hash( ByteString.copyFrom( hash ) );
        for ( ImageDescriptorDTO img : images.values() ) {
            b.addImages( img.getBuilder( knownFiles ) );
        }
        for ( FileLocationDTO l : locations ) {
            ImageProtos.FileLocation.Builder flb = l.getBuilder();
            b.addLocations( flb );
        }
        return b;
    }

    public void buildDebugString( StringBuilder b, String prefix ) {
        b.append( prefix ).append( "ImageFile uuid: " ). append( uuid );
        b.append( ", size: " ).append( size ).append( ", hash " );
        for ( byte byt: hash ) {
            b.append( String.format( "%02x", byt ) );
        }
        b.append( "\n" );
        for ( Map.Entry<String, ImageDescriptorDTO> e: images.entrySet() ) {
            b.append( prefix + "  image " + e.getKey() + ": " );
            e.getValue().buildDebugString( b, prefix + "    " );
            b.append( "\n" );
        }
        for ( FileLocationDTO l : locations ) {
            l.buildDebugString( b, prefix );
            b.append( "\n" );
        }
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        buildDebugString( b, "" );
        return b.toString();
    }

   public static class ProtobufConv implements ProtobufConverter<ImageFileDTO> {

        public Message createMessage( ImageFileDTO obj ) {
            return obj.getBuilder().build();
        }

        public ImageFileDTO createObject( Message msg ) {
            return new ImageFileDTO( (ImageProtos.ImageFile) msg );
        }

    }    
}
