/*
  Copyright (c) 2006-2007 Harri Kaimio
  
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

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.io.InputStream;
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
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.photovault.imginfo.VolumeBase;
import org.xml.sax.SAXException;
import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import java.sql.Connection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ddlutils.DatabaseOperationException;
import org.apache.derby.jdbc.EmbeddedConnectionPoolDataSource;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.photovault.folder.PhotoFolder;
import org.photovault.imginfo.VolumeManager;
import org.photovault.persistence.HibernateUtil;

/**
 * PVDatabase represents metadata about a single Photrovault database. A database 
 * consists of an SQL database for storing photo metadata and 1 or more volumes 
 * for storing the actual photos
 * @author Harri Kaimio
 */
public class PVDatabase {
    
    static private Log log = LogFactory.getLog( PVDatabase.class.getName() );
    
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
        volumes = new ArrayList<VolumeBase>();
    }
    
    private String name;
    private String dbHost = "";
    private String dbName = "";
    private ArrayList<VolumeBase> volumes;

    /**
     UUID of the default volume.
     */
    private UUID defaultVolumeId;
    
    public void setName( String name ) {
        this.name = name;
    }
    
    public String getName() {
        return this.name;
    }
    
    public String getHost() {
        return dbHost;
    }

    public void setHost(String dbHost) {
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
     @deprecated For configuration file parsing backward compatibility only.
     */
    public void addVolume( VolumeBase volume ) throws PhotovaultException {
        VolumeBase v = getVolume( volume.getName() );
        if ( v != null ) {
            throw new PhotovaultException( "Volume with name " + volume.getName() +
                    " already exists" );
        }
        volumes.add( volume );
        
        // Ensure that this volume's basedir is in mount point list
        if ( volume.getBaseDir() != null ) {
            mountPoints.add( volume.getBaseDir() );
        }
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
     Mount points in which volumes for this database can be mounted.
     */
    Set<File> mountPoints = new HashSet<File>();
    
    /**
     Add a new mount point for volumes
     
     @param path Absolute path to the volume mount point.
     */
    public void addMountPoint( String path ) {
        if ( path != null ) {
            mountPoints.add( new File( path ) );
        }
    }
    
    /**
     Get mount points for volumes of this database
     @return
     */
    public Set<File> getMountPoints() {
        return mountPoints;
    }
    
    /**
     Get a list of volumes for this database.
     @deprecated Use VolumeDAO intead
     */       
    public List getVolumes( ) {
        return  volumes;
    }

    /**
     Get default volume
     @deprecated Use VolumeDAO#getDefaultVolume instead
     */
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

    /**
     Path to the directory where data for this database is stored
     
     */
    private File dataDirectory = null;
    
    /**
     Get the data directory of this database. In case of Derby based database,
     both the database and photos reside in this directory. In case of MySQL, 
     only photos are stored here (database resider in server)
     @return Path to data directory.
     */
    public File getDataDirectory() {
        return dataDirectory;
    }

    /**
     Set the data directory for the database.
     @param embeddedDirectory The directory
     */
    public void setDataDirectory( File embeddedDirectory ) {
        this.dataDirectory = embeddedDirectory;
    }
    
    public String getInstanceDir() {
        String res = "";
        if ( dataDirectory != null ) {
            res = dataDirectory.getAbsolutePath();
        }
        return res;
    }

    public void setInstanceDir( String path ) {
        dataDirectory = new File( path );
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
        
        try {
            HibernateUtil.init( user, passwd, this );
        } catch ( PhotovaultException e ) {
            log.error( e.getMessage(), e );
        }
        SchemaExport schexport = new SchemaExport( HibernateUtil.getConfiguration() );
        schexport.create( true, true );
        
        Session s = HibernateUtil.getSessionFactory().openSession();
        Transaction tr = s.beginTransaction();
        
        PhotoFolder topFolder = PhotoFolder.create( PhotoFolder.ROOT_UUID, null );
        topFolder.setName( "Top" );
        s.save( topFolder );
        
//        InputStream schemaIS = getClass().getClassLoader().getResourceAsStream( "photovault_schema.xml" );
//        DatabaseIO dbio = new DatabaseIO();
//        dbio.setValidateXml( false );
//        Database dbModel = dbio.read( new InputStreamReader( schemaIS ) );
//        
//        // Create the datasource for accessing this database
//        
//        String driverName = "com.mysql.jdbc.Driver";
//        String dbUrl = "jdbc:mysql://" + getHost() + "/" + getDbName();
//        
//        DataSource ds = null;
//        if ( !dataDirectory.exists() ) {
//            dataDirectory.mkdirs();
//        }
//        File defVolDir = dataDirectory;
//        
//        if ( instanceType == TYPE_EMBEDDED ) {
//            File derbyDir = new File( dataDirectory, "derby"  );
//            defVolDir = new File( dataDirectory, "photos");
//            System.setProperty( "derby.system.home", derbyDir.getAbsolutePath() );
//            driverName = "org.apache.derby.jdbc.EmbeddedDriver";
//            dbUrl = "jdbc:derby:photovault;create=true";
//            EmbeddedDataSource derbyDs = new EmbeddedConnectionPoolDataSource();
//            derbyDs.setDatabaseName( "photovault" );
//            derbyDs.setCreateDatabase( "create" );
//            ds = derbyDs;
//        } else {
//            MysqlDataSource mysqlDs = new MysqlDataSource();
//            mysqlDs.setURL( dbUrl );
//            mysqlDs.setUser( user );
//            mysqlDs.setPassword( passwd );
//            ds = mysqlDs;
//        }
//        
//        Platform platform = PlatformFactory.createNewPlatformInstance( ds );
//        
//        /*
//         * Do not use delimiters for the database object names in SQL statements.
//         * This is to avoid case sensitivity problems with SQL92 compliant 
//         * databases like Derby - non-delimited identifiers are interpreted as case 
//         *  insensitive.
//         *
//         * I am not sure if this is the correct way to solve the issue, however,
//         * I am not willing to make a big change of schema definitions either.
//         */
//        platform.getPlatformInfo().setDelimiterToken( "" );
//        platform.setUsername( user );
//        platform.setPassword( passwd );
//        platform.createTables( dbModel, true, true );
//        
//        // Insert the seed data to database
//        DataToDatabaseSink sink = new DataToDatabaseSink( platform, dbModel );
//        DataReader reader = new DataReader();
//        reader.setModel( dbModel );
//        reader.setSink( sink );
//        
//
//        InputStream seedDataStream = this.getClass().getClassLoader().getResourceAsStream( "photovault_seed_data.xml" );
//        try {
//            reader.parse( seedDataStream );
//        } catch (SAXException ex) {
//            ex.printStackTrace();
//        } catch (IOException ex) {
//            ex.printStackTrace();
//        }
//        
//        if ( seedDataResource != null ) {
//            seedDataStream = this.getClass().getClassLoader().getResourceAsStream( seedDataResource );
//            try {
//                reader.parse( seedDataStream );
//            } catch (SAXException ex) {
//                ex.printStackTrace();
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
//        }
//        
//        // Create the database 
//        
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
        
        // Create default volume
        Volume defVol = new Volume();
        defVol.setName( "default_volume" );
        s.save( defVol );
        
        DbInfo dbInfo = new DbInfo();
        dbInfo.setCreateTime( new Date() );
        dbInfo.setId( idStr );
        dbInfo.setVersion( CURRENT_SCHEMA_VERSION );
        s.save( dbInfo );
        
        s.flush();
        tr.commit();
        s.close();
        
//        try {
//            DynaBean volInfo = dbModel.createDynaBeanFor( "pv_volumes", false );
//            volInfo.set( "volume_id", defVol.getId().toString() );
//            volInfo.set( "volume_type", "volume" );
//            volInfo.set( "volume_name", defVol.getName() );
//            platform.insert( dbModel, volInfo );
//            VolumeManager.instance().initVolume( defVol, defVolDir );
//            mountPoints.add(  defVolDir );
//
//            DynaBean dbInfo = dbModel.createDynaBeanFor( "database_info", false );
//            dbInfo.set( "database_id", idStr );
//            dbInfo.set( "schema_version", new Integer( CURRENT_SCHEMA_VERSION ) );
//            dbInfo.set( "create_time", new Timestamp( System.currentTimeMillis() ) );
//            dbInfo.set( "default_volume_id", defVol.getId().toString() );
//            platform.insert( dbModel, dbInfo );
//        } catch ( PhotovaultException e ) {
//            log.error( "Error storing volume information to database", e );
//        } catch ( DatabaseOperationException e ) {
//            log.error( "DdlUtils error while initializing database: ", e );
//        }
    }

    /**
     Returns the schema version of the Photovault database
     */ 
    public int getSchemaVersion() {
        DbInfo info = DbInfo.getDbInfo();
        return info.getVersion();
    }

    /**
     Write this object as an XML element
     @param outputWriter The writer into which the object is written
     @param indent Number of spaces to indent each line
     */
    void writeXml(BufferedWriter outputWriter, int indent ) throws IOException {
        String s = "                                ".substring( 0, indent );
        outputWriter.write( s+ "<database name=\"" + name + 
                "\" instanceType=\"" + getInstanceType() +
                "\" instanceDir=\"" + getInstanceDir() + "\"" );
        if ( instanceType == PVDatabase.TYPE_SERVER ) {
            outputWriter.write( " host=\"" + dbHost + 
                    "\" dbName=\"" + dbName + "\"");
        }        
        outputWriter.write( ">\n" );
        outputWriter.write( String.format( "%s  <volume-mounts>\n", s ) );
        for ( File mount : mountPoints ) {
            outputWriter.write( String.format( 
                    "%s    <mountpoint dir=\"%s\"/>\n", 
                    s, mount.getAbsolutePath() ) );
        }
        outputWriter.write( String.format( "%s  </volume-mounts>\n", s ) );
        
        Iterator iter = volumes.iterator();
        outputWriter.write( s + "  " + "<volumes>\n" );
        while( iter.hasNext() ) {
            VolumeBase v = (VolumeBase) iter.next();
            v.writeXml( outputWriter, indent+4 );            
        }
        outputWriter.write( s + "  " + "</volumes>\n" );
        outputWriter.write( s + "</database>\n" );
    }



    /**
     The latest schema version which should be used with this version of 
     Photovault
     */
    static public final int CURRENT_SCHEMA_VERSION = 12;
}
