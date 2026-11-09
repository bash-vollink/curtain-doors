package com.bashvollink.curtaindoors;

import com.bashvollink.curtaindoors.config.ClientConfig;
import com.bashvollink.curtaindoors.config.CommonConfig;
import com.bashvollink.curtaindoors.datagen.DataGenerators;
import com.bashvollink.curtaindoors.registry.ModBlockEntities;
import com.bashvollink.curtaindoors.registry.ModBlocks;
import com.bashvollink.curtaindoors.registry.ModCreativeTabs;
import com.bashvollink.curtaindoors.registry.ModItems;
import com.bashvollink.curtaindoors.registry.ModSounds;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;

@Mod(CurtainDoors.MOD_ID)
public final class CurtainDoors {
    public static final String MOD_ID = "curtaindoors";

    public CurtainDoors(IEventBus modBus, ModContainer container) {
        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modBus);
        ModSounds.SOUNDS.register(modBus);
        ModCreativeTabs.TABS.register(modBus);

        container.registerConfig(ModConfig.Type.COMMON, CommonConfig.SPEC);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);

        modBus.addListener(DataGenerators::gatherData);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            com.bashvollink.curtaindoors.client.CurtainDoorsClient.init(modBus);
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
