package com.example.windfeather;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.List;

public class SurvivalFeatures {

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int tick = server.getTicks();
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                softLanding(p);
                if (tick % 20 == 0) {
                    creeperWarning(p);
                }
            }
        });
    }

    // Shift в падении = парашют
    private static void softLanding(ServerPlayerEntity p) {
        if (!p.isSneaking() || p.isOnGround() || p.isTouchingWater()
                || p.isFallFlying() || p.getAbilities().flying || p.isSpectator()) {
            return;
        }
        if (p.fallDistance > 4 || p.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            p.addStatusEffect(new StatusEffectInstance(
                    StatusEffects.SLOW_FALLING, 10, 0, true, false, false));
            p.fallDistance = 0;
        }
    }

    private static void creeperWarning(ServerPlayerEntity p) {
        if (p.isSpectator()) return;
        List<CreeperEntity> creepers = p.getServerWorld().getEntitiesByClass(
                CreeperEntity.class, p.getBoundingBox().expand(7), CreeperEntity::isAlive);
        if (creepers.isEmpty()) return;

        float min = Float.MAX_VALUE;
        for (CreeperEntity c : creepers) {
            min = Math.min(min, p.distanceTo(c));
        }
        p.sendMessage(Text.literal("Крипер рядом: " + Math.round(min) + " бл."), true);
    }
}
