/*
  Copyright (c) 2007 Harri Kaimio
  
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

import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
  This class represents a change done into some Photovault persistent object
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

    
    protected void setUuid( UUID uuid ) {
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

    protected void setTargetUuid( UUID targetUuid ) {
        this.targetUuid = targetUuid;
    }

    /**
     Get the predecessors of this change
     @return
     */
    @OneToMany( cascade = CascadeType.ALL )
    @JoinTable( name = "change_relations",
                joinColumns = {@JoinColumn( name = "change_uuid" ) },
                inverseJoinColumns = {@JoinColumn( name = "prev_change_uuid" ) } )
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
    protected void addPrevChange( ChangeDesc change ) {
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
     @return BInary persistent description of this change. In practice this is 
     the XML description encoded with specific encoding and (optionally) 
     compressed.
     */
    @Column( name = "change_desc" )
    public byte[] getChangeDesc() {
        try {
            if ( xml != null ) {
                return xml.getBytes( "utf-8" );
            }
        } catch ( UnsupportedEncodingException ex ) {
            log.error( ex.getMessage() );
        }
        return null;
    }
    
    protected void setChangeDesc( byte[] data ) {
        try {
            if ( data != null ) {
                xml = new String( data, "utf-8" );
            }
        } catch ( UnsupportedEncodingException ex ) {
            log.debug( ex );
        }
    }

    /**
     Get the encoding of changeDesc
     @return
     */
    @Column( name = "change_desc_encoding")
    public String getChangeDescEncoding() {
        return "utf-8";
    }
    
    protected void  setChangeDescEncoding( String encoding ) {
    }
        
    public abstract boolean verify();
}
