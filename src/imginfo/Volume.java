// Volume.java
package imginfo;

import java.io.*;
import java.text.*;
import java.util.*;

/**
   The class Volume presents a single volume. e.g. storage area for image files
*/

public class Volume {

    private static Volume defaultVolume = null;

    /**
       Returns the current default volume object
    */
    public static Volume getDefaultVolume() {
	if ( defaultVolume == null ) {
	    defaultVolume = new Volume( "c:\\java\\photovault\\testdb" );
	}
	return defaultVolume;
    }

    public Volume( String volName ) {
	volumeBaseDir = new File( volName );
	if ( !volumeBaseDir.exists() ) {
	    volumeBaseDir.mkdir();
	}
    }
	
    /**
       Sets the specified directory as the root for the default volume
       @param volName Directory that will be assigned as the new volume root
    */
    public static void setDefaultVolume( String volName ) {
	defaultVolume = new Volume( volName );
    }
	

	
	
    /** This function provides a filing name for a certain image. The name given will be 
	based on last modified date of the photograph, which is supposed to match the shooting time in most cases.
    */

    public File getFilingFname( File imgFile ) {
	long mt = imgFile.lastModified();
	java.util.Date modTime = new java.util.Date( mt );
	SimpleDateFormat fmt = new SimpleDateFormat( "yyyy" );
	String strYear = fmt.format( modTime );
	fmt.applyPattern( "yyyyMM" );
	String strMonth = fmt.format( modTime );
	fmt.applyPattern( "yyyyMMdd" );
	String strDate = fmt.format( modTime );

	File yearDir = new File( volumeBaseDir, strYear );
	if ( !yearDir.exists() ) {
	    yearDir.mkdir();
	}

	// Create the month directeory if it does not exist yet
	File monthDir = new File ( yearDir, strMonth );
	if ( !monthDir.exists() ) {
	    monthDir.mkdir();
	}

	// Find a free order num for this file
	String monthFiles[] = monthDir.list();
	int orderNum = 1;
	for ( int n = 0; n < monthFiles.length; n++ ) {
	    if ( monthFiles[n].startsWith( strDate ) ) {
		int delimiterLoc = monthFiles[n].indexOf( "." );
		String strFileNum = monthFiles[n].substring( strDate.length()+1, delimiterLoc );
		int i = 0;
		try {
		    i = Integer.parseInt( strFileNum );
		} catch ( NumberFormatException e ) {}
		if ( i >= orderNum ) {
		    orderNum = i+1;
		}
	    }
	}

	String strOrderNum = String.valueOf( orderNum );

	// Find the file extension
	int extStart = imgFile.getName().indexOf( "." );
	String strExtension = imgFile.getName().substring( extStart+1 );
	String fname = strDate + "_"+ "00000".substring( 0, 5-strOrderNum.length())+ strOrderNum + "." + strExtension;
	File archiveFile = new File( monthDir, fname );
	return archiveFile;
    }

    public File getBaseDir() {
	return volumeBaseDir;
    }
    
    private File volumeBaseDir;
}
