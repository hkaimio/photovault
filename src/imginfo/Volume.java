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
    private static HashMap volumes = null;
    
    /**
       Returns the current default volume object
    */
    public static Volume getDefaultVolume() {
	if ( defaultVolume == null ) {
	    defaultVolume = new Volume( "defaultVolume", "/home/harri/projects/photovault/testdb" );
	}
	return defaultVolume;
    }

    /**
       Returns the volume with a given name or null if such volume does not exist
       @param volName The name to look for
    */
    public static Volume getVolume( String volName ) {
	Volume vol = null;
	// Initialize the volumes array and create default volume if it has not been done yet
	if ( volumes == null ) {
	    getDefaultVolume();
	}
	    
	vol = (Volume) volumes.get( volName );
	return vol;
    }

    public Volume( String volName, String volBaseDir ) {
	volumeName = volName;
	volumeBaseDir = new File( volBaseDir );
	if ( !volumeBaseDir.exists() ) {
	    volumeBaseDir.mkdir();
	}
	registerVolume();
    }

       
    private void registerVolume() {
	if ( volumes == null ) {
	    volumes = new HashMap();
	}
	volumes.put( volumeName, this );
    }
    
    /**
       Sets the specified directory as the root for the default volume
       @param volName Directory that will be assigned as the new volume root
    */
    public static void setDefaultVolume( String volName ) {
	defaultVolume = new Volume( "defaultVolume", volName );
    }
	

	
	
    /** This function provides a filing name for a certain image. The name given will be 
	based on last modified date of the photograph, which is supposed to match the 
	shooting time in most cases.
    */

    public File getFilingFname( File imgFile ) {

	// Use the "last nodified" date  as the basis for the file name
	long mt = imgFile.lastModified();
	java.util.Date modTime = new java.util.Date( mt );

	// Find the file extension
	int extStart = imgFile.getName().indexOf( "." );
	String strExtension = imgFile.getName().substring( extStart+1 );
	return getNewFname( modTime, strExtension );
    }

    /**
       Constructs a file name that can be used as a name for an instance for a given photo
       @param photo the photo whose isntance is to be created
       @extension String to use as extension for the file name
    */
    public File getInstanceName( PhotoInfo photo, String strExtension ) {
	java.util.Date d = photo.getShootTime();
	// shooting time can in practice be null, use current date in this case (Date is used just for readability)
	if ( d == null ) {
	    d = new java.util.Date();
	}
	return getNewFname( d, strExtension );
	
    }
    
    private File getNewFname( java.util.Date date, String strExtension ) {
	SimpleDateFormat fmt = new SimpleDateFormat( "yyyy" );
	String strYear = fmt.format( date );
	fmt.applyPattern( "yyyyMM" );
	String strMonth = fmt.format( date );
	fmt.applyPattern( "yyyyMMdd" );
	String strDate = fmt.format( date );

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
	String fname = strDate + "_"+
	    "00000".substring( 0, 5-strOrderNum.length())+ strOrderNum + "." + strExtension;
	File archiveFile = new File( monthDir, fname );
	return archiveFile;
	
    }

    /**
       Maps a file name to a path in this volume. File names in a volume are unique but for
       performance reasons they are divided to several directories bsaed on year and month of shooting
       @param fname The file name to be mapped
       @return Path to the actual file
       @throws FineNotFoundException if the file does not exist
    */
    public File mapFileName( String fname ) throws FileNotFoundException {
	File yearDir = new File( volumeBaseDir, fname.substring( 0, 4 ) );
	File monthDir = new File ( yearDir, fname.substring( 0, 6 ) );
	File archiveFile = new File( monthDir, fname );
	if ( !archiveFile.exists() ) {
	    throw new FileNotFoundException( archiveFile.getPath() + " does not exist in volume" );
	}
	return archiveFile;
    }

    /**
       Returns the base directory for the volume.
    */
    
    public File getBaseDir() {
	return volumeBaseDir;
    }

    private String volumeName = "";

    /**
       Returns the volume name
     */
    public String getName() {
	return volumeName;
    }

    
    
    /** returns true if the vulome is available, flase otherwise (if e.g. the volume is
	stored on CD-ROM thatis not mounted currently
    */
    
    public boolean isAvailable() {
	return true;
    }
    

    
    private File volumeBaseDir;
}
