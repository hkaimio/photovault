/*
 * Test_ImageIOImage.java
 *
 * Created on March 2, 2007, 9:24 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.photovault.image;

import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import javax.media.jai.RenderableOp;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.photovault.common.PhotovaultException;

/**
 * Test cases for ImageIOImage
 * @author harri
 */
public class Test_ImageIOImage extends TestCase {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( Test_ImageIOImage.class.getName() );    
    /** Creates a new instance of Test_ImageIOImage */
    public Test_ImageIOImage() {
    }
    
    /**
     * Verify that a JPEG file and its metadata can be read correctly
     */
    public void testJPEGImageRead() {
        File f = new File ( "testfiles", "test1.jpg" );
        PhotovaultImageFactory fact = new PhotovaultImageFactory();
        ImageIOImage img = null;
        try {
            img = (ImageIOImage) fact.create(f, false, false);
        } catch (PhotovaultException ex) {
            fail( "Could not load image: " + ex.getMessage() );
        }
        assertNotNull( img );
        assertEquals( img.getCamera(), "Minolta Co., Ltd. DiMAGE 7i" );
        assertEquals( img.getAperture(), 3.5 );
        assertEquals( img.getShutterSpeed(), 0.1 );
        RenderedImage ri = img.getRenderedImage( 1.0, false );
        assertEquals( 2560, ri.getWidth() );
        assertEquals( 1920, ri.getHeight() );
    }
    
    /**
     * Test that an attempt to read nonexistent file works correctly.
     */
    public void testReadNonExistent() {
        File f = null;
        try {
            f = File.createTempFile("pv_testfile", "jpg");
        } catch (IOException ex) {
            fail( "Could not create temp file: " + ex.getMessage() );
        }
        f.delete();
        ImageIOImage img = null;
        PhotovaultImageFactory fact = new PhotovaultImageFactory();
        try {
            img = (ImageIOImage) fact.create(f, false, false);
        } catch (PhotovaultException ex) {
            fail( "Could not load image: " + ex.getMessage() );
        }
        assertNull( img );        
    }
    
    /**
     * Test that reading a file that is not a recognized iamge works correctly
     */
    public void testReadNonImage() {
        File f = new File ( "testfiles", "testconfig.xml" );
        ImageIOImage img = null;
        PhotovaultImageFactory fact = new PhotovaultImageFactory();
        try {
            img = (ImageIOImage) fact.create(f, false, false);
        } catch (PhotovaultException ex) {
            fail( "Could not load image: " + ex.getMessage() );
        }
        assertNull( img );        
    }
    
    public static Test suite() {
        return new TestSuite( Test_ImageIOImage.class );
    }

    public static void main( String[] args ) {
	log.setLevel( org.apache.log4j.Level.DEBUG );
	org.apache.log4j.Logger photoLog = org.apache.log4j.Logger.getLogger( ImageIOImage.class.getName() );
	photoLog.setLevel( org.apache.log4j.Level.DEBUG );
	junit.textui.TestRunner.run( suite() );
    }    
}
