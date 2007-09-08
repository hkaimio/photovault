/*
  Copyright (c) 2007 Harri Kaimio
 
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

import java.awt.geom.Rectangle2D;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.image.ChannelMapOperation;
import org.photovault.image.ChannelMapOperationFactory;

/**
 * CopyImageDescriptor describes the properties a a single image that is stored in an
 * image file.
 */

@Entity
@DiscriminatorValue( "modified" )
public class CopyImageDescriptor extends ImageDescriptorBase {
    
    /**
     * Creates a new instance of CopyImageDescriptor
     */
    public CopyImageDescriptor() {
        super();
    }
    
    public CopyImageDescriptor( ImageFile f, String locator, OriginalImageDescriptor orig ) {
        super( f, locator );
        this.original = orig;
        orig.copies.add( this );
    }
    
    private Rectangle2D cropArea = new Rectangle2D.Double( 0.0, 0.0, 1.0, 1.0 );
    private double rotation = 0.0;
    private RawConversionSettings rawSettings = null;
    private ChannelMapOperation colorChannelMapping = null;    
    private OriginalImageDescriptor original;

    @org.hibernate.annotations.Type( type = "org.photovault.persistence.CropRectUserType" )
    @org.hibernate.annotations.Columns(
        columns = {
            @Column( name = "crop_xmin" ),
            @Column( name = "crop_xmax" ),
            @Column( name = "crop_ymin" ),
            @Column( name = "crop_ymax", length = 4 )
        }
    )
    public Rectangle2D getCropArea() {
        return cropArea;
    }

    public void setCropArea(Rectangle2D cropArea) {
        this.cropArea = cropArea;
    }

    @Column( name = "rotation" )
    public double getRotation() {
        return rotation;
    }

    public void setRotation(double rotation) {
        this.rotation = rotation;
    }

    @Transient
    public RawConversionSettings getRawSettings() {
        return rawSettings;
    }

    public void setRawSettings(RawConversionSettings rawSettings) {
        this.rawSettings = rawSettings;
    }

    @Transient
    public ChannelMapOperation getColorChannelMapping() {
        return colorChannelMapping;
    }

    public void setColorChannelMapping(ChannelMapOperation colorChannelMapping) {
        this.colorChannelMapping = colorChannelMapping;
    }
    
    /**
     Get the XML data for color channel mapping that is stored into database field.
     */    
    @Column( name = "channel_map" )
    protected byte[] getColorChannelMappingXmlData() {
        byte[] data = null;
        if ( colorChannelMapping != null ) {
            String xmlStr = colorChannelMapping.getAsXml();
            data = xmlStr.getBytes();
        }
        return data;
    }
    
    /**
     Set the color channel mapping based on XML data read from database field. For
     Hibernate use.
     @param data The data read from database.
     */
    protected void setColorChannelMappingXmlData( byte[] data ) {
        colorChannelMapping = ChannelMapOperationFactory.createFromXmlData( data );
    }
    
    @ManyToOne( cascade = {CascadeType.PERSIST, CascadeType.MERGE} )
    @org.hibernate.annotations.Cascade( {org.hibernate.annotations.CascadeType.SAVE_UPDATE } )
    @JoinColumn( name = "original_id", nullable = true )
    public OriginalImageDescriptor getOriginal() {
        return original;
    }

    public void setOriginal(OriginalImageDescriptor original) {
        this.original = original;
    }
}
