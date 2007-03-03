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

package org.photovault.image;

import java.awt.image.RenderedImage;
import java.io.File;
import java.util.Date;

/**
 *
 * @author harri
 */
public abstract class PhotovaultImage {
    
    /** Creates a new instance of PhotovaultImage */
    public PhotovaultImage() {
    }

    
    File f = null;

    /**
     * Get aperture (f-stop) used when shooting the image
     * 
     * @return F-stop number reported by dcraw
     */
    public abstract double getAperture();

    /**
     * Get the camera mode used to shoot the image
     * 
     * @return Camera model reported by dcraw
     */
    public abstract String getCamera();

    public abstract RenderedImage getCorrectedImage();

    /**
     * Get the film speed setting used when shooting the image
     * 
     * @return Film speed (in ISO) as reported by dcraw
     */
    public abstract int getFilmSpeed();

    /**
     * Get the focal length from image file meta data.
     * 
     * @return Focal length used when taking the picture (in millimetres)
     */
    public abstract double getFocalLength();

    public abstract RenderedImage getImage();

    
    public File getImageFile() {
        return f;
    }

    /**
     * Get the shutter speed used when shooting the image
     * 
     * @return Exposure time (in seconds) as reported by dcraw
     */
    public abstract double getShutterSpeed();

    /**
     * Get the shooting time of the image
     * 
     * @return Shooting time as reported by dcraw or <CODE>null</CODE> if
     * not available
     */
    public abstract Date getTimestamp();
    
}
