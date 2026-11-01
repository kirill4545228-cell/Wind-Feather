package com.example.windfeather;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

public class UndeadDrops {
    private static final float BASE_CHANCE = 0.05F;     // 5%
    private static final float LOOTING_BONUS = 0.02F;   // +2% за уровень Добычи

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.getWorld().isClient) return;
            if (entity.getGroup() != EntityGroup.UNDEAD) return;
            if (!(source.getAttacker() instanceof PlayerEntity player)) return;

            int looting = EnchantmentHelper.getLooting(player);
            float chance = BASE_CHANCE + LOOTING_BONUS * looting;

            if (entity.getRandom().nextFloat() < chance) {
                entity.dropStack(new ItemStack(WindFeatherMod.GRAVE_INGOT));
            }
        });
    }
}
