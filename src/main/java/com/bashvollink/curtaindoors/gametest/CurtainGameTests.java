package com.bashvollink.curtaindoors.gametest;

import com.bashvollink.curtaindoors.CurtainDoors;
import com.bashvollink.curtaindoors.block.CurtainBlock;
import com.bashvollink.curtaindoors.block.entity.CurtainBlockEntity;
import com.bashvollink.curtaindoors.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Husk;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Server-side behaviour checks. Run with {@code ./gradlew runGameTestServer}.
 * The "platform" template is a 5x6x5 box; its stone floor sits at relative y=1 (GameTest
 * places structures one block above the test origin), with air from y=2 up.
 */
@GameTestHolder(CurtainDoors.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CurtainGameTests {
    private static final BlockPos CEILING = new BlockPos(2, 5, 2);

    private CurtainGameTests() {
    }

    private static CurtainBlock redCurtain() {
        return ModBlocks.WOOL_CURTAINS.get(DyeColor.RED).get();
    }

    /** Hang a red curtain under the ceiling block by clicking its bottom face. */
    private static void hangUnderCeiling(GameTestHelper helper) {
        helper.setBlock(CEILING, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(redCurtain());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.placeAt(player, stack, CEILING.above(), Direction.DOWN);
    }

    private static void assertCurtainColumn(GameTestHelper helper, int length) {
        for (int seg = 0; seg < length; seg++) {
            int expected = seg;
            helper.assertBlockState(CEILING.below(seg + 1), s -> s.is(redCurtain())
                            && s.getValue(CurtainBlock.SEGMENT) == expected
                            && s.getValue(CurtainBlock.LENGTH) == length,
                    () -> "expected segment " + expected + " of " + length + " but found "
                            + helper.getBlockState(CEILING.below(expected + 1)));
        }
    }

    @GameTest(template = "platform")
    public static void placesWithAutoLength(GameTestHelper helper) {
        hangUnderCeiling(helper);
        assertCurtainColumn(helper, 3);
        if (!(helper.getLevel().getBlockEntity(helper.absolutePos(CEILING.below())) instanceof CurtainBlockEntity)) {
            helper.fail("top segment has no block entity", CEILING.below());
        }
        if (helper.getLevel().getBlockEntity(helper.absolutePos(CEILING.below(2))) != null) {
            helper.fail("lower segment should not have a block entity", CEILING.below(2));
        }
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void clickingFloorFindsCeiling(GameTestHelper helper) {
        helper.setBlock(CEILING, Blocks.STONE);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(redCurtain());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        // Click the top face of the floor under the ceiling.
        helper.placeAt(player, stack, new BlockPos(2, 0, 2), Direction.UP);
        assertCurtainColumn(helper, 3);
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void cannotPlaceWithoutCeiling(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(redCurtain());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        helper.placeAt(player, stack, new BlockPos(2, 0, 2), Direction.UP);
        helper.assertBlockNotPresent(redCurtain(), new BlockPos(2, 2, 2));
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void removingSupportDropsOneItem(GameTestHelper helper) {
        hangUnderCeiling(helper);
        helper.setBlock(CEILING, Blocks.AIR);
        for (int y = 2; y <= 4; y++) {
            helper.assertBlockNotPresent(redCurtain(), new BlockPos(2, y, 2));
        }
        helper.assertItemEntityCountIs(redCurtain().asItem(), new BlockPos(2, 3, 2), 3.0, 1);
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void breakingLowerSegmentDropsOneItem(GameTestHelper helper) {
        hangUnderCeiling(helper);
        helper.getLevel().destroyBlock(helper.absolutePos(new BlockPos(2, 2, 2)), true);
        for (int y = 2; y <= 4; y++) {
            helper.assertBlockNotPresent(redCurtain(), new BlockPos(2, y, 2));
        }
        helper.assertItemEntityCountIs(redCurtain().asItem(), new BlockPos(2, 3, 2), 3.0, 1);
        helper.succeed();
    }

    @GameTest(template = "platform")
    public static void hasNoCollision(GameTestHelper helper) {
        hangUnderCeiling(helper);
        for (int y = 2; y <= 4; y++) {
            BlockPos abs = helper.absolutePos(new BlockPos(2, y, 2));
            BlockState state = helper.getLevel().getBlockState(abs);
            if (!state.getCollisionShape(helper.getLevel(), abs).isEmpty()) {
                helper.fail("curtain has collision: " + state, new BlockPos(2, y, 2));
            }
        }
        helper.succeed();
    }

    @GameTest(template = "platform", timeoutTicks = 300)
    public static void mobWalksThroughDoorway(GameTestHelper helper) {
        // A stone wall across z=2 with a one-block doorway at x=2, closed by a curtain.
        for (int x = 0; x < 5; x++) {
            for (int y = 2; y <= 5; y++) {
                if (x != 2 || y == 5) {
                    helper.setBlock(new BlockPos(x, y, 2), Blocks.STONE);
                }
            }
        }
        hangUnderCeiling(helper);
        assertCurtainColumn(helper, 3);

        Husk husk = helper.spawnWithNoFreeWill(EntityType.HUSK, new BlockPos(2, 2, 0));
        Vec3 target = Vec3.atBottomCenterOf(helper.absolutePos(new BlockPos(2, 2, 4)));
        helper.onEachTick(() -> {
            if (husk.getNavigation().isDone()) {
                husk.getNavigation().moveTo(target.x, target.y, target.z, 1.0);
            }
        });
        // Navigation stops within a block of the target, so "well past the doorway at z=2" is success.
        helper.succeedWhen(() -> {
            if (!husk.isAlive() || helper.relativeVec(husk.position()).z < 3.0) {
                helper.fail("husk has not walked through the curtain yet", husk);
            }
        });
    }
}
