package com.example.windfeather;

import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

public class WindFeatherItem extends Item {

    public WindFeatherItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (!world.isClient) {
            Vec3d look = user.getRotationVec(1.0F);
            user.addVelocity(look.x * 1.2, 0.6 + look.y * 0.5, look.z * 1.2);
            user.velocityModified = true;
            user.fallDistance = 0;
            user.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 100));

            world.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS, 1.0F, 1.2F);

            ((ServerWorld) world).spawnParticles(ParticleTypes.CLOUD,
                    user.getX(), user.getY(), user.getZ(), 20, 0.4, 0.1, 0.4, 0.05);
        }

        user.getItemCooldownManager().set(this, 60);
        return TypedActionResult.success(stack, world.isClient());
    }
}
