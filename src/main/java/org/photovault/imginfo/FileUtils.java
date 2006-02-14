// FileUtils.java

package org.photovault.imginfo;

import java.io.*;

/**
   FileUtils is a static class that implements common file handling operations that are often needed.
*/

public class FileUtils {

    /**
       Copies a file
       @param src Source file
       @param dst Destination file
    */
    public static void copyFile( File src, File dst ) throws IOException {
	FileInputStream in = new FileInputStream( src );
	FileOutputStream out = new FileOutputStream( dst );
	byte buf[] = new byte[1024];
	int nRead = 0;
	int offset = 0;
	while ( (nRead = in.read( buf )) > 0 ) {
	    out.write( buf, 0, nRead );
	    offset += nRead;
	}
	out.close();
	in.close();
    }

    /**
       Deleter a directory tree and all files stored in it.
       @param root Root directory of the tree
       @return true if deletion was successful, false otherwise
    */
    public static boolean deleteTree( File root ) {
	boolean success = true;
	if ( root.isDirectory() ) {
	    File entries[] = root.listFiles();
	    for ( int n = 0; n < entries.length; n++ ) {
		if ( !deleteTree( entries[n] ) ) {
		    success = false;
		}
	    }
	}
	if ( !root.delete() ) {
	    success = false;
	}
	return success;
    }
}    
