package photovault.swingui;

// $Id

import java.util.*;
import photovault.folder.*;

public class PhotoFolderTreeEvent extends EventObject {

    PhotoFolder selected = null;
    
    public PhotoFolderTreeEvent( PhotoFolderTree src, PhotoFolder selected ) {
	super( src );
	this.selected = selected;
    }

    public PhotoFolder getSelected () {
	return selected;
    }
}
