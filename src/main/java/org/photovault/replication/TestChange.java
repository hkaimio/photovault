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
public class TestChange {
    
    static private class TestObjectChangeSupport extends ChangeSupport<TestObject, FieldDescriptor> {

        private Change<TestObject, FieldDescriptor> version;
        
        private TestObjectChangeSupport( TestObject obj ) {
            super( obj );
        }

        @Override
        protected void setField( FieldDescriptor f, Object val  ) {
            f.setValue(target, val);
        }

        @Override
        protected Object getField( FieldDescriptor field ) {
            return field.getValue(target);
        }

        static Set<FieldDescriptor> allFields = null;
        
        @Override
        protected Set<FieldDescriptor> allFields() {
            if ( allFields == null ) {
                allFields = new HashSet<FieldDescriptor>();
                allFields.add( f1Desc );
                allFields.add( f2Desc );
                allFields = Collections.unmodifiableSet(allFields);
            }
            return allFields;
        }

        @Override
        protected void setVersion( Change<TestObject, FieldDescriptor> version ) {
            target.version = version;
        }

        @Override
        protected Change<TestObject, FieldDescriptor> getVersion() {
            return target.version;
        }

        @Override
        public UUID getGlobalId() {
            return target.uuid;
        }

    }
    
    static private class TestObject {

        Change version;
        
        TestObjectChangeSupport cs = new TestObjectChangeSupport( this );
        
        UUID uuid = UUID.randomUUID();
        
        public Change getVersion() {
            return version;
        }

        public void setVersion( Change newVersion ) {
            version = newVersion;
        }
        
        int f1;
        int f2;

        public UUID getGlobalId() {
            return UUID.randomUUID();
        }

        public void changeToVersion( Change newVersion ) {
            cs.changeToVersion(newVersion);
        }

        public List<Change<TestObject, FieldDescriptor>> getChanges() {
            return (List<Change<TestObject, FieldDescriptor>>) cs.getChanges();
        }

        public List<Change<TestObject, FieldDescriptor>> getHeads() {
            return (List<Change<TestObject, FieldDescriptor>>) cs.getHeads();
        }

        public Change mergeHeads() {
            return cs.mergeHeads();
        }

        public Change createChange() {
            return cs.createChange();
        }
        
    }
    
    static private class F1Desc extends FieldDescriptor<TestObject> {
        
        public F1Desc() {
            super( "f1" );
        }

        @Override
        Object getValue( TestObject target ) {
            return ((TestObject)target).f1;
        }

        @Override
        void setValue( TestObject target, Object newValue ) {
            TestObject t = (TestObject)target;
            t.f1 = (Integer)newValue;
        }
        
    }

    static private class F2Desc extends FieldDescriptor<TestObject> {

        public F2Desc() {
            super( "f2" );
        }

        @Override
        Object getValue(  TestObject target ) {
            return target.f2;
        }

        @Override
        void setValue(  TestObject target,  Object newValue ) {
            target.f2 = (Integer) newValue;
        }
    }

    static private FieldDescriptor f1Desc = new F1Desc();
    static private FieldDescriptor f2Desc = new F2Desc();
    
    @Test
    public void testApplyChange() {
        TestObject t = new TestObject();
        t.f1 = 1;
        t.f2 = 2;
        
        Change<TestObject, FieldDescriptor<TestObject>> c = t.createChange();
        c.setField( f2Desc, 3 );
        c.freeze();
        assertEquals( 3, t.f2 );
        assertEquals( 1, t.f1 );
        assertEquals( c, t.getVersion() );
        
        Change<TestObject, FieldDescriptor<TestObject>> c2 = t.createChange();
        c2.setPrevChange( c );
        c2.setField(f2Desc, 5 );
        c2.freeze();
        assertEquals( 5, t.f2 );
        assertEquals( c2, t.getVersion() );
        assertEquals( c, c2.getPrevChange() );
        assertTrue( c.getChildChanges().contains( c2 ) );
    }
    
    @Test( expectedExceptions={IllegalStateException.class} )
    public void testApplyWrongChange() {
        TestObject t = new TestObject();
        t.f1 = 1;
        t.f2 = 2;        
        
        Change<TestObject, FieldDescriptor> initialState = t.createChange();
        initialState.freeze();
        
        Change c = t.createChange();
        c.setPrevChange( t.createChange() );
        c.setField(new F2Desc(), 3 );
        c.freeze();
    }
    
    @Test
    public void testMerge() {
        TestObject t = new TestObject();
        t.f1 = 1;
        t.f2 = 2;
        
        Change<TestObject, FieldDescriptor> initialState = t.createChange();
        initialState.freeze();
        
        Change c = t.createChange();
        c.setField( f1Desc, 2 );
        c.setField( f2Desc, 3 );
        c.freeze();
        
        Change c2 = t.createChange();
        c2.setField(f1Desc, 3);
        c2.setField(f2Desc, 5);
        c2.freeze();

        t.changeToVersion( c );
        assertEquals( c, t.version );
        assertEquals( 2, t.f1 );
        assertEquals( 3, t.f2 );
        
        
        Change c3 = t.createChange();
        c3.setPrevChange( c );
        c3.setField(f1Desc, 4);
        c3.setField(f2Desc, 5);
        c3.freeze();
        
        
        Change merged = c2.merge( c3 );
        
        assertTrue( merged.hasConflicts() );
        
        Collection<FieldConflict> conflicts = merged.getFieldConficts();
        assertEquals( 1, merged.getFieldConficts().size() );
        FieldConflict f1c = null;
        // Check the conflicts
        boolean f1Conflict = false;
        boolean f2Conflict = false;
        for ( FieldConflict cf : conflicts ) {
            if ( cf.getField() == f1Desc ) {
                f1Conflict = true;
                f1c = cf;
                assertTrue( cf.getChanges().contains( c2 ) );
                assertTrue( cf.getChanges().contains( c3 ) );
            } else if ( cf.getField() == f2Desc ) {
                f2Conflict = true;
            }
        }
        assertTrue( f1Conflict );
        assertFalse( f2Conflict );
        
        f1c.resolve( c2 );
        assertFalse( merged.hasConflicts() );
        assertEquals( 3, merged.getField(f1Desc) );
    }
    
    @Test
    public void testSerialize() throws IOException, ClassNotFoundException {
        TestObject t = new TestObject();
        t.f1 = 1;
        t.f2 = 2;   
        
        Change c = t.createChange();
        c.setField( f1Desc, 2 );
        c.setField( f2Desc, 3 );
        c.freeze();
        
        Change c2 = t.createChange();
        c2.setField(f1Desc, 3);
        c2.setField(f2Desc, 5);
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
