/*
  Copyright (c) 2009 Harri Kaimio

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

package org.photovault.image;

import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import org.photovault.imginfo.ProtobufConverter;
import org.photovault.common.ProtobufSupport;

/**
 * CropOp describes the cropping and rotation done for an image as part of its
 * processing chain.
 * @author Harri Kaimio
 * @since 0.6.0
 */
@XStreamAlias( "crop" )
public class CropOp extends ImageOp
        implements ProtobufSupport<CropOp, ImageOpDto.CropOp, ImageOpDto.CropOp.Builder> {

    /**
     * Constructor
     */
    public CropOp() {
        super();
        addInputPort( "in" );
        addOutputPort( "out" );
    }

    public CropOp( CropOp op ) {
        super( op );
        initPorts();
        minx = op.minx;
        miny = op.miny;
        maxx = op.maxx;
        maxy = op.maxy;
        rot = op.rot;
    }
    /**
     * Constructor
     * @param chain Chain in which the operation belongs
     * @param string Name of the operation
     */
    public CropOp( ImageOpChain chain, String string ) {
        super();
        initPorts();
        setName( string );
        chain.addOperation( this );
    }

    /**
     * Rotation that will be applied to the photo (in degrees, clockwise)
     */
    @XStreamAsAttribute
    private double rot = 0.0;
    /**
     * Minimum X coordinate (after rotation, normalized to 0..1)
     */
    @XStreamAsAttribute
    private double minx = 0.0;
    /**
     * Maximum X coordinate
     */
    @XStreamAsAttribute
    private double maxx = 1.0;
    /**
     * Minimum Y coordinate
     */
    @XStreamAsAttribute
    private double miny = 0.0;
    /**
     * Maximum Y coordinate
     */
    @XStreamAsAttribute
    private double maxy = 1.0;



    @Override
    protected void initPorts() {
        addInputPort( "in" );
        addOutputPort( "out" );
    }

    /**
     * @return the rot
     */
    public double getRot() {
        return rot;
    }

    /**
     * @param rot the rot to set
     */
    public void setRot( double rot ) {
        this.rot = rot;
    }

    /**
     * @return the xmin
     */
    public double getMinX() {
        return minx;
    }

    /**
     * @param xmin the xmin to set
     */
    public void setMinX( double xmin ) {
        this.minx = xmin;
    }

    /**
     * @return the xmax
     */
    public double getMaxX() {
        return maxx;
    }

    /**
     * @param xmax the xmax to set
     */
    public void setMaxX( double xmax ) {
        this.maxx = xmax;
    }

    /**
     * @return the ymin
     */
    public double getMinY() {
        return miny;
    }

    /**
     * @param ymin the ymin to set
     */
    public void setMinY( double ymin ) {
        this.miny = ymin;
    }

    /**
     * @return the ymax
     */
    public double getMaxY() {
        return maxy;
    }

    /**
     * @param ymax the ymax to set
     */
    public void setMaxY( double ymax ) {
        this.maxy = ymax;
    }

    @Override
    public ImageOp createCopy() {
        return new CropOp( this );
    }

    public ImageOpDto.CropOp.Builder getBuilder() {
        return ImageOpDto.CropOp.newBuilder().setMaxx( maxx ).setMaxy( maxy ).
                setMinx( minx).setMiny( miny ).setRot( rot );
    }

    CropOp( ImageOpDto.CropOp d ) {
        setMaxX( d.getMaxx() );
        setMaxY( d.getMaxy() );
        setMinX( d.getMinx() );
        setMinY( d.getMiny() );
        setRot( d.getRot() );
    }

    @Override
    public boolean equals( Object o ) {
        if ( !( o instanceof CropOp ) ) {
            return false;
        }
        CropOp other = (CropOp) o;
        return ( this.maxx == other.maxx 
                && this.minx == other.minx 
                && this.maxy == other.maxy 
                && this.miny == other.miny 
                && this.rot == other.rot );
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash =
                89 * hash +
                (int) (Double.doubleToLongBits( this.rot ) ^
                (Double.doubleToLongBits( this.rot ) >>> 32));
        hash =
                89 * hash +
                (int) (Double.doubleToLongBits( this.minx ) ^
                (Double.doubleToLongBits( this.minx ) >>> 32));
        hash =
                89 * hash +
                (int) (Double.doubleToLongBits( this.maxx ) ^
                (Double.doubleToLongBits( this.maxx ) >>> 32));
        hash =
                89 * hash +
                (int) (Double.doubleToLongBits( this.miny ) ^
                (Double.doubleToLongBits( this.miny ) >>> 32));
        hash =
                89 * hash +
                (int) (Double.doubleToLongBits( this.maxy ) ^
                (Double.doubleToLongBits( this.maxy ) >>> 32));
        return hash;
    }
   public static class ProtobufConv implements ProtobufConverter<CropOp> {

        public Message createMessage( CropOp obj ) {
            return obj.getBuilder().build();
        }

        public CropOp createObject( Message msg ) {
            return new CropOp( (ImageOpDto.CropOp) msg );
        }

    }
}
