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
    
    public PhotoView() {
	super();
    }
    
    public void paint( Graphics g ) {
	if ( scaledImage == null ) {
	    prepareScaledImage();
	}
	Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	
	if ( scaledImage != null ) {	    
	    g2.drawImage( scaledImage, new AffineTransform(1f,0f,0f,1f,0,0), null );
	}
    }

    public void setImage( BufferedImage img ) {
	origImage = img;
	scaledImage = null;
	revalidate();
    }

    public Dimension getPreferredSize() {
	int w = 0;
	int h = 0;
	if ( origImage != null ) {
	    w = (int)(origImage.getWidth()*imgScale);
	    h = (int)(origImage.getHeight()*imgScale);
	}
	return new Dimension( w, h );
    }

    public void setScale( float newValue ) {
	imgScale = newValue;
	// Invalidate the existing scaled image
	scaledImage = null;
	revalidate();
    }
    
    public float getScale() {
	return imgScale;
    }

    /**
       Returns the width of the currently displayed image
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
    
    private void prepareScaledImage() {
	if ( origImage == null ) {
	    return;
	}
	// Calculate the resolution of the image
	int w = (int)(origImage.getWidth() * imgScale);
	int h = (int)(origImage.getHeight() * imgScale);

	// Create the zoom xform
	AffineTransform at = new AffineTransform();
	at.scale( imgScale, imgScale );
	AffineTransformOp scaleOp = new AffineTransformOp( at, AffineTransformOp.TYPE_BILINEAR );

	// Create the target image
	scaledImage = new BufferedImage( w, h,
					 origImage.getType() );
	scaleOp.filter( origImage, scaledImage );
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
    BufferedImage scaledImage = null;
    // scale of the image
    float imgScale = 1.0f;
}
	
