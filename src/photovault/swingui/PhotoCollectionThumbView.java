// PhotoCollectionThumbView.java

package photovault.swingui;

import imginfo.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.text.*;
import java.util.*;
import photovault.folder.*;
import java.awt.geom.*;
import java.text.*;
import java.awt.font.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;



/**
   This class implements the default thumbnail view for photo
   collections. Some of the planned features include:
   <li> 
   <it> Either vertically or horizontally scrollable view with multiple columns

   <it> Multiple selection for thumbnails 

   <it> Automatic fetching and creation of thumbnails on background if these do not exist 
   </li>

   @author Harri Kaimio

*/

public class PhotoCollectionThumbView
    extends JPanel
    implements ActionListener, PhotoCollectionChangeListener {
    
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoCollectionThumbView.class.getName() );
    
    /**
     * Creates a new <code>PhotoCollectionThumbView</code> instance.
     *
     */
    public PhotoCollectionThumbView() {
        super();
        createUI();
    }

    /**
     * Returns the currently displayed photo collection or <code>null</code> of noe specified
     */
    public PhotoCollection getCollection() {
        return photoCollection;
    }

    

    PhotoCollection photoCollection = null;
    
    /**
     * Set the collection that should be viewed
     * @param v  Value to assign to collection.
     */
    public void setCollection(PhotoCollection  v) {
	    if ( photoCollection != null ) {
            photoCollection.removePhotoCollectionChangeListener( this );
	    }
        
	    photoCollection = v;
	    photoCollection.addPhotoCollectionChangeListener( this );
        repaint();
    }
    

    PhotoCollection collection;

    int columnWidth = 150;
    int rowHeight = 150;
    
    void createUI() {
        
    }

    public void paint( Graphics g ) {
        super.paint( g );
        Graphics2D g2 = (Graphics2D) g;
        Dimension compSize = getSize();
        int numColumns = (int)(compSize.getWidth()/columnWidth);

        int photoCount = 0;
        if ( photoCollection != null ) {
            photoCount = photoCollection.getPhotoCount();
        }

        int col = 0;
        int row = 0;
        for ( int i = 0; i < photoCount; i++ ) {
            PhotoInfo photo = photoCollection.getPhoto( i );
            paintThumbnail( g2, photo, col*columnWidth, row*rowHeight );
            col++;
            if ( col >= numColumns ) {
                row++;
                col = 0;
            }
        }
    }

    boolean showDate = true;
    boolean showPlace = true;
    
    private void paintThumbnail( Graphics2D g2, PhotoInfo photo, int startx, int starty ) {

        // Current position in which attributes can be drawn
        int ypos = starty + rowHeight/2;

        if ( photo != null ) {
            Thumbnail thumbnail = photo.getThumbnail();
        
            if ( thumbnail != null ) {
                // Find the position for the thumbnail
                BufferedImage img = thumbnail.getImage();
                int x = startx + (columnWidth - img.getWidth())/(int)2;
                int y = starty + (rowHeight -  img.getHeight())/(int)2;
                
                g2.drawImage( img, new AffineTransform( 1f, 0f, 0f, 1f, x, y ), null );
                // Increase ypos so that attributes are drawn under the image
                ypos += ((int)img.getHeight())/2 + 4;
            }
            
            // Draw the attributes
            Font attrFont = new Font( "Arial", Font.PLAIN, 10 );
            FontRenderContext frc = g2.getFontRenderContext();
            if ( showDate && photo.getShootTime() != null ) {
                DateFormat df = new SimpleDateFormat( "dd.MM.yyyy k:mm" );
                String dateStr = df.format( photo.getShootTime() );
                TextLayout txt = new TextLayout( dateStr, attrFont, frc );
                // Calculate the position for the text
                Rectangle2D bounds = txt.getBounds();
                int xpos = startx + ((int)(columnWidth - bounds.getWidth()))/2 - (int)bounds.getMinX();
                txt.draw( g2, xpos, (int)(ypos + bounds.getHeight()) );
                ypos += bounds.getHeight() + 4;
            }
            String shootPlace = photo.getShootingPlace();
            if ( showPlace && shootPlace != null && shootPlace.length() > 0  ) {
                TextLayout txt = new TextLayout( photo.getShootingPlace(), attrFont, frc );
                // Calculate the position for the text
                Rectangle2D bounds = txt.getBounds();
                int xpos = startx + ((int)(columnWidth-bounds.getWidth()))/2 - (int)bounds.getMinX();
                txt.draw( g2, xpos, (int)(ypos + bounds.getHeight()) );
                ypos += bounds.getHeight() + 4;
            }
        }
    }

    
    // Implementation of java.awt.event.ActionListener

    /**
     * Describe <code>actionPerformed</code> method here.
     *
     * @param actionEvent an <code>ActionEvent</code> value
     */
    public void actionPerformed(ActionEvent actionEvent) {
        
    }

	public void photoCollectionChanged( PhotoCollectionChangeEvent e ) {
        repaint();
	}
    
    
}