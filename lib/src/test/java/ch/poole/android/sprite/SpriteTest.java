package ch.poole.android.sprite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import com.google.gson.JsonObject;

import android.graphics.Bitmap;
import androidx.test.filters.LargeTest;

@RunWith(RobolectricTestRunner.class)
@LargeTest
public class SpriteTest {

    @Test
    public void getExistingIconTest() {
        InputStream sheet = SpriteTest.class.getResourceAsStream("/osm-liberty.json");
        assertNotNull(sheet);
        InputStream image = SpriteTest.class.getResourceAsStream("/osm-liberty.png");
        assertNotNull(image);
        Sprite sprite = new Sprite(sheet, image);
        assertNotNull(sprite);
        Bitmap icon = sprite.get("aerialway_11");
        assertNotNull(icon);
        JsonObject meta = sprite.getMeta("aerialway_11");
        assertNotNull(meta);
        int height = meta.get(Sprite.ICON_HEIGHT).getAsInt();
        assertEquals(15, height);
        int width = meta.get(Sprite.ICON_WIDTH).getAsInt();
        assertEquals(15, width);
        assertEquals(height, icon.getHeight());
        assertEquals(width, icon.getWidth());
        assertTrue(sprite.cached("aerialway_11"));
    }

    @Test
    public void getNotExistingIconTest() {
        InputStream sheet = SpriteTest.class.getResourceAsStream("/osm-liberty.json");
        assertNotNull(sheet);
        InputStream image = SpriteTest.class.getResourceAsStream("/osm-liberty.png");
        assertNotNull(image);
        Sprite sprite = new Sprite(sheet, image);
        assertNotNull(sprite);
        Bitmap icon = sprite.get("test");
        assertNull(icon);
        JsonObject meta = sprite.getMeta("test");
        assertNull(meta);
        assertTrue(sprite.cached("test"));
    }
}
