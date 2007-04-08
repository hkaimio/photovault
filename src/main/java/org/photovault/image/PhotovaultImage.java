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

package org.photovault.image;

import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.renderable.ParameterBlock;
import java.io.File;
import java.util.Date;
import javax.media.jai.IHSColorSpace;
import javax.media.jai.ImageLayout;
import javax.media.jai.Interpolation;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RenderableOp;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.BandCombineDescriptor;
import javax.media.jai.operator.MultiplyConstDescriptor;
import javax.media.jai.operator.RenderableDescriptor;

/**
 PhotovaultImage is a facade fro Photovault imaging pipeline. It is abstract 
 class, different image providers must derive their own classes from it.
 */
public abstract class PhotovaultImage {
    
    /** Creates a new instance of PhotovaultImage */
    public PhotovaultImage() {
    }

    
    File f = null;

    /**
     * Get aperture (f-stop) used when shooting the image
     * 
     * @return F-stop number reported by dcraw
     */
    public abstract double getAperture();

    /**
     * Get the camera mode used to shoot the image
     * 
     * @return Camera model reported by dcraw
     */
    public abstract String getCamera();

    /**
     Get the original image
     @deprecated Use getRenderedImage instead
     */
    public abstract RenderableOp getCorrectedImage( int minWidth, 
            int minHeight, boolean isLowQualityAllowed );

    
    /**
     Get the original image
     @deprecated Use getRenderedImage instead. TODO: This should be changed to 
     private, but some test cases depend on this.
     */    
    public RenderableOp getCorrectedImage() {
        return getCorrectedImage( Integer.MAX_VALUE, Integer.MAX_VALUE, false );
    }

    RenderableOp previousCorrectedImage = null;
    RenderableOp origRenderable = null;
    RenderableOp cropped = null;
    RenderableOp saturated = null;
    
    /*
    Operation that applies rotation to original image
     */
    RenderableOp rotatedImage = null;
    
    /*
     Operation that crops rotatedImage
     */
    RenderableOp croppedImage = null;
    
    /*
     Operation that transforms croppedImage so that it begins in origo
     */
    RenderableOp xformCroppedImage = null;

    /**
     The multiplyChan operation used to adjust saturation
     */
    RenderableOp saturatedIhsImage = null;
            
    RenderableOp originalImage = null;
    
    protected void buildPipeline(RenderableOp original) {
        originalImage = original;
        cropped = getCropped( original );
//        RenderableOp scaled = getScaled( cropped, maxWidth, maxHeight );
        saturated = getSaturated( cropped );
        
        previousCorrectedImage = original;
    }
        
    /**
     Get the image, adjusted according to current parameters and scaled to a 
     specified resolution.
     @param maxWidth Maximum width of the image in pixels. Image aspect ratio is
     preserved so actual width can be smaller than this.
     @param maxHeight Maximum height of the image in pixels. Image aspect ratio is
     preserved so actual height can be smaller than this.
     @param isLowQualityAllowed Specifies whether image quality can be traded off 
     for speed/memory consumpltion optimizations.
     @return The image as RenderedImage
     */
    
    public RenderedImage getRenderedImage( int maxWidth, int maxHeight, boolean isLowQualityAllowed ) {
        /*
         Calculate the resolution we need for the original image based on crop
         & rotation information
         */
        
        // First, calculate the size of whole inage after rotation
        double rotRad = rot * Math.PI/180.0;
        double rotSin = Math.abs( Math.sin( rotRad ) );
        double rotCos = Math.abs( Math.cos( rotRad ) );
        double rotW = rotCos * getWidth() + rotSin * getHeight();
        double rotH = rotSin * getWidth() + rotCos * getHeight();
        // Size if full image was cropped
        double cropW = rotW * (cropMaxX-cropMinX);
        double cropH = rotH * (cropMaxY-cropMinY);    
        double scaleW = maxWidth / cropW;
        double scaleH = maxHeight / cropH;
        // We are fitting cropped area to max{width x height} so we must use the 
        // scale from dimension needing smallest scale.
        double scale = Math.min( scaleW, scaleH );
        double needW = getWidth() * scale;
        double needH = getHeight() * scale;
        
        RenderableOp original = getCorrectedImage( (int)needW, (int)needH, isLowQualityAllowed );
        if ( previousCorrectedImage != original ) {
            buildPipeline( original );
        }

        int renderingWidth = maxWidth;
        int renderingHeight = (int) (renderingWidth * cropH / cropW);
        if ( renderingHeight > maxHeight ) {
            renderingHeight = maxHeight;
            renderingWidth = (int) (renderingHeight * cropW / cropH);
        }
        RenderingHints hints = new RenderingHints( null );
        hints.put( JAI.KEY_INTERPOLATION, new InterpolationBilinear() );
        
        RenderedImage rendered = saturated.createScaledRendering( renderingWidth, renderingHeight, hints );
        return rendered;
    }
    
