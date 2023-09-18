package com.github.matkubiak.lavaburns;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

import java.util.ArrayList;

@Mod(LavaBurns.MODID)
public class LavaBurns {
    public static final String MODID = "lava_burns";
    static ArrayList<Vec3i> possiblePositions;

    public LavaBurns() {
        MinecraftForge.EVENT_BUS.register(this); // Register ourselves for server and other game events we are interested in
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SPEC); // Register config
    }

    public static void setupPositions() {
        possiblePositions = new ArrayList<>();

        // RADIUS
        final float radF2 = Config.getBurnRadius() * Config.getBurnRadius();
        final int radI = (int) Config.getBurnRadius();
        int x = -radI-1, y = -radI, z = -radI;
        while (true) {
            x++;
            if (x == radI + 1) { x = -radI; y++; }
            if (y == radI + 1) { y = -radI; z++; }
            if (z == radI + 1) break;

            if (x*x + y*y + z*z <= radF2)
                possiblePositions.add(new Vec3i(x, y, z));
        }
    }

    // UTIL
    private Vec3 toVec3(Vec3i vec) {
        return new Vec3(vec.getX(), vec.getY(), vec.getZ());
    }
    private Vec3 toVec3(Player player) {
        return player.getPosition(0);
    }
    private Vec3i toVec3i(Vec3 vec) {
        return new Vec3i((int) vec.x, (int) vec.y, (int) vec.z);
    }

    // LOGIC

    boolean isBurningSource(Block block) {
        return block == Blocks.LAVA || block == Blocks.LAVA_CAULDRON;
    }
    boolean isBurningBlocker(Block block) {
        return (block != Blocks.AIR);
        // return Block.isShapeFullBlock(block.getCollisionShape(null, null, null, null));
    }
    private void burnPlayer(Player player) {
        player.setSecondsOnFire(Config.getBurnDuration());
        if (player.isInWater() && !Config.getWaterProtection())
            player.hurt(player.level().damageSources().onFire(), 1);
    }
    private boolean traceForBlockers(Vec3 start, Vec3 end, Level level) {
        Vec3 offsetF = end.subtract(start).normalize();
        Vec3 iterF = start;

        double distanceLeft = end.distanceTo(start);

        while (true) {
            iterF = iterF.add(offsetF);
            Vec3i iterI = new Vec3i((int) iterF.x, (int) iterF.y, (int) iterF.z);
            distanceLeft -= 1.0d;

            Block block = level.getBlockState(new BlockPos(iterI.getX(), iterI.getY(), iterI.getZ())).getBlock();

            if (isBurningSource(block) || distanceLeft <= 0)
                break;

            if (isBurningBlocker(block)) {
                return true;
            }
        }
        return false;
    }

    // EVENTS

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        Player player = event.player;

        // HOLDABLES
        if (event.player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == Items.LAVA_BUCKET ||
                event.player.getItemInHand(InteractionHand.OFF_HAND).getItem() == Items.LAVA_BUCKET) {
            burnPlayer(event.player);
            return;
        }

        // SOURCES
        ArrayList<Vec3i> sources = new ArrayList<>();
        for (Vec3i pos : possiblePositions) {
            Vec3i possible = toVec3i(toVec3(player)).offset(pos);
            if (isBurningSource(event.player.level().getBlockState(new BlockPos(possible.getX(), possible.getY(), possible.getZ())).getBlock()))
                sources.add(possible);
        }

        // BLOCKERS
        for (Vec3i source : sources) {
            if (!traceForBlockers(toVec3(player), toVec3(source), event.player.level())) {
                burnPlayer(event.player);
                return;
            }

            if (!player.isVisuallyCrawling() && !traceForBlockers(toVec3(player).add(0, 1,0), toVec3(source), event.player.level())) {
                burnPlayer(event.player);
                return;
            }
        }
    }
}
