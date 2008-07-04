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

package org.photovault.replication;

import java.awt.geom.Rectangle2D;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.photovault.imginfo.FuzzyDate;
import org.photovault.imginfo.ImageFile;
import org.photovault.imginfo.OriginalImageDescriptor;
import org.photovault.imginfo.PhotoEditor;
import org.photovault.imginfo.PhotoEditorInvocationHandler;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoChangeSupport;
import org.photovault.imginfo.PhotoInfoDAO;
import org.photovault.imginfo.PhotoInfoFields;
import org.photovault.persistence.HibernateDAOFactory;
import org.photovault.persistence.HibernateUtil;
import org.photovault.test.PhotovaultTestCase;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 Test cases for change persistence
 */
public class Test_ChangePersistence extends PhotovaultTestCase {
    private Session session;
    
    public Test_ChangePersistence() {
        super();
    }

    /**
     Sets ut the test environment
     */
    @Override
    @BeforeMethod
    public void setUp() {
        session = HibernateUtil.getSessionFactory().openSession();
    }

    @Override
    @AfterMethod
    public void tearDown() {
        session.close();
    }

    /**
     Create a small grap of changes, store it into database, load it in another 
     session and verify.
     */
    @Test
    public void testChangePersistence() {
        DTOResolverFactory rf = new HibernateDtoResolverFactory( session );
        PhotoInfo p = PhotoInfo.create();
        VersionedObjectEditor<PhotoInfo> e1 = new VersionedObjectEditor( p.getHistory(), rf );
        e1.apply();
        Change<PhotoInfo,String> c1 = e1.change;
        VersionedObjectEditor<PhotoInfo> e2 = new VersionedObjectEditor( p.getHistory(), rf );
        e2.setField( "camera", "Canon 30D" );
        e2.apply();
        Change<PhotoInfo,String> c2 = e2.change;
        p.getHistory().changeToVersion( c1 );
        VersionedObjectEditor<PhotoInfo> e3 = new VersionedObjectEditor( p.getHistory(), rf );
        e3.setField( "photographer", "Harri" );
        e3.apply();
        Change<PhotoInfo,String> c3 = e3.change;
        Change<PhotoInfo,String> c4 = c2.merge(  c3 );
        c4.freeze();
        Transaction tx = session.beginTransaction();
        session.saveOrUpdate( p );
        tx.commit();
        
        Session sess2 = HibernateUtil.getSessionFactory().openSession();
        PhotoInfoChangeSupport s2cs = 
                (PhotoInfoChangeSupport) sess2.get( 
                PhotoInfoChangeSupport.class, p.getUuid() );
        assertEquals( 1, s2cs.getHeads().size() );
        Change<PhotoInfo,String> s2c4 = s2cs.getHeads().iterator().next();
        assertEquals( c4.getUuid(), s2c4.getUuid() );
        assertEquals( 2, s2c4.getParentChanges().size() );
        Change<PhotoInfo,String> s2c2 = null;
        Change<PhotoInfo,String> s2c3 = null;
        for ( Change<PhotoInfo,String> c : s2c4.getParentChanges() ) {
            if ( c.getUuid().equals( c2.getUuid() )) {
                s2c2 = c;
            }
            if ( c.getUuid().equals( c3.getUuid() )) {
                s2c3 = c;
            }
        }
        assertNotNull( s2c2 );
        assertNotNull( s2c3 );
        assertTrue( s2c2.getChangedFields().containsKey( "camera" ) );
        assertEquals( "Canon 30D", s2c2.getChangedFields().get(  "camera" ) );
        sess2.close();
    }
    
    
    /**
     Test that changes can be serialized correctly and merging them to a 
     persistence context using {@link ChangeFactory} is successfull
     @throws java.io.IOException
     @throws java.lang.ClassNotFoundException
     */
    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        DTOResolverFactory rf = new HibernateDtoResolverFactory( session );
        // Create a new photo with associated history
        PhotoInfo p = PhotoInfo.create();
        VersionedObjectEditor<PhotoInfo> e1 = new VersionedObjectEditor( p.getHistory(), rf );
        e1.apply();               
        Change<PhotoInfo,String> c1 = e1.change;
        Transaction tx = session.beginTransaction();
        session.saveOrUpdate( p );
        tx.commit();
        
