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

package org.photovault.common;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.ddlutils.DdlUtilsException;
import org.apache.ddlutils.Platform;
import org.apache.ddlutils.PlatformFactory;
import org.apache.ddlutils.io.DataReader;
import org.apache.ddlutils.io.DataToDatabaseSink;
import org.apache.ddlutils.io.DatabaseIO;
import org.apache.ddlutils.model.Database;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.folder.PhotoFolder;
import org.photovault.folder.PhotoFolderDAO;
import org.photovault.imginfo.CopyImageDescriptor;
import org.photovault.imginfo.FileUtils;
import org.photovault.imginfo.FuzzyDate;
import org.photovault.imginfo.ImageOperations;
import org.photovault.imginfo.OriginalImageDescriptor;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;
import org.photovault.replication.Change;
import org.photovault.replication.ChangeSupport;
import org.photovault.test.PhotovaultTestCase;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

/**
 Test cases for migrating from the old (0.5.0) schema to the new one used by 
 0.6.0.
 */
public class Test_NewSchemaMigration extends PhotovaultTestCase {
    
    static Log log = LogFactory.getLog( Test_NewSchemaMigration.class.getName() );
    /** 
     Add the tables from previous schema and polupate with data
     */
    @BeforeClass
    public void setUpTestCase() throws DdlUtilsException, SQLException, IOException  {
        PVDatabase db = PhotovaultSettings.getSettings().getCurrentDatabase();
        int oldVersion = db.getSchemaVersion();
        
        // Find needed information fr DdlUtils
        Session session = HibernateUtil.getSessionFactory().openSession();

        Platform platform = null;
        if ( db.getInstanceType() == PVDatabase.TYPE_EMBEDDED ) {
            platform = PlatformFactory.createNewPlatformInstance( "derby" );            
        } else if ( db.getInstanceType() == PVDatabase.TYPE_SERVER ) {
            platform = PlatformFactory.createNewPlatformInstance( "mysql" );
        }
        platform.getPlatformInfo().setDelimiterToken( "" );
        
        // Get the database schema XML file
        InputStream schemaIS = getClass().getClassLoader().getResourceAsStream( "db_schema_0.5.0.xml" );
        DatabaseIO dbio = new DatabaseIO();
        dbio.setValidateXml( false );
        Database dbModel = dbio.read( new InputStreamReader( schemaIS ) );

        // Alter tables to match corrent schema
        Transaction tx = session.beginTransaction();
        final Connection con = session.connection();
        DataSource ds = new DataSource() {

            public Connection getConnection() throws SQLException {
                return con;
            }

            public Connection getConnection( String arg0, String arg1 ) throws SQLException {
                throw new UnsupportedOperationException( "Not supported yet." );
            }

            public PrintWriter getLogWriter() throws SQLException {
                throw new UnsupportedOperationException( "Not supported yet." );
            }

            public void setLogWriter( PrintWriter arg0 ) throws SQLException {
                throw new UnsupportedOperationException( "Not supported yet." );
            }

            public void setLoginTimeout( int arg0 ) throws SQLException {
                throw new UnsupportedOperationException( "Not supported yet." );
            }

            public int getLoginTimeout() throws SQLException {
                throw new UnsupportedOperationException( "Not supported yet." );
            }

            public <T> T unwrap( Class<T> arg0 ) throws SQLException {
                throw new UnsupportedOperationException( "Not supported yet." );
            }

            public boolean isWrapperFor( Class<?> arg0 ) throws SQLException {
                throw new UnsupportedOperationException( "Not supported yet." );
            }
        };
        platform.createTables( con, dbModel, false, true );
        DbInfo dbinfo = DbInfo.getDbInfo();
        dbinfo.setVersion( 11 );
        session.update( dbinfo );
        session.flush();
        tx.commit();

        
        // Load data
        // Insert the seed data to database
        platform = PlatformFactory.createNewPlatformInstance( "derby" );
        platform.getPlatformInfo().setDelimiterToken( "" );
        platform.setDataSource( ds );
        DataToDatabaseSink sink = new DataToDatabaseSink( platform, dbModel );
        DataReader reader = new DataReader();
        reader.setModel( dbModel );
        reader.setSink( sink );
        Log digesterLog = LogFactory.getLog( org.apache.commons.digester.Digester.class.getName() );
        InputStream seedDataStream = this.getClass().getClassLoader().getResourceAsStream( "migration_test_data_0.5.0.xml" );
        try {
            sink.start();
            reader.parse( seedDataStream );
            sink.end();
        } catch (SAXException ex) {
            log.error( "SAXException: " + ex.getMessage(), ex );
        } catch (IOException ex) {
            log.error( "IOException: " + ex.getMessage(), ex );
        }        
        initTestVolume( db );
        initTestExtVolume( db );
        session.close();
    }
    
