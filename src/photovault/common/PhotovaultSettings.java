package photovault.common;

import java.util.Properties;
import java.io.InputStream;
import java.io.FileInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
   This class contains useful UI independent routines
*/


public class PhotovaultSettings {

    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( PhotovaultSettings.class.getName() );
    
    static final String defaultPropFname = "conf/photovault.properties";
    static public void init() {
	// Determine the property file name
	String propFname = System.getProperty( "photovault.propFname", defaultPropFname );
	File propFile = new File( propFname );
	props = new Properties();
	try {
	    InputStream is = new FileInputStream( propFile );
	    props.load( is );
	} catch ( FileNotFoundException e ) {
	    log.error( "Could not find the property file " + propFname );
	} catch ( IOException e ) {
	    log.error( "Could not load property file " + propFname + ": " + e );
	}

    }

    /**
       Return the value of a given property
       @param name Name of the property to fetch
    */
    static public String getProperty( String name ) {
	return props.getProperty( name );
    }

    /**
       Return the value of a given property
       @param name Name of the property to fetch
       @param def Default value if the property is not defined
    */
    static public String getProperty( String name, String def ) {
	return props.getProperty( name, def );
    }


    /**
       Set the configuration used when determining property values
       @param confName Name of the configuration. This must be defined in
       photovault.configNames property - results are undefined otherwise.
    */
    static public void setConfiguration( String confName ) {
	PhotovaultSettings.confName = confName;
    }

    /**
       Returns a value of a property in current configuration
       @param name Name of the property. This is translated into actual property name
       photovault.conf.confName.name.
    */
    static public String getConfProperty( String name ) {
	String propName = "photovault.conf." + confName + "." + name;
	String propValue = props.getProperty( "photovault.conf." + confName + "." + name );
	log.debug( "Getting property " + propName + ", value " + propValue );

	return propValue;
    }

    /**
       Returns names of know configurations
    */
    static public String[] getConfigurationNames() {
	String confNames = props.getProperty( "photovault.configNames" );
	return confNames.split( "\\s" );
    }
	
    static String confName;
    static Properties props;

}