        /**
         Create a few changes that won't be persisted but serialised to 
         a byte array
         */
        tx = session.beginTransaction();
        HibernateDAOFactory daoFactory = new HibernateDAOFactory();
        daoFactory.setSession( session );
        ChangeDAO<PhotoInfo, String> chDao = daoFactory.getChangeDAO( );
        PhotoInfoDAO photoDAO = daoFactory.getPhotoInfoDAO();
        p = photoDAO.findByUUID( p.getUuid() );
        
        VersionedObjectEditor<PhotoInfo> e2 = new VersionedObjectEditor( p.getHistory(), rf );
        e2.setField( "camera", "Canon 30D" );
        e2.apply();       
        Change<PhotoInfo,String> c2 = e2.change;

        p.getHistory().changeToVersion( c1 );
        VersionedObjectEditor<PhotoInfo> e3 = new VersionedObjectEditor( p.getHistory(), rf );
        e3.setField( "photographer", "Harri" );
        e3.apply();
        Change<PhotoInfo,String> c3 = e3.change;
        
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( os );
        oos.writeObject( new ChangeDTO<PhotoInfo, String>( c1 ) );
        oos.writeObject( new ChangeDTO<PhotoInfo, String>( c2 ) );
        oos.writeObject( new ChangeDTO<PhotoInfo, String>( c3 ) );
        byte[] serialized = os.toByteArray();
        tx.rollback();
        
        // Create a new session so that it is not aware of c2 or c3
        session.close();
        session = HibernateUtil.getSessionFactory().openSession();
        
        daoFactory.setSession( session );
        chDao = daoFactory.getChangeDAO( );
        ChangeFactory<PhotoInfo, String> cf = new ChangeFactory( chDao );
        Change<PhotoInfo, String> s2c1 = chDao.findById( c1.getUuid(), false );
        assertEquals( 0, s2c1.getChildChanges().size() );
        assertEquals( c1.getUuid(), s2c1.getUuid() );
        
