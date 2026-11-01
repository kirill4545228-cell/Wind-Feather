package com.example.windfeather;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class WindFeatherMod implements ModInitializer {
    public static final String MOD_ID = "windfeather";

    public static final Item WIND_FEATHER =
            new WindFeatherItem(new Item.Settings().maxCount(1));

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM,
                new Identifier(MOD_ID, "wind_feather"), WIND_FEATHER);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(entries -> entries.add(WIND_FEATHER));
    }
}
