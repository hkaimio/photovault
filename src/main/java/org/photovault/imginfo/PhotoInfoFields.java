/*
  Copyright (c) 2007 Harri Kaimio
  
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

package org.photovault.imginfo;

import java.awt.geom.Rectangle2D;
import java.util.Date;
import java.util.UUID;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.image.ColorCurve;

/**
 Fields in PhotoInfo
 */
public enum PhotoInfoFields {                
    CAMERA ("camera", String.class ),         
    CROP_BOUNDS( "cropBounds", Rectangle2D.class ),         
    DESCRIPTION( "description", String.class ),         
    FSTOP( "FStop", Double.class ),       
    FILM( "film", String.class ),       
    FILM_SPEED( "filmSpeed", Integer.class ),         
    FOCAL_LENGTH( "focalLength", Integer.class ),         
    LENS( "lens", String.class ),         
    ORIG_FNAME( "origFname", String.class ),         
    PHOTOGRAPHER( "photographer", String.class ),         
    PREF_ROTATION( "prefRotation", Double.class ),         
    QUALITY( "quality", Integer.class ),         
    RAW_SETTINGS( "rawSettings", RawConversionSettings.class ),         
    SHOOT_TIME( "shootTime", Date.class ),         
    SHOOTING_PLACE( "shootingPlace", String.class ),         
    SHUTTER_SPEED( "shutterSpeed", Double.class ),         
    TECH_NOTES( "techNotes", String.class ),         
    TIME_ACCURACY( "timeAccuracy", Double.class ),         
    UUID( "UUID", UUID.class ),         
    RAW_BLACK_LEVEL( "blackLevel", Integer.class ),         
    RAW_WHITE_LEVEL( "whiteLevel", Integer.class ),         
    RAW_EV_CORR( "evCorr", Double.class ),         
    RAW_HLIGHT_COMP( "hlightComp", Double.class ),         
    RAW_CTEMP( "colorTemp", Double.class ),         
    RAW_GREEN( "greenGain", Double.class ),         
    RAW_COLOR_PROFILE( "colorProfile", Double.class ),         
    COLOR_CURVE_VALUE( "masterCurve", ColorCurve.class ),         
    COLOR_CURVE_RED( "redColorCurve", ColorCurve.class ),         
    COLOR_CURVE_GREEN( "greenColorCurve", ColorCurve.class ),         
    COLOR_CURVE_BLUE( "blueColorCurve", ColorCurve.class ),         
    COLOR_CURVE_SATURATION( "saturationCurve", ColorCurve.class );



    PhotoInfoFields(String name, Class type) {
        this.name = name;
        this.type = type;
    }
    
    private final String name;
    private final Class type;
    
    public Class getType() {
        return type;
    }
    
    public String getName() {
        return name;
    }
    
}