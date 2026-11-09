package com.bashvollink.curtaindoors.registry;

import com.bashvollink.curtaindoors.CurtainDoors;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.world.item.BlockItem;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CurtainDoors.MOD_ID);

    private static final List<DeferredItem<BlockItem>> ALL_MUTABLE = new ArrayList<>();
    public static final List<DeferredItem<BlockItem>> ALL = Collections.unmodifiableList(ALL_MUTABLE);

    static {
        ModBlocks.ALL.forEach(block -> ALL_MUTABLE.add(ITEMS.registerSimpleBlockItem(block)));
    }

    private ModItems() {
    }
}
