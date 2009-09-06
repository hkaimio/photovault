/*
  Copyright (c) 2009 Harri Kaimio

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
package org.photovault.image;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamOmitField;
import org.photovault.dcraw.RawConversionSettings;

/**
 * DCrawOp describes the parameters used for conversion from Bayer raw data
 * to (linear) sRGB data. Unlike most {@link ImageOp} operations it does not have
 * any input ports.
 *
 * @author Harri Kaimio
 * @since 0.6.0
 */
@XStreamAlias( "dcraw" )
public class DCRawOp extends ImageOp {

    public DCRawOp() {
        super();
        addOutputPort( "out" );
    }

    public DCRawOp( ImageOpChain chain, String name ) {
        super();
        setName( name );
        setChain( chain );
        initPorts();
    }

    @XStreamAsAttribute
    private int white;

    @XStreamAsAttribute
    private int black;

    @XStreamAsAttribute
    private double blueGreenRatio;

    @XStreamAsAttribute
    private double redGreenRatio;

    @XStreamAsAttribute
    private double daylightBlueGreenRatio;
    @XStreamAsAttribute
    private double daylightRedGreenRatio;

    @XStreamOmitField
    private double ctemp;

    @XStreamOmitField
    private double greenGain;
    
    @XStreamAsAttribute
    private int highlightRecovery;

    @XStreamAsAttribute
    private int medianFilterPassCount;

    @XStreamAsAttribute
    private double waveletThreshold;

    private double aberCorr[];

    @Override
    protected void initPorts() {
        addOutputPort( "out" );
    }

    /**
     * Get white level
     * @return the white
     * @deprecated Use {@link DCRawMapOp#getWhite() } instead
     */
    public int getWhite() {
        return white;
    }

    /**
     * Set white level for the conversion
     * @param white the white to set
     * @deprecated Use {@link DCRawMapOp#setWhite(int) } instead
     */
    public void setWhite( int white ) {
        this.white = white;
    }

    /**
     * Get black level
     * @return the black level
     * @deprecated Use {@link DCRawMapOp#getBlack() } instead
     */
    public int getBlack() {
        return black;
    }

    /**
     * Set black level for the conversion
     * @param black the black level to set
     * @deprecated Use {@link DCRawMapOp#setBlack(int) } instead
     */
    public void setBlack( int black ) {
        this.black = black;
    }

    /**
     * Get ratio of blue and green channel multipliers
     * @return the blueGreenRatio
     */
    public double getBlueGreenRatio() {
        return blueGreenRatio;
    }

    /**
     * Set ratio between blue and green channels (green is always scaled with
     * 1.0, blue with value given here)
     * @param blueGreenRatio the blueGreenRatio to set
     */
    public void setBlueGreenRatio( double blueGreenRatio ) {
        this.blueGreenRatio = blueGreenRatio;
        calcColorTemp();
    }

    /**
     * Get ratio of red and green channel multipliers
     * @return the blueGreenRatio
     */
    public double getRedGreenRatio() {
        return redGreenRatio;
    }

    /**
     * Set ratio between blue and green channels (green is always scaled with
     * 1.0, blue with value given here)
     * @param blueGreenRatio the blueGreenRatio to set
     */
    public void setRedGreenRatio( double redGreenRatio ) {
        this.redGreenRatio = redGreenRatio;
        calcColorTemp();
    }

    /**
     * Get the ratio of blue and green in picture taken in 5500 K lightning, This
     * is used for converting comor temperature to channel multipliers
     * @return the daylightBlueGreenRatio
     */
    public double getDaylightBlueGreenRatio() {
        return daylightBlueGreenRatio;
    }

    /**
     * @param daylightBlueGreenRatio the daylightBlueGreenRatio to set
     */
    public void setDaylightBlueGreenRatio( double daylightBlueGreenRatio ) {
        this.daylightBlueGreenRatio = daylightBlueGreenRatio;
        calcColorTemp();
    }

    /**
     * Get the ratio of red and green in picture taken in 5500 K lightning, This
     * is used for converting comor temperature to channel multipliers
     * @return the daylightBlueGreenRatio
     */
    public double getDaylightRedGreenRatio() {
        return daylightRedGreenRatio;
    }

    /**
     * @param daylightRedGreenRatio the daylightRedGreenRatio to set
     */
    public void setDaylightRedGreenRatio( double daylightRedGreenRatio ) {
        this.daylightRedGreenRatio = daylightRedGreenRatio;
        calcColorTemp();
    }

    /**
     * Set color temperature to be used
     * @param ctemp Color temperature in Kelvins
     */
    public void setColorTemp( double ctemp ) {
        this.ctemp = ctemp;
        calcColorMultipliers();
    }

    /**
     * Get color temperature to be used
     * @return Color temperature in Kelvins
     */
    public double getColorTemp() {
        return ctemp;
    }

    /**
     * Set gain applied to green channel
     * @param greenGain
     */
    public void setGreenGain( double greenGain ) {
        this.greenGain = greenGain;
        calcColorMultipliers();
    }

    public double getGreenGain() {
        return greenGain;
    }

    /**
     * Get the amount ofhighlight recovery done
     * @return the highlightRecovery
     */
    public int getHighlightRecovery() {
        return highlightRecovery;
    }

    /**
     * Set the amount of highlight recovery done. The larger the number the
     * larger area will be used for estimating color of highlights.
     * @param highlightRecovery the highlightRecovery to set
     */
    public void setHighlightRecovery( int highlightRecovery ) {
        this.highlightRecovery = highlightRecovery;
    }

    /**
     * Get number of median filter passes applied to the image
     * @return the medianFilterPassCount
     */
    public int getMedianFilterPassCount() {
        return medianFilterPassCount;
    }

    /**
     * @param medianFilterPassCount the medianFilterPassCount to set
     */
    public void setMedianFilterPassCount( int medianFilterPassCount ) {
        this.medianFilterPassCount = medianFilterPassCount;
    }

    /**
     * Get the thresholdused for wavelet denoising filter
     * @return the waveletThreshold
     */
    public double getWaveletThreshold() {
        return waveletThreshold;
    }

    /**
     * @param waveletThreshold the waveletThreshold to set
     */
    public void setWaveletThreshold( double waveletThreshold ) {
        this.waveletThreshold = waveletThreshold;
    }

    /**
     * Get multipliers used for chromatic aberration correction
     * @return the aberCorr
     */
    public double[] getAberCorr() {
        return aberCorr;
    }

    /**
     * @param aberCorr the aberCorr to set
     */
    public void setAberCorr( double[] aberCorr ) {
        this.aberCorr = aberCorr;
    }

    /**
     * Calculate color channel multipliers form color temperature.
     */
    private void calcColorMultipliers() {
        if ( greenGain == 0.0 ) {
            return;
        }
        double[] rgb = RawConversionSettings.colorTempToRGB( ctemp );
        redGreenRatio = (daylightRedGreenRatio*rgb[1]) / (rgb[0]*greenGain);
        blueGreenRatio = (daylightBlueGreenRatio*rgb[1]) / (rgb[2]*greenGain);
    }

    /**
     * Calculate color temperature and green gain from channel multipliers.
     */
    private void calcColorTemp() {
        if ( redGreenRatio == 0.0 || blueGreenRatio == 0.0 ) {
            return;
        }
        double rgb[] = {
            daylightRedGreenRatio/redGreenRatio,
            1.,
            daylightBlueGreenRatio/blueGreenRatio
        };
        double ct[] = RawConversionSettings.rgbToColorTemp( rgb );
        ctemp = ct[0];
        greenGain = ct[1];
    }
}
