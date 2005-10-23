package photovault.swingui.folderpane;


import photovault.folder.*;
import imginfo.*;
import java.util.*;


class FolderNode {

    Object[] model;
    public FolderNode( Object[] model, PhotoFolder folder ) {
	this.folder = folder;
	this.model = model;
    }

    PhotoFolder folder;

    public PhotoFolder getFolder() {
	return folder;
    }

    boolean allAdded = false;
    public void addAllPhotos() {
	allAdded = true;
	allRemoved = false;
    }

    boolean allRemoved = false;
    public void removeAllPhotos() {
	allRemoved = true;
	allAdded = false;
    }

    HashSet photos = new HashSet();
    public void addPhoto( PhotoInfo photo ) {
	photos.add( photo );
    }

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