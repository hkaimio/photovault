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

package org.photovault.imginfo.xml;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
  This class represents a change done into some Photovault persistent object.
 <p>
 <h3>Persistence</h3>
 The change desciptor is persisted in somewhat peculiar manner:
 <ul>
 <li>The relationship between this change and others as well as the change object
 is persisted in normal Hibernate style</li>
 <li>Information about changed fields (i.e. elements in Hibernate jargon) is 
 persisted into a single database field by serializing the relevant data 
 structure into binary form.</li>
 <li>Information about changes in object relationships (e.g.adding a photo into 
 folder) is again persisted using separate table that links these together.</li>
 </ul>
 Reason for this "mixed" design is to keep the ChangeDesc unaffected by changes 
 to the schema of changed object and at the same time to keep the performance of
 persisting changes as well as the size of persisted changes reasonable.
 */
@Entity
@Table(name = "change_history")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "change_class", discriminatorType = DiscriminatorType.STRING)
@DiscriminatorValue("change")
public abstract class ChangeDesc {

    static private Log log = LogFactory.getLog( ChangeDesc.class.getName() );
    
    /**
     Default constructor for persistence layer.
     */    
    protected ChangeDesc() {
        
    }
    
    /**
     UUID of this change
     */
    private UUID uuid;
    
    /**
     UUID of the changed object
     */
    private UUID targetUuid;
    
    /**
     Changes that precede this one. In normal case a change should be applied on
     top of exactly one change. However, if this is a merge there can be several
     predecessors.
     */
    private Set<ChangeDesc> prevChanges = new HashSet<ChangeDesc>();
    
    /**
     XML representation of this change
     */
    private String xml;

    /**
     Is this change a head (i.e. true if there are no known changes that have 
     this one as predecessor)
     */
    private boolean head = true;
    
    /**
     For future use
     */
    private int changeNum = 0;
    
    /**
     Is this change applied to local data set?
     */
    private boolean applied = false;
    
    /**
     Get the UUID of this change
     @return UUID
     */
    @Id
    @Column( name = "change_uuid" )
    @org.hibernate.annotations.Type( type = "org.photovault.persistence.UUIDUserType" )    
    public UUID getUuid() {
        return uuid;
    }

    
    public void setUuid( UUID uuid ) {
        this.uuid = uuid;
    }

    /**
     Get UUID of the target object
     @return UUID of target.
     */
    @Column(name = "target_uuid")
    @org.hibernate.annotations.Type(type = "org.photovault.persistence.UUIDUserType")
    public UUID getTargetUuid() {
        return targetUuid;
    }

    public void setTargetUuid( UUID targetUuid ) {
        this.targetUuid = targetUuid;
    }

    /**
     Get the predecessors of this change
     @return
     */
     @Transient
    public Set<ChangeDesc> getPrevChanges() {
        return prevChanges;
    }

    protected void setPrevChanges( Set<ChangeDesc> prevChanges ) {
        this.prevChanges = prevChanges;
    }

    /**
     Add a new change as predecessor for this one.
     @param change The change to add
     */
    public void addPrevChange( ChangeDesc change ) {
        prevChanges.add( change );
    }
    
    /**
     Get XML representation of this change
     @return
     */
    @Transient
    public String getXml() {
        return xml;
    }

    @Transient
    protected void setXml( String xml ) {
        this.xml = xml;
    }
    
    /**
     Get the persistent description of this change
     @return Binary persistent description of this change. In practice the 
     fields are serialized using subclass specific writeFields() method
     */
    @Column( name = "changed_fields" )
    public byte[] getChangedFields() {
        byte[] res = null;
        try {
            ByteArrayOutputStream s = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream( s );
            writeFields( os );
            res = s.toByteArray();
        } catch ( IOException ex ) {
            log.debug( ex.getMessage(), ex );
        }
        return res;
    }
    
    /**
     Set the changed fields from persistent data. All of the field change info 
     is serialized into a single database field and parsed using readFields() 
     method (that is subclass specific)
     @param data The serialized field change data
     */
    protected void setChangedFields( byte[] data ) {
        ObjectInputStream os = null;
        try {
            ByteArrayInputStream s = new ByteArrayInputStream( data );
            os = new ObjectInputStream( s );
            readFields( os );
        } catch ( ClassNotFoundException ex ) {
            log.error( ex.getMessage(), ex );
        } catch ( IOException ex ) {
            log.debug( ex.getMessage(), ex );
        } finally {
            try {
                os.close();
            } catch ( IOException ex ) {
                log.debug( ex.getMessage(), ex );
            }
        }
    }

    /**
     Write the change as XML
     @param w Writer in which the change is written
     @param indent Number of spaces to use for indentation
     @throws java.io.IOException If there is error writing the XML data to stream
     */
    public abstract void writeXml( BufferedWriter w, int indent ) throws IOException;
    
    /**
     Serialized the field change data to stream. Dericed classes should override
     this method to provide correct serialization.
     @param os Stream in which the serialized data is written
     @throws java.io.IOException If there is na error writing serialized data
     */
    public abstract void writeFields( ObjectOutputStream os ) throws IOException;
    
    /**
     Deserialized field change data. Derived classes should override this method
     so that it can parse data serialized with writeFields()
     @param os Stream from which the serialized data is read.
     @throws java.io.IOException If there is an error reading data
     @throws java.lang.ClassNotFoundException If field class cannot be 
     instantiated.
     */
    public abstract void readFields( ObjectInputStream os ) 
            throws IOException, ClassNotFoundException;
    
    /**
     Verify that the change matches its UUID.
     @return <code>true</code> if the verification is succesfull, <code>false
     </code> otherwise.
     */
    public abstract boolean verify();
}
