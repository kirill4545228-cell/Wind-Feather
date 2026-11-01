package com.example.windfeather;

import net.minecraft.item.ToolMaterial;
import net.minecraft.recipe.Ingredient;

public enum SledgehammerMaterial implements ToolMaterial {
    INSTANCE;

    @Override
    public int getDurability() {
        return 1500;
    }

    @Override
    public float getMiningSpeedMultiplier() {
        return 4.0F;
    }

    @Override
    public float getAttackDamage() {
        return 3.0F;
    }

    @Override
    public int getMiningLevel() {
        return 3;
    }

    @Override
    public int getEnchantability() {
        return 12;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return Ingredient.ofItems(WindFeatherMod.GRAVE_INGOT);
    }
}
