/*
 * PVDatabase.java
 *
 * Created on 18. marraskuuta 2005, 20:45
 */
 
package photovault.common;

import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.io.InputStream;
import  imginfo.Volume;
import java.util.Random;
import org.apache.commons.beanutils.DynaBean;
import org.apache.ddlutils.io.DatabaseIO;
import org.apache.ddlutils.model.Database;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.PlatformFactory;
import org.apache.ddlutils.io.DataToDatabaseSink;
import org.apache.ddlutils.io.DataReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import org.xml.sax.SAXException;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSourceFactory;

/**
 * PVDatabase represents metadata about a single Photrovault database. A database 
 * consists of an SQL database for storing photo metadata and 1 or more volumes 
 * for storing the actual photos
 * @author Harri Kaimio
 */
public class PVDatabase {
    
    /** Creates a new instance of PVDatabase */
    public PVDatabase() {
        volumes = new ArrayList();
    }
    
    private String name;
    private String dbHost = "";
    private String dbName = "";
    private ArrayList volumes;

    public void setName( String name ) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        if ( dbHost != null ) {
            this.dbHost = dbHost;
        } else {
            this.dbHost = "";
        }
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }
    
    public void addVolume( Volume volume ) {
        volumes.add( volume );
    }
    
    public List getVolumes( ) {
        return  volumes;
    }

    public Volume getDefaultVolume() {
        Volume vol = null;
        if ( volumes.size() > 0 ) {
            vol = (Volume) volumes.get(0);
        }
        return vol;
    }
            
    /**
     * Creates a new database with the parameters specified in this object.
     * <P>
     * The database schema is stored in DDLUTILS XML format in resource 
     * photovault.xml.
     *
     * @param user Username used when creating the SQL database.
     * @param passwd Password for the user
     */
    public void createDatabase( String user, String passwd ) {
        //throw new UnsupportedOperationException("Not yet implemented");
        
        // Get the database schema XML file
        InputStream schemaIS = getClass().getClassLoader().getResourceAsStream( "photovault_schema.xml" );
        Database dbModel = new DatabaseIO().read( new InputStreamReader( schemaIS ) );
        
        // Create the datasource for accessing this database
        
        String driverName = "com.mysql.jdbc.Driver";
        String dbUrl = "jdbc:mysql://" + getDbHost() + "/" + getDbName();
       
        MysqlDataSource ds = new MysqlDataSource();
        ds.setURL( dbUrl );
        ds.setUser( user );
        ds.setPassword( passwd );
        
//        Connection conn = null;
//	try {
//	    Class.forName( driverName ).newInstance();
//	} catch ( Exception e ) {
//	    System.err.println( "DB driver not found" );
//	}
//
//	try {
//	    conn = DriverManager.getConnection( dbUrl, user, passwd );
//	} catch ( SQLException e ) {
//	    System.err.println( "ERROR: Could not create DB connection: "
//				+ e.getMessage() );
//	}
        
        Platform platform = PlatformFactory.createNewPlatformInstance( ds );
        // Platform platform = PlatformFactory.createNewPlatformInstance( driverName, dbUrl );
        platform.setUsername( user );
        platform.setPassword( passwd );
        platform.createTables( dbModel, true, false );
        
        // Insert the seed data to database
        DataToDatabaseSink sink = new DataToDatabaseSink( platform, dbModel );
        DataReader reader = new DataReader();
        reader.setModel( dbModel );
        reader.setSink( sink );
        

        InputStream seedDataStream = this.getClass().getClassLoader().getResourceAsStream( "photovault_seed_data.xml" );
        try {
            reader.parse( seedDataStream );
        } catch (SAXException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        
        // Create the database 
        
        // TODO: Since the seed has only 48 significant bits this id is not really an 
        // 128-bit random number!!!
        Random rnd = new Random();
        String idStr = "";
        StringBuffer idBuf = new StringBuffer();
        for ( int n=0; n < 4; n++ ) {
            int r = rnd.nextInt();
            idBuf.append( Integer.toHexString( r ) );
        }
        idStr = idBuf.toString();
        DynaBean dbInfo = dbModel.createDynaBeanFor( "database_info", false );
        dbInfo.set( "database_id", idStr );
        dbInfo.set( "schema_version", new Integer(1) );
        dbInfo.set( "create_time", new Date() );
        platform.insert( dbModel, dbInfo );
    }        
}
