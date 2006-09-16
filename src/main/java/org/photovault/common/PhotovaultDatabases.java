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


package org.photovault.common;

import java.util.HashMap;
import java.util.Collection;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import org.apache.commons.betwixt.io.BeanWriter;
import org.apache.commons.betwixt.io.BeanReader;
import org.photovault.imginfo.ExternalVolume;
import org.photovault.imginfo.Volume;

/**
 * This is a collection of available photovault databases.
 * @author Harri Kaimio
 */
public class PhotovaultDatabases {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotovaultDatabases.class.getName() );

    HashMap databases;
    
    /** Creates a new instance of PhotovaultDatabases */
    public PhotovaultDatabases() {
        databases = new HashMap();
    }
    
    public void addDatabase( PVDatabase db ) throws PhotovaultException {
        if ( databases.containsKey( db.getName() ) ) {
            throw new PhotovaultException( "Database " + db.getDbName() + " already exists!" );
        }
        databases.put( db.getName(), db );
    }
    
    public PVDatabase getDatabase( String dbName ) {
        return (PVDatabase) databases.get( dbName );
    }
    
    public Collection getDatabases() {
        return databases.values();
    }
    
    public void save( File f ) {
        try {
            BufferedWriter outputWriter = new BufferedWriter( new FileWriter( f ));
            outputWriter.write("<?xml version='1.0' ?>\n");
            BeanWriter beanWriter = new BeanWriter( outputWriter );
            beanWriter.getXMLIntrospector().setAttributesForPrimitives(false);
            beanWriter.setWriteIDs(false);
            beanWriter.enablePrettyPrint();
            beanWriter.write("databases", this );
            beanWriter.close();
        } catch( Exception e ) {
            
        }        
    }
            
    public static PhotovaultDatabases loadDatabases( File f ) {
       // Now try to read the info
        BeanReader beanReader = new BeanReader();
        beanReader.getXMLIntrospector().getConfiguration().setAttributesForPrimitives(false);
        beanReader.getBindingConfiguration().setMapIDs(false);
        PhotovaultDatabases databases = null;
        try {
            beanReader.registerBeanClass( "databases", PhotovaultDatabases.class );
            beanReader.registerBeanClass( "database", PVDatabase.class );
            beanReader.registerBeanClass( "volume", Volume.class );
            beanReader.registerBeanClass( "external-volume", ExternalVolume.class );
            databases = (PhotovaultDatabases) beanReader.parse( f );
        } catch ( Exception e ) {
            log.warn( e.getMessage() );
        }        
        return databases;
    }
}
