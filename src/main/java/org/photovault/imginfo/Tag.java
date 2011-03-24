/*
  Copyright (c) 2011 Harri Kaimio

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


package org.photovault.imginfo;

import com.google.protobuf.Message;
import javax.persistence.Embeddable;
import javax.persistence.Transient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.imginfo.dto.ImageProtos;

/**
 * Simple tag that can be added to {@link PhotoInfo} to annotate it. The tag
 * implementation minimal support for categorization, as a tag can contain
 * a type in addition to the actual text string used as tag.
 * @author Harri Kaimio
 * @since 0.6.0
 */
@Embeddable
public class Tag {

    static private Log log = LogFactory.getLog( Tag.class );

    /**
     * Type of this tag
     */
    private String type = null;

    /**
     * Text string used for the tag
     */
    private String name = null;

    /**
     * Type value used to indicate that the tag is intended to be a plain text
     * string.
     */
    static public final String TEXT_TAG = "_pvtag_text";
    /**
     * Type value used to indicate that the tag is intended to describe a person
     */
    static public final String PERSON_TAG = "_pvtag_person";

    protected Tag() {

    }

    public Tag( String name ) {
        type = TEXT_TAG;
        this.name = name;
    }

    public Tag( String type, String name ) {
        this.type = type;
        this.name = name;
    }

    public String getType() {
        return type;
    }

    protected void setType( String type ) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    protected void setName( String name ) {
        this.name = name;
    }

    @Transient
    public ImageProtos.Tag.Builder getBuilder() {
        ImageProtos.Tag.Builder b = ImageProtos.Tag.newBuilder();
        b.setName( name ).setType( type );
        return b;
    }

    @Override
    public boolean equals( Object o ) {
        if ( o == null ) {
            return false;
        }
        if ( !(o instanceof Tag) ) {
            return false;
        }
        Tag other = (Tag) o;

        return (name != null ? name.equals( other.name ) : other.name == null )
                && (type != null ? type.equals( other.type ) : other.type == null );
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 67 * hash + (this.name != null ? this.name.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        if ( type.equals( TEXT_TAG ) || type.equals( PERSON_TAG ) ) {
            return name;
        } else {
            return type + ":" + name;
        }
    }

    public static class ProtobufConv implements ProtobufConverter<Tag> {

        public Message createMessage( Tag obj ) {
            return obj.getBuilder().build();
        }

        public Tag createObject( Message msg ) {
            ImageProtos.Tag tagMsg = (ImageProtos.Tag) msg;
            return new Tag( tagMsg.getType(), tagMsg.getName() );
        }

    }
}
