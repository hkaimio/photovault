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

import org.photovault.imginfo.FuzzyDate;
import java.util.*;
import javax.swing.tree.TreeModel;

public interface PhotoInfoView {
    public void setPhotographer( String newValue );
    public String getPhotographer();
    public void setPhotographerMultivalued( boolean mv );
    public void setFuzzyDate( FuzzyDate newValue );
    public FuzzyDate getFuzzyDate();
    public void setFuzzyDateMultivalued( boolean mv );
    public void setQuality( Number quality );
    public Number getQuality();
    public void setQualityMultivalued( boolean mv );
    public void setShootPlace( String newValue );
    public String getShootPlace();
    public void setShootPlaceMultivalued( boolean mv );
    public void setFocalLength( Number newValue );
    public Number getFocalLength();
    public void setFocalLengthMultivalued( boolean mv );
    public void setFStop( Number newValue );
    public Number getFStop();
    public void setFStopMultivalued( boolean mv );
    public void setCamera( String newValue );
    public String getCamera();
    public void setCameraMultivalued( boolean mv );
    public void setFilm( String newValue );
    public String getFilm();
    public void setFilmMultivalued( boolean mv );
    public void setLens( String newValue );
    public String getLens();
    public void setLensMultivalued( boolean mv );
    public void setDescription( String newValue );
    public String getDescription();
    public void setDescriptionMultivalued( boolean mv );
    public void setTechNote( String newValue );
    public String getTechNote();
    public void setTechNoteMultivalued( boolean mv );
    public void setShutterSpeed( Number newValue );
    public Number getShutterSpeed();
    public void setShutterSpeedMultivalued( boolean mv );
    public void setFilmSpeed( Number newValue );
    public Number getFilmSpeed();
    public void setFilmSpeedMultivalued( boolean mv );
    public void setFolderTreeModel( TreeModel model );
}
    
