// PhotoViewer.java
package photovault.swingui;

import imginfo.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.geom.AffineTransform;
import java.text.*;
import java.util.*;

public class PhotoViewer extends JPanel implements PhotoInfoChangeListener {

    public PhotoViewer() {
	super();
	createUI();
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

	final PhotoViewer viewer = this;
	zoomCombo.addActionListener(  new ActionListener() {
		public void actionPerformed( ActionEvent ev ) {
		    JComboBox cb = (JComboBox) ev.getSource();
		    String selected = (String)cb.getSelectedItem();
		    System.out.println( "Selected: " + selected );

		    // Parse the pattern
		    DecimalFormat percentFormat = new DecimalFormat( "#####.#%" );
		    if ( selected == "Fit" ) {
			System.out.println( "Fitting to window" );
			fit();
			float newScale = getScale();
			String strNewScale = percentFormat.format( newScale );
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
			    System.out.println( "New scale: " + newScale );
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

    public void fit() {
	Dimension displaySize = scrollPane.getSize();
	int origWidth = imageView.getOrigWidth();
	int origHeight = imageView.getOrigHeight();

	if ( origWidth > 0 && origHeight > 0 ) {
	    float widthScale = ((float)displaySize.getWidth())/(float)origWidth;
	    float heightScale = ((float)displaySize.getHeight())/(float)origHeight;

	    float scale = heightScale;
	    if ( widthScale < heightScale ) {
		scale = widthScale;
	    }
	    setScale( scale );
	}
    }

    public void setPhoto( PhotoInfo photo ) {

	if ( this.photo != null ) {
	    this.photo.removeChangeListener( this );
	}
	this.photo = photo;
	photo.addChangeListener( this );

	// Find the original file
	ImageInstance original = null;
	ArrayList instances = photo.getInstances();
	for ( int n = 0; n < instances.size(); n++ ) {
	    ImageInstance instance = (ImageInstance) instances.get( n );
	    if ( instance.getInstanceType() == ImageInstance.INSTANCE_TYPE_ORIGINAL ) {
		original = instance;
		break;
	    } 
	}
	if ( original == null ) {
	    System.err.println( "Error - no original image was found!!!" );
	} else {
	    BufferedImage origImage = null;
	    try {
		origImage = ImageIO.read( original.getImageFile() );
		setImage( origImage );
	    } catch ( IOException e ) {
		System.err.println( "Error reading image: " + e.getMessage() );
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
    
    PhotoInfo photo = null;

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
		    System.err.println( "Getting photo " + id );
		    photo = PhotoInfo.retrievePhotoInfo( id );
		    viewer.setPhoto( photo );
		} catch ( Exception e ) {
		    System.err.println( e.getMessage() );
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
