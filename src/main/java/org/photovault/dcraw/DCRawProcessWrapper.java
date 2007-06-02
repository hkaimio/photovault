/*
  Copyright (c) 2006 Harri Kaimio
  
  This file is part of Photovault.

  Photovault is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.

  Photovault is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
*/

package org.photovault.dcraw;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import org.photovault.common.PhotovaultException;
import org.photovault.common.PhotovaultSettings;

/**
 This class wraps a dcraw instance running as an external process and provides 
 faicilities to set the command line options as well as access to its input and
 output.
 
 */
public class DCRawProcessWrapper {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( DCRawProcessWrapper.class.getName() );
    
    final static String DCRAW_CMD = getDcrawCmd();
    
    /** Creates a new instance of DCRawProcessWrapper */
    public DCRawProcessWrapper() {
    }
    
    /**
     Converts a raw image file to TIFF image using dcraw.
     @param f The raw image file to read
     @return  Input stream to the converted TIFF image
     */
    InputStream getRawImageAsTiff( File rawFile ) 
            throws PhotovaultException, IOException {
        if ( DCRAW_CMD == null ) {
            throw new PhotovaultException( "Cannot find suitable dcraw executable\n" +
                    "for this architecture" );
        }
        ArrayList cmd = new ArrayList();
        cmd.add( DCRAW_CMD );
        // Output to stdout
        cmd.add( "-c" );
        // As a 16-bit linear tiff
        cmd.add( "-T" );
        cmd.add( "-4" );
        // Half size
        if ( halfSize ) {
            cmd.add( "-h" );
        }        
        cmd.add( "-f" );
        if ( useEmbeddedIccProfile ) {
            cmd.add( "-p" );
            cmd.add( "embed" );
        } else if ( iccProfile != null ) {
            cmd.add( "-p" );
            cmd.add( iccProfile.getAbsolutePath() );
        }
        switch ( wbMode ) {
            case WB_CAMERA:
                cmd.add( "-w" );
                break;
            case WB_AUTO:
                cmd.add( "-a" );
                break;
            case WB_MANUAL:
                cmd.add( "-r" );
                cmd.add( Double.toString( wbCoeffs[0] ) );
                cmd.add( Double.toString( wbCoeffs[1] ) );
                cmd.add( Double.toString( wbCoeffs[2] ) );
                cmd.add( Double.toString( wbCoeffs[1] ) );
                break;
            default:
                log.error( "Unknown WB mode " + wbMode );
        }

        cmd.add( rawFile.getAbsolutePath() );
        ProcessBuilder pb = new ProcessBuilder( cmd );
        Process p = pb.start();
        return p.getInputStream();
    }
    
    /**
     Get information about a raw image file
     @param rawFile The file to be inspected
     @return Map containing the keyword-value pairs reported by dcraw
     @throws PhotovaultException if dcraw was not found
     @throws IOException if there was an error while reading the data.
     */
    Map getFileInfo( File rawFile )
    throws IOException, PhotovaultException {
        if ( DCRAW_CMD == null ) {
            throw new PhotovaultException( "Cannot find suitable dcraw executable\n" +
                    "for this architecture" );
        }
        
        ProcessBuilder pb = new ProcessBuilder( DCRAW_CMD, "-i", "-v",
                rawFile.getAbsolutePath() );
        Process p = pb.start();
        InputStream is = p.getInputStream();
        BufferedReader r = new BufferedReader( new InputStreamReader( is ) );
        Map values = new HashMap();
        String line = null;
        while ( (line = r.readLine()) != null ) {
            int colonPos = line.indexOf( ':' );
            if ( colonPos >= 0 && colonPos < line.length()-1 ) {
                String key = line.substring( 0, colonPos );
                String value = line.substring( colonPos+1 ).trim();
                values.put( key, value );
            }
        }
        r.close();
        return values;
    }
    
    /**
     ICC profile file to use. If <code>null</code> no color profile is applied.
     
     */
    File iccProfile = null;
    
    public void setIccProfile( File f ) {
        iccProfile = f;
    }
    
    public File getIccProfile( ) {
        return iccProfile;
    }
    
    boolean useEmbeddedIccProfile = false;
    
    public void setUseEmbeddedICCProfile( boolean b ) {
        useEmbeddedIccProfile = b;
    }
    
    public boolean getUseEmbeddedICCProfile() {
        return useEmbeddedIccProfile;
    }
    
    public final static String ICC_EMBEDDED = "embed";
    
    public void setWbCoeffs( double coeffs[] ) {
        wbCoeffs = coeffs.clone();
        wbMode = WB_MANUAL;
    }
    
    public static final int WB_CAMERA = 1;
    public static final int WB_AUTO = 2;
    public static final int WB_MANUAL = 3;
    
    int wbMode = WB_CAMERA;
    
    boolean halfSize = true;
    
    /**
     Create only half-sized image to about time consuming Bayer filtering
     @param b if <code>true</code>, scale resolution to half. If <code>false
     </code>, use AHD interpolation to get full resolution image.
     */
    public void setHalfSize( boolean b ) {
        halfSize = b;
    }
    
    /**
     Are we creating half-sized images?
     */
    public boolean ishalfSize() {
        return halfSize;
    }
    
    /**
     Figure out the dcraw executable path in the following order:
     <ul>
     <li>
     If configuration parameter <code>
     dcraw.cmd</code>  has been defined Photovault uses that. 
     </li>
     <li>
     Otherwise it tries
     to find the program under current folder or folder specified in java 
     property pv.basedir. dcraw is searched in subdirectory lib/&lt;os&gt;-&lt;arch
     &gt; where os is the operating system (linux, win32, mac, ...) and 
     arch is the processor architecture (i386, ...). 
     </li>
     <li>
     If dcraw is still not found, the &lt;os&gt;-&lt;arch&gt; directories are 
     searched directly under the current directory of pv.basedir.
     </li>
     </ul>
     @return Absolute path to dcraw or <code>null</code> if no suitable executable 
     is found.
     */
    static private String getDcrawCmd() {
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        String dcrawCmd = System.getProperty( "dcraw.cmd", settings.getProperty( "dcraw.cmd") );
        log.debug( "dcraw.cmd property value: " + dcrawCmd );
        if ( dcrawCmd == null ) {
            // Try to find dcraw in standard location
            String os = System.getProperty( "os.name" ).toLowerCase();
            String arch = System.getProperty( "os.arch" ).toLowerCase();
            log.debug( "os = " + os + ", arch = " + arch );
            String nativedir = "";
            String dcraw = "dcraw";
            if ( os.startsWith( "linux" ) ) {
                nativedir = "linux-" + arch;
            } else if ( os.startsWith( "windows" ) ) {
                nativedir = "win32-" + arch;
                dcraw = "dcraw.exe";
            }
            
            /*
             If system property pv.basedir is set, search under that directory.
             Otherwise, search from current directory.
             */
            String basepath = System.getProperty( "pv.basedir", "." );
            File basedir = new File( basepath );
            File libdir = new File( basedir, "lib" );
            File dcrawSearchPath[] = { libdir, basedir };
            for ( int n = 0; n < dcrawSearchPath.length; n++ ) {
            File ndir = new File( dcrawSearchPath[n], nativedir );
                File f = new File( ndir, dcraw );
                if ( f.exists() ) {
                    dcrawCmd = f.getAbsolutePath();
                    break;
                }
            }
        }
        log.debug( "dcraw command: " + dcrawCmd );
        return dcrawCmd;
    }

    /**
     R, G, B multipliers to use with WB_MANUAL
     */
    double wbCoeffs[] = null;
}
