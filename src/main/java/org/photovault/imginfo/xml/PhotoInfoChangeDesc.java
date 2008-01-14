/*
  Copyright (c) 2007 Harri Kaimio
  
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

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.xml.security.c14n.Canonicalizer;
import org.photovault.folder.PhotoFolder;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoFields;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.bootstrap.DOMImplementationRegistry;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSSerializer;

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
        
        DOMImplementationRegistry registry = null;
        try {
            registry = DOMImplementationRegistry.newInstance();
        } catch ( Exception e ) {
            log.error( "Error instantiating DOM implementation" );
        }

        DOMImplementation domImpl =
                (DOMImplementation) registry.getDOMImplementation( "LS" );


        Document doc = domImpl.createDocument( null, "object-history", null );
        Element root = doc.getDocumentElement();

        Element changeEnvelope = doc.createElement( "change" );
        root.appendChild( changeEnvelope );
        Element changeDesc = doc.createElement( "change-desc" );
        changeEnvelope.appendChild( changeDesc );
        changeDesc.setAttribute( "target", changedPhoto.getUuid().toString() );
        Element chClass = doc.createElement( "change-class" );
        chClass.setAttribute( "class", this.getClass().getName() );
        changeDesc.appendChild( chClass );

        Element predecessors = doc.createElement( "predecessors" );
        changeDesc.appendChild( predecessors );

        for ( ChangeDesc pre : getPrevChanges() ) {
            Element pred = doc.createElement( "change-ref" );
            predecessors.appendChild( pred );
            pred.setAttribute( "uuid", pre.getUuid().toString() );
        }
        
        Element fields = doc.createElement( "fields" );
        changeDesc.appendChild( fields );

        for ( Map.Entry<PhotoInfoFields, Object> e : changedFields.entrySet() ) {
            Element field = doc.createElement( "field" );
            fields.appendChild( field );
            field.setAttribute( "name", e.getKey().getName() );
            field.appendChild( doc.createTextNode( e.getValue().toString() ) );
        }

        if ( addedToFolders.size() > 0 ) {
            Element addedFolders = doc.createElement( "folders-added" );
            changeDesc.appendChild( addedFolders );
            for ( PhotoFolder f : addedToFolders ) {
                Element fe = doc.createElement( "folder" );
                fe.setAttribute( "uuid", f.getUUID().toString() );
                addedFolders.appendChild( fe );
            }
        }
        if ( removedFromFolders.size() > 0 ) {
            Element removedFolders = doc.createElement( "folders-removed" );
            changeDesc.appendChild( removedFolders );
            for ( PhotoFolder f : removedFromFolders ) {
                Element fe = doc.createElement( "folder" );
                fe.setAttribute( "uuid", f.getUUID().toString() );
                removedFolders.appendChild( fe );
            }
        }
        try {
            if ( !org.apache.xml.security.Init.isInitialized() ) {
                org.apache.xml.security.Init.init();
            }
            Canonicalizer c14n = Canonicalizer.getInstance(
                    "http://www.w3.org/TR/2001/REC-xml-c14n-20010315#WithComments" );
            byte[] xmlData = c14n.canonicalizeSubtree( changeDesc );
            log.debug( "Canonicalized description:\n" + new String( xmlData, "utf-8" ) );
            setUuid( UUID.nameUUIDFromBytes( xmlData ) );
            changeEnvelope.setAttribute( "uuid", getUuid().toString() );
        } catch ( Exception e ) {
            log.warn( "Exception calculating uuid: " + e.getMessage() );
        }
        LSSerializer writer = ((DOMImplementationLS) domImpl).createLSSerializer();
        setXml( writer.writeToString( doc ) );
    }

}
