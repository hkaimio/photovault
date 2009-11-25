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


package org.photovault.dcraw;

import java.awt.RenderingHints;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.media.jai.ColorModelFactory;
import javax.media.jai.ColorSpaceJAI;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.BandCombineDescriptor;
import javax.media.jai.operator.ColorConvertDescriptor;
import org.testng.annotations.Test;

/**
 *
 * @author harri
 */
public class Test_DCRawReaderOp {

    @Test
    public void testRead() throws IOException {
        AHDInterpolateDescriptor.register();
        DCRawReaderDescriptor.register();
        ParameterBlock b = new ParameterBlockJAI( "DCRawReader" );
        b.set( "/home/harri/Kuvat/kamerasta/2009-09-14/IMG_7017.CR2", 0 );
        RenderedOp op = JAI.create( "DCRawReader", b );
        System.out.println( "width " + op.getWidth() );
        System.out.println( "width " + op.getHeight() );
        for ( String prop : op.getPropertyNames() ) {
            System.out.println( prop + ": "  + op.getProperty( prop ) );
        }


        ParameterBlock b2 = new ParameterBlockJAI( "AHDInterpolate" );
        b2.setSource( op, 0 );
        b2.set( 2.35, 0 );
        b2.set( 1.0, 1 );
        b2.set( 1.35, 2 );
        b2.set( 8, 3 );
        RenderedOp interpolated = JAI.create( "AHDInterpolate", b2 );

        double[][] camToRGB = new double[3][4];
        float[] rgb_cam = (float[]) op.getProperty( "dcraw_rgb_cam" );
        StringBuffer camToRGBDebug = new StringBuffer();
        for ( int r = 0 ; r < 3; r++ ) {
            for ( int c = 0 ; c< 3 ; c++ ) {
                camToRGB[r][c] = rgb_cam[r*4+c];
                camToRGBDebug.append( String.format( "%06f ", rgb_cam[r*4+c] ) );
            }
            camToRGBDebug.append( "\n" );
        }
        System.out.println( "rgb_cam: " );
        System.out.println( camToRGBDebug.toString() );

        RenderingHints xyzHints = new RenderingHints( JAI.KEY_COLOR_MODEL_FACTORY, new LinRGBColorModelFactory() );
        RenderedOp xyzImage = BandCombineDescriptor.create( interpolated, camToRGB, null );

        ColorModel cm = new ComponentColorModel(  
                ColorSpaceJAI.getInstance( ColorSpaceJAI.CS_sRGB ), false, false,
                ColorModel.OPAQUE, DataBuffer.TYPE_USHORT );
        RenderedOp srgbImage = ColorConvertDescriptor.create( xyzImage, cm, xyzHints);
        long startTime = System.currentTimeMillis();
        JAI.getDefaultInstance().getTileScheduler().setParallelism( 2 );
        ImageIO.write( srgbImage, "tiff", new File( "/tmp/test.tif" ) );
        long execTime = System.currentTimeMillis() - startTime;
        System.out.println( "Converted in " + execTime + " ms" );
    }

    static private class LinRGBColorModelFactory implements ColorModelFactory {

        public ColorModel createColorModel( SampleModel sampleModel, List sources,
                Map configuration ) {
            return new ComponentColorModel(
                    ColorSpaceJAI.getInstance( ColorSpaceJAI.CS_LINEAR_RGB ), false,
                    false, ColorModel.OPAQUE, sampleModel.getDataType() );

        }

    }

}
