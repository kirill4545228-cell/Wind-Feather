package com.example.windfeather;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.damage.DamageTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WindFeatherMod implements ModInitializer {
    public static final String MOD_ID = "windfeather";

    public static final Identifier AIR_JUMP_PACKET = new Identifier(MOD_ID, "air_jump");
    public static final Identifier DASH_PACKET = new Identifier(MOD_ID, "dash");

    public static final Item WIND_FEATHER =
            new WindFeatherItem(new Item.Settings().maxCount(1));

    public static final Item GRAVE_INGOT =
            new Item(new Item.Settings());

    public static final Item SLEDGEHAMMER =
            new SledgehammerItem(SledgehammerMaterial.INSTANCE, 6, -3.4F,
                    new Item.Settings().maxCount(1));

    private static final int DASH_COOLDOWN_TICKS = 40;

    // Всего прыжков (1 = выкл, 2 = двойной, ...)
    private static final Map<UUID, Integer> MAX_JUMPS = new HashMap<>();
    // Сколько воздушных прыжков уже потрачено
    private static final Map<UUID, Integer> USED_JUMPS = new HashMap<>();
    private static final Map<UUID, Long> LAST_DASH = new HashMap<>();

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM,
                new Identifier(MOD_ID, "wind_feather"), WIND_FEATHER);
        Registry.register(Registries.ITEM,
                new Identifier(MOD_ID, "grave_ingot"), GRAVE_INGOT);
        Registry.register(Registries.ITEM,
                new Identifier(MOD_ID, "sledgehammer"), SLEDGEHAMMER);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(entries -> entries.add(WIND_FEATHER));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.INGREDIENTS)
                .register(entries -> entries.add(GRAVE_INGOT));
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.COMBAT)
                .register(entries -> entries.add(SLEDGEHAMMER));

        registerCommands();
        TreeAndVeinFeatures.register();
        SurvivalFeatures.register();
        UndeadDrops.register();

        ServerPlayNetworking.registerGlobalReceiver(AIR_JUMP_PACKET,
                (server, player, handler, buf, sender) ->
                        server.execute(() -> handleAirJump(player)));

        ServerPlayNetworking.registerGlobalReceiver(DASH_PACKET,
                (server, player, handler, buf, sender) ->
                        server.execute(() -> handleDash(player)));

        // Сброс счётчика прыжков при приземлении
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                if (p.isOnGround() || p.isTouchingWater() || p.isClimbing()
                        || p.getAbilities().flying) {
                    USED_JUMPS.remove(p.getUuid());
                }
            }
        });

        // Перо в руке отменяет урон от падения
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof PlayerEntity p && source.isOf(DamageTypes.FALL)
                    && (p.getMainHandStack().isOf(WIND_FEATHER)
                    || p.getOffHandStack().isOf(WIND_FEATHER))) {
                return false;
            }
            return true;
        });
    }

    private static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("multijump")
                        .then(CommandManager.argument("jumps", IntegerArgumentType.integer(1, 4))
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayerOrThrow();
                                    int jumps = IntegerArgumentType.getInteger(ctx, "jumps");
                                    MAX_JUMPS.put(player.getUuid(), jumps);
                                    USED_JUMPS.remove(player.getUuid());

                                    String name = switch (jumps) {
                                        case 2 -> "Двойной прыжок включён";
                                        case 3 -> "Тройной прыжок включён";
                                        case 4 -> "Четверной прыжок включён";
                                        default -> "Мультипрыжок выключен";
                                    };
                                    ctx.getSource().sendFeedback(() -> Text.literal(name), false);
                                    return jumps;
                                }))));
    }

    private static void handleAirJump(ServerPlayerEntity player) {
        if (!player.isAlive() || player.isSpectator() || player.isOnGround()) return;

        UUID id = player.getUuid();
        int extra = MAX_JUMPS.getOrDefault(id, 1) - 1;
        int used = USED_JUMPS.getOrDefault(id, 0);
        if (extra <= 0 || used >= extra) return;

        USED_JUMPS.put(id, used + 1);

        Vec3d v = player.getVelocity();
        player.setVelocity(v.x, 0.55, v.z);
        player.velocityModified = true;
        player.fallDistance = 0;

        ServerWorld world = player.getServerWorld();
        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PHANTOM_FLAP, SoundCategory.PLAYERS,
                1.0F, 1.0F + 0.2F * used);
        world.spawnParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY(), player.getZ(), 12, 0.3, 0.05, 0.3, 0.02);

        int left = extra - (used + 1);
        player.sendMessage(Text.literal("Прыжков осталось: " + left), true);
    }

    private static void handleDash(ServerPlayerEntity player) {
        if (!player.isAlive() || player.isSpectator()) return;

        ServerWorld world = player.getServerWorld();
        long now = world.getTime();
        Long last = LAST_DASH.get(player.getUuid());
        if (last != null && now - last < DASH_COOLDOWN_TICKS) return;
        LAST_DASH.put(player.getUuid(), now);

        Vec3d look = player.getRotationVec(1.0F);
        Vec3d dir = new Vec3d(look.x, 0, look.z).normalize().multiply(1.6);
        player.setVelocity(dir.x, Math.max(player.getVelocity().y, 0.1), dir.z);
        player.velocityModified = true;

        world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, SoundCategory.PLAYERS, 1.0F, 1.3F);
        world.spawnParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY() + 0.5, player.getZ(), 25, 0.3, 0.3, 0.3, 0.05);
    }
}
