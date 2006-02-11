// $Id: ImageXform.java,v 1.2 2003/02/14 19:22:25 kaimio Exp $


package org.photovault.image;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;


/**
   This class provides a set of static methods for creating image transforms
*/

public class ImageXform {

    /**
       Returns a transform that scales & rotates a image with given resolution
       @param scale The scale for the transformation
       @param rot Rotation for the transformation (in degrees) - positive means clockwise
       @param width Width of the image in pixels - used to calculate the needed translation after the roation
       @param height Heighjt of the image
    */
    public static AffineTransform getScaleXform( double scale, double rot, int width, int height ) {
	AffineTransform at = new AffineTransform();
 	at.rotate( rot*Math.PI/180.0 );
	
	Rectangle2D bounds = getBounds( at, width, height );

	at.preConcatenate( at.getTranslateInstance( -scale*bounds.getMinX(), -scale*bounds.getMinY() ) );
	at.scale( scale, scale );
	return at;
    }

    public static AffineTransform getFittingXform( int newWidth, int newHeight, double rot, int curWidth, int curHeight ) {
	AffineTransform at = new AffineTransform();
 	at.rotate( rot*Math.PI/180.0 );
	
	Rectangle2D bounds = getBounds( at, curWidth, curHeight );

	double widthScale = ((double)newWidth)/bounds.getWidth();
	double heightScale = ((double)newHeight)/bounds.getHeight();

	double scale = widthScale;
	if ( heightScale < scale ) {
	    scale = heightScale;
	}
	at.preConcatenate( at.getTranslateInstance( -scale*bounds.getMinX(), -scale*bounds.getMinY() ) );
	at.scale( scale, scale );
	return at;
    }
	

    private static Rectangle2D getBounds( AffineTransform xform, int w, int h ) {
	double[] corners = {0.0f,              0.0f,
			   0.0f,              (double) h,
			   (double) w, (double) h,
			   (double) w, 0.0f };
	xform.transform( corners, 0, corners, 0, 4 );
	double minX = corners[0];
	double maxX = corners[0];
	double minY = corners[1];
	double maxY = corners[1];
	for ( int n = 2; n < corners.length; n += 2 ) {
	    if ( corners[n+1] < minY ) {
		minY = corners[n+1];
	    }
	    if ( corners[n+1] > maxY ) {
		maxY = corners[n+1];
	    }
	    if ( corners[n] < minX ) {
		minX = corners[n];
	    }
	    if ( corners[n] > maxX ) {
		maxX = corners[n];
	    }
	}

	Point p = new Point();
	p.setLocation( minX, minY );
	Dimension d = new Dimension();
	d.setSize( maxX-minX, maxY-minY );
	 
	return new Rectangle( p, d );
    }
}
