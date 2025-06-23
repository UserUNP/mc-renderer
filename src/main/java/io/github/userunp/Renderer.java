package io.github.userunp;

import java.util.ArrayList;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.NotNull;

public class Renderer {

    private static final String PIXEL_STR = "⏹ ";
    private static final String EMPTY_STR = "\u200A\u200A";
    private static final int CHAR_WIDTH = 5;
    private static final int PIXEL_WIDTH = CHAR_WIDTH * 2;
    private static final float CHAR_LENGTH = CHAR_WIDTH / 40f;

    private final int aMask, rMask, gMask, bMask;
    public Renderer(int aMask, int rMask, int gMask, int bMask) {
        this.aMask = aMask;
        this.rMask = rMask;
        this.gMask = gMask;
        this.bMask = bMask;
    }

    public Renderer() {
        this(0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF);
    }

    public Surface surface(PixelGetter getter, EntityFactory entityFactory) {
        return new Surface(getter, entityFactory);
    }

    public boolean renderNextField(Surface surface) {
        while (!surface.next()) {}
        return surface.stopped;
    }

    private boolean isPixelVisible(int p, Surface s) {
        return s.x < s.width && s.y < s.height && (aMask == 0 || (p & aMask) != 0);
    }

    private int getColor(int pixel) {
        return (pixel & rMask) | (pixel & gMask) | (pixel & bMask);
    }

    public interface Entity {
        void update(int lineWidth);
        void render(ComponentLike text);
    }

    @FunctionalInterface
    public interface EntityFactory {
        Entity get(float x, float y);
    }

    @FunctionalInterface
    public interface PixelGetter {
        int get(int x, int y);
    }

    public class Surface {

        private final PixelGetter getter;
        private volatile boolean stopped;
        private int width, height;

        private int evenWidth, evenHeight, x, y;
        private final SurfaceField[] fields = new SurfaceField[4];

        public Surface(PixelGetter getter, EntityFactory entityFactory) {
            this.getter = getter;
            fields[0] = new SurfaceField(entityFactory, 0, 1);
            fields[1] = new SurfaceField(entityFactory, 1, 1);
            fields[2] = new SurfaceField(entityFactory, 0, 0);
            fields[3] = new SurfaceField(entityFactory, 1, 0);
        }

        public int width() {
            return width;
        }

        public int height() {
            return height;
        }

        public int fieldsCount() {
            return fields.length;
        }

        public void update(int width, int height) {
            if ((long) width * height > Integer.MAX_VALUE) throw new RuntimeException("Surface too large");
            this.width = width;
            this.height = height;

            evenWidth = width + (width % 2);
            evenHeight = height + (height % 2);
            for (SurfaceField field : fields) field.update(evenWidth / 2, evenHeight / 2);

            x = y = 0;
            stopped = false;
        }

        public boolean stopped() {
            return stopped;
        }

        public void stop() {
            stopped = true;
        }

        private boolean next() {
            if (stopped) return true;
            if (y >= evenHeight) x = y = 0;
            int pixel = getter.get(x, y);
            SurfaceField field = fields[(y % 2) * 2 + (x % 2)];
            boolean fieldFinished;
            if (isPixelVisible(pixel, this)) fieldFinished = field.next(getColor(pixel));
            else fieldFinished = field.nextEmpty();
            if (++x >= evenWidth) {
                x = 0;
                y++;
            }
            return fieldFinished;
        }
    }

    //TODO: multiple tiles for one field (to fix stupid network issues)
    static class SurfaceField {
        int offX, offY;
        final Tile tile;
        SurfaceField(EntityFactory entityFactory, int offX, int offY) {
            this.offX = offX;
            this.offY = offY;
            tile = new Tile(entityFactory.get(offX * CHAR_LENGTH / 2, offY * CHAR_LENGTH / 2));
        }

        boolean next(int color) {
            return tile.next(color);
        }

        boolean nextEmpty() {
            return tile.next(null);
        }

        void update(int width, int height) {
            tile.update(width, height);
        }
    }

    static class Tile implements ComponentLike {
        final ArrayList<Component> components = new ArrayList<>();
        final Entity entity;
        int index, lineWidth, width;
        Tile(Entity entity) {
            this.entity = entity;
        }

        void update(int width, int height) {
            this.width = width;
            int size = width * height;
            components.ensureCapacity(size);
            if (components.size() > size) components.subList(size, components.size()).clear();
            else while (components.size() < size) components.add(null);
            components.trimToSize();
            entity.update(width * PIXEL_WIDTH);
        }

        boolean renderIfReset() {
            boolean reset = index >= components.size();
            if (reset) {
                entity.render(this);
                index = 0;
            }
            return reset;
        }

        boolean next(Integer color) {
            boolean reset = renderIfReset();
            TextComponent.Builder builder = Component.text();
            if (color == null) builder.append(Component.text(EMPTY_STR));
            else builder.append(Component.text(PIXEL_STR, TextColor.color(color)));
            if (!reset && index != 0 && (index+1) % width == 0) builder.appendNewline();
            components.set(index++, builder.build());
            return reset;
        }

        @Override
        public @NotNull Component asComponent() {
            return components.stream().collect(Component::text, TextComponent.Builder::append, TextComponent.Builder::append).build();
        }
    }
}
