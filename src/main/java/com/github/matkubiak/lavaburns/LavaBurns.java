package com.github.matkubiak.lavaburns;

import com.github.matkubiak.lavaburns.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod(LavaBurns.MODID)
public class LavaBurns {
    public static final String MODID = "lava_burns";
    private Vec3i[] adjacentStanding, adjacentCrawling, sameBorderStanding, sameBorderCrawling;

    public LavaBurns() {
        MinecraftForge.EVENT_BUS.register(this); // Register ourselves for server and other game events we are interested in

        adjacentStanding = new Vec3i[]{
                new Vec3i(-1, 0, 0), new Vec3i(1, 0, 0), // LOWER LEFT and RIGHT
                new Vec3i(0, -1, 0), new Vec3i(0, 2, 0), // DOWN and UP
                new Vec3i(0, 0, -1), new Vec3i(0, 0, 1), // LOWER BACK and FRONT
                new Vec3i(-1, 1, 0), new Vec3i(1, 1, 0), // UPPER LEFT and UPPER RIGHT
                new Vec3i(0, 1, -1), new Vec3i(0, 1, 1) // UPPER BACK AND UPPER FRONT
        };

        adjacentCrawling = new Vec3i[]{
                new Vec3i(-1, 0, 0), new Vec3i(1, 0, 0), // LEFT and RIGHT
                new Vec3i(0, -1, 0), new Vec3i(0, 1, 0), // DOWN and UP
                new Vec3i(0, 0, -1), new Vec3i(0, 0, 1) // BACK and FRONT
        };

        sameBorderStanding = new Vec3i[]{
                new Vec3i(-1, 2, 0), new Vec3i(1, 2, 0), // UP
                new Vec3i(0, 2, -1), new Vec3i(0, 2, 1),

                new Vec3i(1, 1, 1), new Vec3i(-1, 1, 1), // UPPER MID
                new Vec3i(1, 1, -1), new Vec3i(-1, 1, -1),

                new Vec3i(1, 0, 1), new Vec3i(-1, 0, 1), // LOWER MID
                new Vec3i(1, 0, -1), new Vec3i(-1, 0, -1),

                new Vec3i(-1, -1, 0), new Vec3i(1, -1, 0), // DOWN
                new Vec3i(0, -1, -1), new Vec3i(0, -1, 1)
        };

        sameBorderCrawling = new Vec3i[]{
                new Vec3i(-1, 1, 0), new Vec3i(1, 1, 0), // UP
                new Vec3i(0, 1, -1), new Vec3i(0, 1, 1),

                new Vec3i(1, 0, 1), new Vec3i(-1, 0, 1), // MID
                new Vec3i(1, 0, -1), new Vec3i(-1, 0, -1),

                new Vec3i(-1, -1, 0), new Vec3i(1, -1, 0), // DOWN
                new Vec3i(0, -1, -1), new Vec3i(0, -1, 1)
        };
    }

    boolean isBurningSource(Block block) {
        return block == Blocks.LAVA || block == Blocks.LAVA_CAULDRON;
    }

    boolean isBurningBlocker(Block block) {
        if (block == Blocks.AIR || block == Blocks.WATER)
            return false;
        return true;
        // return Block.isShapeFullBlock(block.getCollisionShape(null, null, null, null));
    }

    private void burnPlayer(Player player) {
        player.setSecondsOnFire(1);
        if (player.isInWater())
            player.hurt(player.level().damageSources().onFire(), 1);
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        BlockPos pos = event.player.blockPosition();
        Level level = event.player.level();

        // SET UP STANDING/CRAWLING
        Vec3i[] posAdjacent;
        Vec3i[] posSameBorder;
        if (!event.player.isVisuallyCrawling()) {
            posAdjacent = adjacentStanding;
            posSameBorder = sameBorderStanding;
        } else {
            posAdjacent = adjacentCrawling;
            posSameBorder = sameBorderCrawling;
        }

        // HANDLING ADJACENT BLOCKS
        for (Vec3i check_pos : posAdjacent) {
            if (isBurningSource(level.getBlockState(pos.offset(check_pos)).getBlock())) {
                burnPlayer(event.player);
                return;
            }
        }

        // HANDLING BLOCKS THAT HAVE THE SAME BORDER
        for (Vec3i check_pos : posSameBorder) {
            if (isBurningSource(level.getBlockState(pos.offset(check_pos)).getBlock())) {

                Vec3i x = new Vec3i(check_pos.getX(), 0, 0);
                Vec3i y = new Vec3i(0, check_pos.getY(), 0);
                Vec3i z = new Vec3i(0, 0, check_pos.getZ());

                boolean isCovered = true;
                for (Vec3i cover_pos : new Vec3i[]{x, y, z}) {
                    Block theBlock = level.getBlockState(pos.offset(cover_pos)).getBlock();
                    if (!isBurningBlocker(theBlock) && !cover_pos.equals(Vec3i.ZERO)) {
                        isCovered = false;
                        break;
                    }
                }

                if (!isCovered) {
                    burnPlayer(event.player);
                    return;
                }
            }
        }

        // HOLDING A BUCKET OF LAVA
        if (event.player.getItemInHand(InteractionHand.MAIN_HAND).getItem() == Items.LAVA_BUCKET ||
                event.player.getItemInHand(InteractionHand.OFF_HAND).getItem() == Items.LAVA_BUCKET) {
            burnPlayer(event.player);
        }
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {}
    }
}
