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
import org.xml.sax.SAXParseException;

/**
 Apache digester rule for parsing a change descriptor tag XML presentation.
 The rule first checks whether a change object with the same uuid exists in 
 database. If not, the rule creates a local replica f the object. Otehrwise, 
 it just pushes a placeholder in stack so that the following rules can be 
 processed.

 @author Harri Kaimio
 @since 0.6.0
 */
public class ChangeDescParserRule extends Rule {

    /**
     Matching start tag is found. Try to find the corresponding object from 
     database or create local instance if it is not found.
     @param namespace Namespace of this tag
     @param name Tag name
     @param attrs Attributes of the tag
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
        ChangeDesc cd = cdDao.findChange( uuid );
        Class clazz = null;
        if ( cd == null ) {
            // No local copy of this change exists
            try {
            clazz = Class.forName( attrs.getValue("class") );
            } catch ( ClassNotFoundException e ) {
                dg.error( new SAXParseException( 
                        "Unknown change class " + attrs.getValue("class"),
                        dg.getDocumentLocator(), e ) );
                return;
            }
            
            if ( !ChangeDesc.class.isAssignableFrom(clazz) ) {
                dg.error( new SAXParseException( clazz.getName() + " is not a change class",
                        dg.getDocumentLocator() ) );
                return;
            }
            try {
                cd = (ChangeDesc) clazz.newInstance();
                cd.setUuid( uuid );
                cdDao.makePersistent( cd );
            } catch ( InstantiationException e ) {
                dg.error( new SAXParseException( "Cannot instantiate change", 
                        dg.getDocumentLocator() ) );
            }
        }
        dg.push( cd );
        
    }
    
    /**
     End tag is found. Verify that the created change descriptor is valid (i.e. 
     its has matches its uuid.
     @param namespace Namespace of the tag
     @param name Name of the tag
     @throws java.lang.Exception If the tag has wrong hash.
     */
    @Override
    public void end( String namespace, String name ) throws Exception {
        Digester dg = getDigester();
        ChangeDesc cd = (ChangeDesc) dg.pop();
        if ( !cd.verify() ) {
            dg.error( new SAXParseException( 
                    "Hash for change " + cd.getUuid().toString() + " does not match",
                    dg.getDocumentLocator() ) ); 
        }
    }
}
