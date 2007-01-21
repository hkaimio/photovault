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

package org.photovault.dcraw;

/**
  Interface for receiving information about changes to a {@linkto RawImage} 
 in the form of {@linkto RawImageChangeEvent}s
 */
public interface RawImageChangeListener {
    /**
     Called when some settings (exposure, color balance etc.) of an
     {@linkto RawImage} have been changed
     */
    public void rawImageSettingsChanged( RawImageChangeEvent ev );
}