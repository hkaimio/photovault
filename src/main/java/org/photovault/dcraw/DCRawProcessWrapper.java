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
import java.util.Map;
import org.photovault.common.PhotovaultException;
import org.photovault.common.PhotovaultSettings;

/**
 This class wraps a dcraw instance running as an external process and provides 
 faicilities to set the command line options as well as access to its input and
 output.
 
 */
public class DCRawProcessWrapper {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( DCRawProcessWrapper.class.getName() );
    
    final static String DEFAULT_DCRAW_CMD = "/home/harri/tmp/dcraw";
    
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
        ArrayList cmd = new ArrayList();
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        String dcrawCmd = settings.getProperty( "dcraw.cmd", DEFAULT_DCRAW_CMD );
        cmd.add( dcrawCmd );
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
    
    Map getFileInfo( File rawFile ) throws IOException {
        ProcessBuilder pb = new ProcessBuilder( "/home/harri/tmp/dcraw", "-i", "-v",
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
     R, G, B multipliers to use with WB_MANUAL
     */
    double wbCoeffs[] = null;
}
