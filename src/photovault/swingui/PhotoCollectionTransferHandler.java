// PhotoCollectionTransferHandler.java
package photovault.swingui;

import java.io.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import javax.swing.*;
import photovault.folder.PhotoFolder;
import imginfo.*;
import java.util.Collection;
import java.util.Iterator;


/**
   PhotoCollectionTransferHandler implements drag-n-drop and clipboard support 
   for @see PhotoCollectionThumbView. Currently data is only transferred inside
   the same application. Future improvement plans include
   <ul>
   <li> Transfer of persistent PhotoInfo objects between several applications based
   on photovault engine </li>
   <li> Transfer of images as files from Photovault database to other applications </li>
   <li> Importing files to Photovault database</li>
   <li> Transfer of image data to other applications </li>
   </ul>
*/

public class PhotoCollectionTransferHandler extends TransferHandler {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotoCollectionTransferHandler.class.getName() );

    /**
       Data flavor for an array of PhotoInfo objects. This is used when transferring
       photos inside the same virtual machine
    */
    DataFlavor photoInfoFlavor = null;
    
    /**
       Array of photos that were transferred from the source
    */
    PhotoInfo[] sourcePhotos = null;

    PhotoCollectionThumbView view = null;
    
    /**
       Constructor
       @param c Component whose transfers are handled
    */
    public PhotoCollectionTransferHandler( PhotoCollectionThumbView c ) {
	super();
	view = c;
	try {
	    photoInfoFlavor = new DataFlavor( DataFlavor.javaJVMLocalObjectMimeType
					      + ";class=\"" + PhotoInfo[].class.getName()
					      + "\"" );
	} catch ( Exception e ) {
	}

    }

    /**
       Indicates whether a component would accept an import of the given set of
       data flavors prior to actually attempting to import it.

       @param c  the component to receive the transfer; this argument is provided
       to enable sharing of TransferHandlers by multiple components but is not used
       by this implementation
       @param flavors the data formats available
       @return true if the data can be inserted into the component, false otherwise
    */
    
    public boolean canImport(JComponent c, DataFlavor[] flavors) {
	for (int i = 0; i < flavors.length; i++) {
	    if (photoInfoFlavor.equals(flavors[i])) {
		return true;
	    }
	}
	return false;
    }
  
    /**
       Causes a transfer to a component from a clipboard or a DND drop operation.
       The Transferable represents the data to be imported into the component.
    */
    
    public boolean importData(JComponent c, Transferable t) {
	log.warn( "importData" );
        if (canImport(c, t.getTransferDataFlavors())) {

            //Don't drop on myself.
//             if (source == c) {
//                 shouldRemove = false;
//                 return true;
//             }
	    PhotoCollection collection = view.getCollection();
	    if ( collection instanceof PhotoFolder ) {
		log.warn( "importing" );
		// Photos were dropped to a folder so we can insert them
		PhotoFolder folder = (PhotoFolder) collection;
		try {
		    PhotoInfo[] photos = (PhotoInfo[])t.getTransferData(photoInfoFlavor);
		    for ( int n = 0; n < photos.length; n++ ) {
			folder.addPhoto( photos[n] );
		    }
		    return true;
		} catch (UnsupportedFlavorException ufe) {
		    log.warn("importData: unsupported data flavor");
		} catch (IOException ioe) {
		    log.warn("importData: I/O exception");
		}
	    }
	}
        return false;
    }

    /**
       Creates a Transferable to use as the source for a data transfer.
       Returns the representation of the data to be transferred, or null
       if the component's property is null
       @return A @see PhotoCollectionTransferable object that represents current
       selection of the component
    */
    protected Transferable createTransferable(JComponent c) {
	log.warn( "createTransferable" );
	Collection selection = view.getSelection();
	sourcePhotos = new PhotoInfo[selection.size()];
	Iterator iter = selection.iterator();
	int i = 0;
	while ( iter.hasNext() ) {
	    sourcePhotos[i] = (PhotoInfo) iter.next();
	    i++;
	}
	log.warn( "" + i + " photos selected" );
//         shouldRemove = true;
        return new PhotoCollectionTransferable(sourcePhotos);
    }
    
    public int getSourceActions(JComponent c) {
        return COPY_OR_MOVE;
    }

    /**
       This method is called after the data has been exported. If the action was MOVE
       it removes all transferred photos from the folder.
    */
    protected void exportDone(JComponent c, Transferable data, int action) {
	PhotoCollection coll = view.getCollection();
        if (/*shouldRemove && */ (action == MOVE) && coll instanceof PhotoFolder ) {
	    PhotoFolder folder = (PhotoFolder) coll;
	    for ( int i = 0; i < sourcePhotos.length; i++ ) {
		folder.removePhoto( sourcePhotos[i] );
	    }
        }
    }

    class PhotoCollectionTransferable implements Transferable {
        private PhotoInfo[] photos = null;

        PhotoCollectionTransferable( PhotoInfo[] sourcePhotos ) {
            photos = sourcePhotos;
        }

        public Object getTransferData(DataFlavor flavor)
	    throws UnsupportedFlavorException {
            if (!isDataFlavorSupported(flavor)) {
                throw new UnsupportedFlavorException(flavor);
            }
            return photos;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return new DataFlavor[] { photoInfoFlavor };
        }

        public boolean isDataFlavorSupported(DataFlavor flavor) {
            return photoInfoFlavor.equals(flavor);
        }
    }    

}

   
   