    private void initTestVolume( PVDatabase db ) throws IOException {
        File voldir = File.createTempFile( "pv_conversion_testvol", "" );
        voldir.delete();
        voldir.mkdir();
        File d1 = new File( voldir, "2006" );
        File d2 = new File(  d1, "200605" );
        d2.mkdirs();
        File f1 = new File( "testfiles", "test1.jpg" );
        File df1 = new File( d2, "20060516_00002.jpg");
        FileUtils.copyFile( f1, df1);
        File df2 = new File( d2, "20060516_00003.jpg");
        FileUtils.copyFile( f1,df2 );
        PVDatabase.LegacyVolume lvol = 
                new PVDatabase.LegacyVolume( "defaultVolume", voldir.getAbsolutePath() );
        db.addLegacyVolume( lvol );
    }
    
    
    
    private void initTestExtVolume( PVDatabase db ) throws IOException {
        File voldir = File.createTempFile( "pv_conversion_extvol", "" );
        voldir.delete();
        voldir.mkdir();
        File d1 = new File( "testdir2" );
        d1.mkdirs();
        File f1 = new File( "testfiles", "test2.jpg" );
        File df1 = new File( d1, "outi1.jpg");
        FileUtils.copyFile( f1, df1);
        File df2 = new File( d1, "outi10.jpg");
        FileUtils.copyFile( f1,df2 );
        PVDatabase.LegacyVolume lvol = 
                new PVDatabase.LegacyExtVolume( "extvol_photos", voldir.getAbsolutePath(), 4 );
        db.addLegacyVolume( lvol );
    }
    
    @Test
    public void testMigrationToVersioned() {
        SchemaUpdateAction sua = new SchemaUpdateAction( PhotovaultSettings.getSettings().getCurrentDatabase() );
        sua.upgradeDatabase();
        
        // Verify that the photos are persisted correctly
        Session s = HibernateUtil.getSessionFactory().openSession();
        HibernateDAOFactory df = 
                (HibernateDAOFactory) DAOFactory.instance( HibernateDAOFactory.class );
        df.setSession( s );
        PhotoInfoDAO photoDao = df.getPhotoInfoDAO();
        
        PhotoInfo p1 = photoDao.findByUUID( 
                UUID.fromString( "f5aa2e8c-9290-4ce5-9598-e311ed51668d") );
        OriginalImageDescriptor o1 = p1.getOriginal();
        assertEquals( 1850, o1.getWidth() );
        assertEquals( 2770, o1.getHeight() );
        assertEquals( "Place1", p1.getShootingPlace() );
        assertEquals( "Harri Kaimio", p1.getPhotographer() );
        FuzzyDate fd = p1.getFuzzyShootTime();
        assertEquals( 0.5, fd.getAccuracy(), 0.001 );
        
        ChangeSupport<PhotoInfo> h1 = p1.getHistory();
        Set<Change<PhotoInfo>> ch1 = h1.getChanges();
        assertEquals( 2, ch1.size() );
        assertNull( p1.getRawSettings() );
        
        Set<PhotoFolder> p1folders = p1.getFolders();
        assertEquals( 2, p1folders.size() );
        PhotoFolderDAO folderDao = df.getPhotoFolderDAO();
        PhotoFolder f1 = folderDao.findById( 
                UUID.fromString( "15283a2c-1000-4b51-ac45-ba250cca551b"), false );
        PhotoFolder f2 = folderDao.findById( 
                UUID.fromString( "0e09a6a8-f34b-4a28-9430-ae722e7f2767"), false );
        assertTrue( p1folders.contains( f1 ) );
        assertTrue( p1folders.contains( f2 ) );

        // Photo with raw image
        PhotoInfo p2 = photoDao.findByUUID( 
                UUID.fromString( "e3f4b466-d1a3-48c1-ac86-01d9babf373f") );
        assertEquals( 1, p2.getFolders().size() );
        assertTrue( p2.getFolders().contains( f2 ) );
        RawConversionSettings r2 = p2.getRawSettings();
        assertEquals( 31347, r2.getWhite() );
        assertEquals( 0.5, r2.getHighlightCompression() );
        
        OriginalImageDescriptor o2 = p2.getOriginal();
        CopyImageDescriptor t2 = 
                (CopyImageDescriptor) p2.getPreferredImage( 
                EnumSet.of( ImageOperations.RAW_CONVERSION ), 
                EnumSet.allOf( ImageOperations.class ), 
                66, 66, 200, 200 );
        
        assertEquals( r2, t2.getRawSettings() );
        boolean f = false;
        for ( CopyImageDescriptor c : o2.getCopies() ) {
            if ( c != t2 ) {
                assertEquals( 17847, c.getRawSettings().getWhite() );
                f = true;
            }
        }
        assertTrue( f );
        s.close();
    }


}
