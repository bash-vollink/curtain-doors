package com.bashvollink.curtaindoors.client.sim;

import com.bashvollink.curtaindoors.block.CurtainBlock;
import com.bashvollink.curtaindoors.block.CurtainType;
import com.bashvollink.curtaindoors.block.entity.CurtainBlockEntity;
import com.bashvollink.curtaindoors.config.ClientConfig;
import com.bashvollink.curtaindoors.registry.ModSounds;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Owns every curtain's cloth simulation on the client. Sims are created lazily the first time a
 * curtain is rendered, stepped at 20 Hz, put to sleep when still, and dropped once the curtain
 * hasn't been rendered for a couple of seconds.
 */
public final class SimManager {
    private static final int SUBSTEPS = 2;
    private static final float SUBSTEP_DT = 1F / (20F * SUBSTEPS);
    private static final float CALM_DISPLACEMENT_SQ = 1.0e-7F;
    private static final int CALM_TICKS_TO_SLEEP = 20;
    private static final int UNRENDERED_TICKS_TO_DROP = 40;

    private static final Map<BlockPos, Entry> ENTRIES = new HashMap<>();
    private static long clientTicks;
    private static int activeLastTick;

    /** Per-curtain simulation state. */
    public static final class Entry {
        CurtainBlockEntity blockEntity;
        BlockState builtFor;
        ClientConfig.Quality builtQuality;
        boolean builtSplit;
        ClothSim sim;
        /** Scratch buffer the renderer fills with interpolated positions. */
        public float[] renderPositions;
        long lastRenderTick;
        boolean asleep;
        boolean atRest = true;
        int calmTicks;
        boolean outdoors;
        long outdoorsCheckedAt = Long.MIN_VALUE;
        final IntOpenHashSet touching = new IntOpenHashSet();
        final List<Cylinder> colliders = new ArrayList<>();
        final List<Cylinder> pool = new ArrayList<>();

        public ClothSim sim() {
            return sim;
        }
    }

    private SimManager() {
    }

    /** Get (creating or rebuilding as needed) the simulation for a curtain about to be rendered. */
    public static Entry get(CurtainBlockEntity be) {
        Entry e = ENTRIES.computeIfAbsent(be.getBlockPos().immutable(), p -> new Entry());
        BlockState state = be.getBlockState();
        ClientConfig.Quality quality = ClientConfig.quality();
        boolean split = isLoneWoolCurtain(be, state);
        if (e.blockEntity != be || e.builtFor != state || e.builtQuality != quality || e.builtSplit != split || e.sim == null) {
            e.blockEntity = be;
            e.builtFor = state;
            e.builtQuality = quality;
            e.builtSplit = split;
            e.sim = build(state, quality, split);
            e.renderPositions = new float[e.sim.count * 3];
            e.asleep = false;
            e.atRest = true;
            e.calmTicks = 0;
            e.touching.clear();
        }
        e.lastRenderTick = clientTicks;
        return e;
    }

    /**
     * A wool curtain on its own is two panels that part in the middle. Wool curtains side by side
     * along the same axis are one panel each, so a row reads as a wide curtain of panels.
     */
    static boolean isLoneWoolCurtain(CurtainBlockEntity be, BlockState state) {
        if (!(state.getBlock() instanceof CurtainBlock cb) || cb.getCurtainType() != CurtainType.WOOL || be.getLevel() == null) {
            return false;
        }
        Direction.Axis axis = state.getValue(CurtainBlock.AXIS);
        BlockPos pos = be.getBlockPos();
        return !isWoolNeighbor(be, pos.relative(axis, 1), axis) && !isWoolNeighbor(be, pos.relative(axis, -1), axis);
    }

    private static boolean isWoolNeighbor(CurtainBlockEntity be, BlockPos pos, Direction.Axis axis) {
        BlockState other = be.getLevel().getBlockState(pos);
        return other.getBlock() instanceof CurtainBlock cb && cb.getCurtainType() == CurtainType.WOOL
                && other.getValue(CurtainBlock.AXIS) == axis;
    }