    /**
     Get the image, adjusted according to current parameters and scaled with a 
     specified factor.
     @param scale Scaling compared to original image file.
     @param isLowQualityAllowed Specifies whether image quality can be traded off 
     for speed/memory consumpltion optimizations.
     @return The image as RenderedImage
     */
    
    public RenderedImage getRenderedImage( double scale, boolean isLowQualityAllowed ) {
        /*
         Calculate the resolution we need for the original image based on crop
         & rotation information
         */
        
        // First, calculate the size of whole inage after rotation
        double rotRad = rot * Math.PI/180.0;
        double rotSin = Math.abs( Math.sin( rotRad ) );
        double rotCos = Math.abs( Math.cos( rotRad ) );
        double rotW = rotCos * getWidth() + rotSin * getHeight();
        double rotH = rotSin * getWidth() + rotCos * getHeight();
        // Size if full image was cropped
        double cropW = rotW * (cropMaxX-cropMinX);
        double cropH = rotH * (cropMaxY-cropMinY);    

        RenderableOp original = getCorrectedImage( (int)(getWidth()*scale),
                (int)(getHeight()*scale), isLowQualityAllowed );
        if ( previousCorrectedImage != original ) {
            buildPipeline( original );
        }
        RenderingHints hints = new RenderingHints( null );
        hints.put( JAI.KEY_INTERPOLATION, new InterpolationBilinear() );
        RenderedImage rendered = 
                saturated.createScaledRendering( (int) (scale*cropW), 
                (int) (scale*cropH), hints );
        return rendered;
    }
    
    
    /**
     Get width of the original image
     @return width in pixels
     */
    public abstract int getWidth();

    /**
     Get height of the original image
     @return height in pixels
     */
    public abstract int getHeight();
    
    /**
     Amount of rotation that is applied to the image.
     */
    double rot = 0.0;
    
    /**
     Set the rotation applied to original image
     @param r Rotation in degrees. The image is rotated clockwise
     */
    public void setRotation( double r ) {
        rot = r;
        applyRotCrop();
    }
    
    /**
     Get current rotation
     @return Rotation in degrees, clockwise.
     */
    public double getRotation() {
        return rot;
    }
    
    double saturation = 1.0;
    
    public void setSaturation( double s ) {
        saturation = s;
        saturatedIhsImage.setParameter( new double[]{1.0, 1.0, s}, 0 );
    }
    
    public double getSaturation() {
        return saturation;
    }
    
    double cropMinX = 0.0;
    double cropMinY = 0.0;
    double cropMaxX = 1.0;
    double cropMaxY = 1.0;
    
    /**
     Set new crop bounds for the image. Crop bounds are applied after rotation,
     so that top left corner is (0, 0) and bottom right corner (1, 1)
     @param c New crop bounds
     */
    public void setCropBounds( Rectangle2D c ) {
        cropMinX = Math.min( 1.0, Math.max( 0.0, c.getMinX() ) );
        cropMinY = Math.min( 1.0, Math.max( 0.0, c.getMinY() ) );
        cropMaxX = Math.min( 1.0, Math.max( 0.0, c.getMaxX() ) );
        cropMaxY = Math.min( 1.0, Math.max( 0.0, c.getMaxY() ) );

        if ( cropMaxX - cropMinX <= 0.0) {
            double tmp = cropMaxX;
            cropMaxX = cropMinX;
            cropMinX = tmp;
        }
        if ( cropMaxY - cropMinY <= 0.0) {
            double tmp = cropMaxY;
            cropMaxY = cropMinY;
            cropMinY = tmp;
        }        
        applyRotCrop();
    }

