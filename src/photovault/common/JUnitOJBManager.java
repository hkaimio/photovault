/*
 * JUnitOJBManager.java
 *
 * Created on 29. lokakuuta 2005, 21:53
 */

package photovault.common;
import org.photovault.dbhelper.ODMG;

/**
 * This class handles proper OJB initialization for unti tests. This class is 
 * singlton and it just sets up the OJB environment according to configuration files
 * so it is enough that the unit tests just get a reference to it.
 *<p>
 * Environment setup is doe like this:
 * <ul>
 * <li> First, property file is looked based on system property photovault.propFname
 * <li> Then the configuration is set to "pv_junit"
 * <li> Last, ODMG is initialized using no username or password
 * </ul>
 * @author harri Kaimio
 */
public class JUnitOJBManager {
    static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger( JUnitOJBManager.class.getName() );

    /** Creates a new instance of JUnitOJBManager */
    private JUnitOJBManager() {
        log.error( "Initializing OB for JUnit tests" );
        PhotovaultSettings settings = PhotovaultSettings.getSettings();
        settings.setConfiguration( "pv_junit" );
	PVDatabase db = settings.getDatabase( "pv_junit" );

	if ( db == null ) {
	    log.error( "Could not find dbname for configuration " );
	    return;
	}
	    

	if ( ODMG.initODMG( "", "", db ) ) {
	    log.debug( "Connection succesful!!!" );
	} else {
	    log.error( "Error logging into Photovault" );
	}
    }

    static JUnitOJBManager mgr = null;
    public static JUnitOJBManager getOJBManager() {
        if ( mgr == null ) {
            mgr = new JUnitOJBManager();
        }
        return mgr;
    }
}
