/*
  Copyright (c) 2008 Harri Kaimio
 
  This file is part of Photovault.
 
  Photovault is free software; you can redistribute it and/or modify it
  under the terms of the GNU General Public License as published by
  the Free Software Foundation; either version 2 of the License, or
  (at your option) any later version.
 
  Photovault is distributed in the hope that it will be useful, but
  WITHOUT ANY WARRANTY; without even therrore implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
  General Public License for more details.
 
  You should have received a copy of the GNU General Public License
  along with Photovault; if not, write to the Free Software Foundation,
  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA
 */

package org.photovault.imginfo;

import org.photovault.replication.ObjectEditorInvocationHandler;

/**
 Invocation handler for {@link PhotoEditor}
 */
public class PhotoEditorInvocationHandler 
        extends ObjectEditorInvocationHandler<PhotoInfo, String> {

    public PhotoEditorInvocationHandler( PhotoInfoChangeSupport history ) {
        super( history );
    }
    
    @Override
    protected String getFieldForName( String name ) {
        return name;
    }

}
