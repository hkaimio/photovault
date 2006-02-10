// ThumbnailTableRenderer.java

package photovault.swingui;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.io.*;
import java.awt.geom.AffineTransform;
import org.photovault.imginfo.*;
import org.photovault.imginfo.PhotoInfo;


/**
   This class implements a table renderer for displaying PhotoInfo objects as tumbnails
*/
class ThumbnailTableRenderer extends ThumbnailView implements TableCellRenderer {

    /**
       Constructor
    */
    public ThumbnailTableRenderer() {
	super();
	setOpaque( true );
    }

    /**
       Implementation of TableCellRenderer interface
    */
    public Component getTableCellRendererComponent( JTable table, Object obj, 
						    boolean isSelected, boolean hasFocus,
						    int row, int column) {
	PhotoInfo p = (PhotoInfo) obj;
	setPhoto(  p );
	if (isSelected) {
	    if (selectedBorder == null) {
		setBackground( table.getSelectionBackground() );
	    }
	    setBorder(selectedBorder);
	} else {
	    if (unselectedBorder == null) {
		setBackground( table.getBackground() );
	    }
	    setBorder(unselectedBorder);
	}
	return this;
    }

    Border selectedBorder = null;
    Border unselectedBorder = null;
}
