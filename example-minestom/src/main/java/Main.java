import net.minestom.server.MinecraftServer;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.*;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.GameMode;
import net.minestom.server.entity.metadata.display.TextDisplayMeta;
import net.minestom.server.entity.metadata.display.AbstractDisplayMeta;
import net.minestom.server.timer.TaskSchedule;
import net.kyori.adventure.text.ComponentLike;
import io.github.userunp.Renderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class Main {

    private static final String IMG_PATH = "./image.png";

    public static void main(String[] args) {
        MinecraftServer server = MinecraftServer.init();

        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();

        instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));
        instanceContainer.setChunkSupplier(LightingChunk::new);

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            final Player player = event.getPlayer();
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(new Pos(0, 42, 0));
            player.setGameMode(GameMode.CREATIVE);
        });
        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> {
            Player player = event.getPlayer();
        });

        // Start the server on port 25565
        server.start("0.0.0.0", 25565);

        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(IMG_PATH));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        int width = image.getWidth();
        int height = image.getHeight();
        final int[] pixels = new int[width * height];
        image.getRGB(0, 0, width, height, pixels, 0, width);

        int[] z = new int[]{0};
        Renderer renderer = new Renderer(0xFF000000, 0x00FF0000, 0x0000FF00, 0x000000FF);
        Renderer.Surface surface = renderer.surface((x, y) -> pixels[y * width + x], (x, y) -> {
            Entity entity = new Entity(EntityType.TEXT_DISPLAY);
            TextDisplayMeta meta = (TextDisplayMeta) entity.getEntityMeta();
            meta.setHasNoGravity(true);
            meta.setTranslation(new Vec(x, y, (float) z[0] * 0));
            z[0]++;
            meta.setScale(new Vec(0.5, 0.5, 0.5));
            meta.setBackgroundColor(0x00000000);
            meta.setBillboardRenderConstraints(AbstractDisplayMeta.BillboardConstraints.VERTICAL);
            entity.setInstance(instanceContainer, new Pos(3, 45, 0));
            return new Renderer.Entity() {
                @Override
                public void update(int lineWidth) {
                    meta.setLineWidth(lineWidth);
                }

                @Override
                public void render(ComponentLike text) {
                    meta.setText(text.asComponent());
                }
            };
        });

        surface.update(width, height);
        // full render only once
        for (int i = 0; i < surface.fieldsCount(); i++) {
            if (renderer.renderNextField(surface)) break;
        }
    }
}
