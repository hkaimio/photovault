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

package org.photovault.swingui;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.SwingWorker;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.geocoding.GeocodeProvider;
import org.photovault.geocoding.OSMGeocoder;
import org.photovault.imginfo.location.Location;
import org.photovault.swingui.selection.PhotoSelectionController;
import org.photovault.swingui.selection.FieldController;

/**
 * Action that finds address information for the currently selected photos based
 * on coordinate information. The address lookup is done asynchronously using
 * {@link OSMGeocoder} in a worker thread. If address information is found, it is
 * stored via selection controller only for the fields that have not been set
 * @author harri
 */
class FindAddressAction extends AbstractAction {

    private static final Log log = LogFactory.getLog( FindAddressAction.class );
    final PhotoSelectionController ctrl;
    static final GeocodeProvider geocoder = new OSMGeocoder();

    public FindAddressAction( PhotoSelectionController ctrl ) {
        this.ctrl = ctrl;
    }

    public void actionPerformed( ActionEvent e ) {
        final String geoHashStr = (String) ctrl.getFieldController(
                "shotLocation.geoHashString" ).getValue();
        SwingWorker<Location, Object> w = new SwingWorker<Location, Object>() {

            @Override
            protected Location doInBackground() throws Exception {
                return geocoder.findByGeoHash( geoHashStr );
            }

            @Override
            protected void done() {
                try {
                    updateAddress( get() );
                } catch ( Exception ignore ) {
                }
            }
        };
        w.execute();
    }

    private void updateField( String field, String newValue ) {
        FieldController tc = ctrl.getFieldController( field );
        if ( ( newValue != null && !newValue.equals( "" ) )
                && (tc.getValue() == null || tc.getValue().equals( "" ) ) ) {
            // Change value only if it is not yet set
            tc.setValue( newValue );
        }
    }

    void updateAddress( Location l ) {
        updateField( "shotLocation.adminArea", l.getAdminArea() );
        updateField( "shotLocation.city", l.getCity() );
        updateField( "shotLocation.country", l.getCountry() );
        updateField( "shotLocation.countryCode", l.getCountryCode() );
        updateField( "shotLocation.description", l.getDescription() );
        updateField( "shotLocation.road", l.getRoad() );
        updateField( "shotLocation.roadNumber", l.getRoadNumber() );
        updateField(  "shotLocation.suburb", l.getSuburb() );
    }

}
