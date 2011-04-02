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


package org.photovault.imginfo.location;

import ch.hsr.geohash.GeoHash;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import javax.persistence.Embeddable;
import javax.persistence.Transient;

/**
 * Structure to represent location related information, either outdoors or indoors
 * @author Harri Kaimio
 * @since 0.6.0
 */
@Embeddable
public class Location {


    private LocationType locationType = LocationType.UNDEFINED;
    private String description;
    private String room;
    private Integer floor;
    private String building;
    private String roadNumber;
    private String road;
    private String suburb;
    private String city;
    private String adminArea;
    private String country;
    private String countryCode;
    private GeoHash coordinate;
    
    private PropertyChangeSupport beanSupport;


    public Location() {
        beanSupport = new PropertyChangeSupport( this );
    }

    /**
     * Get the free-form description of the location.
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Set thedescription of the location
     * @param description the description to set
     */
    public void setDescription( String description ) {
        String oldDescription = this.description;
        this.description = description;
        beanSupport.firePropertyChange( "description", oldDescription, description );
    }

    /**
     * Get the road of the location
     * @return the road, or null if not set
     */
    public String getRoad() {
        return road;
    }

    /**
     * @param road the road to set
     */
    public void setRoad( String road ) {
        String oldRoad = this.road;
        this.road = road;
        beanSupport.firePropertyChange( "road", oldRoad, road );
    }

    /**
     * Get the suburb in which the location resides
     * @return the suburb
     */
    public String getSuburb() {
        return suburb;
    }

    /**
     * @param suburb the suburb to set
     */
    public void setSuburb( String suburb ) {
        String oldSuburb = this.suburb;
        this.suburb = suburb;
        beanSupport.firePropertyChange( "suburb", oldSuburb, suburb );
    }

    /**
     * @return the city
     */
    public String getCity() {
        return city;
    }

    /**
     * @param city the city to set
     */
    public void setCity( String city ) {
        String oldCity = this.city;
        this.city = city;
        beanSupport.firePropertyChange( "city", oldCity, city );
    }

    /**
     * @return the adminArea
     */
    public String getAdminArea() {
        return adminArea;
    }

    /**
     * @param adminArea the adminArea to set
     */
    public void setAdminArea( String adminArea ) {
        String oldAdminArea = this.adminArea;
        this.adminArea = adminArea;
        beanSupport.firePropertyChange( "adminArea", oldAdminArea, adminArea );
    }

    /**
     * @return the country
     */
    public String getCountry() {
        return country;
    }

    /**
     * @param country the country to set
     */
    public void setCountry( String country ) {
        String oldCountry = this.country;
        this.country = country;
        beanSupport.firePropertyChange( "country", oldCountry, country );

    }

    /**
     * @return the countryCode
     */
    public String getCountryCode() {
        return countryCode;
    }

    /**
     * @param countryCode the countryCode to set
     */
    public void setCountryCode( String countryCode ) {
        String oldCountryCode = this.countryCode;
        this.countryCode = countryCode;
        beanSupport.firePropertyChange( "countryCode", oldCountryCode, countryCode );
    }

    /**
     * @return the coordinate
     */
    @Transient
    public GeoHash getCoordinate() {
        return coordinate;
    }

    /**
     * @param coordinate the coordinate to set
     */
    public void setCoordinate( GeoHash coordinate ) {
        GeoHash oldCoordinate = this.coordinate;
        this.coordinate = coordinate;
        beanSupport.firePropertyChange( "coordinate", oldCoordinate, coordinate );

    }

    public String getGeoHashString() {
        return ( coordinate != null ) ? coordinate.toBase32() : null;
    }

    public void setGeoHashString( String str ) {
        if ( str != null ) {
            setCoordinate( GeoHash.fromGeohashString( str ) );
        } else {
            setCoordinate( null );
        }
    }

    /**
     * @return the locationType
     */
    public LocationType getLocationType() {
        return locationType;
    }

    /**
     * @param locationType the locationType to set
     */
    public void setLocationType( LocationType locationType ) {
        LocationType oldLocationType = this.locationType;
        this.locationType = locationType;
        beanSupport.firePropertyChange( "locationType", oldLocationType, locationType );

    }

    /**
     * @return the room
     */
    public String getRoom() {
        return room;
    }

    /**
     * @param room the room to set
     */
    public void setRoom( String room ) {
        String oldRoom = this.room;
        this.room = room;
        beanSupport.firePropertyChange( "room", oldRoom, room );
    }

    /**
     * @return the floor
     */
    public Integer getFloor() {
        return floor;
    }

    /**
     * @param floor the floor to set
     */
    public void setFloor( Integer floor ) {
        Integer oldFloor = this.floor;
        this.floor = floor;
        beanSupport.firePropertyChange( "floor", oldFloor, floor );
    }

    /**
     * @return the building
     */
    public String getBuilding() {
        return building;
    }

    /**
     * @param building the building to set
     */
    public void setBuilding( String building ) {
        String oldBuilding = this.building;
        this.building = building;
        beanSupport.firePropertyChange( "building", oldBuilding, building );

    }

    /**
     * @return the roadNumber
     */
    public String getRoadNumber() {
        return roadNumber;
    }

    /**
     * @param roadNumber the roadNumber to set
     */
    public void setRoadNumber( String roadNumber ) {
        String oldRoadNumber = this.roadNumber;
        this.roadNumber = roadNumber;
        beanSupport.firePropertyChange( "roadNumber", oldRoadNumber, roadNumber );
    }

    public void addPropertyChangeListener( PropertyChangeListener listener ) {
        beanSupport.addPropertyChangeListener( listener );
    }

    public void removePropertyChangeListener( PropertyChangeListener listener ) {
        beanSupport.removePropertyChangeListener( listener );
    }

}
