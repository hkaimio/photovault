// ThumbnailView.java

package photovault.swingui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import java.awt.font.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.geom.*;
import imginfo.*;
import java.text.*;

/**
   ThumbnailView is a very simple component for displaying Thumbnails.
*/

public class ThumbnailView extends JPanel implements PhotoInfoChangeListener {

    public ThumbnailView() {
	super();
    }

    public void paint( Graphics g ) {
	super.paint( g );
	Graphics2D g2 = (Graphics2D) g;
	// Current position in which attributes can be drawn
	Dimension compSize = getSize();
	int ypos = ((int)compSize.getHeight())/2;

	if ( thumbnail != null ) {
	    // Find the position for the thumbnail
	    BufferedImage img = thumbnail.getImage();
	    int x = ((int)(compSize.getWidth() - img.getWidth()))/(int)2;
	    int y = ((int)(compSize.getHeight() - img.getHeight()))/(int)2;
		
	    g2.drawImage( img, new AffineTransform( 1f, 0f, 0f, 1f, x, y ), null );
	    // Increase ypos so that attributes are drawn under the image
	    ypos += ((int)img.getHeight())/2 + 4;
	}

	// Draw the attributes
	if ( photo != null ) {
	    Font attrFont = new Font( "Arial", Font.PLAIN, 10 );
	    FontRenderContext frc = g2.getFontRenderContext();
	    if ( showDate && photo.getShootTime() != null ) {
		DateFormat df = new SimpleDateFormat( "dd.MM.yyyy k:mm" );
		String dateStr = df.format( photo.getShootTime() );
		TextLayout txt = new TextLayout( dateStr, attrFont, frc );
		// Calculate the position for the text
		Rectangle2D bounds = txt.getBounds();
		int xpos = ((int)(compSize.getWidth()-bounds.getWidth()))/2 - (int)bounds.getMinX();
		txt.draw( g2, xpos, (int)(ypos + bounds.getHeight()) );
		ypos += bounds.getHeight() + 4;
	    }
	    if ( showPlace && photo.getShootingPlace() != null ) {
		TextLayout txt = new TextLayout( photo.getShootingPlace(), attrFont, frc );
		// Calculate the position for the text
		Rectangle2D bounds = txt.getBounds();
		int xpos = ((int)(compSize.getWidth()-bounds.getWidth()))/2 - (int)bounds.getMinX();
		txt.draw( g2, xpos, (int)(ypos + bounds.getHeight()) );
		ypos += bounds.getHeight() + 4;
	    }
	}
    }


    boolean showDate = true;
    boolean showPlace = true;
	
    public void setShowShootingTime( boolean b ) {
	showDate = b;
	repaint( 0, 0, 0, getWidth(), getHeight() );
    }

    public boolean getShowShootingTime() {
	return showDate;
    }

    public void setShowShootingPlace( boolean b ) {
	showPlace = b;
	repaint( 0, 0, 0, getWidth(), getHeight() );
    }

    public boolean getShowShootingPlace() {
	return showPlace;
    }
    
    public Dimension getPreferredSize() {
	return new Dimension( 150, 150 );
    }

    /**
       Implementation of @see PhotoInfoChangeListener. Checks if the Thumbnail has changed (e.g. the preferred
       rotation has changed) and updates thumbnail if appropriate
    */
    public void photoInfoChanged( PhotoInfoChangeEvent e ) {
	Thumbnail newThumb = photo.getThumbnail();
	if ( newThumb != thumbnail ) {
	    thumbnail = newThumb;
	    repaint();
	}
    }
    
    /**
       Set the photo that is displayed as a thumbnail
    */
    public void setPhoto( PhotoInfo photo ) {
	if ( this.photo != null ) {
	    this.photo.removeChangeListener( this );
	}
	this.photo = photo;
	photo.addChangeListener( this );
	if ( photo != null ) {
	    thumbnail = photo.getThumbnail();
	} else {
	    thumbnail = null;
	}
	repaint();
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
		     
	    
