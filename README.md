# mc-renderer
Render images on (4) text display entities

```java
Renderer renderer = new Renderer(0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF);

Renderer.Surface surface = renderer.surface((x, y) -> {
    /* get pixel from (x,y) */
}, (x, y) -> {
    /* summon text display entity at (x,y) */
    return new Renderer.Entity() {
        @Override
        public void update(int lineWidth) {
            /* update display entity's line width */
        }

        @Override
        public void render(ComponentLike text) {
            /* set display entity's text */
        }
    }
});

surface.update(width, height);

// full render only once
for (int i = 0; i < surface.fieldsCount(); i++) {
    if (renderer.renderNextField(surface)) break;
}
```
