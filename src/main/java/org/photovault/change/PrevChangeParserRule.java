/*
  Copyright (c) 2008 Harri Kaimio
  
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

package org.photovault.change;

import java.util.UUID;
import org.apache.commons.digester.Digester;
import org.apache.commons.digester.Rule;
import org.photovault.imginfo.xml.ChangeDesc;
import org.photovault.persistence.DAOFactory;
import org.xml.sax.Attributes;

/**
 Rule for parsing the <prev-change> tag
 @author Harri Kaimio
 @since 0.6.0
 */
class PrevChangeParserRule extends Rule {
    
    /**
     Matching start tag was found. Find local instance of the change referred and 
     add it as predecessor for the currently proccessed change.
     @param namespace
     @param name
     @param attrs
     @throws java.lang.Exception
     */
    @Override
    public void begin( String namespace, String name, Attributes attrs ) 
            throws Exception {
        String uuidStr = attrs.getValue( "uuid" );
        UUID uuid = UUID.fromString(uuidStr);        
        Digester dg = this.getDigester();
        DAOFactory df = (DAOFactory) dg.peek( "daofactorystack" );
        ChangeDescDAO cdDao = df.getChangeDescDAO();
        ChangeDesc prev = cdDao.findChange( uuid );
        if ( prev != null ) {
            ChangeDesc cd = (ChangeDesc) dg.peek();
            cd.addPrevChange( prev );
        }
    }
}
