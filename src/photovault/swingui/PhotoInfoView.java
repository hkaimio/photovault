// PhotoInfoView.java

package photovault.swingui;

import java.util.*;


public interface PhotoInfoView {
    public void setPhotographer( String newValue );
    public String getPhotographer();
    public void setShootTime( java.util.Date newValue );
    public Date getShootTime();
    public void setTimeAccuracy( Number timeAccuracy );
    public Number getTimeAccuracy();
    public void setShootPlace( String newValue );
    public String getShootPlace();
    public void setFocalLength( Number newValue );
    public Number getFocalLength();
    public void setFStop( Number newValue );
    public Number getFStop();
    public void setCamera( String newValue );
    public String getCamera();
    public void setFilm( String newValue );
    public String getFilm();
    public void setLens( String newValue );
    public String getLens();
    public void setDescription( String newValue );
    public String getDescription();
    public void setShutterSpeed( Number newValue );
    public Number getShutterSpeed();
    public void setFilmSpeed( Number newValue );
    public Number getFilmSpeed();
}
    
