package com.bashvollink.curtaindoors.client;

import com.bashvollink.curtaindoors.block.CurtainBlock;
import com.bashvollink.curtaindoors.client.render.CurtainRenderer;
import com.bashvollink.curtaindoors.client.sim.SimManager;
import com.bashvollink.curtaindoors.registry.ModBlockEntities;
import com.bashvollink.curtaindoors.registry.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.CustomizeGuiOverlayEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

/** Client-only setup. Only referenced behind a {@code Dist.CLIENT} check. */
public final class CurtainDoorsClient {
    private CurtainDoorsClient() {
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(CurtainDoorsClient::registerRenderers);
        modBus.addListener(CurtainDoorsClient::registerItemColors);
        NeoForge.EVENT_BUS.addListener(CurtainDoorsClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(CurtainDoorsClient::onLevelUnload);
        NeoForge.EVENT_BUS.addListener(CurtainDoorsClient::onDebugText);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.CURTAIN.get(), CurtainRenderer::new);
    }

    private static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        for (var holder : ModItems.ALL) {
            BlockItem item = holder.get();
            DyeColor color = ((CurtainBlock) item.getBlock()).getColor();
            if (color != null) {
                int rgb = color.getTextureDiffuseColor();
                event.register((stack, tintIndex) -> tintIndex == 0 ? 0xFF000000 | rgb : -1, item);
            }
        }
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        SimManager.tick();
    }

    private static void onLevelUnload(LevelEvent.Unload event) {
        if (event.getLevel().isClientSide()) {
            SimManager.clear();
        }
    }

    private static void onDebugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (net.minecraft.client.Minecraft.getInstance().getDebugOverlay().showDebugScreen()) {
            event.getLeft().add(SimManager.debugLine());
        }
    }
}
