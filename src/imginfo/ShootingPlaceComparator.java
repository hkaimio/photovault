/*
 * ShootingPlaceComparator.java
 *
 * Created on 29. tammikuuta 2006, 9:39
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package imginfo;

import java.util.Comparator;
import java.util.Date;
    
/**
 * Comparator that orders the photos based on their Shooting place.
 */
public class ShootingPlaceComparator implements Comparator {
    public int compare( Object o1, Object o2 ) {
        PhotoInfo p1 = (PhotoInfo) o1;
        PhotoInfo p2 = (PhotoInfo) o2;
        String place1 = p1.getShootingPlace();
        String place2 = p2.getShootingPlace();
        if ( place1 == null ) {
            place1 = "";
        }
        if ( place2 == null ) {
            place2 = "";
        }
        int res = 0;
        if ( place1 != null ) {
            res = place1.compareTo( place2 );
        }
        return res;
    }
}
        