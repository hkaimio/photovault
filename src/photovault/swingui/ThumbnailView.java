// ThumbnailView.java

package photovault.swingui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.geom.AffineTransform;
import imginfo.*;


/**
   ThumbnailView is a very simple component for displaying Thumbnails.
*/

public class ThumbnailView extends JPanel {

    public ThumbnailView() {
	super();
    }

    public void paint( Graphics g ) {
	super.paint( g );

	if ( thumbnail != null ) {
	    // Find the position for the thumbnail
	    BufferedImage img = thumbnail.getImage();
	    Dimension compSize = getSize();
	    int x = ((int)(compSize.getWidth() - img.getWidth()))/(int)2;
	    int y = ((int)(compSize.getHeight() - img.getHeight()))/(int)2;
		
	    Graphics2D g2 = (Graphics2D) g;
	    g2.drawImage( img, new AffineTransform( 1f, 0f, 0f, 1f, x, y ), null );
	}
    }

    public Dimension getPreferredSize() {
	return new Dimension( 150, 150 );
    }

    /**
       Set the photo that is displayed as a thumbnail
    */
    public void setPhoto( PhotoInfo photo ) {
	this.photo = photo;
	thumbnail = photo.getThumbnail();
	revalidate();
    }

    /**
       Returns the currently displayed photo.
    */
    public PhotoInfo getPhoto() {
	return photo;
    }

    PhotoInfo photo = null;
    Thumbnail thumbnail = null;

    public static void main( String args[] ) {
	File f = new File("c:\\java\\photovault\\testfiles\\test1.jpg" );
	try {
	    final PhotoInfo photo = PhotoInfo.addToDB( f );
	    JFrame frame = new JFrame( "ThumbnailView test" );
	    ThumbnailView view = new ThumbnailView();
	    frame.getContentPane().add( view, BorderLayout.CENTER );
	    frame.addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {
			photo.delete();
			System.exit(0);
		    }
		} );
	    
	    view.setPhoto( photo );
	    frame.pack();
	    frame.setVisible( true );
	} catch ( Exception e ) {
	    return;
	}
    }


}
		     
	    
