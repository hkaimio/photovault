/*
  Copyright (c) 2007, Harri Kaimio
  
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

package org.photovault.swingui.color;

import java.awt.Frame;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Set;
import org.hibernate.Session;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoFields;
import org.photovault.swingui.framework.AbstractController;
import org.photovault.swingui.selection.*;

/**
  ColorSettingsDlgController manages the interaction between color settings
 dialog, selected photos and preview in JAIPhotoViewer.
 
 */
public class ColorSettingsDlgController extends PhotoSelectionController {
    
    /** Creates a new instance of ColorSettingsDlgController */
    public ColorSettingsDlgController( Frame parentFrame, AbstractController parentCtrl, Session session ) {
        super( parentCtrl, session );
        this.parentFrame = parentFrame;
        createUI();
    }
    
    
    Frame parentFrame;
    ColorSettingsDlg dlg = null;
    ColorSettingsPreview preview = null;


    /**
     Create the color settings dialog
     */
    void createUI() {
        dlg = new ColorSettingsDlg( parentFrame, this, false, null );
        addView( dlg );
    }

    /**
     This method overrides the super type viewChanged method so that it also 
     updates the preview control.
     TODO: More elegant solution would be if also JAIPhotoViewer would a real 
     view for selection controller. However, this will require larger refactoring
     (e.g. to accommodate hierarchical selection controllers with different
     changes tied to different views.)
     @param view The view that initiated field change
     @param field The changed field
     @param value New value for field
     */
    public void viewChanged( PhotoSelectionView view, PhotoInfoFields field, Object value ) {
        super.viewChanged( view, field, value );
        if ( preview != null && photos != null &&
                photos.length == 1 && photos[0] == preview.getPhoto() ) {
            updatePreview( field, value );
        }
    } 
    
    protected void photosChanged() {
        boolean allRaw = true;
        if ( photos != null ) {
            for ( PhotoInfo p: photos ) {
                if ( p.getRawSettings() == null ) {
                    allRaw = false;
                }
            }
        } else {
            allRaw = false;
        }
        dlg.setRawControlsEnabled( allRaw );        
    }

    /**
     Discard all changes. OVerridden from superclass to ensure that preview image 
     is returned to original state.
     */
    public void discard() {
        super.discard();
        if ( preview != null && photos != null &&
                photos.length == 1 && photos[0] == preview.getPhoto() ) {
            for ( PhotoInfoFields f : EnumSet.allOf( PhotoInfoFields.class ) ) {
                updatePreview( f, PhotoInfoFields.getFieldValue( photos[0], f ) );
            }
        }        
    }
    
    /**
     Update the preview control to match change that has happened in other views
     @param field The changed field
     @param value new value for field
     */    
    private void updatePreview( PhotoInfoFields field, Object value ) {
        Set fieldValues = this.getFieldValues( field );
        
        preview.setField( field, value, new ArrayList( fieldValues ) );
    }
    
    /**
     Set the preview control used by this controller.
     @param preview The control to use
     */
    public void setPreviewControl( ColorSettingsPreview preview ) {
        this.preview = preview;
    }

    public void showDialog() {
        dlg.showDialog();
    }
}
