package com.github.matkubiak.lavaburns;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = LavaBurns.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static ForgeConfigSpec.BooleanValue
        BUCKET_BURNS = BUILDER
            .comment(" Whether holding a bucket of lava will burn the player")
            .define("bucketBurns", true),
        WATER_PROTECTION_SPEC = BUILDER
            .comment(" Whether being in water will protect the player from burning")
            .define("waterProtection", false);

    private static ForgeConfigSpec.ConfigValue<Integer>
        BURN_DURATION_SPEC =  BUILDER
            .comment(" For how many seconds to set the player on fire")
            .define("burnDuration", 1);

    private static boolean bucketBurns, waterProtection;
    private static int burnDuration;
    public static boolean getBucketBurns() {
        return bucketBurns;
    }
    public static boolean getWaterProtection() {
        return waterProtection;
    }
    public static int getBurnDuration() {
        return burnDuration;
    }

    public static ForgeConfigSpec SPEC = BUILDER.build();

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        bucketBurns = BUCKET_BURNS.get();
        waterProtection = WATER_PROTECTION_SPEC.get();
        burnDuration = BURN_DURATION_SPEC.get();
    }
}
