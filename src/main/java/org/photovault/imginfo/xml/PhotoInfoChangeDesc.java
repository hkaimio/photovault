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

package org.photovault.imginfo.xml;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.annotations.MapKey;
import org.photovault.change.AssocChangeType;
import org.photovault.change.FieldChangeDesc;
import org.photovault.folder.PhotoFolder;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoFields;

/**
 This class is a persistent description of a change into a {@link PhotoInfo}
 */
@Entity
@DiscriminatorValue("photo")
public class PhotoInfoChangeDesc extends ChangeDesc {
    static private Log log
            = LogFactory.getLog( PhotoInfoChangeDesc.class.getName() );
    
    /**
     Default constructor for persistence layer
     */
    protected PhotoInfoChangeDesc( ) {
        super();
    }
    
    /**
     Construct change descriptor
     @param changedPhoto The photo that is changed
     @param changedFields Values for the changed fields
     @param addedToFolders Folders into which the photo is added
     @param removedFromFolders Folders from which the photo is deleted
     */
    public PhotoInfoChangeDesc( PhotoInfo changedPhoto,
            Map<PhotoInfoFields, Object> changedFields,
            Set<PhotoFolder> addedToFolders,
            Set<PhotoFolder> removedFromFolders ) {
        
        super();
        init( changedPhoto, changedFields, addedToFolders, removedFromFolders );
    }
    
    /**
     Get the fields that are changed. This method is intended only for 
     persistence layer use.

     @return Map from field name to field value as a "union" that can be stored 
     in database
     */
    @org.hibernate.annotations.CollectionOfElements
    @JoinTable( name = "field_changes", joinColumns=@JoinColumn( name="change_uuid"  ) )
    @MapKey( columns=@Column( name="field_name" ))
    protected Map<String, FieldChangeDesc> getFields() {
        Map<String, FieldChangeDesc> ret = new HashMap<String, FieldChangeDesc>();
        for ( Map.Entry<PhotoInfoFields, Object> e : changedFields.entrySet() ) {
            ret.put( e.getKey().getName(), new FieldChangeDesc( e.getValue() ) );
        }
        return ret;
    }
        
    /**
     Set the field values from persistence layer.
     @param m The field values
     */
    protected void setFields( Map<String, FieldChangeDesc> m ) {
        for ( Map.Entry<String, FieldChangeDesc> e : m.entrySet() ) {
            PhotoInfoFields f = PhotoInfoFields.getByName( e.getKey() );
            if ( f != null ) {
                changedFields.put( f, e.getValue().getValue() );
            } else {
                log.error( "Field " + e.getValue() + " not known" );
            }
        }
    }
    
    /**
     Changes made to folders of this photo
     */
    Map<UUID, AssocChangeType> folderChanges = 
            new HashMap<UUID,AssocChangeType>();
    

    /**
     Helper method for transferring folder change information to persistence 
     layer. Due to limitations of Hibernate it seems to be impossible to persist
     maps with elements of user defined types, so we convert then to Strings here.
     
     @return Map with the folder changes converted into strings.
     */
    @org.hibernate.annotations.CollectionOfElements
    @JoinTable( name = "changes_photo_collections", joinColumns=@JoinColumn( name="change_uuid"  ) )
    @MapKey( columns=@Column( name="collection_uuid" ) )
    @Column( name="operation" )
    protected Map<String, String> getFolderChanges() {
        Map<String, String> ret = new HashMap<String, String>();
        for ( Map.Entry<UUID, AssocChangeType> e : folderChanges.entrySet() ) {
            ret.put( e.getKey().toString(), e.getValue().toString() );
        }
        return ret;
    }
    
    /**
     Initialize folder changes from persistence layer.
     
     @param changes Map of folder changes converted to Strings (see 
     getFolderChanges for details)
     */
    protected void setFolderChanges( Map<String, String> changes ) {
        folderChanges.clear();
        for ( Map.Entry<String, String> e : changes.entrySet() ) {
            AssocChangeType val = e.getValue().equals( "ADDED" ) ? 
                AssocChangeType.ADDED : AssocChangeType.REMOVED;
            folderChanges.put( UUID.fromString( e.getKey() ), val );
        }
    }
    
    /**
     Calculate the UUID of this change. UID is calculated as a hash of changed 
     properties.
     @return UUID that should be used for this change.
     */
    protected UUID calcId() {
        StringBuffer b = new StringBuffer();
        b.append( "changed-object PhotoInfo" );
        b.append( "\n" );
        b.append( "predecessors " + getPrevChanges().size() );
        b.append( "\n" );
        for ( ChangeDesc p : getPrevChanges() ) {
            b.append( p.getUuid().toString() + "\n" );
        }
        b.append( "changed-fields" + changedFields.size() + "\n" );
        for ( Map.Entry<PhotoInfoFields, Object> e : changedFields.entrySet() ) {
            String v = e.getValue().toString();
                    
            b.append( "field " + e.getKey().getName() + " " + v.length() + "\n" );
            b.append( v ).append("\n" );
        }
        
        for ( Map.Entry<UUID,AssocChangeType> e : folderChanges.entrySet() ) {
            b.append("folder " ).append( e.getKey().toString() );
            switch( e.getValue() ) {
                case ADDED:
                    b.append( " added" );
                    break;
                case REMOVED:
                    b.append( " removed" );
                    break;
            }
        }
        UUID uuid = null;
        try {
            byte[] bytes = b.toString().getBytes( "utf-8" );
            uuid = UUID.nameUUIDFromBytes( bytes );
        } catch ( UnsupportedEncodingException e ) {

        }
        return uuid;
    }

