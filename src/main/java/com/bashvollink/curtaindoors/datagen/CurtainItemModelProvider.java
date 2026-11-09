package com.bashvollink.curtaindoors.datagen;

import com.bashvollink.curtaindoors.CurtainDoors;
import com.bashvollink.curtaindoors.block.CurtainBlock;
import com.bashvollink.curtaindoors.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Flat item sprites. Dyed curtains share a grayscale layer 0 tinted per color, plus an untinted layer 1. */
public class CurtainItemModelProvider extends ItemModelProvider {
    public CurtainItemModelProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CurtainDoors.MOD_ID, files);
    }

    @Override
    protected void registerModels() {
        for (var holder : ModBlocks.ALL) {
            CurtainBlock block = holder.get();
            String name = holder.getId().getPath();
            switch (block.getCurtainType()) {
                case WOOL -> withExistingParent(name, mcLoc("item/generated"))
                        .texture("layer0", modLoc("item/curtain_cloth"))
                        .texture("layer1", modLoc("item/curtain_rod"));
                case BEADED -> withExistingParent(name, mcLoc("item/generated"))
                        .texture("layer0", modLoc("item/beaded_curtain_beads"))
                        .texture("layer1", modLoc("item/beaded_curtain_strings"));
                case PLASTIC -> withExistingParent(name, mcLoc("item/generated"))
                        .texture("layer0", modLoc("item/plastic_strip_curtain"));
            }
        }
    }
}