    static ClothSim build(BlockState state, ClientConfig.Quality quality, boolean split) {
        CurtainType type = state.getBlock() instanceof CurtainBlock cb ? cb.getCurtainType() : CurtainType.WOOL;
        int length = state.getValue(CurtainBlock.LENGTH);
        int q = quality.ordinal(); // 0 low, 1 medium, 2 high
        return switch (type) {
            case WOOL -> new ClothSim(split
                    ? ClothLayout.panels(2, new int[]{3, 5, 7}[q], new int[]{4, 8, 12}[q], length,
                            0F, 0F, SimParams.WOOL.shearStiffness(), SimParams.WOOL.bendStiffness())
                    : ClothLayout.panels(1, new int[]{5, 9, 13}[q], new int[]{4, 8, 12}[q], length,
                    0F, 0F, SimParams.WOOL.shearStiffness(), SimParams.WOOL.bendStiffness()), SimParams.WOOL);
            case PLASTIC -> new ClothSim(ClothLayout.panels(4, new int[]{2, 3, 3}[q], new int[]{4, 8, 12}[q], length,
                    0.06F, 0.012F, SimParams.PLASTIC.shearStiffness(), SimParams.PLASTIC.bendStiffness()), SimParams.PLASTIC);
            case BEADED -> new ClothSim(ClothLayout.strands(new int[]{6, 8, 10}[q], new int[]{5, 7, 9}[q], length,
                    SimParams.BEADED.bendStiffness()), SimParams.BEADED);
        };
    }

    public static void clear() {
        ENTRIES.clear();
        activeLastTick = 0;
    }

    public static String debugLine() {
        int asleep = 0;
        for (Entry e : ENTRIES.values()) {
            if (e.asleep || e.atRest) {
                asleep++;
            }
        }
        return "[Curtain Doors] curtains: " + ENTRIES.size() + ", simulating: " + activeLastTick + ", idle: " + asleep;
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        clientTicks++;

        Iterator<Entry> it = ENTRIES.values().iterator();
        while (it.hasNext()) {
            Entry e = it.next();
            if (e.blockEntity == null || e.blockEntity.isRemoved() || e.blockEntity.getLevel() != level
                    || clientTicks - e.lastRenderTick > UNRENDERED_TICKS_TO_DROP) {
                it.remove();
            }
        }

        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        double maxDist = ClientConfig.simDistance();
        double maxDistSq = maxDist * maxDist;
        List<Entry> candidates = new ArrayList<>(ENTRIES.size());
        for (Entry e : ENTRIES.values()) {
            if (e.blockEntity.getBlockPos().getCenter().distanceToSqr(cam) <= maxDistSq) {
                candidates.add(e);
            } else if (!e.atRest) {
                // Out of range: hang still at the rest pose.
                e.sim.reset();
                e.atRest = true;
                e.asleep = true;
                e.touching.clear();
            }
        }
        int maxActive = ClientConfig.maxActiveSims();
        if (candidates.size() > maxActive) {
            candidates.sort(Comparator.comparingDouble(e -> e.blockEntity.getBlockPos().getCenter().distanceToSqr(cam)));
            for (int i = maxActive; i < candidates.size(); i++) {
                Entry e = candidates.get(i);
                if (!e.atRest) {
                    e.sim.reset();
                    e.atRest = true;
                    e.asleep = true;
                }
            }
            candidates = candidates.subList(0, maxActive);
        }

        float time = (level.getGameTime() % 24000L) / 20F;
        int active = 0;
        for (Entry e : candidates) {
            if (tickEntry(level, e, time)) {
                active++;
            }
        }
        activeLastTick = active;
    }