    /**
     Get the current crop bounds
     @return Crop bounds
     */
    public Rectangle2D getCropBounds() {
        return new Rectangle2D.Double( cropMinX, cropMinY, 
                cropMaxX-cropMinX, cropMaxY-cropMinY );
    }

    protected RenderableOp getCropped( RenderableOp uncroppedImage ) {
        float origWidth = uncroppedImage.getWidth();
        float origHeight = uncroppedImage.getHeight();
        
        AffineTransform xform = org.photovault.image.ImageXform.getRotateXform(
                rot, origWidth, origHeight );
        
        ParameterBlockJAI rotParams = new ParameterBlockJAI( "affine" );
        rotParams.addSource( uncroppedImage );
        rotParams.setParameter( "transform", xform );
        rotParams.setParameter( "interpolation",
                Interpolation.getInstance( Interpolation.INTERP_NEAREST ) );
        RenderingHints hints = new RenderingHints( null );
        /* 
         In practice, JAI optimizes the pipeline so that this trasnformation is
         concatenated with scaling from MultiresolutionImage to desired resolution.
         This transformation obeys KYE_INTERPOLATION hint so we must set it here,
         otherwise nearest neighborough interpolation will be used regardless of
         the settings on parameter block.
         */
        hints.put( JAI.KEY_INTERPOLATION, new InterpolationBilinear() );
        rotatedImage = JAI.createRenderable( "affine", rotParams, hints );
        
        ParameterBlockJAI cropParams = new ParameterBlockJAI( "crop" );
        cropParams.addSource( rotatedImage );
        float cropWidth = (float) (cropMaxX - cropMinX);
        cropWidth = ( cropWidth > 0.000001 ) ? cropWidth : 0.000001f;
        float cropHeight = (float) (cropMaxY - cropMinY);
        cropHeight = ( cropHeight > 0.000001 ) ? cropHeight : 0.000001f;        
        float cropX = (float)(rotatedImage.getMinX() + cropMinX * rotatedImage.getWidth());
        float cropY = (float)(rotatedImage.getMinY() + cropMinY * rotatedImage.getHeight());
        float cropW = cropWidth * rotatedImage.getWidth();
        float cropH = cropHeight * rotatedImage.getHeight();
        cropParams.setParameter( "x", cropX );
        cropParams.setParameter( "y", cropY );
        cropParams.setParameter( "width", cropW );
        cropParams.setParameter( "height", cropH );
        croppedImage = JAI.createRenderable("crop", cropParams, hints );
        // Translate the image so that it begins in origo
        ParameterBlockJAI pbXlate = new ParameterBlockJAI( "translate" );
        pbXlate.addSource( croppedImage );
        pbXlate.setParameter( "xTrans", (float) (-croppedImage.getMinX() ) );
        pbXlate.setParameter( "yTrans", (float) (-croppedImage.getMinY() ) );
        xformCroppedImage = JAI.createRenderable( "translate", pbXlate );
        return xformCroppedImage;
    }
    
    /**
     Apply the current rotation & cropping settings to JAi image pipeline.
     */
    protected void applyRotCrop( ) {
        if ( originalImage == null ) {
            return;
        }
        
        float origWidth = originalImage.getWidth();
        float origHeight = originalImage.getHeight();
        
        AffineTransform xform = org.photovault.image.ImageXform.getRotateXform(
                rot, origWidth, origHeight );

        ParameterBlockJAI rotParams = (ParameterBlockJAI) rotatedImage.getParameterBlock();
        rotParams.setParameter( "transform", xform );
        rotParams.setParameter( "interpolation",
                Interpolation.getInstance( Interpolation.INTERP_NEAREST ) );
        rotatedImage.setParameterBlock( rotParams ); 
        
        ParameterBlockJAI cropParams = (ParameterBlockJAI) croppedImage.getParameterBlock();
        float cropWidth = (float) (cropMaxX - cropMinX);
        cropWidth = ( cropWidth > 0.000001 ) ? cropWidth : 0.000001f;
        float cropHeight = (float) (cropMaxY - cropMinY);
        cropHeight = ( cropHeight > 0.000001 ) ? cropHeight : 0.000001f;        
        float cropX = (float)(rotatedImage.getMinX() + cropMinX * rotatedImage.getWidth());
        float cropY = (float)(rotatedImage.getMinY() + cropMinY * rotatedImage.getHeight());
        float cropW = cropWidth * rotatedImage.getWidth();
        float cropH = cropHeight * rotatedImage.getHeight();
        cropParams.setParameter( "x", cropX );
        cropParams.setParameter( "y", cropY );
        cropParams.setParameter( "width", cropW );
        cropParams.setParameter( "height", cropH );
        croppedImage.setParameterBlock( cropParams );
        
        // Translate the image so that it begins in origo
        ParameterBlockJAI pbXlate = (ParameterBlockJAI) xformCroppedImage.getParameterBlock();
        pbXlate.addSource( croppedImage );
        pbXlate.setParameter( "xTrans", (float) (-croppedImage.getMinX() ) );
        pbXlate.setParameter( "yTrans", (float) (-croppedImage.getMinY() ) );
        xformCroppedImage.setParameterBlock( pbXlate );
    }
    
