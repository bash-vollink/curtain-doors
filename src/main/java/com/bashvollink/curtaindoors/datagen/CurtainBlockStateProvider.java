package com.bashvollink.curtaindoors.datagen;

import com.bashvollink.curtaindoors.CurtainDoors;
import com.bashvollink.curtaindoors.block.CurtainBlock;
import com.bashvollink.curtaindoors.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Curtains are drawn by a block entity renderer; their block models only supply a break particle. */
public class CurtainBlockStateProvider extends BlockStateProvider {
    public CurtainBlockStateProvider(PackOutput output, ExistingFileHelper files) {
        super(output, CurtainDoors.MOD_ID, files);
    }

    @Override
    protected void registerStatesAndModels() {
        for (var holder : ModBlocks.ALL) {
            CurtainBlock block = holder.get();
            ResourceLocation particle = switch (block.getCurtainType()) {
                case WOOL -> mcLoc("block/" + block.getColor().getName() + "_wool");
                case PLASTIC -> modLoc("block/plastic_strip");
                case BEADED -> mcLoc("block/" + block.getColor().getName() + "_concrete");
            };
            ModelFile model = models().getBuilder(holder.getId().getPath()).texture("particle", particle);
            simpleBlock(block, model);
        }
    }
}
