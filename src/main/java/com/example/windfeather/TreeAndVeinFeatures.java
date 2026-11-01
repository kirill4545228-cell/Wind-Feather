package com.example.windfeather;

import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.PickaxeItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

public class TreeAndVeinFeatures {
    private static final int MAX_TREE_BLOCKS = 160;
    private static final int MAX_VEIN_BLOCKS = 64;
    private static final TagKey<Block> ORES =
            TagKey.of(RegistryKeys.BLOCK, new Identifier("c", "ores"));

    // Защита от рекурсии: мы сами ломаем блоки, а событие срабатывает на каждый
    private static boolean busy = false;

    public static void register() {
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (busy || world.isClient || !(player instanceof ServerPlayerEntity sp) || sp.isCreative()) {
                return;
            }
            ItemStack tool = sp.getMainHandStack();

            if (state.isIn(BlockTags.LOGS) && tool.getItem() instanceof AxeItem && !sp.isSneaking()) {
                fellTree((ServerWorld) world, sp, pos, state);
            } else if (state.isIn(ORES) && tool.getItem() instanceof PickaxeItem && sp.isSneaking()) {
                mineVein((ServerWorld) world, sp, pos, state);
            }
        });
    }

    private static void fellTree(ServerWorld world, ServerPlayerEntity player,
                                 BlockPos start, BlockState startState) {
        List<BlockPos> logs = collect(world, start, s -> s.isIn(BlockTags.LOGS), MAX_TREE_BLOCKS);
        if (logs.isEmpty() || !hasNaturalLeaves(world, logs, start)) return;

        // Запоминаем, какие блоки были, чтобы потом посадить саженцы
        Map<BlockPos, Block> original = new LinkedHashMap<>();
        original.put(start, startState.getBlock());
        for (BlockPos p : logs) {
            original.put(p, world.getBlockState(p).getBlock());
        }

        breakAll(world, player, logs);
        replant(world, original);
    }

    private static void mineVein(ServerWorld world, ServerPlayerEntity player,
                                 BlockPos start, BlockState state) {
        Block ore = state.getBlock();
        List<BlockPos> vein = collect(world, start, s -> s.isOf(ore), MAX_VEIN_BLOCKS);
        breakAll(world, player, vein);
    }

    // Поиск связанных блоков (включая диагонали)
    private static List<BlockPos> collect(ServerWorld world, BlockPos start,
                                          Predicate<BlockState> match, int limit) {
        List<BlockPos> result = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        seen.add(start);
        queue.add(start);

        while (!queue.isEmpty() && result.size() < limit) {
            BlockPos cur = queue.poll();
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        if (result.size() >= limit) return result;
                        BlockPos next = cur.add(dx, dy, dz);
                        if (seen.add(next) && match.test(world.getBlockState(next))) {
                            result.add(next);
                            queue.add(next);
                        }
                    }
                }
            }
        }
        return result;
    }

    // Дерево = рядом есть хотя бы 4 естественных листа (не поставленных игроком)
    private static boolean hasNaturalLeaves(ServerWorld world, List<BlockPos> logs, BlockPos start) {
        List<BlockPos> all = new ArrayList<>(logs);
        all.add(start);
        int found = 0;
        for (BlockPos log : all) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockState s = world.getBlockState(log.add(dx, dy, dz));
                        if (s.isIn(BlockTags.LEAVES)
                                && s.contains(LeavesBlock.PERSISTENT)
                                && !s.get(LeavesBlock.PERSISTENT)) {
                            if (++found >= 4) return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    private static void breakAll(ServerWorld world, ServerPlayerEntity player, List<BlockPos> blocks) {
        busy = true;
        try {
            for (BlockPos p : blocks) {
                ItemStack tool = player.getMainHandStack();
                // Не ломаем инструмент: останавливаемся за 1 единицу до поломки
                if (tool.isEmpty() || tool.getMaxDamage() - tool.getDamage() <= 1) break;
                player.interactionManager.tryBreakBlock(p);
            }
        } finally {
            busy = false;
        }
    }

    private static void replant(ServerWorld world, Map<BlockPos, Block> original) {
        for (Map.Entry<BlockPos, Block> e : original.entrySet()) {
            Block sapling = saplingFor(e.getValue());
            if (sapling == null) continue;
            BlockPos p = e.getKey();
            if (world.getBlockState(p).isAir() && sapling.getDefaultState().canPlaceAt(world, p)) {
                world.setBlockState(p, sapling.getDefaultState());
            }
        }
    }

    private static Block saplingFor(Block log) {
        String id = Registries.BLOCK.getId(log).getPath();
        if (id.contains("dark_oak")) return Blocks.DARK_OAK_SAPLING;
        if (id.contains("oak")) return Blocks.OAK_SAPLING;
        if (id.contains("spruce")) return Blocks.SPRUCE_SAPLING;
        if (id.contains("birch")) return Blocks.BIRCH_SAPLING;
        if (id.contains("jungle")) return Blocks.JUNGLE_SAPLING;
        if (id.contains("acacia")) return Blocks.ACACIA_SAPLING;
        if (id.contains("cherry")) return Blocks.CHERRY_SAPLING;
        return null;
    }
}