        ByteArrayInputStream is = new ByteArrayInputStream(  serialized );
        ObjectInputStream ios = new ObjectInputStream( is );
        Change<PhotoInfo, String> serc1 = cf.readChange( ios );
        assertTrue( serc1 == s2c1 );
        tx = session.beginTransaction();
        Change<PhotoInfo, String> serc2 = cf.readChange( ios );
        Change<PhotoInfo, String> serc3 = cf.readChange( ios );
        assertEquals( c2.getUuid(), serc2.getUuid() );
        assertEquals( c3.getUuid(), serc3.getUuid() );
        assertEquals( 2, s2c1.getChildChanges().size() );
        assertEquals( 3, s2c1.getTargetHistory().getChanges().size() );
        assertEquals( 2, s2c1.getTargetHistory().getHeads().size() );
        tx.commit();
    }
    
    /**
     Test that references to immutable object are serialized correctly.
     
     The test creates (but does not save) a {@link PhotoInfo} that references
     original image in file stored in database. serializes it and creates 
     database instance by deserializing it.
     */
    @Test
    public void testRefSerialization() throws IOException, ClassNotFoundException {
        DTOResolverFactory fieldResolver = new HibernateDtoResolverFactory( session );
        
        // Create image file in the database
        ImageFile ifile = new ImageFile();
        ifile.setFileSize( 100000 );
        ifile.setId( UUID.randomUUID() );
        OriginalImageDescriptor orig = new OriginalImageDescriptor( ifile, "image#0" );
        Transaction tx = session.beginTransaction();
        session.saveOrUpdate( ifile );
        tx.commit();
                
        tx = session.beginTransaction();
        PhotoInfo p = PhotoInfo.create();
        VersionedObjectEditor<PhotoInfo> e1 = new VersionedObjectEditor<PhotoInfo>(  p.getHistory(), fieldResolver );
        e1.setField( "original", orig );
        e1.apply();
        
        VersionedObjectEditor<PhotoInfo> e2 = new VersionedObjectEditor<PhotoInfo>(  p.getHistory(), fieldResolver );
        e2.setField(PhotoInfoFields.PHOTOGRAPHER.getName(), "Harri" );
        e2.setField(PhotoInfoFields.FSTOP.getName(), 5.6 );
        e2.setField( "film", "Tri-X" );
        e2.apply();
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream( os );
        oos.writeObject( new ChangeDTO<PhotoInfo, String>( e1.getChange() ) );
        oos.writeObject( new ChangeDTO<PhotoInfo, String>( e2.getChange() ) );
        byte[] serialized = os.toByteArray();
        tx.rollback();

        // Start a new session
        session.close();
        session = HibernateUtil.getSessionFactory().openSession();
        HibernateDAOFactory daoFactory = new HibernateDAOFactory();
        daoFactory.setSession( session );
        ifile = daoFactory.getImageFileDAO().findById( ifile.getId(), false ); 
        fieldResolver = new HibernateDtoResolverFactory( session );
        
        // Try to deserialize the changes and verify that the photo references
        // the exising image
        ChangeDAO<PhotoInfo,String> chDao = daoFactory.getChangeDAO( );
        ChangeFactory<PhotoInfo, String> cf = new ChangeFactory( chDao );
        
        ByteArrayInputStream is = new ByteArrayInputStream(  serialized );
        ObjectInputStream ios = new ObjectInputStream( is );
        tx = session.beginTransaction();
        Change<PhotoInfo, String> serc1 = cf.readChange( ios );
        Change<PhotoInfo, String> serc2 = cf.readChange( ios );
        VersionedObjectEditor<PhotoInfo> e3 = new VersionedObjectEditor<PhotoInfo>(  (AnnotatedClassHistory<PhotoInfo>) serc1.getTargetHistory(), fieldResolver  );
        e3.changeToVersion( serc2 );
        p = serc1.getTargetHistory().getOwner();
        assert( p.getOriginal().getFile() == ifile );
    }    
    
    @Test
    public void testEditor() {
        PhotoInfo p = PhotoInfo.create();
        Change<PhotoInfo,String> c1 = p.getHistory().createChange();
        c1.freeze();
        Transaction tx = session.beginTransaction();
        session.saveOrUpdate( p );
        tx.commit();
        
        PhotoEditorInvocationHandler ih = 
                new PhotoEditorInvocationHandler( p.getHistory() );
        PhotoEditor e = 
                (PhotoEditor) Proxy.newProxyInstance( 
                PhotoEditor.class.getClassLoader(), new Class[]{PhotoEditor.class}, ih );
        e.setCamera( "Canon FTb" );
        e.setPhotographer( "Harri" );
        e.setCropBounds( new Rectangle2D.Double( 0.1, 0.2, 0.7, 0.8 ) );
        e.setDescription( "Desc" );
        e.setFStop( 5.6 );
        e.setFilm( "Tri-X" );
        e.setFilmSpeed( 400 );
        e.setFocalLength( 50 );
        e.setFuzzyShootTime( new FuzzyDate( new Date(), 10 ) );
        e.setLens( "FD 50mm/2.0" );
        e.setOrigFname( "test.jpg" );
        e.setPrefRotation( 45.0 );
        e.setQuality( 2 );
        e.setShootingPlace( "Rantasalmi" );
        e.setShutterSpeed( 0.008 );
        e.setTechNotes( "Test photo" );
        ih.getChange().freeze();
        Map<String, Object> changes = ih.getChange().getChangedFields();
        assertEquals( "Canon FTb", p.getCamera() );
        assertEquals( "Harri", p.getPhotographer() );
        assertEquals( 5.6, p.getFStop() );
        assertEquals( "Desc", p.getDescription() );
        assertEquals( "Tri-X", p.getFilm() );
        assertEquals( 400, p.getFilmSpeed() );
        assertEquals( 50.0, p.getFocalLength() );
        assertEquals( "FD 50mm/2.0", p.getLens() );
        assertEquals( "test.jpg", p.getOrigFname() );
        assertEquals( 45.0, p.getPrefRotation() );
        assertEquals( 2, p.getQuality() );
        assertEquals( "Rantasalmi", p.getShootingPlace() );
        assertEquals( 0.008, p.getShutterSpeed() );
    }
}
