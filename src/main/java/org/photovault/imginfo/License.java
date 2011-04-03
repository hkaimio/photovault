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

import com.google.protobuf.Message;
import java.util.EnumSet;
import org.photovault.imginfo.dto.ImageProtos;

/**
 * Enumeration of standard licenses used for images
 * @author harri
 */
public enum License {

    /**
     * Creative Commons Attribution license
     */
    CC_BY( "CC Attribution", "http://creativecommons.org/licenses/by/3.0" ),
    /**
     * Creative Commons Attribution, Share Alike license
     */
    CC_BY_SA( "CC Attribution, Share Alike", "http://creativecommons.org/licenses/by-sa/3.0" ),
    /**
     * Creative Commons Attribution, No Derivative Works license
     */
    CC_BY_ND( "CC Attribution, No Derivative Works", "http://creativecommons.org/licenses/by-nd/3.0"),
    /**
     * Creative Commons Attribution, Non-commercial license
     */
    CC_BY_NC( "CC Attribution, Non-commercial", "http://creativecommons.org/licenses/by-nc/3.0"),
    /**
     * Creative Commons Attribution, Non-commercial, Share Alike license
     */
    CC_BY_NC_SA( "CC Attribution, Non-commercial, Share Alike", "http://creativecommons.org/licenses/by-nc-sa/3.0" ),
    /**
     * Creative Commons Attribution, Non-commercial, No Derivative Works license
     */
    CC_BY_NC_ND( "CC Attribution, Non-commercial, No Derivative Works", "http://creativecommons.org/licenses/by-nc-nd/3.0" ),

    /**
     * Public domain content, with no known copyright
     */
    PD( "Public domain (no known copyright)", "https://creativecommons.org/about/pdm" ),

    /**
     * Content set to public domain by copyright holder
     */
    CC0( "Public domain (waiving existing rights)", "https://creativecommons.org/about/cc0" );

    private String licenseUrl;
    private String licenseName;

    License( String name, String url ) {
        licenseUrl = url;
        licenseName = name;
    }

    /**
     * Get the URN of the license
     * @return
     */
    public String getLicenseUrl() {
        return licenseUrl;
    }

    /**
     * Get name of the license
     * @return
     */
    public String getLicenseName() {
        return licenseName;
    }
    
    @Override
    public String toString() {
        return licenseName;
    }


    public static License getByUrl( String url ) {
        if ( url == null ) {
            return null;
        }
        for ( License l : EnumSet.allOf( License.class ) ) {
            if ( l.getLicenseUrl().equals( url ) ) {
                return l;
            }
        }
        return null;
    }

    static public class ProtobufConv implements ProtobufConverter<License> {

        public Message createMessage( License l ) {
            ImageProtos.License.Builder b = ImageProtos.License.newBuilder();
            b.setLicenseName( l.licenseName );
            b.setLicenseUrn( l.licenseUrl );
            return b.build();
        }

        public License createObject( Message msg ) {
            ImageProtos.License lmsg = (ImageProtos.License) msg;
            return License.getByUrl( lmsg.getLicenseUrn() );
        }

    }

}
