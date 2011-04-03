/*
  Copyright (c) 2011 Harri Kaimio

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

package org.photovault.imginfo;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

/**
 * Java bean for describing the usage rights of an image
 * @author Harri Kaimio
 * @since 0.6.0
 */
@Embeddable
public class UsageRights {

    private String copyright;
    private String usageTerms;
    private String attributionName;
    private String attributionUrl;
    private License license;
    private PropertyChangeSupport propertySupport;

    public UsageRights() {
        propertySupport = new PropertyChangeSupport( this );
    }

    /**
     * Get the copyright statement for the image
     * @return the copyright
     */

    public String getCopyright() {
        return copyright;
    }

    /**
     * @param copyright the copyright to set
     */
    public void setCopyright( String copyright ) {
        String oldValue = this.copyright;
        this.copyright = copyright;
        propertySupport.firePropertyChange( "copyright", oldValue, copyright );
    }

    /**
     * Get the free form text describing usage terms of the image
     * @return the usageTerms
     */
    public String getUsageTerms() {
        return usageTerms;

    }

    /**
     * @param usageTerms the usageTerms to set
     */
    public void setUsageTerms( String usageTerms ) {
        String oldValue = this.usageTerms;
        this.usageTerms = usageTerms;
        propertySupport.firePropertyChange( "usageTerms", oldValue, usageTerms );
    }

    /**
     * Returns the name that should be used when attributing the image
     * @return the attributionName
     */
    public String getAttributionName() {
        return attributionName;
    }

    /**
     * @param attributionName the attributionName to set
     */
    public void setAttributionName( String attributionName ) {
        String oldValue = this.attributionName;
        this.attributionName = attributionName;
        propertySupport.firePropertyChange( "attributionName", oldValue, attributionName );
    }

    /**
     * Returns the URL that should be used when attributing the image
     * @return 
     */
    public String getAttributionUrl() {
        return attributionUrl;
    }

    /**
     * @param attributionUrl the attributionUrl to set
     */
    public void setAttributionUrl( String attributionUrl ) {
        String oldValue = this.attributionUrl;
        this.attributionUrl = attributionUrl;
        propertySupport.firePropertyChange( "attributionUrl", oldValue, attributionUrl );
    }

    /**
     * Return the license descriptor if the image is licensed under a
     * standard open license
     * @return the license
     */
    @Transient
    public License getLicense() {
        return license;
    }

    /**
     * @param license the license to set
     */
    public void setLicense( License license ) {
        License oldValue = this.license;
        this.license = license;
        propertySupport.firePropertyChange( "license", oldValue, license );
    }

    protected String getLicenseUrn() {
        return license != null ? license.getLicenseUrl() : null;
    }

    protected void setLicenseUrn( String url ) {
        License oldLicense = license;
        license = License.getByUrl( url );
        propertySupport.firePropertyChange( "license", oldLicense, license );
    }

    public void addPropertyChangeListener( PropertyChangeListener l ) {
        propertySupport.addPropertyChangeListener( l );
    }

    public void removePropertyChangeListener( PropertyChangeListener l ) {
        propertySupport.removePropertyChangeListener( l );
    }
}
