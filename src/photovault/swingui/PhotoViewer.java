// PhotoViewer.java
package photovault.swingui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.geom.AffineTransform;

public class PhotoViewer extends JPanel {

    public PhotoViewer() {
	super();
	createUI();
    }

    public void createUI() {
	imageView = new PhotoView();
	scrollPane = new JScrollPane( imageView );
	scrollPane.setPreferredSize( new Dimension( 500, 500 ) );
	add( scrollPane, BorderLayout.CENTER );
    }

    void setScale( float scale ) {
	imageView.setScale(scale);
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

	File f = new File("c:\\java\\photovault\\testfiles\\test1.jpg" );
	try {
	    BufferedImage bi = ImageIO.read(f);
	    viewer.setImage( bi );
	    viewer.setScale( 0.3f );
	    System.out.println( "Succesfully loaded \""+ f.getPath() + "\"" );
	} catch (IOException e ) {
	    System.out.println( "Error loading image \""+ f.getPath() + "\"" );
	}
	
	frame.pack();
	frame.setVisible( true );
    }
	    
    PhotoView imageView = null;
    JScrollPane scrollPane = null;
}    
