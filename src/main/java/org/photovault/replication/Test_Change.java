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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;
/**
 Test cases for {@link Change} class
 */
public class Test_Change {
    
    static private class TestObjectChangeSupport extends ChangeSupport<TestObject> {

        private Change<TestObject> version;
        
        private TestObjectChangeSupport( TestObject obj ) {
            super( obj );
        }

        @Override
        protected void setField( String f, Object val  ) {
            if ( f.equals("f1" ) ) {
                target.f1 = (Integer)val;
            } else if ( f.equals("f2" ) ) {
                target.f2 = (Integer)val;
            }
        }

        @Override
        protected Object getField( String field ) {
            if ( field.equals( "f1")) {
                return target.f1;
            } else if ( field.equals( "f2")) {
                return target.f2;
            } 
            return null;
        }

        static Set<String> allFields = null;
        
        @Override
        protected Set<String> allFields() {
            if ( allFields == null ) {
                allFields = new HashSet<String>();
                allFields.add( "f1" );
                allFields.add( "f2" );
                allFields = Collections.unmodifiableSet(allFields);
            }
            return allFields;
        }

        @Override
        protected void setVersion( Change<TestObject> version ) {
            target.version = version;
        }

        @Override
        protected Change<TestObject> getVersion() {
            return target.version;
        }

        @Override
        public UUID getGlobalId() {
            return target.uuid;
        }

        @Override
        protected TestObject createTarget() {
            throw new UnsupportedOperationException( "Not supported yet." );
        }

    }
    
    static private class TestDtoResolvFactory implements DTOResolverFactory {

        static DTOResolver resolver = new DefaultDtoResolver();
        public DTOResolver getResolver( Class<? extends DTOResolver> clazz ) {
            return resolver;
        }
        
    }
    
    DTOResolverFactory resolvFactory = new TestDtoResolvFactory();
    
    static interface TestObjectEditor {
        public void setF1( int i );
        public void setF2( int i );
    }
    
    @Versioned(editor=TestObjectEditor.class )
    static private class TestObject {

        Change version;
        
        AnnotatedClassHistory<TestObject> cs = new AnnotatedClassHistory<TestObject>( this ) {

            @Override
            protected TestObject createTarget() {
                TestObject o = new TestObject();
                o.cs = this;
                return o;
            }

            @Override
            public UUID getGlobalId() {
                return getTargetUuid();
            }
            
            Change<TestObject> currentVersion = null;

            @Override
            protected void setVersion( Change<TestObject> v ) {
                currentVersion = v;
            }

            @Override
            protected Change<TestObject> getVersion() {
                return currentVersion;
            }
        };
        
        UUID uuid = UUID.randomUUID();
        
        public Change getVersion() {
            return cs.getVersion();
        }
        
        int f1;
        int f2;
        
        @Setter(field="f2")
        public void setF2( int i ) {
            f2 = i;
        }
        
        public int getF2() {
            return f2;
        }
        
        @Setter( field="f1")
        public void setF1( int i ) {
            f1 = i;
        }
        
        public int getF1() {
            return f1;
        }

        public UUID getGlobalId() {
            return uuid;
        }

        public void changeToVersion( Change newVersion ) {
            cs.changeToVersion(newVersion);
        }

        public List<Change<TestObject>> getChanges() {
            return (List<Change<TestObject>>) cs.getChanges();
        }

        public List<Change<TestObject>> getHeads() {
            return (List<Change<TestObject>>) cs.getHeads();
        }

        public Change mergeHeads() {
            return cs.mergeHeads();
        }

        public Change createChange() {
            return cs.createChange();
        }
        
    }
    @Test
    public void testApplyChange() {
        TestObject t = new TestObject();
        t.f1 = 1;
        t.f2 = 2;
        
        
        VersionedObjectEditor<TestObject> e1 = new VersionedObjectEditor<TestObject>(  t.cs, resolvFactory );
           
        e1.setField( "f2", 3 );
        e1.apply();
        Change<TestObject> c = e1.getChange();
        assertEquals( 3, t.f2 );
        assertEquals( 1, t.f1 );
        assertEquals( c, t.getVersion() );
        
        VersionedObjectEditor<TestObject> e2 = new VersionedObjectEditor<TestObject>(  t.cs, resolvFactory );
        e2.setField("f2", 5 );
        e2.apply();
        assertEquals( 5, t.f2 );
        Change<TestObject> c2 = e2.getChange();
        assertEquals( c2, t.getVersion() );
        assertEquals( c, c2.getPrevChange() );
        assertTrue( c.getChildChanges().contains( c2 ) );
    }
    
