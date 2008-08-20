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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.photovault.imginfo.FuzzyDate;
import org.photovault.imginfo.OriginalImageDescriptor;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoChangeSupport;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.persistence.DAOFactory;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;
import org.photovault.replication.Change;
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
    public void setUpTestCase() throws DdlUtilsException, SQLException  {
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
        
    }
    
    @Test
    public void testTest() {
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
        
        PhotoInfoChangeSupport h1 = p1.getHistory();
        Set<Change<PhotoInfo,String>> ch1 = h1.getChanges();
        assertEquals( 2, ch1.size() );
        
    }


}
