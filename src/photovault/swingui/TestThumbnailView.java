// TestThumbnailView.java

package photovault.swingui;

import junit.framework.*;
import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.imageio.*;
import javax.imageio.stream.*;
import abbot.tester.*;
import imginfo.*;

/**
   Implements unit tests for ThumbnailView class
*/

public class TestThumbnailView extends TestCase {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( TestThumbnailView.class.getName() );

    public TestThumbnailView() {
	testRefImageDir = new File( "c:\\java\\photovault\\tests\\images\\photovault\\swingui\\TestThumbnailView\\" );
	testRefImageDir.mkdirs();
    }
    
    private JFrame frame;
    private JPanel pane;
    private ComponentTester tester;
    PhotoInfo photo = null;
    
    File testRefImageDir = null; 

    protected void setUp() {
        // Create a frame with the test instance name as the title
        frame = new JFrame(getName());        
        pane = (JPanel)frame.getContentPane();
        pane.setLayout(new FlowLayout());
        pane.setBorder(new EmptyBorder(50, 50, 50, 50));
        tester = ComponentTester.getTester(ThumbnailView.class);

	File f = new File("c:\\java\\photovault\\testfiles\\test1.jpg" );
	try {
	    photo = PhotoInfo.addToDB( f );
	} catch( PhotoNotFoundException e ) {
	    fail( "error creating photo" );
	}
	photo.setShootingPlace( "TESSTPLACE" );
    }

    protected void tearDown() {
        frame.dispose();
	photo.delete();
    }

    // handy abbreviation for displaying our test frame
    private void showFrame() {
        // Always do direct component manipulation on the event thread
        tester.invokeAndWait(new Runnable() {
            public void run() { frame.pack(); frame.show(); }
        });
    }

    public void testThumbnailShow() {
	ThumbnailView view = new ThumbnailView();
	view.setPhoto( photo );
	pane.add( view );
	showFrame();
	view.repaint();
	tester.waitForIdle();
// 	Iterator writers = ImageIO.getImageWritersByFormatName("png");
// 	ImageWriter writer = (ImageWriter)writers.next();

	BufferedImage bi = abbot.tester.Robot.capture( view );


	File f = new File( testRefImageDir, "thumbnailShow1.png" );
	assertTrue( photovault.test.ImgTestUtils.compareImgToFile( bi, f ) );
	
	view.setShowShootingTime( false );
	tester.waitForIdle();
	bi = abbot.tester.Robot.capture( view );
	f = new File( testRefImageDir, "thumbnailShow2.png" );
	assertTrue( photovault.test.ImgTestUtils.compareImgToFile( bi, f ) );
	
	view.setShowShootingPlace( false );
	tester.waitForIdle();
	bi = abbot.tester.Robot.capture( view );
	f = new File( testRefImageDir, "thumbnailShow3.png" );
	assertTrue( photovault.test.ImgTestUtils.compareImgToFile( bi, f ) );
    }

        public void testRotationChange() {
	ThumbnailView view = new ThumbnailView();
	view.setPhoto( photo );
	pane.add( view );
	showFrame();
	Iterator writers = ImageIO.getImageWritersByFormatName("png");
	ImageWriter writer = (ImageWriter)writers.next();

	tester.waitForIdle();
	BufferedImage bi = abbot.tester.Robot.capture( view );


	File f = new File( testRefImageDir, "thumbnailRotation1.png" );
	assertTrue( "thumbnailRotationnnot correct", photovault.test.ImgTestUtils.compareImgToFile( bi, f ) );
	
	photo.setPrefRotation( 107 );
	tester.waitForIdle();
	bi = abbot.tester.Robot.capture( view );
	f = new File( testRefImageDir, "thumbnailRotation2.png" );
	assertTrue( "107 deg rotation not correct", photovault.test.ImgTestUtils.compareImgToFile( bi, f ) );
    }

    private boolean compareImgToFile( BufferedImage img, File file ) {
	if ( file.exists() ) {
	    log.debug( "File exists" );
	    BufferedImage fImg = null;
	    try {
		fImg = ImageIO.read( file );
	    log.debug( "Read image" );
	    } catch ( IOException e ) {
		log.warn( "Error reading image: " + e.getMessage() );
		return false;
	    }
	    boolean eq = equals( img, fImg );
	    if ( !eq ) {
		File f = new File( "c:\\temp\\errorFile.png" );
		Iterator writers = ImageIO.getImageWritersByFormatName("png");
		ImageWriter writer = (ImageWriter)writers.next();
		try {
		    ImageOutputStream ios = ImageIO.createImageOutputStream(f);
		    writer.setOutput(ios);
		    writer.write( img );
		} catch( IOException e ) {
		    fail( e.getMessage() );
		}
	    }
		
	    return eq;
	}

	// The image file does not yet exist, so save it
	Iterator writers = ImageIO.getImageWritersByFormatName("png");
	ImageWriter writer = (ImageWriter)writers.next();
	try {
	    ImageOutputStream ios = ImageIO.createImageOutputStream(file);
	    writer.setOutput(ios);
	    writer.write(img);
	} catch( IOException e ) {
	    fail( e.getMessage() );
	}
	return true;
    }	
	    
    
    /**
       Returns true if the 2 images are equal, false otherwise
    */
    boolean equals( BufferedImage img1, BufferedImage img2 ) {
	if ( img1.getWidth() != img2.getWidth() ) {
	    return false;
	}
	if ( img1.getHeight() != img2.getHeight() ) {
	    return false;
	}
	log.debug( "Equal size" );
	
	
	// Compare the images pixel by pixel
	int diffCount = 0;
	for ( int row = 0; row < img1.getHeight(); row++ ) {
	    for ( int col = 0; col < img1.getWidth(); col++ ) {
		if ( img1.getRGB( row, col ) != img2.getRGB( row, col ) ) {
		    //		    System.err.println( "Differ at (" + row + ", " + col + ")" );
		    diffCount++;
		}
	    }
	}
	return (diffCount < 10) ? true : false;
    }
	
    
}
