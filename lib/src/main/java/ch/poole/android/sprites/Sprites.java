/*
 * Copyright 2021 Simon Poole
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package ch.poole.android.sprites;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

/**
 * Very small class to handle sprites
 * 
 * @author Simon Poole
 *
 */
public class Sprites implements Serializable {

    private static final long serialVersionUID = 3L;

    private static final String DEBUG_TAG = Sprites.class.getSimpleName();

    public static final String ICON_X      = "x";
    public static final String ICON_Y      = "y";
    public static final String ICON_WIDTH  = "width";
    public static final String ICON_HEIGHT = "height";

    private static final String SPRITE_CACHE = "sprites";

    private boolean retina = false;
    private String  bitmapCachePath;

    private transient JsonObject          sheet;
    private transient BitmapRegionDecoder regionDecoder;

    private transient Map<String, Bitmap>   cache   = new HashMap<>();
    private transient Rect                  rect    = new Rect();
    private transient BitmapFactory.Options options = new BitmapFactory.Options();

    /**
     * Create a new instance
     * 
     * @param ctx an Android Context
     * @param jsonIn InputStream for the JSON sheet describing the icon locations
     * @param imageIn InputStream for the sprite image
     */
    public Sprites(@NonNull Context ctx, @NonNull InputStream jsonIn, @NonNull InputStream imageIn) {
        loadJson(jsonIn);
        loadImage(ctx, imageIn);
    }

    /**
     * CHeck if the icons a 2x resolution
     * 
     * @return true if the icons are "retina" icons
     */
    public boolean retina() {
        return retina;
    }

    /**
     * Set the retina flag
     * 
     * @param retina if true indicate that these are "retina" icons
     */
    public void setRetina(boolean retina) {
        this.retina = retina;
    }

    /**
     * Retrieve an icon from the sprite
     * 
     * @param name the icon name
     * @return a Bitmap or null if not found
     */
    @Nullable
    public Bitmap get(@NonNull String name) {
        // "aerialway_11": {
        // "height": 15,
        // "pixelRatio": 1,
        // "width": 15,
        // "x": 314,
        // "y": 0
        // },
        if (cache.containsKey(name)) {
            return cache.get(name);
        }
        try {
            if (sheet != null && regionDecoder != null) {
                JsonObject meta = getMeta(name);
                if (meta != null) {
                    int x = getInt(ICON_X, meta);
                    int y = getInt(ICON_Y, meta);
                    int width = getInt(ICON_WIDTH, meta);
                    int height = getInt(ICON_HEIGHT, meta);
                    rect.set(x, y, x + width, y + height);
                    Bitmap result = regionDecoder.decodeRegion(rect, options);
                    cache.put(name, result);
                    return result;
                }
            }
            Log.e(DEBUG_TAG, "Setup error");
        } catch (IllegalArgumentException iae) {
            Log.e(DEBUG_TAG, "Error in sprite sheet for " + name + " " + iae.getLocalizedMessage());
        }
        Log.w(DEBUG_TAG, "Icon not found " + name);
        cache.put(name, null);
        return null;
    }

    /**
     * Check if the icon is cached
     * 
     * @param name the icon name
     * @return true if in cache
     */
    public boolean cached(@NonNull String name) {
        return cache.containsKey(name);
    }

    /**
     * Get the JsonObject describing the icon in question from the sprite sheet
     * 
     * @param name the icon name
     * @return an JsonObject or null if not found
     */
    @Nullable
    public JsonObject getMeta(@NonNull String name) {
        JsonElement element = sheet.get(name);
        if (element != null && element.isJsonObject()) {
            return (JsonObject) element;
        }
        return null;
    }

    private int getInt(@NonNull String name, @NonNull JsonObject object) {
        JsonElement e = object.get(name);
        if (e != null && e.isJsonPrimitive() && ((JsonPrimitive) e).isNumber()) {
            return ((JsonPrimitive) e).getAsInt();
        }
        throw new IllegalArgumentException("No int for " + name + " found");
    }

    /**
     * Load sprite sheet from an InputStream
     * 
     * @param is the InputStream
     */
    private void loadJson(@NonNull InputStream is) {
        try (BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")))) {
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
            }
            JsonElement root = JsonParser.parseString(sb.toString());

            if (root.isJsonObject()) {
                sheet = (JsonObject) root;
            }
        } catch (IOException | JsonSyntaxException e) {
            Log.d(DEBUG_TAG, "Opening " + e.getMessage());
        }
    }

    /**
     * Saves the image to a cache file and then loads it in to a BitmapRegionDecoder
     * 
     * @param ctx an Android Context
     * @param is the InputStream
     */
    private void loadImage(@NonNull Context ctx, @NonNull InputStream is) {
        File[] cacheDirs = ContextCompat.getExternalCacheDirs(ctx);
        File cacheDir = new File(cacheDirs.length > 1 && cacheDirs[1] != null ? cacheDirs[1] : cacheDirs[0], SPRITE_CACHE);
        cacheDir.mkdir();
        File temp = new File(cacheDir, UUID.randomUUID().toString());
        try (FileOutputStream tempOut = new FileOutputStream(temp)) {
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                tempOut.write(buffer, 0, len);
            }
        } catch (IOException e) {
            Log.e(DEBUG_TAG, "Unable to create temp cache file" + e.getMessage());
            return;
        }
        try {
            bitmapCachePath = temp.getAbsolutePath();
            regionDecoder = BitmapRegionDecoder.newInstance(bitmapCachePath, false);
        } catch (IOException ioe) {
            Log.w(DEBUG_TAG, "Error decoding sprite images " + ioe.getLocalizedMessage());
        }
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }

    /**
     * Remove the cached bitmap 
     * 
     * After calling this, the class can no longer be serialised/deserialised
     */
    public void emptyCache() {
        if (bitmapCachePath != null) {
            new File(bitmapCachePath).delete();
        }
    }
    
    /**
     * Serialize this object
     * 
     * @param out ObjectOutputStream to write to
     * @throws IOException if writing fails
     */
    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeObject(sheet.toString());
    }

    /**
     * Read serialized object
     * 
     * @param in the input stream
     * @throws IOException if reading fails
     * @throws ClassNotFoundException if the Class to deserialize can't be found
     */
    private void readObject(@NonNull ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Object temp = in.readObject();
        sheet = temp != null ? (JsonObject) JsonParser.parseString(temp.toString()) : null;
        if (bitmapCachePath != null) {
            regionDecoder = BitmapRegionDecoder.newInstance(bitmapCachePath, false);
        } else {
            Log.e(DEBUG_TAG, "No saved input image file");
        }
        cache = new HashMap<>();
        rect = new Rect();
        options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }
}