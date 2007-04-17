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
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.UUID;
import java.util.Vector;
import org.photovault.common.PhotovaultSettings;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.folder.PhotoFolder;
import org.photovault.imginfo.ImageInstance;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoQuery;

/**
 This class exports metadata from the whole database or part of it into a XML
 file.
 */
public class XmlExporter {
    
    /** Creates a new instance of XmlExporter */
    public XmlExporter( BufferedWriter writer ) {
        this.writer = writer;
    }
    
    /**
     Indentation currently used
     */
    int indent = 0;
    
    /**
     Wtire that is used for writing the XML data.
     */
    BufferedWriter writer = null;
    
    /**
     Get the indentation that should be used
     @return String consisting of {@link indent} spaces
     */
    private String getIndent() {
        String spaces = "                                                                             ";
        return spaces.substring( 0, indent );
    }
    
    /**
     Write selected subset of folder hierarchy to file
     @param folders Set of folders to write.
     @throws IOException if error occurs during writing
     */
    public void writeFolders( Set folders ) throws IOException {
        Set remainingFolders = new HashSet( folders );
        PhotoFolder rootFolder = PhotoFolder.getRoot();
        writer.write( getIndent() + "<folders root-uuid=\"" + 
                rootFolder.getUUID() + "\">" );
        writer.newLine();
        indent += 2;
        writeFolder( rootFolder, remainingFolders, true );
        indent -= 2;
        writer.write( getIndent() + "</folders>" );
        writer.newLine();
    }

