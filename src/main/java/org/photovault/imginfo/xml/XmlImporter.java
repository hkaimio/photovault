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

import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import org.apache.commons.digester.AbstractObjectCreationFactory;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.photovault.common.PhotovaultException;
import org.photovault.dcraw.RawSettingsFactory;
import org.photovault.folder.PhotoFolder;
import org.photovault.imginfo.FuzzyDate;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoNotFoundException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 Imports data from XML file previously saved with {@link XmlExporter} 
 
 
 */
public class XmlImporter {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( XmlImporter.class.getName() );
    
    /**
     Reader from which the data is read.
     */
    private BufferedReader reader;
    
    /**
     * Creates a new instance of XmlImporter
     */
    public XmlImporter( BufferedReader reader ) {
        this.reader = reader;
    }
    
    /**
     Factory used for creating and fetching PhotoFolder in Digester rule. It
     first stris to find a fodler with the uuid specified in XML. If no such 
     folder is found, a new folder is created.
     */
    public class FolderFactory extends AbstractObjectCreationFactory {

        /**
         If true, always set the parent of the folder from either parent-id 
         attribute or if it does not exist from the parent XML node. 
         <p>
         If false, do not set parent for an <i>existing</i> folder unless parent 
         is explicitly set with parent-id attribute.
         */
        private boolean forceReparent;
        
        public FolderFactory( boolean forceReparent ) {
            this.forceReparent = forceReparent;
        }
        
        /**
         This callback returns the folder to Diester. Unlike the name suggests,
         the method tries not to create a folder, but returns an existing folder
         with given uuid if such one exists.
         @param attr attributes of the &lt;folder&gr; element.
         @return The folder with give uuid.
         */
        public Object createObject( Attributes attrs ) {
            String uuidStr = attrs.getValue( "id" );
            UUID uuid = UUID.fromString( uuidStr );
            PhotoFolder folder = PhotoFolder.getFolderByUUID( uuid );
            
            // Find out the parent
            PhotoFolder parent = PhotoFolder.getRoot();
            String parentUuidStr = attrs.getValue( "parent-id" );
            if ( parentUuidStr == null ) {
                // No parent specified in XML file, use topmost object in stack
                Object top = getDigester().peek();
                if ( top instanceof PhotoFolder ) {
                    parent = (PhotoFolder) top;
                }
            } else {
                PhotoFolder parentCandidate =
                        PhotoFolder.getFolderByUUID( UUID.fromString( parentUuidStr ) );
                if ( parentCandidate != null ) {
                    parent = parentCandidate;
                }
                
            }
            
            if ( folder == null ) {
                folder = PhotoFolder.create( uuid, parent );
            } else if ( parentUuidStr != null || forceReparent ) {
                folder.setParentFolder( parent );
            }
            return folder;
        }
    }

    /**
     Factory used for creating and fetching PhotoInfo in Digester rule. It
     first tries to find a photo with the uuid specified in XML. If no such 
     photo is found, a new folder is created.
     */
    public class PhotoFactory extends AbstractObjectCreationFactory {
        public PhotoFactory() {
            
        }
        
        public Object createObject( Attributes attrs ) {
            String uuidStr = attrs.getValue( "id" );
            UUID uuid = UUID.fromString( uuidStr );
            PhotoInfo p = null;
            try {
                p = PhotoInfo.retrievePhotoInfo(uuid);
            } catch (PhotoNotFoundException ex) {
                log.error( "Error while finding PhotoInfo with uuid " + uuid + 
                        ":" + ex.getMessage() );
                ex.printStackTrace();
            }
            
            if ( p == null ) {
                p = PhotoInfo.create( uuid );
            } 
            return p;
        }
    }

    /**
     Factory object for creating a rectangle2D object based on crop element.
     */
    public class RectangleFactory extends AbstractObjectCreationFactory {
        public RectangleFactory() {
            
        }
        
        public Object createObject( Attributes attrs ) {
            String xminStr = attrs.getValue( "xmin" );
            double xmin = Double.parseDouble( xminStr );
            String xmaxStr = attrs.getValue( "xmax" );
            double xmax = Double.parseDouble( xmaxStr );
            String yminStr = attrs.getValue( "ymin" );
            double ymin = Double.parseDouble( yminStr );
            String ymaxStr = attrs.getValue( "ymax" );
            double ymax = Double.parseDouble( ymaxStr );
            Rectangle2D r = new Rectangle2D.Double( xmin, ymin, xmax-xmin, ymax-ymin );
            return r;
        }
    }    

    /**
     Factory for creating fuzzy date in Digester rule
     */
    public class FuzzyDateFactory extends AbstractObjectCreationFactory {
        public FuzzyDateFactory() {
            
        }
        
        public Object createObject( Attributes attrs ) {
            String timeStr = attrs.getValue( "time" );
            long timeMsec = Long.parseLong( timeStr );
            String accStr = attrs.getValue( "accuracy" );
            long accMsec = Long.parseLong( accStr );
            Date time = new Date( timeMsec );
            double accDays = ((double) accMsec ) / (24.0 * 3600000.0 );
            FuzzyDate fd = new FuzzyDate( time, accDays );
            return fd;
        }
    }    

