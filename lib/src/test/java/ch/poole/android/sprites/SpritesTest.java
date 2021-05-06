package ch.poole.android.sprites;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.zip.Checksum;
import java.util.zip.CRC32;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import com.google.gson.JsonObject;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.filters.LargeTest;

@RunWith(RobolectricTestRunner.class)
@LargeTest
public class SpritesTest {

    private static final String OSM_LIBERTY_PNG  = "/osm-liberty.png";
    private static final String OSM_LIBERTY_JSON = "/osm-liberty.json";
    private static final String AERIALWAY_11     = "aerialway_11";

    @Test
    public void getExistingIconTest() {
        InputStream sheet = SpritesTest.class.getResourceAsStream(OSM_LIBERTY_JSON);
        assertNotNull(sheet);
        InputStream image = SpritesTest.class.getResourceAsStream(OSM_LIBERTY_PNG);
        assertNotNull(image);
        Sprites sprites = new Sprites(ApplicationProvider.getApplicationContext(), sheet, image);
        assertNotNull(sprites);
        Bitmap icon = sprites.get(AERIALWAY_11);
        assertNotNull(icon);
        JsonObject meta = sprites.getMeta(AERIALWAY_11);
        assertNotNull(meta);
        int height = meta.get(Sprites.ICON_HEIGHT).getAsInt();
        assertEquals(15, height);
        int width = meta.get(Sprites.ICON_WIDTH).getAsInt();
        assertEquals(15, width);
        assertEquals(height, icon.getHeight());
        assertEquals(width, icon.getWidth());
        assertTrue(sprites.cached(AERIALWAY_11));
    }

    @Test
    public void getNotExistingIconTest() {
        InputStream sheet = SpritesTest.class.getResourceAsStream(OSM_LIBERTY_JSON);
        assertNotNull(sheet);
        InputStream image = SpritesTest.class.getResourceAsStream(OSM_LIBERTY_PNG);
        assertNotNull(image);
        Sprites sprites = new Sprites(ApplicationProvider.getApplicationContext(), sheet, image);
        assertNotNull(sprites);
        Bitmap icon = sprites.get("test");
        assertNull(icon);
        JsonObject meta = sprites.getMeta("test");
        assertNull(meta);
        assertTrue(sprites.cached("test"));
    }

    @Test
    public void serializeTest() {
        try {
            InputStream sheet = SpritesTest.class.getResourceAsStream(OSM_LIBERTY_JSON);
            assertNotNull(sheet);
            InputStream image = SpritesTest.class.getResourceAsStream(OSM_LIBERTY_PNG);
            assertNotNull(image);
            Sprites sprites = new Sprites(ApplicationProvider.getApplicationContext(), sheet, image);
            assertNotNull(sprites);
            Bitmap icon = sprites.get(AERIALWAY_11);
            assertNotNull(icon);
            int iconSize = icon.getByteCount();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
            Checksum crc32 = new CRC32();
            byte[] buffer = stream.toByteArray();
            crc32.update(buffer, 0, buffer.length);
            long checksum = crc32.getValue();

            FileOutputStream out = ApplicationProvider.getApplicationContext().openFileOutput("TEST", Context.MODE_PRIVATE);
            try (ObjectOutputStream objOut = new ObjectOutputStream(out)) {
                objOut.writeObject(sprites);
                objOut.flush();
            }
            InputStream in = ApplicationProvider.getApplicationContext().openFileInput("TEST");
            Sprites newSprites = null;
            try (ObjectInputStream objIn = new ObjectInputStream(in)) {
                newSprites = (Sprites) objIn.readObject();
            }
            assertNotNull(newSprites);
            icon = newSprites.get(AERIALWAY_11);
            assertNotNull(icon);
            assertEquals(iconSize, icon.getByteCount());
            stream = new ByteArrayOutputStream();
            icon.compress(Bitmap.CompressFormat.PNG, 100, stream);
            buffer = stream.toByteArray();
            crc32 = new CRC32();
            crc32.update(buffer, 0, buffer.length);
            assertEquals(checksum, crc32.getValue());
        } catch (IOException | ClassNotFoundException ex) {
            fail(ex.getMessage());
        }
    }
}
