package org.photovault.swingui;
// $Id: ImageFilter.java,v 1.1 2003/03/15 13:17:03 kaimio Exp $


import java.io.File;
import javax.swing.*;
import javax.swing.filechooser.*;

/**
   FileFilter to display only image files in JFileChooser. Copied from Java tutorial
*/

public class ImageFilter extends FileFilter {
    
    // Accept all directories and all gif, jpg, or tiff files.
    public boolean accept(File f) {
        if (f.isDirectory()) {
            return true;
        }

        String extension = getExtension(f);
	if (extension != null) {
            if (extension.equals(tiff) ||
                extension.equals(tif) ||
                extension.equals(gif) ||
                extension.equals(jpeg) ||
                extension.equals(jpg)) {
                    return true;
            } else {
                return false;
            }
    	}

        return false;
    }

    public final static String jpeg = "jpeg";
    public final static String jpg = "jpg";
    public final static String gif = "gif";
    public final static String tiff = "tiff";
    public final static String tif = "tif";

    /*
     * Get the extension of a file.
     */  
    public static String getExtension(File f) {
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }
    
    // The description of this filter
    public String getDescription() {
        return "Just Images";
    }
}
