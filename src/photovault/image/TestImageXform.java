// $Id: TestImageXform.java,v 1.1 2003/02/14 19:22:25 kaimio Exp $


package photovault.image;

import junit.framework.*;
import java.io.*;
import java.awt.image.*;
import javax.imageio.*;
import java.awt.geom.*;

public class TestImageXform extends TestCase {
    
    BufferedImage source = null;
    File testDir = null;
    
    /**
       Sets ut the test environment
    */
    public void setUp() {
	File f = new File( "c:\\java\\photovault\\testfiles\\test1.jpg" );
	try {
	    source = ImageIO.read( f );
	} catch ( IOException e ) {
	    System.err.println( "Error reading image: " + e.getMessage() );
	    return;
	}
	testDir = new File( "c:\\java\\photovault\\tests\\images\\photovault\\image\\ImageXform" );
    }

    public void testScaling() {
	AffineTransform at = ImageXform.getScaleXform( 0.5, 0, source.getWidth(), source.getHeight() );
	AffineTransformOp atop = new AffineTransformOp( at, AffineTransformOp.TYPE_BILINEAR );
	BufferedImage dst = atop.filter( source, null );
	File testFile = new File( testDir, "scaling1.png" );
	assertTrue( "50% scaling not correct",
		    photovault.test.ImgTestUtils.compareImgToFile( dst, testFile ) );
	
	at = ImageXform.getScaleXform( 0.5, 45, source.getWidth(), source.getHeight() );
	atop = new AffineTransformOp( at, AffineTransformOp.TYPE_BILINEAR );
	dst = atop.filter( source, null );
	testFile = new File( testDir, "scaling2.png" );
	assertTrue( "50% scaling & rotation not correct",
		    photovault.test.ImgTestUtils.compareImgToFile( dst, testFile ) );
    }
	
    public void testFitting() {
	AffineTransform at = ImageXform.getFittingXform( 100, 100, 0, source.getWidth(), source.getHeight() );
	AffineTransformOp atop = new AffineTransformOp( at, AffineTransformOp.TYPE_BILINEAR );
	BufferedImage dst = atop.filter( source, null );
	File testFile = new File( testDir, "fitting1.png" );
	assertTrue( "fitting not correct",
		    photovault.test.ImgTestUtils.compareImgToFile( dst, testFile ) );
	
	at = ImageXform.getFittingXform( 100, 100, 45, source.getWidth(), source.getHeight() );
	atop = new AffineTransformOp( at, AffineTransformOp.TYPE_BILINEAR );
	dst = atop.filter( source, null );
	testFile = new File( testDir, "fitting2.png" );
	assertTrue( "fitting & rotation not correct",
		    photovault.test.ImgTestUtils.compareImgToFile( dst, testFile ) );
    }


    public static Test suite() {
	return new TestSuite( TestImageXform.class );
    }

}

    
    
	
