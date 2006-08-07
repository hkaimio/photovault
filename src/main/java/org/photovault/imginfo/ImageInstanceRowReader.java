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

package org.photovault.imginfo;


import org.apache.ojb.broker.accesslayer.*;
import org.apache.ojb.broker.metadata.ClassDescriptor;
import java.util.Map;

/**
   This class extends the standard OJB object loading so that it initializes the instanceFile field correctly
   as it does not map directly to any database field
*/

public class ImageInstanceRowReader extends RowReaderDefaultImpl {

    public ImageInstanceRowReader( ClassDescriptor cld ) {
	super( cld );
    }
    
    public Object readObjectFrom(Map row ) {
	Object result = super.readObjectFrom(row);
	if ( result instanceof ImageInstance ) {
	    ImageInstance inst = (ImageInstance) result;
	    inst.initFileAttrs();
	}
	return result;
    }
}