    /**
     Werify that the change object is consistent, i.e. its UUID matches change.
     @return <code>True</code> if change and its uuid are consistent, <code>false
     </code> otherwise.
     */
    public boolean verify() {
        return this.getUuid().equals( calcId() );
    }
    
    /**
     Fields changed by this change
     */
    Map<PhotoInfoFields, Object> changedFields = new HashMap<PhotoInfoFields, Object>( );

    
    /**
     Build the XML representation of change & populat other fields
     
     @param changedPhoto The photo that is changed
     @param changedFields Values for the changed fields
     @param addedToFolders Folders into which the photo is added
     @param removedFromFolders Folders from which the photo is deleted
     */
    private void init( PhotoInfo changedPhoto,
            Map<PhotoInfoFields, Object> changedFields,
            Set<PhotoFolder> addedToFolders,
            Set<PhotoFolder> removedFromFolders ) {
        
        ChangeDesc prevVersion = changedPhoto.getVersion();
        if ( prevVersion != null ) {
            addPrevChange( prevVersion );
        }
        setTargetUuid( changedPhoto.getUuid() );
        for ( PhotoFolder f : addedToFolders ) {
            folderChanges.put( f.getUUID(), AssocChangeType.ADDED );
        }
        for ( PhotoFolder f : removedFromFolders ) {
            folderChanges.put( f.getUUID(), AssocChangeType.REMOVED );
        }
        
        this.changedFields = changedFields;
        setUuid( calcId() );        
    }

    /**
     Create XML for the predecessors of this change
     @param w BufferedWriter in which the XML is written
     @param indent Indentation (in spaces) to be used
     @throws java.io.IOException If there is an erroe while writing
     */
    private void writeXmlPredecessors( BufferedWriter w, int indent ) throws IOException {
        for ( ChangeDesc c : getPrevChanges() ) {
            w.write( String.format( "<prev-change uuid=\"%s\"/>", c.getUuid().toString() ) );
            w.newLine();
        }
    }
    
    /**
     Write XML for the changed fields
     @param w BufferedWriter in which the XML is written
     @param indent Indentation (in spaces) to be used
     @throws java.io.IOException If there is an erroe while writing
     */
    private void writeXmlFields( BufferedWriter w, int indent ) throws IOException {
        for ( Map.Entry<PhotoInfoFields, Object> e : changedFields.entrySet() ) {
            String v = e.getValue().toString();
            w.write( "<field name=\"" );
            w.write( e.getKey().toString() );
            w.write( "\">");
            if ( e.getValue() instanceof String ) {
                w.write( cdata( (String)e.getValue() ) );
            } else {
                w.write( e.getValue().toString() );
            }
            w.write( "</field>" );
            w.newLine();
        }
        
    }
    
    /**
     Write XML for change folders
     @param w BufferedWriter in which the XML is written
     @param indent Indentation (in spaces) to be used
     @throws java.io.IOException If there is an erroe while writing
     */
    private void writeXmlFolderChanges( BufferedWriter w, int indent ) throws IOException {
        for ( Map.Entry<UUID,AssocChangeType> e : folderChanges.entrySet() ) {
            w.write( String.format("<folder-change uuid=\"%s\" operation=\"%s\"/>",
                    e.getKey().toString(), 
                    e.getValue() == AssocChangeType.ADDED ? "add" : "remove" ) );
            w.newLine();
        }
        
    }

    /**
     Create a XML cdata section from a string. If there is an end marker (]]> in 
     the string, replace it with ] ]>
     @param text the original string
     @return text wrapped inside CDATA markers
     */
    private String cdata( java.lang.String text ) {
        int endmarkerStart = 0;
        if ( text == null ) {
            text = "";
        }
        while ( ( endmarkerStart = text.indexOf( "]]>") ) >= 0 ) {
            text = text.substring( 0, endmarkerStart ) + "] ]>" 
                    + text.substring( endmarkerStart+3 );
        }
        return "<![CDATA[" + text + "]]>";
        
    }

    /**
     Write the change as XML
     @param w BufferedWriter in which the XML is written
     @param indent Indentation (in spaces) to be used
     @throws java.io.IOException If there is an erroe while writing
     */
    public void writeXml( BufferedWriter w, int indent ) throws IOException {
        w.write( String.format( "<change uuid=\"%s\" class=\"%s\">", 
                getUuid().toString(), this.getClass().getName() ) );
        w.newLine();
        writeXmlPredecessors(w, indent);
        writeXmlFields(w, indent);
        writeXmlFolderChanges(w, indent);
        w.write( "</change>");
        w.newLine();
        
    }
}