    protected PlanarImage getScaled( RenderableOp unscaledImage, int maxWidth, int maxHeight ) {
        AffineTransform thumbScale = org.photovault.image.ImageXform.getFittingXform( maxWidth, maxHeight,
                0, unscaledImage.getWidth(), unscaledImage.getHeight() );
        ParameterBlockJAI scaleParams = new ParameterBlockJAI( "affine" );
        scaleParams.addSource( unscaledImage );
        scaleParams.setParameter( "transform", thumbScale );
        scaleParams.setParameter( "interpolation",
                Interpolation.getInstance( Interpolation.INTERP_NEAREST ) );
        
        RenderedOp scaledImage = JAI.create( "affine", scaleParams );
        return scaledImage;        
    }
    
    protected PlanarImage getScaled( PlanarImage unscaledImage, double scale ) {
        AffineTransform thumbScale = org.photovault.image.ImageXform.getScaleXform( scale,
                0, unscaledImage.getWidth(), unscaledImage.getHeight() );
        ParameterBlockJAI scaleParams = new ParameterBlockJAI( "affine" );
        scaleParams.addSource( unscaledImage );
        scaleParams.setParameter( "transform", thumbScale );
        scaleParams.setParameter( "interpolation",
                Interpolation.getInstance( Interpolation.INTERP_BILINEAR ) );
        
        RenderedOp scaledImage = JAI.create( "affine", scaleParams );
        return scaledImage;        
    }    
    
    protected RenderableOp getSaturated( RenderableOp src ) {
        IHSColorSpace ihs = IHSColorSpace.getInstance();
        ColorModel ihsColorModel =
                new ComponentColorModel(ihs,
                new int[]{8,8,8},
                false,false,
                Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE );
        // Create a ParameterBlock for the conversion.
        ParameterBlock pb = new ParameterBlock();
        pb.addSource( src );
        pb.add(ihsColorModel);
        // Do the conversion.
        RenderableOp ihsImage  = JAI.createRenderable("colorconvert", pb );
        saturatedIhsImage =
                MultiplyConstDescriptor.createRenderable( ihsImage, new double[] {1.0, 1.0, saturation}, null );
        pb = new ParameterBlock();
        pb.addSource(saturatedIhsImage);
        ColorSpace sRGB = ColorSpace.getInstance( ColorSpace.CS_sRGB );
        ColorModel srgbColorModel =
                new ComponentColorModel(sRGB,
                new int[]{8,8,8},
                false,false,
                Transparency.OPAQUE,
                DataBuffer.TYPE_BYTE );
        pb.add(srgbColorModel); // RGB color model!        
        RenderableOp saturatedImage = JAI.createRenderable("colorconvert", pb );
        
        return saturatedImage;
    }
    
    /**
     * Get the film speed setting used when shooting the image
     * 
     * @return Film speed (in ISO) as reported by dcraw
     */
    public abstract int getFilmSpeed();

    /**
     * Get the focal length from image file meta data.
     * 
     * @return Focal length used when taking the picture (in millimetres)
     */
    public abstract double getFocalLength();

    public abstract RenderedImage getImage();

    
    public File getImageFile() {
        return f;
    }

    /**
     * Get the shutter speed used when shooting the image
     * 
     * @return Exposure time (in seconds) as reported by dcraw
     */
    public abstract double getShutterSpeed();

    /**
     * Get the shooting time of the image
     * 
     * @return Shooting time as reported by dcraw or <CODE>null</CODE> if
     * not available
     */
    public abstract Date getTimestamp();


}