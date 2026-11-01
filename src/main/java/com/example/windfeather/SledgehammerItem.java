package com.example.windfeather;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SwordItem;
import net.minecraft.item.ToolMaterial;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

public class SledgehammerItem extends SwordItem {

    public SledgehammerItem(ToolMaterial material, int attackDamage, float attackSpeed, Settings settings) {
        super(material, attackDamage, attackSpeed, settings);
    }

    // Удар: отброс и замедление цели + ударная волна по соседним врагам
    @Override
    public boolean postHit(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        World world = attacker.getWorld();
        if (world instanceof ServerWorld serverWorld) {
            target.takeKnockback(1.0,
                    attacker.getX() - target.getX(),
                    attacker.getZ() - target.getZ());
            target.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 40, 1));

            DamageSource source = attacker instanceof PlayerEntity p
                    ? serverWorld.getDamageSources().playerAttack(p)
                    : serverWorld.getDamageSources().mobAttack(attacker);

            List<LivingEntity> nearby = serverWorld.getEntitiesByClass(
                    LivingEntity.class,
                    target.getBoundingBox().expand(2.5),
                    e -> e != attacker && e != target && e.isAlive() && e instanceof Monster);

            for (LivingEntity e : nearby) {
                e.damage(source, 4.0F);
                e.takeKnockback(0.6,
                        attacker.getX() - e.getX(),
                        attacker.getZ() - e.getZ());
            }

            serverWorld.spawnParticles(ParticleTypes.EXPLOSION,
                    target.getX(), target.getBodyY(0.5), target.getZ(), 1, 0, 0, 0, 0);
            serverWorld.playSound(null, target.getX(), target.getY(), target.getZ(),
                    SoundEvents.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.PLAYERS, 1.0F, 0.8F);
        }
        return super.postHit(stack, target, attacker);
    }

    // ПКМ на земле: удар о землю
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (!user.isOnGround()) {
            return TypedActionResult.pass(stack);
        }

        if (world instanceof ServerWorld serverWorld) {
            DamageSource source = serverWorld.getDamageSources().playerAttack(user);

            List<LivingEntity> victims = serverWorld.getEntitiesByClass(
                    LivingEntity.class,
                    user.getBoundingBox().expand(4.0, 1.5, 4.0),
                    e -> e != user && e.isAlive() && e instanceof Monster);

            for (LivingEntity e : victims) {
                e.damage(source, 8.0F);
                Vec3d v = e.getVelocity();
                e.setVelocity(v.x, 0.7, v.z);
                e.velocityModified = true;
            }

            serverWorld.playSound(null, user.getX(), user.getY(), user.getZ(),
                    SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.PLAYERS, 1.0F, 0.6F);
            serverWorld.spawnParticles(ParticleTypes.CLOUD,
                    user.getX(), user.getY() + 0.1, user.getZ(), 40, 2.0, 0.1, 2.0, 0.05);
            serverWorld.spawnParticles(ParticleTypes.EXPLOSION,
                    user.getX(), user.getY() + 0.1, user.getZ(), 3, 1.5, 0.1, 1.5, 0);

            stack.damage(3, user, p -> p.sendToolBreakStatus(hand));
        }

        user.getItemCooldownManager().set(this, 200);
        return TypedActionResult.success(stack, world.isClient());
    }

    @Override
    public void appendTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Удар: отброс и ударная волна по врагам").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("ПКМ на земле: удар о землю").formatted(Formatting.GRAY));
    }
}
