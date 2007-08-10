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
import org.hibernate.Session;
import org.photovault.command.CommandException;
import org.photovault.command.PhotovaultCommandHandler;
import org.photovault.imginfo.*;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.swingui.framework.DataAccessAction;

/**
  This action class rotates the selected images by the specified amount. In practice,
 for cropped photos the rotation must be in 90 degrees increments - otherwise the 
 effect of rotation is unspecified
*/
class RotateSelectedPhotoAction extends DataAccessAction implements SelectionChangeListener {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( RotateSelectedPhotoAction.class.getName() );

    PhotoViewController ctrl;
    double rot;
    
    
    /**
       Constructor.
       @param view The view this action object is associated with. 
    */
    public RotateSelectedPhotoAction( PhotoViewController ctrl, 
				      double r ) {
	// super( text, icon );
        super();
	this.ctrl = ctrl;
	rot = r;
    }

    public void selectionChanged( SelectionChangeEvent e ) {
	setEnabled( ctrl.getSelection().size() > 0 );
    }
    
    public void actionPerformed( ActionEvent ev, Session session ) {
        Collection selectedPhotos = ctrl.getSelection();
        PhotovaultCommandHandler cmdHandler = new PhotovaultCommandHandler( null );
        Iterator iter = selectedPhotos.iterator();
        while ( iter.hasNext() ) {
            PhotoInfo photo = (PhotoInfo) iter.next();
            if ( photo != null ) {
                ChangePhotoInfoCommand cmd = new ChangePhotoInfoCommand( photo.getId() );
                double curRot = photo.getPrefRotation();
                cmd.setPrefRotation( curRot + rot );
                Rectangle2D origCrop = photo.getCropBounds();
                Rectangle2D newCrop = calcNewCrop( origCrop );
                cmd.setCropBounds( newCrop );
                try {
                    cmdHandler.executeCommand( cmd );
                    PhotoInfo[] changedPhotos = cmd.getChangedPhotos().toArray( new PhotoInfo[1] );
                    photo = ctrl.getDAOFactory().getPhotoInfoDAO().makePersistent( changedPhotos[0] );
                } catch (CommandException ex) {
                    ex.printStackTrace();
                }
            }
        }
        session.flush();
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
    
}