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

import java.awt.geom.Rectangle2D;
import java.io.BufferedWriter;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.EnumSet;
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
import org.photovault.folder.PhotoFolder;
import org.photovault.image.ColorCurve;
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
    public PhotoInfoChangeDesc( ) {
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
     Set the value of a field
     @param field Field to set
     @param value New value for field
     */
    private void _setField( PhotoInfoFields field, Object value ) {
        changedFields.put(field, value);
    }
    
    /**
     Set the value of a field
     @param field Field to set
     @param value New value for field
     */
    public void setField( PhotoInfoFields field, String value ) {
        _setField(field, value);
    } 
    
    /**
     Set the value of a field
     @param field Field to set
     @param value New value for field
     */
    public void setField( PhotoInfoFields field, Date value ) {
        _setField(field, value);
    } 
    
    /**
     Set the value of a field
     @param field Field to set
     @param value New value for field
     */
    public void setField( PhotoInfoFields field, Integer value ) {
        _setField(field, value);
    } 
    
    /**
     Set the value of a field
     @param field Field to set
     @param value New value for field
     */
    public void setField( PhotoInfoFields field, Double value ) {
        _setField(field, value);
    } 
    
    /**
     Set the value of a field
     @param field Field to set
     @param value New value for field
     */
    public void setField( PhotoInfoFields field, ColorCurve value ) {
        _setField(field, value);
    } 
    
    /**
     Set the value of a field
     @param field Field to set
     @param value New value for field
     */
    public void setField( PhotoInfoFields field, Rectangle2D value ) {
        _setField(field, value);
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
     Add a folder change
     @param uuidStr UUID of the changed folder
     @param operationStr Operation done to the folder (added or deleted)
     */
    public void addFolderChange( String uuidStr, String operationStr ) {
        UUID uuid = UUID.fromString( uuidStr );
        AssocChangeType oper =
                operationStr.equals( "add" ) ? 
                    AssocChangeType.ADDED : AssocChangeType.REMOVED;
        folderChanges.put( uuid, oper );
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
     Verify that the change object is consistent, i.e. its UUID matches change.
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
        for ( PhotoInfoFields f : EnumSet.allOf(PhotoInfoFields.class) ) {
            if ( changedFields.containsKey( f ) ) {
                String fieldName = f.getName();
                Object val = changedFields.get( f );
                if ( val instanceof String ) {
                    w.write( "<" + fieldName + ">" );
                    w.write( cdata( (String) val ) );
                    w.write( "</" + fieldName + ">" );
                } else if ( val instanceof Rectangle2D ) {
                    Rectangle2D rect = (Rectangle2D) val;
                    w.write( String.format( 
                            "<%s xmin=\"%f\" ymin=\"%f\" xmax=\"%f\" ymax=\"%f\"/>",
                            fieldName, rect.getMinX(), rect.getMinY(), 
                            rect.getMaxX(), rect.getMaxY() ));
                } else if ( val instanceof ColorCurve ) {
                    ColorCurve c = (ColorCurve) val;
                    w.write( "<" + fieldName + ">" );
                    w.write( "<curve>" );
                    for ( int n = 0 ; n < c.getPointCount() ; n++ ) {
                        w.write( String.format( "<point x=\"%f\" y=\"%f\"/>",
                                c.getX( n ), c.getY( n ) ) );
                    }
                    w.write( "</curve>" );
                    w.write( "</" + fieldName + ">" );
                } else {
                    w.write( "<" + fieldName + ">" );
                    w.write( val.toString() );
                    w.write( "</" + fieldName + ">" );
                }
            }
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

    /**
     Serialize information about changed fields. This is serializd as pairs of 
     field id - value objects.
     @param os Stream used for writing the data
     @throws java.io.IOException If the data cannot be written.
     */
    @Override
    public void writeFields( ObjectOutputStream os ) throws IOException {
        for ( PhotoInfoFields f : EnumSet.allOf(PhotoInfoFields.class) ) {
            if ( changedFields.containsKey( f ) ) {
                Object val = changedFields.get( f );
                os.writeObject( f );
                os.writeObject( val );
            }
        }        
    }
    
    /**
     Read serialized field data.
     @param os Stream used for reading
     @throws java.io.IOException If os cannot be read.
     @throws java.lang.ClassNotFoundException If field value cannot be 
     instantiated.
     */
    @Override
    public void readFields( ObjectInputStream os ) 
            throws IOException, ClassNotFoundException {
        while( true ) {
            try {
                PhotoInfoFields f = (PhotoInfoFields) os.readObject();
                Object val = os.readObject( );
                changedFields.put( f, val );
            } catch (EOFException ex ) {
                break;
            }
        }        
    }
}
