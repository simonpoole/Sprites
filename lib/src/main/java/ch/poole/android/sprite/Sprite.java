/*
   Copyright 2021 Simon Poole

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

package ch.poole.android.sprite;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Rect;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Very small class to handle sprites
 * 
 * @author Simon Poole
 *
 */
public class Sprite {

    public static final String ICON_X      = "x";
    public static final String ICON_Y      = "y";
    public static final String ICON_WIDTH  = "width";
    public static final String ICON_HEIGHT = "height";

    private static final String DEBUG_TAG = Sprite.class.getSimpleName();

    private JsonObject          sheet;
    private BitmapRegionDecoder regionDecoder;
    private Map<String, Bitmap> cache = new HashMap<>();

    private Rect rect = new Rect();

    private BitmapFactory.Options options = new BitmapFactory.Options();

    /**
     * Create a new instance
     * 
     * @param jsonIn InputStream for the JSON sheet describing the icon locations
     * @param imageIn InputStream for the sprite image
     */
    public Sprite(@NonNull InputStream jsonIn, @NonNull InputStream imageIn) {
        loadJson(jsonIn);
        loadImage(imageIn);
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

    private void loadImage(@NonNull InputStream is) {
        try {
            regionDecoder = BitmapRegionDecoder.newInstance(is, false);
        } catch (IOException ioe) {
            Log.w(DEBUG_TAG, "Error decoding sprite images " + ioe.getLocalizedMessage());
        }
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
    }
}