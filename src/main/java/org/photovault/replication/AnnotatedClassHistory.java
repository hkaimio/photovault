/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.photovault.replication;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * @author harri
 */
@Entity
@DiscriminatorValue( "annotated" )
public abstract class AnnotatedClassHistory<T> extends ChangeSupport<T> {

    Log log = LogFactory.getLog( AnnotatedClassHistory.class.getName() );
    
    static Map<Class, VersionedClassDesc> analyzedClasses = 
            new HashMap<Class, VersionedClassDesc>();
    
    private VersionedClassDesc classDesc;
    
    public AnnotatedClassHistory( T owner ) {
        super( owner );
        classDesc = analyzedClasses.get( owner.getClass() );
        if ( classDesc == null ) {
            classDesc = new VersionedClassDesc( owner.getClass() );
            analyzedClasses.put(  owner.getClass(), classDesc );
        }
    }

    public AnnotatedClassHistory() {
        super();
    }
    
    
    /**
     Returns the class descriptor for the target object
     @return
     */
    @Transient
    VersionedClassDesc getClassDesc() {
            return classDesc;
    }
    
    @Override
    public void setOwner( T owner ) {
        super.setOwner( owner );
        if ( owner == null ) {
            return;
        }
        classDesc = analyzedClasses.get( owner.getClass() );
        if ( classDesc == null ) {
            classDesc = new VersionedClassDesc( owner.getClass() );
            analyzedClasses.put(  owner.getClass(), classDesc );
        }                
    }
}
