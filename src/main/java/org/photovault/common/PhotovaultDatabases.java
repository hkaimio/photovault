/*
 * PhotovaultDatabases.java
 *
 * Created on 19. marraskuuta 2005, 9:09
 */

package org.photovault.common;

import java.util.HashMap;
import java.util.Collection;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import org.apache.commons.betwixt.io.BeanWriter;
import org.apache.commons.betwixt.io.BeanReader;
import org.photovault.imginfo.Volume;

/**
 * This is a collection of available photovault databases.
 * @author Harri Kaimio
 */
public class PhotovaultDatabases {

    HashMap databases;
    
    /** Creates a new instance of PhotovaultDatabases */
    public PhotovaultDatabases() {
        databases = new HashMap();
    }
    
    public void addDatabase( PVDatabase db ) {
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
            databases = (PhotovaultDatabases) beanReader.parse( f );
        } catch ( Exception e ) {
            System.err.println( e.getMessage() );
        }        
        return databases;
    }
}