// PhotoInfoView.java

package photovault.swingui;

import java.util.*;


public interface PhotoInfoView {
    public void setPhotographer( String newValue );
    public String getPhotographer();
    public void setShootTime( java.util.Date newValue );
    public Date getShootTime();
    public void setShootPlace( String newValue );
    public String getShootPlace();
    public void setFocalLength( Number newValue );
    public Number getFocalLength();
    public void setFStop( Number newValue );
    public Number getFStop();
}
    
