package com.bashvollink.curtaindoors.datagen;

import com.bashvollink.curtaindoors.block.CurtainBlock;
import com.bashvollink.curtaindoors.registry.ModBlocks;
import java.util.Set;
import java.util.function.Supplier;
import net.minecraft.advancements.critereon.StatePropertiesPredicate;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

/** One item per curtain, dropped only by the top segment. */
public class CurtainLootProvider extends BlockLootSubProvider {
    public CurtainLootProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        for (var holder : ModBlocks.ALL) {
            CurtainBlock block = holder.get();
            add(block, LootTable.lootTable().withPool(applyExplosionCondition(block, LootPool.lootPool()
                    .setRolls(ConstantValue.exactly(1))
                    .add(LootItem.lootTableItem(block)
                            .when(LootItemBlockStatePropertyCondition.hasBlockStateProperties(block)
                                    .setProperties(StatePropertiesPredicate.Builder.properties()
                                            .hasProperty(CurtainBlock.SEGMENT, 0)))))));
        }
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.ALL.stream().map(Supplier::get).map(Block.class::cast)::iterator;
    }
}