    @Test( expectedExceptions={IllegalStateException.class} )
    public void testApplyWrongChange() {
        TestObject t = new TestObject();
        VersionedObjectEditor<TestObject> e1 = new VersionedObjectEditor<TestObject>(  t.cs, resolvFactory );
        TestObjectEditor te = (TestObjectEditor) e1.getProxy();
                
        te.setF1( 1 );
        te.setF2( 2 );
        e1.apply();
        
        Change<TestObject> initialState = e1.getChange();
        
        Change<TestObject> c = new Change<TestObject>( t.cs );
        c.freeze();
        Change<TestObject> c2 = new Change<TestObject>( t.cs );        
        c2.setPrevChange( c );
        c.setField("f2", 3 );
        c.freeze();
    }
    
    @Test
    public void testMerge() {
        TestObject t = new TestObject();
        VersionedObjectEditor<TestObject> e = new VersionedObjectEditor<TestObject>( t.cs, resolvFactory );
        TestObjectEditor te = (TestObjectEditor) e.getProxy();
                
        te.setF1( 1 );
        te.setF2( 2 );
        e.apply();
        
        Change<TestObject> initialState = e.getChange();
        
        e = new VersionedObjectEditor<TestObject>( t.cs, resolvFactory );
        e.setField( "f1", 2 );
        e.setField( "f2", 3 );
        e.apply();
        Change<TestObject> c = e.getChange();
        
        e = new VersionedObjectEditor<TestObject>( t.cs, resolvFactory );
        e.setField("f1", 3);
        e.setField("f2", 5);
        e.apply();
        Change<TestObject> c2 = e.getChange();

        e = new VersionedObjectEditor<TestObject>( t.cs, resolvFactory );
        e.changeToVersion( c );
        assertEquals( c, t.getVersion() );
        assertEquals( 2, t.f1 );
        assertEquals( 3, t.f2 );
        
        
        e.setField("f1", 4);
        e.setField("f2", 5);
        e.apply();
        Change<TestObject> c3 = e.getChange();
        
        
        Change merged = c2.merge( c3 );
        
        assertTrue( merged.hasConflicts() );
        
        Collection<FieldConflict> conflicts = merged.getFieldConficts();
        assertEquals( 1, merged.getFieldConficts().size() );
        FieldConflict f1c = null;
        // Check the conflicts
        boolean f1Conflict = false;
        boolean f2Conflict = false;
        for ( FieldConflict cf : conflicts ) {
            if ( cf.getFieldName().equals( "f1" ) ) {
                f1Conflict = true;
                f1c = cf;
                assertTrue( cf.getConflictingValues().contains( 4 ) );
                assertTrue( cf.getConflictingValues().contains( 3 ) );
            } else if ( cf.getFieldName().equals( "f2" ) ) {
                f2Conflict = true;
            }
        }
        assertTrue( f1Conflict );
        assertFalse( f2Conflict );
        
        f1c.resolve( 0 );
        assertFalse( merged.hasConflicts() );
        assertEquals( 3, merged.getField("f1") );
    }
    
    @Test
    public void testSerialize() throws IOException, ClassNotFoundException {
        TestObject t = new TestObject();
        t.f1 = 1;
        t.f2 = 2;   
        
        Change c = t.createChange();
        c.setField( "f1", 2 );
        c.setField( "f2", 3 );
        c.freeze();
        
        Change c2 = t.createChange();
        c2.setField("f1", 3);
        c2.setField("f2", 5);
        c2.freeze();
        
        ByteArrayOutputStream s = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream( s );
        ChangeDTO dto1 = new ChangeDTO( c );
        ChangeDTO dto2 = new ChangeDTO( c2 );
        os.writeObject( dto1 );
        os.writeObject( dto2 );
        
        byte[] serialized = s.toByteArray();
        
        ByteArrayInputStream is = new ByteArrayInputStream( serialized );
        ObjectInputStream ios = new ObjectInputStream( is );
        ChangeDTO readDto1 = (ChangeDTO) ios.readObject();
        ChangeDTO readDto2 = (ChangeDTO) ios.readObject();
        
        readDto1.verify();
        readDto2.verify();
    }

}
