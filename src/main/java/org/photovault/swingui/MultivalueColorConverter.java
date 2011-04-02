/*
  Copyright (c) 2011 Harri Kaimio

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

import com.jgoodies.binding.value.AbstractConverter;
import com.jgoodies.binding.value.ValueModel;
import java.awt.Color;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class to bind the isMultivalued property to field color
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class MultivalueColorConverter extends AbstractConverter {
    private static final Log log = LogFactory.getLog( MultivalueColorConverter.class );
    
    public MultivalueColorConverter( ValueModel m ) {
        super( m );
    }

    @Override
    public Object convertFromSubject( Object o ) {
        Color ret = Color.WHITE;
        if ( o instanceof Boolean ) {
            Boolean b = (Boolean) o;
            if ( b ) {
                ret = Color.LIGHT_GRAY;
            }
        }
        return ret;
    }

    public void setValue( Object o ) {
        log.debug( "Setting value " + o );
    }
}
