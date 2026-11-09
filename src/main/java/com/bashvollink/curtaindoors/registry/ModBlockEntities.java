package com.bashvollink.curtaindoors.registry;

import com.bashvollink.curtaindoors.CurtainDoors;
import com.bashvollink.curtaindoors.block.entity.CurtainBlockEntity;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, CurtainDoors.MOD_ID);

    @SuppressWarnings("DataFlowIssue")
    public static final Supplier<BlockEntityType<CurtainBlockEntity>> CURTAIN = BLOCK_ENTITIES.register("curtain",
            () -> BlockEntityType.Builder.of(CurtainBlockEntity::new,
                    ModBlocks.ALL.stream().map(Supplier::get).toArray(Block[]::new)).build(null));

    private ModBlockEntities() {
    }
}
