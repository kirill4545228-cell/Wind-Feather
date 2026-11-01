package com.example.windfeather;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class WindFeatherClient implements ClientModInitializer {
    private static KeyBinding dashKey;
    private boolean jumpWasPressed = false;

    @Override
    public void onInitializeClient() {
        dashKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.windfeather.dash",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "key.categories.windfeather"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ClientPlayerEntity player = client.player;
            if (player == null) {
                jumpWasPressed = false;
                return;
            }

            boolean pressed = client.options.jumpKey.isPressed();
            if (pressed && !jumpWasPressed && client.currentScreen == null && canAirJump(player)) {
                ClientPlayNetworking.send(WindFeatherMod.AIR_JUMP_PACKET, PacketByteBufs.empty());
            }
            jumpWasPressed = pressed;

            while (dashKey.wasPressed()) {
                if (client.currentScreen == null) {
                    ClientPlayNetworking.send(WindFeatherMod.DASH_PACKET, PacketByteBufs.empty());
                }
            }
        });
    }

    private static boolean canAirJump(ClientPlayerEntity player) {
        return !player.isOnGround()
                && !player.getAbilities().flying
                && !player.isFallFlying()
                && !player.isClimbing()
                && !player.isTouchingWater()
                && !player.isInLava()
                && !player.hasVehicle()
                && !player.isSpectator();
    }
}
