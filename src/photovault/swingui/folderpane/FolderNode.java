package photovault.swingui.folderpane;


import org.photovault.imginfo.PhotoInfo;
import photovault.folder.*;
import org.photovault.imginfo.*;
import java.util.*;

/**
 * FolderNode keeps track of photos currently in the model that belong to a cretain folder.
 * It determises the presentation of the folder in folder tree.
 * <P>
 * FolderNode is used as an user object of DefaultMutableTreeNode. 
 * <p>
 * TODO: Note that currently this object is used only as a "dummy" data structure 
 * to get the node representation for DefaultMutableTree, since the representation 
 * needs information from both field model and PhotoFolder. However almost all 
 * processing is done in @see FolderController - it might be more logical to 
 * encapsulate also the relean logic here.
 */

class FolderNode {

    Object[] model;
    /**
     * Constructor
     * @param model The model (@see FieldController) that is represented in the folder tree
     * @param folder The PhotoFolder that this FoldeNode represents.
     */
    public FolderNode( Object[] model, PhotoFolder folder ) {
	this.folder = folder;
	this.model = model;
    }

    PhotoFolder folder;

    public PhotoFolder getFolder() {
	return folder;
    }

    boolean allAdded = false;
    /** Adds all photos in the model to this folder
     */
    public void addAllPhotos() {
	allAdded = true;
	allRemoved = false;
    }

    boolean allRemoved = false;
    
    /**
     * Removes all photos that belong to model from this folder
     */
    public void removeAllPhotos() {
	allRemoved = true;
	allAdded = false;
    }

    HashSet photos = new HashSet();
    
    /**
     * Adds a photo to this folder.
     */
    public void addPhoto( PhotoInfo photo ) {
	photos.add( photo );
    }

    /**
     * Returns <code>true</code> if the FolderNode contains photos, 
     * <code>false</code> otherwise.
     */
    
    public boolean containsPhotos() {
        return allAdded || ((photos.size() > 0) && !allRemoved);
    }
    
    /**
     * Returns the string that represents this folder in folder tree. <p>
     *
     * The representation is determined like this:
     * <ul>
     * <li> If the folder contains no photos the folder name is displayed as normal text
     * <li> If the folder contains all photos in the model the folder name is 
     * displayed as bolded black text.
     * <li> If the folder contains some of the photos in model the fodler name is 
     * displayed in gray bolded text.
     * <ul>
     */
    public String toString() {
	StringBuffer strbuf = new StringBuffer();
	strbuf.append( "<html>" );
	boolean hasPhotos = ((photos.size() > 0 || allAdded) && !allRemoved);
	boolean hasAllPhotos = ((photos.size() == model.length || allAdded)
				&& !allRemoved );
	if ( hasPhotos ) {
	    strbuf.append( "<b>" );
	    if ( !hasAllPhotos ) {
		strbuf.append( "<font color=gray>" );
	    }
	}

	strbuf.append( folder.getName() );
	
	if ( hasPhotos ) {
	    if ( !hasAllPhotos ) {
		strbuf.append( "</font>" );
	    }
	    strbuf.append( "</b>" );
	}
	return strbuf.toString();
    }
	


}