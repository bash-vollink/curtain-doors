package com.bashvollink.curtaindoors.registry;

import com.bashvollink.curtaindoors.CurtainDoors;
import com.bashvollink.curtaindoors.block.CurtainBlock;
import com.bashvollink.curtaindoors.block.CurtainType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CurtainDoors.MOD_ID);

    public static final Map<DyeColor, DeferredBlock<CurtainBlock>> WOOL_CURTAINS = new EnumMap<>(DyeColor.class);
    public static final Map<DyeColor, DeferredBlock<CurtainBlock>> BEADED_CURTAINS = new EnumMap<>(DyeColor.class);
    public static final DeferredBlock<CurtainBlock> PLASTIC_STRIP_CURTAIN;
    private static final List<DeferredBlock<CurtainBlock>> ALL_MUTABLE = new ArrayList<>();
    /** Every curtain block, in creative-tab order. */
    public static final List<DeferredBlock<CurtainBlock>> ALL = Collections.unmodifiableList(ALL_MUTABLE);

    static {
        for (DyeColor color : DyeColor.values()) {
            WOOL_CURTAINS.put(color, register(color.getName() + "_curtain", CurtainType.WOOL, color));
        }
        PLASTIC_STRIP_CURTAIN = register("plastic_strip_curtain", CurtainType.PLASTIC, null);
        for (DyeColor color : DyeColor.values()) {
            BEADED_CURTAINS.put(color, register(color.getName() + "_beaded_curtain", CurtainType.BEADED, color));
        }
    }

    private ModBlocks() {
    }

    private static DeferredBlock<CurtainBlock> register(String name, CurtainType type, DyeColor color) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .noCollission()
                .noOcclusion()
                .strength(0.3F)
                .pushReaction(PushReaction.DESTROY)
                .isViewBlocking((s, l, p) -> false)
                .isSuffocating((s, l, p) -> false)
                .isRedstoneConductor((s, l, p) -> false);
        switch (type) {
            case WOOL -> props.mapColor(color.getMapColor()).sound(SoundType.WOOL).ignitedByLava();
            case PLASTIC -> props.mapColor(MapColor.NONE).sound(SoundType.SLIME_BLOCK);
            case BEADED -> props.mapColor(color.getMapColor()).sound(SoundType.CHAIN);
        }
        DeferredBlock<CurtainBlock> block = BLOCKS.register(name, () -> new CurtainBlock(type, color, props));
        ALL_MUTABLE.add(block);
        return block;
    }
}
