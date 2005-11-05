// PhotoView.java
package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.geom.AffineTransform;
import java.awt.image.renderable.ParameterBlock;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.OperationRegistry;
import javax.media.jai.PlanarImage;
import javax.media.jai.InterpolationBilinear;
import javax.media.jai.widget.ImageCanvas;
import javax.media.jai.widget.ScrollingImagePanel;


/**
   PhotoView the actual viewing component for images stored in the database.
*/
public class JAIPhotoView extends JPanel {
    
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( JAIPhotoView.class.getName() );

    ScrollingImagePanel canvas;
    public JAIPhotoView() {
	super();
	imgRot = 0;
    }
    
    public void paint( Graphics g ) {
        super.paint( g );
	if ( xformImage == null && origImage != null ) {
	    buildXformImage();
	}
	
 	Graphics2D g2 = (Graphics2D) g;
	g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
 	int compWidth = getWidth();
 	int compHeight = getHeight();
	
 	if ( xformImage != null ) {

 	    // Determine the place for the image. If the image is smaller that window, center it
	    int imgWidth = xformImage.getWidth();
	    int imgHeight = xformImage.getHeight();
	    int x = (compWidth-imgWidth)/2;
	    int y = (compHeight-imgHeight)/2;
	    g2.drawRenderedImage( xformImage, new AffineTransform(1f,0f,0f,1f, x, y) );
	}
    }

    public void setImage( RenderedImage img ) {
	boolean isFirst = (origImage == null ) ? true : false;
	origImage = img;
	xformImage = null;
 	repaint();
    }

    public Dimension getPreferredSize() {
	
	int w = 0;
	int h = 0;
	if ( origImage != null ) {
	    if ( xformImage == null ) {
		buildXformImage();
	    }
	    w = xformImage.getWidth();
	    h = xformImage.getHeight();
	}
	return new Dimension( w, h );
    }

    public void setScale( float newValue ) {
	imgScale = newValue;
	xformImage = null;
	fitSize = false;
	revalidate();
        // Revalidate issues a paint request if the component's size changes. 
        // Hovewer if this does not happen we need to explicitly repaint thi component
        repaint();
    }
    
    public float getScale() {
	return imgScale;
    }

    public void setRotation( double newRot ) {
	imgRot = newRot;
	xformImage = null;
	revalidate();
        repaint();
    }

    public double getRotation() {
	return imgRot;
    }

    private double imgRot;
    
    /**
       Returns the width of the currently displayed image. Note that the original image may be rotated for
       displaying it correctly, so this cannot be used to calculate e.g. scaling needed to fit the image
       to certain dimensions.
    */
    public int getOrigWidth() {
	if ( origImage == null ) {
	    return 0;
	}
	return origImage.getWidth();
    }
    
    /**
       Returns the height of the currenty displayed image
    */
    public int getOrigHeight() {
	if ( origImage == null ) {
	    return 0;
	}
	return origImage.getHeight();
    }

    /**
       Set the scaling so that the image fits inside given dimensions
       @param width The maximum width of the scaled image
       @param height The maximum height of the scaped image
    */
    public void fitToRect( double width, double height ) {
	//	System.err.println( "Fitting to " + width + ", " + height );
	fitSize = true;
	maxWidth = width;
	maxHeight = height;
	// Revalidate the geometry & image size
	xformImage = null;
        revalidate();
	repaint();
    }
    
    private void buildXformImage() {
	float scale = 1.0f;
	if ( fitSize ) {
	    log.debug( "fitSize" );
	    float widthScale = ((float)maxWidth)/origImage.getWidth();
	    float heightScale = ((float)maxHeight)/origImage.getHeight();

	    scale = widthScale;
	    if ( heightScale < scale ) {
		scale = heightScale;
	    }
	    log.debug( "scale: " + scale );
	} else {
	    scale = (float) imgScale;
	    log.debug( "scale: " + scale );
	}

	// Set the hourglass cursor
	Cursor oldCursor = getCursor();
	setCursor( new Cursor( Cursor.WAIT_CURSOR ) );
	
	// Create the zoom xform
	AffineTransform at = null;
	if ( fitSize ) {
	    at = photovault.image.ImageXform.getFittingXform( (int)maxWidth, (int)maxHeight, imgRot,
						       origImage.getWidth(), origImage.getHeight() );
	} else {
	    at = photovault.image.ImageXform.getScaleXform( imgScale, imgRot,
						       origImage.getWidth(), origImage.getHeight() );
	}

	
	// Create a ParameterBlock and specify the source and
	// parameters
	ParameterBlockJAI scaleParams = new ParameterBlockJAI( "affine" );
	scaleParams.addSource( origImage );
	scaleParams.setParameter( "transform", at );
	scaleParams.setParameter( "interpolation", new InterpolationBilinear());
	
	// Create the scale operation
	xformImage = JAI.create("affine", scaleParams, null);

	setCursor( oldCursor );
    }
	
    // The image that is viewed
    RenderedImage origImage = null;
    RenderedImage xformImage = null;

    // scale of the image
    float imgScale = 1.0f;

    /**
       If true, fits the image size to a maximum.
    */
    boolean fitSize = false;

    /**
       Maximum width of the image if fitSize is true. Otherwise ignored
    */
    double maxWidth;
    /**
       Maximum height of the image if fitSize is true. Otherwise ignored
    */
    double maxHeight;
    
}
	
