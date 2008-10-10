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
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.testng.annotations.Test;
import static org.testng.AssertJUnit.*;
/**
 Test cases for {@link Change} class
 */
public class Test_Change {
    
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
        
        ObjectHistory<TestObject> cs = new ObjectHistory<TestObject>( this );
        
        @History
        public ObjectHistory<TestObject> getHistory() { return cs; };
        
        UUID uuid = UUID.randomUUID();
        
        public Change getVersion() {
            return cs.getVersion();
        }
        
        int f1;
        int f2;
        
        public void setF2( int i ) {
            f2 = i;
        }

        @ValueField
        public int getF2() {
            return f2;
        }
        
        public void setF1( int i ) {
            f1 = i;
        }
        
        @ValueField
        public int getF1() {
            return f1;
        }
        
        Set<Integer> numbers = new HashSet<Integer>();
        
        @SetField( elemClass=int.class )
        public Set<Integer> getNumbers() { return numbers; }
        
        public void addNumber( int n ) { numbers.add( n ); }

        public void removeNumber( int n ) { numbers.remove( n ); }

        
        public UUID getGlobalId() {
            return uuid;
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
        
        
        VersionedObjectEditor<TestObject> e1 = new VersionedObjectEditor<TestObject>(  t, resolvFactory );
           
        e1.setField( "f2", 3 );
        e1.addToSet(  "numbers", 2 );
        e1.addToSet(  "numbers", 3 );
        e1.apply();
        Change<TestObject> c = e1.getChange();
        assertEquals( 3, t.f2 );
        assertEquals( 1, t.f1 );
        assertEquals( c, t.getVersion() );
        
        VersionedObjectEditor<TestObject> e2 = new VersionedObjectEditor<TestObject>(  t, resolvFactory );
        e2.setField("f2", 5 );
        e2.removeFromSet(  "numbers", 2 );
        e2.addToSet(  "numbers", 4 );
        e2.apply();
        assertEquals( 5, t.f2 );
        assertFalse( t.getNumbers().contains( 2 ) );
        assertTrue( t.getNumbers().contains( 3 ) );
        assertTrue( t.getNumbers().contains( 4 ) );
        Change<TestObject> c2 = e2.getChange();
        assertEquals( c2, t.getVersion() );
        assertEquals( c, c2.getPrevChange() );
        assertTrue( c.getChildChanges().contains( c2 ) );
    }
    
    @Test( expectedExceptions={IllegalStateException.class} )
    public void testApplyWrongChange() {
        TestObject t = new TestObject();
        VersionedObjectEditor<TestObject> e1 = new VersionedObjectEditor<TestObject>(  t, resolvFactory );
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
        VersionedObjectEditor<TestObject> e = new VersionedObjectEditor<TestObject>( t, resolvFactory );
        TestObjectEditor te = (TestObjectEditor) e.getProxy();
                
        te.setF1( 1 );
        te.setF2( 2 );
        e.apply();
        assertEquals( 1, t.f1 );
        assertEquals( 2, t.f2 );
        
        Change<TestObject> initialState = e.getChange();
        
        e = new VersionedObjectEditor<TestObject>( t, resolvFactory );
        e.setField( "f1", 2 );
        e.setField( "f2", 3 );
        e.addToSet( "numbers", 1 );
        e.addToSet( "numbers", 2 );        
        e.apply();
        Change<TestObject> c = e.getChange();
        
        e = new VersionedObjectEditor<TestObject>( t, resolvFactory );
        e.setField("f1", 3);
        e.setField("f2", 5);
        e.addToSet( "numbers", 3 );
        e.removeFromSet( "numbers", 1 );
        e.apply();
        Change<TestObject> c2 = e.getChange();

        e = new VersionedObjectEditor<TestObject>( t, resolvFactory );
        e.changeToVersion( c );
        assertEquals( c, t.getVersion() );
        assertEquals( 2, t.f1 );
        assertEquals( 3, t.f2 );
        assertTrue( t.numbers.contains( 1 ) );
        assertFalse( t.numbers.contains( 3 ) );
        
        e.setField("f1", 4);
        e.setField("f2", 5);
        e.addToSet( "numbers", 1 );
        e.addToSet( "numbers", 4 );
        e.removeFromSet(  "numbers", 2 );        
        e.apply();
        Change<TestObject> c3 = e.getChange();
        
        
        Change merged = c2.merge( c3 );
        
        assertTrue( merged.hasConflicts() );
        
        Collection<FieldConflictBase> conflicts = merged.getFieldConficts();
        assertEquals( 2, merged.getFieldConficts().size() );
        ValueFieldConflict f1c = null;
        SetFieldConflict sfc = null;
        // Check the conflicts
        boolean f1Conflict = false;
        boolean f2Conflict = false;
        boolean numbersConflict = false;
        for ( FieldConflictBase cf : conflicts ) {
            if ( cf.getFieldName().equals( "f1" ) ) {
                f1Conflict = true;
                f1c = (ValueFieldConflict) cf;
                assertTrue( f1c.getConflictingValues().contains( 4 ) );
                assertTrue( f1c.getConflictingValues().contains( 3 ) );
            } else if ( cf.getFieldName().equals( "f2" ) ) {
                f2Conflict = true;
            } else if ( cf.getFieldName().equals( "numbers" ) ) {
                sfc = (SetFieldConflict)cf;
                assertEquals( SetOperation.REMOVE, sfc.getOperations().get(0) );
                assertEquals( SetOperation.ADD, sfc.getOperations().get(1) );
                numbersConflict = true;
            }
        }
        assertTrue( f1Conflict );
        assertFalse( f2Conflict );
        assertTrue( numbersConflict );
        
        f1c.resolve( 0 );
        sfc.resolve( 1 );
        assertFalse( merged.hasConflicts() );
        /*
         TODO: current API is broken as we must manually freeze the change after 
         resolving conflicts
         */
        merged.freeze();
        assertEquals( 3, merged.getField("f1") );
        e = new VersionedObjectEditor<TestObject>( t, resolvFactory );
        e.changeToVersion( merged );
        assertTrue( t.numbers.contains( 1 ) );
        assertTrue( t.numbers.contains( 4 ) );
        assertFalse( t.numbers.contains( 2 ) );
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
