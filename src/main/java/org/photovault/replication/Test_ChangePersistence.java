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

package org.photovault.replication;

import org.hibernate.Transaction;
import org.hibernate.classic.Session;
import org.photovault.imginfo.PhotoInfo;
import org.photovault.imginfo.PhotoInfoChangeSupport;
import org.photovault.imginfo.PhotoInfoFields;
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
        PhotoInfo p = PhotoInfo.create();
        Change<PhotoInfo,PhotoInfoFields> c1 = p.getHistory().createChange();
        c1.freeze();
        Change<PhotoInfo,PhotoInfoFields> c2 = p.getHistory().createChange();
        c2.setField( PhotoInfoFields.CAMERA, "Canon 30D" );
        c2.freeze();
        p.getHistory().changeToVersion( c1 );
        Change<PhotoInfo,PhotoInfoFields> c3 = p.getHistory().createChange();
        c3.setField( PhotoInfoFields.PHOTOGRAPHER, "Harri" );
        c3.freeze();
        Change<PhotoInfo,PhotoInfoFields> c4 = c2.merge(  c3 );
        c4.freeze();
        Transaction tx = session.beginTransaction();
        session.saveOrUpdate( p.getHistory() );
        tx.commit();
        
        Session sess2 = HibernateUtil.getSessionFactory().openSession();
        PhotoInfoChangeSupport s2cs = 
                (PhotoInfoChangeSupport) sess2.get( 
                PhotoInfoChangeSupport.class, p.getUuid() );
        assertEquals( 1, s2cs.getHeads().size() );
        Change<PhotoInfo,PhotoInfoFields> s2c4 = s2cs.getHeads().iterator().next();
        assertEquals( c4.getUuid(), s2c4.getUuid() );
        assertEquals( 2, s2c4.getParentChanges().size() );
        Change<PhotoInfo,PhotoInfoFields> s2c2 = null;
        Change<PhotoInfo,PhotoInfoFields> s2c3 = null;
        for ( Change<PhotoInfo,PhotoInfoFields> c : s2c4.getParentChanges() ) {
            if ( c.getUuid().equals( c2.getUuid() )) {
                s2c2 = c;
            }
            if ( c.getUuid().equals( c3.getUuid() )) {
                s2c3 = c;
            }
        }
        assertNotNull( s2c2 );
        assertNotNull( s2c3 );
        assertTrue( s2c2.getChangedFields().containsKey( PhotoInfoFields.CAMERA ) );
        assertEquals( "Canon 30D", s2c2.getChangedFields().get(  PhotoInfoFields.CAMERA ) );
        sess2.close();
    }
}
