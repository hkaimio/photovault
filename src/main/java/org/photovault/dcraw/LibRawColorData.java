/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.photovault.dcraw;

import com.sun.jna.Pointer;
import com.sun.jna.Structure;

/**
 * Java wrapper for libraw ColorData structure
 * @author Harri Kaimio
 * @since 0.6.0
 */
public class LibRawColorData extends Structure {

    public int color_data_state;

    public short[] white_box = new short[64];
    public float[] cam_mul = new float[4];
    public float[] pre_mul = new float[4];
    public float[] cmatrix = new float[12];
    public float[] rgb_cam = new float[12];
    public float[] cam_xyz = new float[12];
    public short[] curve = new short[0x4001];
    public int black;
    public int maximum;

    public static class PhaseOneData extends Structure {
        public int format, key_off, t_black, black_off, split_col, tag_21a;
        public float tag_210;
    }

    public PhaseOneData ph1_data;
    public float flash_used;
    public float canon_ev;
    public byte[] model = new byte[64];
    public Pointer profile;
    public int profile_length;
}
