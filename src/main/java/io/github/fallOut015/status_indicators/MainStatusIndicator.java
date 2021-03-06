package io.github.fallOut015.status_indicators;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.fallOut015.status_indicators.client.ConfigStatusIndicators;
import io.github.fallOut015.status_indicators.client.StatusType;
import io.github.fallOut015.status_indicators.client.gui.screens.ConfigScreenStatusIndicators;
import io.github.fallOut015.status_indicators.server.PacketHandlerStatusIndicators;
import io.github.fallOut015.status_indicators.server.PostStatusPacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.lifecycle.InterModProcessEvent;
import net.minecraftforge.fmlserverevents.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Mod(MainStatusIndicator.MODID)
public class MainStatusIndicator {
    private static final ResourceLocation STATUSES_TEXTURE = new ResourceLocation("status_indicators", "textures/gui/statuses.png");
    public static final String MODID = "status_indicators";
    @OnlyIn(Dist.CLIENT)
    private static final Map<UUID, StatusType> STATUSES;

    static {
        STATUSES = new HashMap<>();
    }

    public static void updatePlayerStatus(final UUID uuid, final StatusType status) {
        STATUSES.replace(uuid, status);
    }

    public MainStatusIndicator() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::enqueueIMC);
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::processIMC);

        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ConfigStatusIndicators.CLIENT_SPEC);
        ModLoadingContext.get().registerExtensionPoint(ExtensionPoint.CONFIGGUIFACTORY, () -> (mc, screen) -> new ConfigScreenStatusIndicators(screen));
    }

    private void setup(final FMLCommonSetupEvent event) {
        PacketHandlerStatusIndicators.setup(event);
    }
    private void enqueueIMC(final InterModEnqueueEvent event) {
    }
    private void processIMC(final InterModProcessEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(FMLServerStartingEvent event) {
    }

    @Mod.EventBusSubscriber
    public static class Events {
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onLoggedIn(final ClientPlayerNetworkEvent.LoggedInEvent event) {
            STATUSES.put(event.getPlayer().getUUID(), StatusType.NONE);
        }
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onLoggedOut(final ClientPlayerNetworkEvent.LoggedOutEvent event) {
            if(event.getPlayer() != null) {
                STATUSES.remove(event.getPlayer().getUUID());
            }
        }
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onRenderGameOverlay(final RenderGameOverlayEvent event) {
            AtomicBoolean paused = new AtomicBoolean(false);
            STATUSES.forEach((uuid, statusType) -> {
                if(statusType.equals(StatusType.PAUSE)) {
                    paused.set(true);
                }
            });

            if(paused.get()) {
                if(event.getType() == RenderGameOverlayEvent.ElementType.ALL) { // change to player list
                    RenderSystem.setShader(GameRenderer::getPositionTexShader);
                    RenderSystem.setShaderTexture(0, MainStatusIndicator.STATUSES_TEXTURE);
                    RenderSystem.enableDepthTest();

                    event.getMatrixStack().pushPose();

                    int s = 32;
                    int ts = 16;
                    int u = 0, v = 0;

                    event.getMatrixStack().translate(s, s, 0);
                    Gui.blit(event.getMatrixStack(), -s / 2, -s / 2, s, s, u, v, ts, ts, 48, 16);
                    event.getMatrixStack().popPose();
                }
            }
        }
        @OnlyIn(Dist.CLIENT)
        @SubscribeEvent
        public static void onGuiOpen(final GuiOpenEvent event) {
            if(Minecraft.getInstance().player != null) {
                if(event.getGui() != null && event.getGui().isPauseScreen()) {
                    System.out.println("Sending status update to server");
                    PacketHandlerStatusIndicators.INSTANCE.sendToServer(new PostStatusPacketHandler(Minecraft.getInstance().player.getUUID(), StatusType.PAUSE));
                } else {
                    System.out.println("Sending status update to server");
                    PacketHandlerStatusIndicators.INSTANCE.sendToServer(new PostStatusPacketHandler(Minecraft.getInstance().player.getUUID(), StatusType.NONE));
                }
            }
        }
    }
}