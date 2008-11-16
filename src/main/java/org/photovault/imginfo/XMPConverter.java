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

package org.photovault.imginfo;

import com.adobe.xmp.XMPException;
import com.adobe.xmp.XMPMeta;
import com.adobe.xmp.XMPMetaFactory;
import com.adobe.xmp.XMPSchemaRegistry;
import com.adobe.xmp.options.PropertyOptions;
import java.awt.geom.Rectangle2D;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.image.ChannelMapOperation;
import org.photovault.imginfo.xml.Base64;
import org.photovault.persistence.DAOFactory;
import org.photovault.replication.ObjectHistory;
import org.photovault.replication.ObjectHistoryDTO;

/**
 Utility class for converting metadata between Photovault database format and 
 XMP.
 
 @author Harri Kaimio
 @since 0.6.0
 */
public class XMPConverter {

    private static Log log = LogFactory.getLog( XMPConverter.class.getName() );
    
    // XMP namespaces
    
    /**
     XMP Basic namespace
     */
    static private final String NS_XMP_BASIC = "http://ns.adobe.com/xap/1.0/";

    /**
     Dublin Core namespace
     */
    static private final String NS_DC = "http://purl.org/dc/elements/1.1/";

    /**
     XMP Media Management namespace
     */
    static private final String NS_MM = "http://ns.adobe.com/xap/1.0/mm/";

    /**
     XMP EXIF tag namespace
     */
    static private final String NS_EXIF = "http://ns.adobe.com/exif/1.0/";

    /**
     XMP auxiliary EXIF namespace
     */
    static private final String NS_EXIF_AUX = "http://ns.adobe.com/exif/1.0/aux/";

    /**
     XMP EXIF TIFF specific tag namespace
     */
    static private final String NS_TIFF = "http://ns.adobe.com/tiff/1.0/";
    
    /**
     Photovault XMP extensions namespace
     */
    static private final String NS_PV = "http://ns.photovault.org/xmp/1.0/";
        
    private DAOFactory df;
    
    /**
     Constructor
     @param df DAOFactory to fetch connected objects from database
     */
    public XMPConverter( DAOFactory df ) {
        this.df = df;
    }

