package com.bashvollink.curtaindoors.datagen;

import com.bashvollink.curtaindoors.CurtainDoors;
import com.bashvollink.curtaindoors.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public class CurtainLanguageProvider extends LanguageProvider {
    public CurtainLanguageProvider(PackOutput output) {
        super(output, CurtainDoors.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("itemGroup." + CurtainDoors.MOD_ID, "Curtain Doors");
        ModBlocks.ALL.forEach(holder -> add(holder.get(), DataGenerators.titleCase(holder.getId().getPath())));
        add("subtitles." + CurtainDoors.MOD_ID + ".curtain.wool.brush", "Curtain rustles");
        add("subtitles." + CurtainDoors.MOD_ID + ".curtain.plastic.swish", "Plastic strips swish");
        add("subtitles." + CurtainDoors.MOD_ID + ".curtain.beads.clack", "Beads clack");
    }
}
