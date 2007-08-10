/*
 * ImageInstanceModifier.java
 *
 * Created on August 9, 2007, 5:02 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package org.photovault.imginfo;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.UUID;
import org.photovault.dcraw.RawConversionSettings;
import org.photovault.image.ChannelMapOperation;

/**
  Interface for applying changes to {@link ImageInstance} 
 */
public interface ImageInstanceModifier {
    /**
     *     Set the color channel mapping from original to this instance
     * 
     * @param cm the new color channel mapping
     */
    void setColorChannelMapping(ChannelMapOperation cm);

    /**
     * Set the preferred cropping operation
     * 
     * @param cropBounds New crop bounds
     */
    void setCropBounds(Rectangle2D cropBounds);

    /**
     *     Set the file size. NOTE!!! This method should only be used by XmlImporter.
     */
    void setFileSize(long s);

    void setHash(byte[] hash);

    /**
     * Set the value of height.
     * 
     * @param v  Value to assign to height.
     */
    void setHeight(int v);

    /**
     * Set the value of imageFile.
     * 
     * @param v  Value to assign to imageFile.
     */
    void setImageFile(File v);

    /**
     * Set the value of instanceType.
     * 
     * @param v  Value to assign to instanceType.
     */
    void setInstanceType(int v);

    void setPhoto(PhotoInfo photo);

    /**
     *     Set the raw conversion settings for this photo
     * 
     * @param s The new raw conversion settings used to create this instance.
     *     The method makes a clone of the object.
     */
    void setRawSettings(RawConversionSettings s);

    /**
     * Set the amount this image is rotated when compared to the original image
     * 
     * @param v  Value to assign to rotated.
     */
    void setRotated(double v);

    void setUUID(UUID uuid);

    /**
     * Set the value of volume.
     * 
     * @param v  Value to assign to volume.
     */
    void setVolume(VolumeBase v);

    /**
     * Set the value of width.
     * 
     * @param v  Value to assign to width.
     */
    void setWidth(int v);
    
}
