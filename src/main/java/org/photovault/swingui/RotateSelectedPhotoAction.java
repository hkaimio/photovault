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

package org.photovault.swingui;


import java.awt.geom.Rectangle2D;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Collection;
import java.util.Iterator;
import org.photovault.imginfo.*;
import org.photovault.imginfo.PhotoInfo;

/**
  This action class rotates the selected images by the specified amount. In practice,
 for cropped photos the rotation must be in 90 degrees increments - otherwise the 
 effect of rotation is unspecified
*/
class RotateSelectedPhotoAction extends AbstractAction implements SelectionChangeListener {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( RotateSelectedPhotoAction.class.getName() );

    /**
       Constructor.
       @param view The view this action object is associated with. 
    */
    public RotateSelectedPhotoAction( PhotoCollectionThumbView view, 
				      double r,
				      String text, ImageIcon icon,
				      String desc, int mnemonic) {
	super( text, icon );
	this.view = view;
	putValue(SHORT_DESCRIPTION, desc);
        putValue(MNEMONIC_KEY, new Integer( mnemonic ) );
	view.addSelectionChangeListener( this );
	setEnabled( view.getSelectedCount() > 0 );
	rot = r;
    }

    public void selectionChanged( SelectionChangeEvent e ) {
	setEnabled( view.getSelectedCount() > 0 );
    }
    
    public void actionPerformed( ActionEvent ev ) {
        Collection selectedPhotos = view.getSelection();
        Iterator iter = selectedPhotos.iterator();
        while ( iter.hasNext() ) {
            PhotoInfo photo = (PhotoInfo) iter.next();
            if ( photo != null ) {
                double curRot = photo.getPrefRotation();
                photo.setPrefRotation( curRot + rot );
                Rectangle2D origCrop = photo.getCropBounds();
                Rectangle2D newCrop = calcNewCrop( origCrop );
                photo.setCropBounds( newCrop );
            }
        }
    }

    /**
     Calculates how the cropped area will be rotated as the specified rotation
     is applied. Currently this function expects that the rotation is multiple of 
     90 degrees - anyway, the whole rotation of cropped photo operation is not 
     well defined if the rotation is something else.
     
     @oldRot The cropping before rotation.
     @return The same crop area after rotation
     */
    private Rectangle2D calcNewCrop( Rectangle2D oldCrop ) {
        double x1 = oldCrop.getMinX() - 0.5;
        double y1 = oldCrop.getMinY() - 0.5;
        double x2 = oldCrop.getMaxX() - 0.5;
        double y2 = oldCrop.getMaxY() - 0.5;
        double sin = Math.sin( Math.toRadians( rot ) );
        double cos = Math.cos( Math.toRadians( rot ) );
        double nx1 = x1*cos - y1*sin;
        double ny1 = x1*sin + y1*cos;
        double nx2 = x2*cos - y2*sin;
        double ny2 = x2*sin + y2*cos;
        Rectangle2D newCrop = new Rectangle2D.Double( nx1+0.5, ny1+0.5, 0.0, 0.0 );
        newCrop.add( nx2+0.5, ny2+0.5 );
        return newCrop;
    }
    
    PhotoCollectionThumbView view;
    double rot;
}