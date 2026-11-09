package com.bashvollink.curtaindoors.datagen;

import java.util.List;
import java.util.Set;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.data.event.GatherDataEvent;

public final class DataGenerators {
    private DataGenerators() {
    }

    public static void gatherData(GatherDataEvent event) {
        DataGenerator gen = event.getGenerator();
        PackOutput out = gen.getPackOutput();
        var lookup = event.getLookupProvider();
        var files = event.getExistingFileHelper();

        gen.addProvider(event.includeClient(), new CurtainBlockStateProvider(out, files));
        gen.addProvider(event.includeClient(), new CurtainItemModelProvider(out, files));
        gen.addProvider(event.includeClient(), new CurtainLanguageProvider(out));
        gen.addProvider(event.includeServer(), new CurtainRecipeProvider(out, lookup));
        gen.addProvider(event.includeServer(), new LootTableProvider(out, Set.of(),
                List.of(new LootTableProvider.SubProviderEntry(CurtainLootProvider::new, LootContextParamSets.BLOCK)), lookup));
    }

    static String titleCase(String snake) {
        StringBuilder sb = new StringBuilder();
        for (String part : snake.split("_")) {
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return sb.toString();
    }
}
