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

import java.io.IOException;
import java.io.StringReader;
import org.apache.commons.digester.Digester;
import org.apache.ojb.broker.accesslayer.conversions.FieldConversion;
import org.xml.sax.SAXException;

/**
 Conversion between ChannelMapOperation and it srepresentation in database.
 The object is stored in its XML representation in binary database field
 */
public class ChannelMapOJBConversion implements FieldConversion {
    
    /** Creates a new instance of ChannelMapOJBConversion */
    public ChannelMapOJBConversion() {
    }

    public Object javaToSql(Object object) {
        if ( object instanceof ChannelMapOperation ) {
            String xmlStr = ((ChannelMapOperation)object).getAsXml();
            byte[] data = xmlStr.getBytes(); 
            return data;
        }
        return object;
    }

    public Object sqlToJava(Object object) {
        if ( object instanceof byte[] ) {
            String xmlStr = new String( (byte[]) object );
            Digester d = new Digester();
            d.addRuleSet( new ChannelMapRuleSet() );
            ChannelMapOperationFactory f;
            try {
                f = (ChannelMapOperationFactory) d.parse(new StringReader(xmlStr));
            } catch (IOException ex) {
                return object;
            } catch (SAXException ex) {
                return object;
            }
            return f.create();            
        }
        return object;
    }
    
}