    /**
     Import data from XML file according to current settings in this object.
     */
    public void importData() {
        Digester digester = new Digester();
        digester.push(this); // Push controller servlet onto the stack
        digester.setValidating(false);
        digester.addFactoryCreate( "*/folders/folder", 
                new FolderFactory( true ) );
        digester.addFactoryCreate( "*/folder/folder", 
                new FolderFactory( true ) );
        digester.addCallMethod( "*/folder/name", "setName", 0 );
        digester.addCallMethod( "*/folder/description", "setDescription", 0 );
        
        // PhotoInfo mappings
        digester.addFactoryCreate( "*/photos/photo", new PhotoFactory() );
        digester.addCallMethod( "*/photos/photo/shooting-place", "setShootingPlace", 0 );
        digester.addCallMethod( "*/photos/photo/photographer", "setPhotographer", 0 );
        digester.addCallMethod( "*/photos/photo/camera", "setCamera", 0 );
        digester.addCallMethod( "*/photos/photo/lens", "setLens", 0 );
        digester.addCallMethod( "*/photos/photo/film", "setFilm", 0 );
        digester.addCallMethod( "*/photos/photo/orig-fname", "setOrigFname", 0 );
        digester.addCallMethod( "*/photos/photo/description", "setDesc", 0 );
        digester.addCallMethod( "*/photos/photo/tech-notes", "setTechNotes", 0 );
        digester.addCallMethod( "*/photos/photo/f-stop", "setFStop", 0, new Class[] {Double.class} );
        digester.addCallMethod( "*/photos/photo/shutter-speed", "setShutterSpeed", 0, new Class[] {Double.class} );
        digester.addCallMethod( "*/photos/photo/focal-length", "setFocalLength", 0, new Class[] {Double.class} );
        digester.addCallMethod( "*/photos/photo/quality", "setQuality", 0, new Class[] {Integer.class} );
        digester.addCallMethod( "*/photos/photo/film-speed", "setFilmSpeed", 0, new Class[] {Integer.class} );
        
        digester.addFactoryCreate( "*/photos/photo/shoot-time", new FuzzyDateFactory() );
        digester.addSetNext( "*/photos/photo/shoot-time", "setShootTime" );
        
        // Crop settings
        digester.addCallMethod( "*/photos/photo/crop", "setPrefRotation", 1, new Class[] {Double.class} );
        digester.addCallParam( "*/photos/photo/crop", 0, "rot" );
        digester.addFactoryCreate( "*/photos/photo/crop", new RectangleFactory() );
        digester.addSetNext( "*/photos/photo/crop", "setCropBounds" );
        
        // Raw conversion settings
        digester.addObjectCreate( "*/raw-conversion", RawSettingsFactory.class );
        digester.addCallMethod( "*/raw-conversion/whitepoint", "setWhite", 0, new Class[] {Integer.class} );
        digester.addCallMethod( "*/raw-conversion/blackpoint", "setBlack", 0, new Class[] {Integer.class} );
        digester.addCallMethod( "*/raw-conversion/ev-corr", "setEvCorr", 0, new Class[] {Double.class} );
        digester.addCallMethod( "*/raw-conversion/hlight-corr", "setHlightComp", 0, new Class[] {Double.class} );
        digester.addRule( "*/raw-conversion/color-balance", new Rule() {
            public void begin( String namespace, String name, Attributes attrs ) {
                String rgStr = attrs.getValue( "red-green-ratio" );
                String bgStr = attrs.getValue( "blue-green-ratio" );
                double bg;
                double rg;
                try {
                    bg = Double.parseDouble( bgStr );
                    rg = Double.parseDouble(rgStr);
                } catch (NumberFormatException ex) {
                    digester.createSAXException( ex );
                }
                RawSettingsFactory f = (RawSettingsFactory) digester.peek();
                f.setRedGreenRation( rg );            
                f.setBlueGreenRatio( bg );            
            } 
        });
        digester.addRule( "*/raw-conversion/daylight-color-balance", new Rule() {
            public void begin( String namespace, String name, Attributes attrs ) {
                String rgStr = attrs.getValue( "red-green-ratio" );
                String bgStr = attrs.getValue( "blue-green-ratio" );
                double bg;
                double rg;
                
                try {
                    bg = Double.parseDouble( bgStr );
                    rg = Double.parseDouble(rgStr);
                } catch (NumberFormatException ex) {
                    ex.printStackTrace();
                }
                RawSettingsFactory f = (RawSettingsFactory) digester.peek();
                f.setDaylightMultipliers( new double[] {rg, 1.0, bg} );
            } 
        });
        digester.addRule( "*/raw-conversion", new Rule() {
            public void end( String namespace, String name ) {
                PhotoInfo p = (PhotoInfo)digester.peek(1);
                RawSettingsFactory f = (RawSettingsFactory) digester.peek();
                try {
                    p.setRawSettings( f.create() );
                } catch (PhotovaultException ex) {
                    digester.createSAXException( ex );
                }
            }
        });  
        
        // TODO instance handling
        
        
        // folder handling
        digester.addFactoryCreate( "*/photos/photo/folders/folder-ref", new FolderFactory( false ) );
        digester.addSetTop( "*/photos/photo/folders/folder-ref", "addPhoto" );
        
        try {
            digester.parse( reader );
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }  
}