    /**
     Create XMP metadata based on given photo and image file
     @param f The image file, used to initialize media management information
     @param p The photo used to initialize descriptive fields
     @return An XMP metadata object initialized based on the given information
     @throws com.adobe.xmp.XMPException If an error occurs while creating the 
     metadata
     */
    public XMPMeta getXMPMetadata( ImageFile f, PhotoInfo p ) throws XMPException {
        XMPMeta meta = XMPMetaFactory.create();
        XMPSchemaRegistry reg = XMPMetaFactory.getSchemaRegistry();

        // Check for Photovault schemas
        if ( reg.getNamespacePrefix( NS_PV ) == null ) {
            try {
                reg.registerNamespace( NS_PV, "pv" );
            } catch ( XMPException e ) {
                log.error( "CMPException: " + e.getMessage() );
            }
        }

        byte[] data = null;
        try {
            URI ifileURI = new URI( "uuid", f.getId().toString(), null );
            meta.setProperty( NS_MM, "InstanceID", ifileURI.toString() );
            meta.setProperty( NS_MM, "Manager", "Photovault 0.5.0dev" );
            meta.setProperty( NS_MM, "ManageTo", ifileURI.toString() );
        } catch ( URISyntaxException ex ) {
            log.error( ex );
        }
        CopyImageDescriptor firstImage = (CopyImageDescriptor) f.getImage( "image#0" );
        OriginalImageDescriptor orig = firstImage.getOriginal();
        String rrNS = reg.getNamespaceURI( "stRef" );
        try {
            URI origURI =
                    new URI( "uuid", orig.getFile().getId().toString(), orig.getLocator() );
            meta.setStructField( NS_MM, "DerivedFrom",
                    rrNS, "InstanceID", origURI.toString() );
        } catch ( URISyntaxException ex ) {
            log.error( ex );
        }
        meta.setStructField( NS_MM, "DerivedFrom", NS_PV, "Rotation",
                Double.toString( firstImage.getRotation() ) );
        Rectangle2D cropArea = firstImage.getCropArea();
        meta.setStructField( NS_MM, "DerivedFrom", NS_PV, "XMin",
                Double.toString( cropArea.getMinX() ) );
        meta.setStructField( NS_MM, "DerivedFrom", NS_PV, "XMax",
                Double.toString( cropArea.getMaxX() ) );
        meta.setStructField( NS_MM, "DerivedFrom", NS_PV, "YMin",
                Double.toString( cropArea.getMinY() ) );
        meta.setStructField( NS_MM, "DerivedFrom", NS_PV, "YMax",
                Double.toString( cropArea.getMaxY() ) );
        ChannelMapOperation cm = firstImage.getColorChannelMapping();
        if ( cm != null ) {
            try {
                ByteArrayOutputStream cms = new ByteArrayOutputStream();
                ObjectOutputStream cmos = new ObjectOutputStream( cms );
                cmos.writeObject( cm );
                String cmBase64 = Base64.encodeBytes( cms.toByteArray(),
                        Base64.GZIP | Base64.DONT_BREAK_LINES );
                meta.setStructField( NS_MM, "DerivedFrom", NS_PV, "ChannelMap",
                        cmBase64 );
            } catch ( IOException e ) {
                log.error( "Error serializing channel map", e );
            }
        }
        RawConversionSettings rs = firstImage.getRawSettings();
        if ( rs != null ) {
            try {
                ByteArrayOutputStream rss = new ByteArrayOutputStream();
                ObjectOutputStream rsos = new ObjectOutputStream( rss );
                rsos.writeObject( rs );
                String rsBase64 = Base64.encodeBytes( rss.toByteArray(),
                        Base64.GZIP | Base64.DONT_BREAK_LINES );
                meta.setStructField( NS_MM, "DerivedFrom", NS_PV, "RawConversion",
                        rsBase64 );
            } catch ( IOException e ) {
                log.error( "Error serializing raw settings", e );
            }
        }

        /*
        Set the image metadata based the photo we are creating this copy.
        There may be other photos associated with the origial image file,
        so we should store information about these in some proprietary part 
        of metadata.
         */
        meta.appendArrayItem( NS_DC, "creator",
                new PropertyOptions().setArrayOrdered( true ),
                p.getPhotographer(), null );
        meta.setProperty( NS_DC, "description", p.getDescription() );

//            photo.getFStop();
//            photo.getFilm();
//            photo.getFilmSpeed();
//            photo.getFocalLength();
//            photo.getQuality();
//            photo.getShootingPlace();
//            photo.getShutterSpeed();
//            photo.getTechNotes();


        Date shootDate = p.getShootTime();
        if ( shootDate != null ) {
            DateFormat dfmt = new SimpleDateFormat(
                    "yyyy-MM-dd'T'HH:mm:ss.SSSZ" );
            String xmpShootDate = dfmt.format( shootDate );
            meta.setProperty( NS_XMP_BASIC, "CreateDate", xmpShootDate );
        }

        // Save technical data
        meta.setProperty( NS_TIFF, "Model", p.getCamera() );
        meta.setProperty( NS_EXIF_AUX, "Lens", p.getLens() );
        // TODO: add other photo attributes as well


        // Save the history of the image
        ObjectHistory<PhotoInfo> h = p.getHistory();
        ObjectHistoryDTO<PhotoInfo> hdto = new ObjectHistoryDTO<PhotoInfo>( h );
        ByteArrayOutputStream histStream = new ByteArrayOutputStream();
        try {
            ObjectOutputStream histoStream = new ObjectOutputStream( histStream );
            histoStream.writeObject( hdto );
            histoStream.flush();
            histStream.flush();
            byte histData[] = histStream.toByteArray();
            String histBase64 = Base64.encodeBytes( histData,
                    Base64.GZIP | Base64.DONT_BREAK_LINES );
            meta.setProperty( NS_PV, "History", histBase64 );
        } catch ( IOException e ) {
            log.warn( "Error serializing history", e );
        }
        return meta;
    }
    
    /**
     Updates a {@link PhotoInfo} object based on XMP metadata. 
     @param metadata The XMP metadata used to initialize or update the PhotoInfo
     @param pe editor for changing the PhotoInfo
     @throws com.adobe.xmp.XMPException If an error occurs while reading metadata
     */
    public void updatePhoto( XMPMeta metadata, PhotoEditor pe ) throws XMPException {
        String creator = (String) metadata.getArrayItem( NS_DC, "creator", 0 ).getValue();
        pe.setPhotographer( creator.trim() );
        String cameraMark = (String) metadata.getPropertyString( NS_TIFF, "Mark" );
        String cameraModel = (String) metadata.getPropertyString( NS_TIFF, "Model" );
        StringBuffer camera = new StringBuffer();
        if ( cameraMark != null ) {
            camera.append( cameraMark.trim() );
        }
        if ( cameraModel != null ) {
            if ( cameraMark != null ) {
                camera.append( " " );
            }
            camera.append( cameraModel );
        }        
        /*
         Use the default description
         */
        
        String description = (String) metadata.getArrayItem( NS_DC, "description", 0 ).getValue();
        if ( description != null ) {
            pe.setDescription( description.trim() );
        }
        Calendar c = metadata.getPropertyCalendar( NS_XMP_BASIC, "CreateDate" );
        Date d = c.getTime();
        pe.setFuzzyShootTime( new FuzzyDate( d, 0.0 ) );
    }
}
