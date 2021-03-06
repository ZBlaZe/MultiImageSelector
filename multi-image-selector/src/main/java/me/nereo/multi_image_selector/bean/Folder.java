package me.nereo.multi_image_selector.bean;

import java.util.List;

/**
 * Folder
 * Created by Nereo on 2015/4/7.
 */
public class Folder {
    public String name;
    public String path;
    public Image cover;
    public List<Image> images;

    public Folder() {
        this.path = "";
    }

    public Folder(String name, String path, Image cover) {
        this.name = name;
        this.path = path;
        this.cover = cover;
    }

    @Override
    public boolean equals(Object o) {
        try {
            Folder other = (Folder) o;
            return this.path.equalsIgnoreCase(other.path);
        } catch (ClassCastException e) {
            e.printStackTrace();
        }
        return super.equals(o);
    }
}
