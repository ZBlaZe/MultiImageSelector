package me.nereo.multi_image_selector.bean;

/**
 * Pictures entity
 * Created by Nereo on 2015/4/7.
 */
public class Image {
    public String path;
    public String name;
    public long timeAdded;
    public long timeModified;
    public long size;

    public Image(String path, String name, long timeAdded, long timeModified, long size) {
        this.path           = path;
        this.name           = name;
        this.timeAdded      = timeAdded * 1000L;
        this.timeModified   = timeModified * 1000L;
        this.size           = size;
    }

    @Override
    public boolean equals(Object o) {
        try {
            Image other = (Image) o;
            return this.path.equalsIgnoreCase(other.path);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return super.equals(o);
    }
}
