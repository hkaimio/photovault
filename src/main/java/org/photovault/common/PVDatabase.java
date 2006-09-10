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

import java.io.File;
import java.io.IOException;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.io.InputStream;
import org.apache.ddlutils.DynaSqlException;
import org.apache.ojb.broker.PBKey;
import org.apache.ojb.broker.PersistenceBroker;
import org.apache.ojb.broker.PersistenceBrokerFactory;
import org.apache.ojb.broker.accesslayer.LookupException;
import org.apache.ojb.broker.metadata.ConnectionRepository;
import org.apache.ojb.broker.metadata.JdbcConnectionDescriptor;
import org.apache.ojb.broker.metadata.MetadataManager;
import org.odmg.Implementation;
import org.odmg.OQLQuery;
import org.photovault.dbhelper.ODMG;
import org.photovault.dbhelper.ODMGXAWrapper;
import org.photovault.imginfo.ImageInstance;
import org.photovault.imginfo.PhotoInfo;
import  org.photovault.imginfo.Volume;
import java.util.Random;
import javax.sql.DataSource;
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
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.photovault.imginfo.VolumeBase;
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
    
    /**
     * Database type for a Photovault instance which uses an embedded Derby database
     */
    public static final String TYPE_EMBEDDED = "TYPE_EMBEDDED";
    
    /**
     * Database type for a Photovault instance which uses database server for 
     * storing meta data
     */
    public static final String TYPE_SERVER = "TYPE_SERVER";
    
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
    
    /**
     Add a new volume to this database.
     @param volume The new volume
     @throws @see PhotovaultException if another volume with the same name is already present
     */
    public void addVolume( VolumeBase volume ) throws PhotovaultException {
        VolumeBase v = getVolume( volume.getName() );
        if ( v != null ) {
            throw new PhotovaultException( "Volume with name " + volume.getName() +
                    " already exists" );
        }
        volumes.add( volume );
    }
    
    /**
     Removes a volume from this database object. The data in volume is not deleted,
     it is just removed form the volumes known for this database.
     @param volume The volume that will be removed
     */
    public void removeVolume(Volume volume) {
        volumes.remove( volume );
    }
    
    /**
     Get a list of volumes for this database.
     */       
    public List getVolumes( ) {
        return  volumes;
    }

    public VolumeBase getDefaultVolume() {
        VolumeBase vol = null;
        if ( volumes.size() > 0 ) {
            vol = (VolumeBase) volumes.get(0);
        }
        return vol;
    }
        
    
    private String instanceType = TYPE_SERVER;

    /**
     * Get the instacne type of the database This can be either 
     * TYPE_EMBEDDED or TYPE_SERVER
     */
    public String getInstanceType() {
        return instanceType;
    }

    
    public void setInstanceType(String instanceType) {
        if ( instanceType.equals( TYPE_EMBEDDED ) ) {
            this.instanceType = TYPE_EMBEDDED;
        } else if ( instanceType.equals( TYPE_SERVER ) ) {
            this.instanceType = TYPE_SERVER;
        } 
    }    


    private File embeddedDirectory = null;
    
    public File getEmbeddedDirectory() {
        return embeddedDirectory;
    }

    public void setEmbeddedDirectory(File embeddedDirectory) {
        this.embeddedDirectory = embeddedDirectory;
    }
    
    public String getInstancePath() {
        String res = "";
        if ( embeddedDirectory != null ) {
            res = embeddedDirectory.getAbsolutePath();
        }
        return res;
    }

    public void setInstancePath( String path ) {
        embeddedDirectory = new File( path );
    }
    
    /**
     * Returns a volume with the given name or <code>null</code> if it does not exist
     *
     */
    public VolumeBase getVolume( String volumeName ) {
        VolumeBase vol = null;
        
        /* TODO: Should we use HashMap for volumes instead of iterating a list?
         * This method is urgenty needed for a WAR for bug 44 so I am doing it 
         * in the least disruptive way - and since the number of volumes per database 
         * is currently low this may be enough.
         */
        Iterator iter = volumes.iterator();
        while ( iter.hasNext() ) {
            VolumeBase candidate = (VolumeBase) iter.next();
            if ( candidate.getName().equals( volumeName ) ) {
                vol = candidate;
            }
        }
        return vol;
    }


    /**
     Tries to find the volume into which a fiel belongs, i.e. whether it is under 
     the base directory of some volume.
     @param f The file whose volume is of interest
     @return The volume the file belongs to or <code>null</code> if it does not 
     belong to any volume of this database
     @throws IOException if there is an error constructing canonical form of the file
     */
    public VolumeBase getVolumeOfFile( File f ) throws IOException {
        Iterator iter = volumes.iterator();
        VolumeBase v = null;
        while ( iter.hasNext() ) {
            VolumeBase candidate = (VolumeBase) iter.next();
            if ( candidate.isFileInVolume( f ) ) {
                v = candidate;
                break;
            }
        }
        return v;
    }
    
    /**
     * Creates a new database with the parameters specified in this object.
     * <P>
     * The database schema is stored in DDLUTILS XML format in resource 
     * photovault_schema.xml.
     *
     * @param user Username used when creating the SQL database.
     * @param passwd Password for the user
     */
    public void createDatabase( String user, String passwd ) {
        createDatabase( user, passwd, null );
    }
    
    /**
     * Creates a new database with the parameters specified in this object and
     * populate it with given seed data.
     * <P>
     * The database schema is stored in DDLUTILS XML format in resource 
     * photovault_schema.xml.
     *
     * @param user Username used when creating the SQL database.
     * @param passwd Password for the user
     * @param seedDataResource A resource URI that contains data that should be 
     * loaded to the database. This data must be in Apache DDL format. If no 
     * additional seed data is required this must be <code>null</code>
     */
    public void createDatabase( String user, String passwd, String seedDataResource ) {
        
        // Get the database schema XML file
        InputStream schemaIS = getClass().getClassLoader().getResourceAsStream( "photovault_schema.xml" );
        Database dbModel = new DatabaseIO().read( new InputStreamReader( schemaIS ) );
        
        // Create the datasource for accessing this database
        
        String driverName = "com.mysql.jdbc.Driver";
        String dbUrl = "jdbc:mysql://" + getDbHost() + "/" + getDbName();
        
        DataSource ds = null;
        if ( instanceType == TYPE_EMBEDDED ) {
            if ( !embeddedDirectory.exists() ) {
                embeddedDirectory.mkdirs();
            }
            File derbyDir = new File( embeddedDirectory, "derby" );
            File photoDir = new File( embeddedDirectory, "photos");
            Volume vol = new Volume( "photos", photoDir.getAbsolutePath() );
            try {
                addVolume( vol );
            } catch (PhotovaultException ex) {
                // This should not happen since this is the first volume, there cannot be 
                // name conflict!!!
            }
            System.setProperty( "derby.system.home", derbyDir.getAbsolutePath() );
            driverName = "org.apache.derby.jdbc.EmbeddedDriver";
            dbUrl = "jdbc:derby:photovault;create=true";
            EmbeddedDataSource derbyDs = new EmbeddedDataSource();
            derbyDs.setDatabaseName( "photovault" );
            derbyDs.setCreateDatabase( "create" );
            ds = derbyDs;
        } else {
        
            MysqlDataSource mysqlDs = new MysqlDataSource();
            mysqlDs.setURL( dbUrl );
            mysqlDs.setUser( user );
            mysqlDs.setPassword( passwd );
            ds = mysqlDs;
        }
        
        Platform platform = PlatformFactory.createNewPlatformInstance( ds );
        
        /*
         * Do not use delimiters for the database object names in SQL statements.
         * This is to avoid case sensitivity problems with SQL92 compliant 
         * databases like Derby - non-delimited identifiers are interpreted as case 
         *  insensitive.
         *
         * I am not sure if this is the correct way to solve the issue, however,
         * I am not willing to make a big change of schema definitions either.
         */
        platform.getPlatformInfo().setDelimiterToken( "" );
        platform.setUsername( user );
        platform.setPassword( passwd );
        platform.createTables( dbModel, true, true );
        
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
        
        if ( seedDataResource != null ) {
            seedDataStream = this.getClass().getClassLoader().getResourceAsStream( seedDataResource );
            try {
                reader.parse( seedDataStream );
            } catch (SAXException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
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
        dbInfo.set( "schema_version", new Integer( CURRENT_SCHEMA_VERSION ) );
        dbInfo.set( "create_time", new Timestamp( System.currentTimeMillis() ) );
        platform.insert( dbModel, dbInfo );
    }        

    /**
     Returns the schema version of the Photovault database
     */ 
    public int getSchemaVersion() {
        DbInfo info = DbInfo.getDbInfo();
        return info.getVersion();
    }



    /**
     The latest schema version which should be used with this version of 
     Photovault
     */
    static public final int CURRENT_SCHEMA_VERSION = 6;
}
