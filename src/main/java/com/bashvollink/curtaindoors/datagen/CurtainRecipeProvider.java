package com.bashvollink.curtaindoors.datagen;

import com.bashvollink.curtaindoors.CurtainDoors;
import com.bashvollink.curtaindoors.block.CurtainBlock;
import com.bashvollink.curtaindoors.registry.ModBlocks;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.neoforged.neoforge.registries.DeferredBlock;

public class CurtainRecipeProvider extends RecipeProvider {
    public CurtainRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput out) {
        for (DyeColor color : DyeColor.values()) {
            Item wool = BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(color.getName() + "_wool"));
            CurtainBlock woolCurtain = ModBlocks.WOOL_CURTAINS.get(color).get();
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, woolCurtain, 3)
                    .group("curtaindoors:wool_curtain")
                    .pattern("SSS").pattern("WWW").pattern("WWW")
                    .define('S', Items.STICK).define('W', wool)
                    .unlockedBy("has_wool", has(wool))
                    .save(out);

            DyeItem dye = DyeItem.byColor(color);
            CurtainBlock beaded = ModBlocks.BEADED_CURTAINS.get(color).get();
            ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, beaded, 3)
                    .group("curtaindoors:beaded_curtain")
                    .pattern("SSS").pattern("D#D").pattern("###")
                    .define('S', Items.STICK).define('D', dye).define('#', Items.STRING)
                    .unlockedBy("has_string", has(Items.STRING))
                    .save(out);

            redye(out, ModBlocks.WOOL_CURTAINS, color, dye, "curtaindoors:dye_wool_curtain");
            redye(out, ModBlocks.BEADED_CURTAINS, color, dye, "curtaindoors:dye_beaded_curtain");
        }

        ShapedRecipeBuilder.shaped(RecipeCategory.DECORATIONS, ModBlocks.PLASTIC_STRIP_CURTAIN.get(), 3)
                .pattern("III").pattern("PSP").pattern("PPP")
                .define('I', Items.IRON_NUGGET).define('P', Items.GLASS_PANE).define('S', Items.SLIME_BALL)
                .unlockedBy("has_slime_ball", has(Items.SLIME_BALL))
                .save(out);
    }

    /** Any curtain of the family plus a dye gives that color. */
    private static void redye(RecipeOutput out, Map<DyeColor, DeferredBlock<CurtainBlock>> family, DyeColor color,
                              DyeItem dye, String group) {
        CurtainBlock result = family.get(color).get();
        Ingredient others = Ingredient.of(family.entrySet().stream()
                .filter(e -> e.getKey() != color)
                .map(e -> (ItemLike) e.getValue().get())
                .toArray(ItemLike[]::new));
        ShapelessRecipeBuilder.shapeless(RecipeCategory.DECORATIONS, result)
                .group(group)
                .requires(dye).requires(others)
                .unlockedBy("has_needed_dye", has(dye))
                .save(out, CurtainDoors.id("dye_" + family.get(color).getId().getPath()));
    }
}
