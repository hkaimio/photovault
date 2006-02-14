// PhotoViewer.java
package org.photovault.swingui;

import org.photovault.imginfo.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.geom.AffineTransform;
import java.text.*;
import java.util.*;
import org.photovault.imginfo.ImageInstance;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoChangeEvent;
import org.photovault.imginfo.PhotoInfoChangeListener;

public class PhotoViewer extends JPanel implements PhotoInfoChangeListener, ComponentListener {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoViewer.class.getName() );

    public PhotoViewer() {
	super();
	createUI();
	addComponentListener( this );
    }

    public void createUI() {
	setLayout( new BorderLayout() );
	imageView = new PhotoView();
	scrollPane = new JScrollPane( imageView );
	scrollPane.setPreferredSize( new Dimension( 500, 500 ) );
	add( scrollPane, BorderLayout.CENTER );

	JToolBar toolbar = new JToolBar();

	String[] defaultZooms = {
	    "12%",
	    "25%",
	    "50%",
	    "100%",
	    "Fit"
	};
	JLabel zoomLabel = new JLabel( "Zoom" );
	JComboBox zoomCombo = new JComboBox( defaultZooms );
	zoomCombo.setEditable( true );
	zoomCombo.setSelectedItem( "Fit" );

	final PhotoViewer viewer = this;
	zoomCombo.addActionListener(  new ActionListener() {
		public void actionPerformed( ActionEvent ev ) {
		    JComboBox cb = (JComboBox) ev.getSource();
		    String selected = (String)cb.getSelectedItem();
		    log.debug( "Selected: " + selected );
		    isFit = false;

		    // Parse the pattern
		    DecimalFormat percentFormat = new DecimalFormat( "#####.#%" );
		    if ( selected == "Fit" ) {
			isFit = true;
			log.debug( "Fitting to window" );
			fit();
			float newScale = getScale();
			String strNewScale = "Fit";
			cb.setSelectedItem( strNewScale );
		    } else {
			// Parse the number. First remove all white space to ease the parsing
			StringBuffer b = new StringBuffer( selected );
			int spaceIndex =  b.indexOf( " " );
			while ( spaceIndex >= 0  ) {
			    b.deleteCharAt( spaceIndex );
			    spaceIndex =  b.indexOf( " " );
			}
			selected = b.toString();
			boolean success = false;
			float newScale = 0;
			try {
			    newScale = Float.parseFloat( selected );
			    newScale /= 100.0;
			    success = true;
			} catch (NumberFormatException e ) {
			    // Not a float
			}
			if ( !success ) {
			    try {
				newScale = percentFormat.parse( selected ).floatValue();
				success = true;
			    } catch ( ParseException e ) {
			    }
			}
			if ( success ) {
			    log.debug( "New scale: " + newScale );
			    viewer.setScale( newScale );
			    String strNewScale = percentFormat.format( newScale );
			    cb.setSelectedItem( strNewScale );
			} 
		    }
		}
	    });

	toolbar.add( zoomLabel );
	toolbar.add( zoomCombo );

	add( toolbar, BorderLayout.NORTH );
	    
    }

    public void setScale( float scale ) {
	imageView.setScale(scale);
    }

    public float getScale() {
	return imageView.getScale();
    }

    
    /**
     * Fit the photo into component size
     */
    public void fit() {
	Dimension displaySize = scrollPane.getSize();
	imageView.fitToRect( displaySize.getWidth(), displaySize.getHeight() );
	// 	int origWidth = imageView.getOrigWidth();
	// 	int origHeight = imageView.getOrigHeight();

	// 	if ( origWidth > 0 && origHeight > 0 ) {
	// 	    float widthScale = ((float)displaySize.getWidth())/(float)origWidth;
	// 	    float heightScale = ((float)displaySize.getHeight())/(float)origHeight;

	// 	    float scale = heightScale;
	// 	    if ( widthScale < heightScale ) {
	// 		scale = widthScale;
	// 	    }
	// 	    setScale( scale );
	// 	}
    }

    public void setPhoto( PhotoInfo photo ) {

	if ( this.photo != null ) {
	    this.photo.removeChangeListener( this );
	}
	this.photo = photo;
	if ( photo == null ) {
	    // Free the large BufferedImage objects
	    setImage( null );
	    return;
	}

        log.debug( "PhotoViewer.setPhoto: " + photo.getUid());

	photo.addChangeListener( this );

	// Find the original file
	ImageInstance original = null;
	Vector instances = photo.getInstances();
	for ( int n = 0; n < instances.size(); n++ ) {
	    ImageInstance instance = (ImageInstance) instances.get( n );
	    if ( instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL ) {
		original = instance;
		break;
	    } 
	}
	if ( original == null ) {
	    log.debug( "Error - no original image was found!!!" );
	} else {
	    BufferedImage origImage = null;
	    try {
		final File imageFileName = original.getImageFile();
                log.debug( "reading file " + imageFileName.getAbsolutePath() );
                origImage = ImageIO.read( imageFileName );
                log.debug( "File read" );
		setImage( origImage );
	    } catch ( IOException e ) {
		log.warn( "Error reading image: " + e.getMessage() );
		return;
	    }
	    instanceRotation = original.getRotated();
	    double rot = photo.getPrefRotation() - instanceRotation;
	    imageView.setRotation( rot );
	}
    }

    // Rotation of the currently displayed instance (compared to the original)
    double instanceRotation = 0;
    
    public PhotoInfo  getPhoto() {
	return photo;
    }



    public void componentHidden( ComponentEvent e) {
    }

    public void componentMoved( ComponentEvent e) {
    }

    public void componentResized( ComponentEvent e) {
	if ( isFit ) {
            // We cannot call fit() directly since it depends on the size of the scroll pane which is a subcomponent of this
            // component. 
	    SwingUtilities.invokeLater( new java.lang.Runnable() {
                public void run() { fit(); }
            } );
	}
			 
    }

    public void componentShown( ComponentEvent e) {
    }
    
    
    
    
    PhotoInfo photo = null;
    boolean isFit = true;

    /**
       Implementation of the photoInfoChangeListener interface. Checks if the preferred rotation is changed
       and adjusts displayed image accordingly
    */
    public void photoInfoChanged( PhotoInfoChangeEvent e ) {
	double newRotation = photo.getPrefRotation() - instanceRotation;
	if ( Math.abs( newRotation - imageView.getRotation() ) > 0.1 ) {
	    imageView.setRotation( newRotation );
	}
    }
    
    void setImage( BufferedImage bi ) {
	imageView.setImage( bi );
    }

    
	
    
    public static void main( String args[] ) {
	JFrame frame = new JFrame( "PhotoInfoEditorTest" );
	PhotoViewer viewer = new PhotoViewer();
	frame.getContentPane().add( viewer, BorderLayout.CENTER );
	frame.addWindowListener(new WindowAdapter() {
		public void windowClosing(WindowEvent e) {
		    System.exit(0);
		}
	    } );

	PhotoInfo photo = null;
	if ( args.length == 2 ) {
	    if ( args[0].equals( "-id" ) ) {
		try {
		    int id = Integer.parseInt( args[1] );
		    log.debug( "Getting photo " + id );
		    photo = PhotoInfo.retrievePhotoInfo( id );
		    viewer.setPhoto( photo );
		} catch ( Exception e ) {
		    log.warn( e.getMessage() );
		    e.printStackTrace();
		}
	    }
	}
	
	frame.pack();
	frame.setVisible( true );
    }
	    
    PhotoView imageView = null;
    JScrollPane scrollPane = null;
}    