// $Id: ImgTestUtils.java,v 1.2 2003/02/15 08:00:02 kaimio Exp $

package org.photovault.test;

import javax.imageio.*;
import javax.imageio.stream.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

/**
   This class contains helper functions that can be used in test cases that need to analyze images.
*/
public class ImgTestUtils {

  static String tempDir = "/tmp";
    public static boolean compareImgToFile( BufferedImage img, File file ) {
	if ( file.exists() ) {
	    System.err.println( "File " + file.getName() + " exists" );
	    BufferedImage fImg = null;
	    try {
		fImg = ImageIO.read( file );
	    System.err.println( "Read image" );
	    } catch ( IOException e ) {
		System.err.println( "Error reading image: " + e.getMessage() );
		return false;
	    }
	    boolean eq = equals( img, fImg );
	    if ( !eq ) {
		File f = new File( tempDir, "error_" + file.getName() );
		Iterator writers = ImageIO.getImageWritersByFormatName("png");
		ImageWriter writer = (ImageWriter)writers.next();
		ImageOutputStream ios = null;
		try {
		    ios = ImageIO.createImageOutputStream(f);
		    writer.setOutput(ios);
		    writer.write( img );
		} catch( IOException e ) {
		    System.err.println( "Cannot write to " + f.getName());
		    return false;
		} finally {
	    if ( ios != null ) {
		try {
		    ios.close();
		} catch (IOException e ) {}
	    }
	}
	    }
		
	    return eq;
	}

	// The image file does not yet exist, so save it
	// First, make sure that the directory is created

	System.err.println( "Image " + file.getName() + " does not exist, creating a new." );
	Iterator writers = ImageIO.getImageWritersByFormatName("png");
	ImageWriter writer = (ImageWriter)writers.next();
	file.getParentFile().mkdirs();
	// Create the image file with a name like candidate_name.png
	file = new File( file.getParentFile(), "candidate_" + file.getName() );
	ImageOutputStream ios = null;
	try {
	    ios = ImageIO.createImageOutputStream(file);
	    writer.setOutput(ios);
	    writer.write(img);
	} catch( IOException e ) {
	    System.err.println( "Could not write file " + file.getName());
	    return false;
	} finally {
	    if ( ios != null ) {
		try {
		    ios.close();
		} catch (IOException e ) {}
	    }
	}
	return false;
    }	
	    
    
    /**
       Returns true if the 2 images are equal, false otherwise
    */
    public static boolean equals( BufferedImage img1, BufferedImage img2 ) {
	if ( img1.getWidth() != img2.getWidth() ) {
	    return false;
	}
	if ( img1.getHeight() != img2.getHeight() ) {
	    return false;
	}
	System.err.println( "Equal size" );
	
	int[] data1 = img1.getRGB( 0, 0, img1.getWidth(), img1.getHeight(), null, 0, img2.getWidth() );
	int[] data2 = img2.getRGB( 0, 0, img2.getWidth(), img2.getHeight(), null, 0, img2.getWidth() );
	
	// Compare the images pixel by pixel
	int diffCount = 0;
	for ( int n = 0; n < data1.length; n++ ) {
	    if ( data1[n] != data2[n] ) {
		diffCount++;
	    }
	}
	return (diffCount < 10) ? true : false;
    }

}