    /** @return true if the sim was stepped this tick */
    private static boolean tickEntry(ClientLevel level, Entry e, float time) {
        CurtainBlockEntity be = e.blockEntity;
        BlockState state = be.getBlockState();
        if (!(state.getBlock() instanceof CurtainBlock block)) {
            return false;
        }
        Direction.Axis axis = state.getValue(CurtainBlock.AXIS);
        int length = state.getValue(CurtainBlock.LENGTH);
        BlockPos top = be.getBlockPos();
        BlockPos bottom = top.below(length - 1);
        AABB bounds = be.getCurtainBounds();

        // Entities near the curtain become cylinders in local space.
        e.colliders.clear();
        List<Entity> entities = level.getEntities((Entity) null, bounds.inflate(0.6, 0.1, 0.6), en -> !en.isSpectator());
        AABB contactSlab = axis == Direction.Axis.X ? bounds.deflate(0, 0, 0.4) : bounds.deflate(0.4, 0, 0);
        IntOpenHashSet stillTouching = new IntOpenHashSet();
        for (int i = 0; i < entities.size(); i++) {
            Entity en = entities.get(i);
            AABB box = en.getBoundingBox();
            double cx = (box.minX + box.maxX) * 0.5 - bottom.getX();
            double cz = (box.minZ + box.maxZ) * 0.5 - bottom.getZ();
            double vx = (en.getX() - en.xo) * 20.0;
            double vz = (en.getZ() - en.zo) * 20.0;
            Cylinder c = i < e.pool.size() ? e.pool.get(i) : add(e.pool);
            c.set(CurtainSpace.localX(axis, cx, cz), CurtainSpace.localZ(axis, cx, cz),
                    (float) Math.max(box.getXsize(), box.getZsize()) * 0.5F,
                    (float) (box.minY - bottom.getY()), (float) (box.maxY - bottom.getY()),
                    CurtainSpace.dirX(axis, vx, vz), CurtainSpace.dirZ(axis, vx, vz));
            e.colliders.add(c);

            if (box.intersects(contactSlab)) {
                stillTouching.add(en.getId());
                if (!e.touching.contains(en.getId()) && vx * vx + vz * vz > 0.25) {
                    playContactSound(level, block.getCurtainType(), en);
                }
            }
        }
        e.touching.clear();
        e.touching.addAll(stillTouching);

        float wind = 0F;
        if (ClientConfig.wind()) {
            if (level.getGameTime() - e.outdoorsCheckedAt > 100) {
                Direction normal = axis == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
                e.outdoors = level.canSeeSky(bottom.relative(normal)) || level.canSeeSky(bottom.relative(normal.getOpposite()));
                e.outdoorsCheckedAt = level.getGameTime();
            }
            if (e.outdoors) {
                wind = (level.isRaining() ? 3.0F : 1.2F) * (0.5F + 0.5F * (float) Math.sin(time * 0.31F + top.getX() * 0.7F + top.getZ() * 1.3F));
            }
        }

        boolean disturbed = !e.colliders.isEmpty() || wind != 0F;
        if (e.asleep && !disturbed) {
            return false;
        }
        e.asleep = false;
        e.atRest = false;

        ClothSim sim = e.sim;
        sim.beginTick();
        for (int s = 0; s < SUBSTEPS; s++) {
            sim.step(SUBSTEP_DT, e.colliders, wind, time + s * SUBSTEP_DT);
        }
        if (sim.hasInvalidState()) {
            sim.reset();
        }

        if (!disturbed && sim.maxTickDisplacementSq() < CALM_DISPLACEMENT_SQ) {
            if (++e.calmTicks >= CALM_TICKS_TO_SLEEP) {
                e.asleep = true;
                sim.beginTick(); // freeze interpolation on the final pose
            }
        } else {
            e.calmTicks = 0;
        }
        return true;
    }

    private static Cylinder add(List<Cylinder> pool) {
        Cylinder c = new Cylinder();
        pool.add(c);
        return c;
    }

    private static void playContactSound(ClientLevel level, CurtainType type, Entity entity) {
        if (!ClientConfig.contactSounds()) {
            return;
        }
        Holder<SoundEvent> sound;
        float volume;
        switch (type) {
            case PLASTIC -> { sound = ModSounds.PLASTIC_SWISH; volume = 0.5F; }
            case BEADED -> { sound = ModSounds.BEADS_CLACK; volume = 0.6F; }
            default -> { sound = ModSounds.WOOL_BRUSH; volume = 0.4F; }
        }
        float pitch = 0.9F + level.random.nextFloat() * 0.2F;
        level.playLocalSound(entity.getX(), entity.getY() + entity.getBbHeight() * 0.5, entity.getZ(),
                sound.value(), SoundSource.BLOCKS, volume, pitch, false);
    }
}