    /**
     Write the database as XML
     @throws IOException if error occurs during writing
     */
    public void write( ) throws IOException {
        writer.write( "<?xml version='1.0' ?>\n" );
        writer.write( "<!--\n  This data was exported from Photovault database\n-->");
        writer.newLine();
        writer.write( "<photovault-data version=\"0.5.0\">" );
        writer.newLine();
        indent += 2;
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        settings.getCurrentDatabase().getDbName();
        writer.write( getIndent() + "<originator export-time=\"" + new Date().getTime() + "\"/>" );
        writer.newLine();
        writeFolders( new HashSet() );
        
        PhotoQuery query = new PhotoQuery();
        Vector photos = new Vector();
        for ( int n = 0; n < query.getPhotoCount(); n++ ) {
            photos.add( query.getPhoto( n ) );
        }
        writePhotos( photos );
        indent -= 2;
        writer.write( getIndent() + "</photovault-data>" );
        writer.newLine();
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
     Writes XML element describing a single folder and its subfolders
     @param folder Folder to write
     @param remainingFolders Set of remaining folders, removes the written 
     folders from the set.
     @param writeParentId Write parent id for the topmost folder. Parent ID
     is NOT written for other folders (it should be obvious from the XML
     structure.
     @throws IOException if error occurs during writing
     */
    private void writeFolder(PhotoFolder folder, Set remainingFolders, boolean writeParentId) throws IOException {
        UUID folderUUID = folder.getUUID();
        writer.write( getIndent() + "<folder id=\"" + folderUUID + "\"" );
        if ( writeParentId ) {
            PhotoFolder parent = folder.getParentFolder();
            if ( parent != null ) {
                writer.write( " parent-id=\"" + parent.getUUID() + "\"" );
            }
        }
        Date createTime = folder.getCreationDate();
        if ( createTime != null ) {
            writer.write( " created=\"" + createTime.getTime() + "\"" );
        }
        writer.write( ">\n" );
        indent += 2;
        writer.write( getIndent() + "<name>" + cdata( folder.getName() ) + "</name>\n" );
        writer.write( getIndent() + "<description>" + cdata( folder.getDescription() ) + "</description>\n" );
        remainingFolders.remove( folder );
        for ( int n = 0 ; n < folder.getSubfolderCount(); n++ ) {
            writeFolder( folder.getSubfolder( n ), remainingFolders, false );
        }
        indent -= 2;
        writer.write( getIndent() + "</folder>\n" );
    }
    
    private void writePhotos( Collection photos ) throws IOException {
        Iterator iter = photos.iterator();
        writer.write( getIndent() + "<photos>" );
        writer.newLine();
        indent += 2;
        while ( iter.hasNext() ) {
            PhotoInfo p = ( PhotoInfo ) iter.next();
            writePhoto( p );
        }
        indent -= 2;
        writer.write( getIndent() + "</photos>" );
        writer.newLine();
    }
    
    /**
     Writes an XM? element that describes a single photo
     @param p The photo to write
     @throws IOException if error occurs during writing     
     */
    private void writePhoto( PhotoInfo p ) throws IOException {
        writer.write( getIndent() + "<photo id=\"" + p.getUUID() + "\"" );
        writer.write( " modified=\"" + p.getLastModified() + "\">" );
        writer.newLine();
        indent += 2;
        Date shootTime = p.getShootTime();
        if ( shootTime != null ) {
            double timeAccuracy = p.getTimeAccuracy();
            long timeAccuracyMsecs = (long)( 24*3600000*timeAccuracy );
            writer.write( getIndent() + "<shoot-time time=\"" + shootTime.getTime() +
                    "\" accuracy=\"" + timeAccuracyMsecs + "\"/>");
            writer.newLine();
        }
        writer.write( getIndent() + "<shooting-place>" + cdata( p.getShootingPlace() ) + 
                "</shooting-place>" );
        writer.newLine();
        writer.write( getIndent() + "<photographer>" + cdata( p.getPhotographer() ) + "</photographer>" );
        writer.newLine();
        writer.write( getIndent() + "<camera>" + cdata( p.getCamera() ) + "</camera>" );
        writer.newLine();
        writer.write( getIndent() + "<lens>" + cdata( p.getLens() ) + "</lens>" );
        writer.newLine();
        writer.write( getIndent() + "<f-stop>" + p.getFStop() + "</f-stop>" );
        writer.newLine();
        writer.write( getIndent() + "<shutter-speed>" + p.getShutterSpeed() + "</shutter-speed>" );
        writer.newLine();
        writer.write( getIndent() + "<focal-length>" + p.getFocalLength() + "</focal-length>" );
        writer.newLine();
        writer.write( getIndent() + "<film>" + cdata( p.getFilm() ) + "</film>" );
        writer.newLine();
        writer.write( getIndent() + "<film-speed>" + p.getFilmSpeed() + "</film-speed>" );
        writer.newLine();
        writer.write( getIndent() + "<orig-fname>" + cdata( p.getOrigFname() ) + "</orig-fname>" );
        writer.newLine();
        writer.write( getIndent() + "<description>" + cdata( p.getDescription() ) + "</description>" );
        writer.newLine();
        writer.write( getIndent() + "<tech-notes>" + cdata( p.getTechNotes() ) + "</tech-notes>" );
        writer.newLine();
        writer.write( getIndent() + "<quality>" + p.getQuality() + "</quality>" );
        writer.newLine();
        writer.write( getIndent() + "<crop rot=\"" + p.getPrefRotation() + "\" " );
        Rectangle2D c = p.getCropBounds();
        writer.write( "xmin=\"" + c.getMinX() + "\" " );
        writer.write( "xmax=\"" + c.getMaxX() + "\" " );
        writer.write( "ymin=\"" + c.getMinY() + "\" " );
        writer.write( "ymax=\"" + c.getMaxY() + "\"/>" );
        writer.newLine();
        RawConversionSettings rs = p.getRawSettings();
        if ( rs != null ) {
            writer.write( getIndent() + "<raw-conversion> " );
            writer.newLine();
            indent += 2;
            writer.write( getIndent() + "<whitepoint>" + rs.getWhite() + "</whitepoint>" );
            writer.newLine();
            writer.write( getIndent() + "<blackpoint>" + rs.getBlack() + "</blackpoint>" );
            writer.newLine();
            writer.write( getIndent() + "<ev-corr>" + rs.getEvCorr() + "</ev-corr>" );
            writer.newLine();
            writer.write( getIndent() + "<hlight-corr>" + rs.getHighlightCompression() + "</hlight-corr>" );
            writer.newLine();            
            writer.write( getIndent() + "<wb-type>" + rs.getWhiteBalanceType() + "</wb-type>" );
            writer.newLine();            
            writer.write( getIndent() + "<color-balance red-green-ratio=\"" + rs.getRedGreenRatio() );
            writer.write( "\" blue-green-ratio=\"" + rs.getBlueGreenRatio() + "\"/>" );
            writer.newLine();            
            writer.write( getIndent() + "<daylight-color-balance red-green-ratio=\"" + rs.getDaylightRedGreenRatio() );
            writer.write( "\" blue-green-ratio=\"" + rs.getDaylightBlueGreenRatio() + "\"/>" );
            writer.newLine();     
            indent -= 2;
            writer.write( getIndent() + "</raw-conversion>" );
            writer.newLine();     
        }
        // Write instances
        writer.write( getIndent() + "<instances>" );
        writer.newLine();
        indent += 2;
        for ( int n = 0; n < p.getNumInstances(); n++ ) {
            ImageInstance i = p.getInstance( n );
            writeInstance( i );
        }
        indent -= 2;
        writer.write( getIndent() + "</instances>" );
        writer.newLine();
        Collection folders = p.getFolders();
        if ( folders.size() > 0 ) {
            writer.write( getIndent() + "<folders>" );
            writer.newLine();
            indent += 2;
            Iterator iter = folders.iterator();
            while ( iter.hasNext() ) {
                PhotoFolder f = (PhotoFolder) iter.next();
                writer.write( getIndent() + "<folder-ref id=\"" + f.getUUID() + "\"/>" );
                writer.newLine();
            }
            indent -= 2;
            writer.write( getIndent() + "</folders>" );
            writer.newLine();
        }
        indent -= 2;
        writer.write( getIndent() + "</photo>" );
        writer.newLine();
    }
    
    /**
     Writes XML element that describes a single instance
     @param The ImageInstance to write.
     @throws IOException if error occurs during writing     
     */
    private void writeInstance( ImageInstance i ) throws IOException {
        writer.write( getIndent() + "<instance id=\"" + i.getUUID() + "\" type=\"" );
        switch ( i.getInstanceType() ) {
            case ImageInstance.INSTANCE_TYPE_ORIGINAL:
                writer.write( "original" );
                break;
            case ImageInstance.INSTANCE_TYPE_THUMBNAIL:
                writer.write( "thumbnail" );
                break;
            case ImageInstance.INSTANCE_TYPE_MODIFIED:
                writer.write( "modified" );
                break;
        }
        writer.write( "\">" );
        writer.newLine();
        indent += 2;
        writer.write( getIndent() + "<hash>" + Base64.encodeBytes( i.getHash() ) + "</hash>" );
        writer.newLine();
        writer.write( getIndent() + "<file-size>" + i.getFileSize() + "</file-size>" );
        writer.newLine();
        writer.write( getIndent() + "<width>" + i.getWidth() + "</width>" );
        writer.newLine();
        writer.write( getIndent() + "<height>" + i.getHeight() + "</height>" );
        writer.newLine();
        writer.write( getIndent() + "<crop rot=\"" + i.getRotated() + "\" " );
        Rectangle2D c = i.getCropBounds();
        writer.write( "xmin=\"" + c.getMinX() + "\" " );
        writer.write( "xmax=\"" + c.getMaxX() + "\" " );
        writer.write( "ymin=\"" + c.getMinY() + "\" " );
        writer.write( "ymax=\"" + c.getMaxY() + "\"/>" );
        writer.newLine();
        File f = i.getImageFile();
        if ( f != null ) {
            writer.write( getIndent() + "<location file=\"" + f.getPath() + "\"/>" );
            writer.newLine();
        }
        indent -= 2;
        writer.write( getIndent() + "</instance>" );
        writer.newLine();        
    }
}