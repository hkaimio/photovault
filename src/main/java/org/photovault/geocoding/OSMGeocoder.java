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

package org.photovault.geocoding;

import ch.hsr.geohash.GeoHash;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.photovault.imginfo.location.Location;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Geocode provider that uses the Open Street Maps API for finding the location
 * information
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class OSMGeocoder implements GeocodeProvider {

    private static Log log = LogFactory.getLog( OSMGeocoder.class );

    private XPathExpression reverseAddrExpr = null;
    private XPathExpression reverseHouseExpr = null;
    private XPathExpression reverseRoadExpr = null;
    private XPathExpression reverseSuburbExpr = null;
    private XPathExpression reverseCityExpr = null;
    private XPathExpression reverseCountryExpr = null;
    private XPathExpression reverseCountryCodeExpr = null;

    public OSMGeocoder() {
        XPathFactory xpf = XPathFactory.newInstance();
        XPath path = xpf.newXPath();
        try {
        reverseAddrExpr = path.compile( "/reversegeocode/result/text()" );
        reverseHouseExpr = path.compile( "/reversegeocode/addressparts/house/text()" );
        reverseRoadExpr = path.compile( "/reversegeocode/addressparts/road/text()" );
        reverseSuburbExpr = path.compile( "/reversegeocode/addressparts/suburb/text()" );
        reverseCityExpr = path.compile( "/reversegeocode/addressparts/city/text()" );
        reverseCountryExpr = path.compile( "/reversegeocode/addressparts/country/text()" );
        reverseCountryCodeExpr = path.compile( "/reversegeocode/addressparts/country_code/text()" );
        } catch ( XPathExpressionException e ) {
            log.error( e );
        }
    }


    public Location findLocation( String addr ) {
        String urlbase = "http://nominatim.openstreetmap.org/search?q=";
        try {
            String encoded = urlbase + URLEncoder.encode( addr, "utf-8" )
                    + "&format=xml&addressdetails=1";
            URL url = new URL( encoded );
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream is = conn.getInputStream();
            BufferedReader rd = new BufferedReader( new InputStreamReader( conn.
                    getInputStream() ) );
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware( true ); // never forget this!
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse( is );
            parsePlaces( doc );
        } catch ( IOException e ) {
            log.error( e );
        } catch ( ParserConfigurationException e ) {
            log.error( e );
        } catch ( SAXException e ) {
            log.error( e );
        }
        return null;
    }

    private List<Location> parsePlaces( Document doc ) {
        List<Location> ret = new ArrayList();
        XPathFactory xpf = XPathFactory.newInstance();
        XPath nodesPath = xpf.newXPath();
        try {
        NodeList placeNodes = (NodeList) nodesPath.evaluate(
                "/searchresults/place", doc, XPathConstants.NODESET );
        for ( int n = 0 ; n < placeNodes.getLength() ; n++ ) {
            Location loc = new Location();
            Node placeNode = placeNodes.item( n );
            String road = nodesPath.evaluate( "//road/text()", placeNode );
            String suburb = nodesPath.evaluate( "//suburb/text()", placeNode );
            String city = nodesPath.evaluate( "//city/text()", placeNode );
            String county = nodesPath.evaluate( "//county/text()", placeNode );
            String country = nodesPath.evaluate( "//country/text()", placeNode );
            String countryCode = nodesPath.evaluate( "//country_code/text()", placeNode );
            String latStr = nodesPath.evaluate( "@lat", placeNode );
            String lonStr = nodesPath.evaluate( "@lon", placeNode );
            String desc = nodesPath.evaluate( "@display_name", placeNode );
            loc.setRoad( road );
            loc.setSuburb( suburb );
            loc.setCity( city );
            loc.setCountry( country );
            loc.setCountryCode( countryCode );
            double lat = Double.parseDouble( latStr );
            double lon = Double.parseDouble( lonStr );
            GeoHash geohash = GeoHash.withBitPrecision( lat, lon, 60 );
            loc.setCoordinate( geohash );
            ret.add( loc );
        }

        } catch ( XPathExpressionException e ) {
            log.error( e );
        }
        return ret;

    }



    public Location findByGeoHash( String geohashStr ) {
        GeoHash geohash = GeoHash.fromGeohashString( geohashStr );
        double lat = geohash.getPoint().getLatitude();
        double lon = geohash.getPoint().getLongitude();
        String urlbase = "http://nominatim.openstreetmap.org/reverse?";
        Location ret = null;
        try {
            String encoded = urlbase + "lat=" + lat + "&lon=" + lon +
                    "&format=xml&addressdetails=1&zoom=18";
            URL url = new URL( encoded );
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            InputStream is = conn.getInputStream();
            BufferedReader rd = new BufferedReader( new InputStreamReader( conn.
                    getInputStream() ) );
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware( true ); // never forget this!
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse( is );
            ret = parseReverseResult( doc );
            ret.setCoordinate( geohash );
        } catch ( IOException e ) {
            log.error( e );
        } catch ( ParserConfigurationException e ) {
            log.error( e );
        } catch ( SAXException e ) {
            log.error( e );
        }
        return ret;
    }

    Location parseReverseResult( Document doc ) {
        Location ret = new Location();
        try {
            String address = reverseAddrExpr.evaluate( doc );
            ret.setDescription( address );
            String house = reverseHouseExpr.evaluate( doc );
            ret.setBuilding( house );
            String road = reverseRoadExpr.evaluate( doc );
            ret.setRoad( road );
            String suburb = reverseSuburbExpr.evaluate( doc );
            ret.setSuburb( suburb );
            String city = reverseCityExpr.evaluate( doc );
            ret.setCity( city );
            String country = reverseCountryExpr.evaluate( doc );
            ret.setCountry( country );
            String countryCode = reverseCountryCodeExpr.evaluate( doc );
            ret.setCountryCode( countryCode );
        } catch ( XPathExpressionException e ) {
            log.error( e );
        }
        return ret;
    }

    static public void main( String args[] ) {
        GeocodeProvider p = new OSMGeocoder();
        p.findLocation( "Mannerheimintie,Finland" );

        Location l = p.findByGeoHash( "ud9wzn7vxgrs" );
        System.out.println( l.getDescription() );
    }
}
