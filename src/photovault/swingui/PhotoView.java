// PhotoView.java
package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.geom.AffineTransform;


/**
   PhotoView the actual viewing component for images stored in the database.
*/
public class PhotoView extends JPanel {
    
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( BrowserWindow.class.getName() );

    public PhotoView() {
	super();
	imgRot = 0;
    }
    
    public void paint( Graphics g ) {
        super.paint( g );
	if ( xformImage == null ) {
	    prepareXformImage();
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
	    g2.drawImage( xformImage, new AffineTransform(1f,0f,0f,1f, x, y), null );
	}
    }

    public void setImage( BufferedImage img ) {
	origImage = img;
	xformImage = null;
	repaint();
    }

    public Dimension getPreferredSize() {
	int w = 0;
	int h = 0;
	if ( origImage != null ) {
	    if ( xformImage == null ) {
		prepareXformImage();
	    }
	    w = xformImage.getWidth();
	    h = xformImage.getHeight();
	}
	return new Dimension( w, h );
    }

    public void setScale( float newValue ) {
	imgScale = newValue;
	// Invalidate the existing scaled image
	xformImage = null;
	fitSize = false;
	revalidate();
    }
    
    public float getScale() {
	return imgScale;
    }

    public void setRotation( double newRot ) {
	imgRot = newRot;
	// Invalidate the existing transformed image
	xformImage = null;
	revalidate();
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
       @param maxWidth The maximum width of the scaled image
       @param maxHeight The maximum height of the scaped image
    */
    public void fitToRect( double width, double height ) {
	//	System.err.println( "Fitting to " + width + ", " + height );
	fitSize = true;
	maxWidth = width;
	maxHeight = height;
	// Revalidate the geometry & image size
	xformImage = null;
	revalidate();
    }
    
    private void prepareXformImage() {
	if ( origImage == null ) {
	    return;
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
	AffineTransformOp scaleOp = new AffineTransformOp( at, AffineTransformOp.TYPE_BILINEAR );

	// Create the target image
	xformImage = scaleOp.filter( origImage, null );

	// Return the cursor that was shown before this method
	setCursor( oldCursor );
    }	
	
    public static void main( String args[] ) {
	JFrame frame = new JFrame( "PhotoInfoEditorTest" );
	PhotoView view = new PhotoView();
	frame.getContentPane().add( view, BorderLayout.CENTER );
	frame.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    } );

	File f = new File("c:\\java\\photovault\\testfiles\\test1.jpg" );
	try {
	    BufferedImage bi = ImageIO.read(f);
	    view.setImage( bi );
	    view.setScale( 0.3f );
	    System.out.println( "Succesfully loaded \""+ f.getPath() + "\"" );
	} catch (IOException e ) {
	    System.out.println( "Error loading image \""+ f.getPath() + "\"" );
	}
	
	frame.pack();
	frame.setVisible( true );
    }
	
	
    // The image that is viewed
    BufferedImage origImage = null;
    BufferedImage xformImage = null;
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
	
