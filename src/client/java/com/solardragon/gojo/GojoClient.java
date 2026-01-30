package com.solardragon.gojo;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.text.Text;

public class GojoClient implements ClientModInitializer {
    // HUD data
    public static float currentEnergy = 1000.0f;
    public static float maxEnergy = 1000.0f;
    public static boolean showHUD = false;
    public static String activeAbility = "NONE";
    
    // Active ability states
    public static boolean infinityActive = false;
    public static boolean sixEyesActive = false;
    public static boolean blueAmplifiedActive = false;
    public static boolean reverseCursedActive = false;
    public static boolean maxBlueActive = false;
    
    // Cooldown tracking (in ticks)
    public static int blueCooldown = 0;
    public static int redCooldown = 0;
    public static int hollowPurpleCooldown = 0;
    public static int blueAmplifiedCooldown = 0;
    public static int maxBlueCooldown = 0;
    public static int domainCooldown = 0;
    public static int sixEyesCooldown = 0;

    // Keybinds
    private static KeyBinding infinityKey;
    private static KeyBinding sixEyesKey;
    private static KeyBinding blueKey;
    private static KeyBinding redKey;
    private static KeyBinding hollowPurpleKey;
    private static KeyBinding domainKey;
    private static KeyBinding blueAmplifiedKey;
    private static KeyBinding reverseKey;

    @Override
    public void onInitializeClient() {
        // Initialize keybinds
        infinityKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gojo.infinity", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_I, "Gojo Abilities"
        ));
        sixEyesKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gojo.sixeyes", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_P, "Gojo Abilities"
        ));
        blueKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gojo.blue", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_J, "Gojo Abilities"
        ));
        redKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gojo.red", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_K, "Gojo Abilities"
        ));
        hollowPurpleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gojo.hollowpurple", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_L, "Gojo Abilities"
        ));
        domainKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gojo.domain", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_O, "Gojo Abilities"
        ));
        blueAmplifiedKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gojo.blueamplified", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_U, "Gojo Abilities"
        ));
        reverseKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.gojo.reverse", InputUtil.Type.KEYSYM, InputUtil.GLFW_KEY_R, "Gojo Abilities"
        ));

        // Register keybind handler
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                handleKeybinds(client);
            }
        });

        // Register HUD renderer
        HudRenderCallback.EVENT.register((drawContext, renderTickCounter) -> {
            renderHUD(drawContext, renderTickCounter.getTickDelta(false));
        });

        System.out.println("Gojo Client Initialized - KEYBINDS + HUD READY!");
        System.out.println("Keybinds: I(Infinity), P(Six Eyes), J(Blue), K(Red), L(Hollow Purple), O(Domain), U(Blue Amplified), R(Reverse)");
        System.out.println("Sneak+J for MAX Blue");
    }

    private void handleKeybinds(MinecraftClient client) {
        if (infinityKey.wasPressed()) {
            client.player.networkHandler.sendChatCommand("gojo infinity");
            // Toggle infinity status for GUI
            infinityActive = !infinityActive;
            if (infinityActive) {
                activeAbility = "INFINITY";
                updateEnergy(currentEnergy - 50, maxEnergy);
            } else {
                activeAbility = "NONE";
            }
        }
        if (sixEyesKey.wasPressed()) {
            if (sixEyesCooldown > 0) return; // Don't allow if on cooldown
            client.player.networkHandler.sendChatCommand("gojo sixeyes");
            // Toggle six eyes status for GUI
            sixEyesActive = !sixEyesActive;
            if (sixEyesActive) {
                activeAbility = "SIX_EYES";
                updateEnergy(currentEnergy - 30, maxEnergy);
            } else {
                activeAbility = "NONE";
                sixEyesCooldown = 600; // 30 seconds cooldown when deactivated
            }
        }
        if (blueKey.wasPressed()) {
            if (client.player.isSneaking()) {
                if (maxBlueCooldown > 0) return; // Don't allow if on cooldown
                client.player.networkHandler.sendChatCommand("gojo maxblue");
                updateEnergy(currentEnergy - 150, maxEnergy);
                activeAbility = "MAX_BLUE";
                maxBlueActive = true;
                maxBlueCooldown = 600; // 30 seconds cooldown
            } else {
                if (blueCooldown > 0) return; // Don't allow if on cooldown
                client.player.networkHandler.sendChatCommand("gojo blue");
                updateEnergy(currentEnergy - 75, maxEnergy);
                activeAbility = "BLUE";
                blueCooldown = 300; // 15 seconds cooldown
            }
        }
        if (redKey.wasPressed()) {
            if (redCooldown > 0) return; // Don't allow if on cooldown
            if (client.player.isSneaking()) {
                client.player.networkHandler.sendChatCommand("gojo red");
                updateEnergy(currentEnergy - 200, maxEnergy);
                activeAbility = "MAX_RED";
            } else {
                client.player.networkHandler.sendChatCommand("gojo red");
                updateEnergy(currentEnergy - 75, maxEnergy);
                activeAbility = "RED";
            }
            redCooldown = 300; // 15 seconds cooldown
        }
        if (hollowPurpleKey.wasPressed()) {
            if (hollowPurpleCooldown > 0) return; // Don't allow if on cooldown
            client.player.networkHandler.sendChatCommand("gojo hollowpurple");
            updateEnergy(currentEnergy - 150, maxEnergy);
            activeAbility = "HOLLOW_PURPLE";
            hollowPurpleCooldown = 900; // 45 seconds cooldown
        }
        if (domainKey.wasPressed()) {
            if (domainCooldown > 0) return; // Don't allow if on cooldown
            client.player.networkHandler.sendChatCommand("gojo domain");
            updateEnergy(currentEnergy - 300, maxEnergy);
            activeAbility = "DOMAIN";
            domainCooldown = 900; // 45 seconds cooldown
        }
        if (blueAmplifiedKey.wasPressed()) {
            if (blueAmplifiedCooldown > 0) return; // Don't allow if on cooldown
            client.player.networkHandler.sendChatCommand("gojo blueamplified");
            updateEnergy(currentEnergy - 50, maxEnergy);
            blueAmplifiedActive = true;
            activeAbility = "BLUE_AMPLIFIED";
            blueAmplifiedCooldown = 300; // 15 seconds cooldown
        }
        if (reverseKey.wasPressed()) {
            client.player.networkHandler.sendChatCommand("gojo reverse");
            updateEnergy(currentEnergy - 100, maxEnergy);
            reverseCursedActive = true;
            activeAbility = "REVERSE_CURSED";
        }
        
        // Energy regeneration simulation (client-side)
        if (client.player.age % 20 == 0) { // Every second
            updateEnergy(Math.min(currentEnergy + 5, maxEnergy), maxEnergy);
        }
        
        // Maintenance costs for active abilities
        if (infinityActive && client.player.age % 40 == 0) { // Every 2 seconds
            updateEnergy(Math.max(currentEnergy - 5, 0), maxEnergy);
            if (currentEnergy <= 0) {
                infinityActive = false;
                activeAbility = "NONE";
            }
        }
        if (sixEyesActive && client.player.age % 60 == 0) { // Every 3 seconds
            updateEnergy(Math.max(currentEnergy - 3, 0), maxEnergy);
            if (currentEnergy <= 0) {
                sixEyesActive = false;
                activeAbility = "NONE";
            }
        }
    }

    private void renderHUD(DrawContext drawContext, float tickDelta) {
        if (!showHUD) return;

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer textRenderer = client.textRenderer;

        // Background box - increased size for more info
        int x = 10;
        int y = 10;
        int width = 280;
        int height = 200;

        // Draw semi-transparent background
        drawContext.fill(x, y, x + width, y + height, 0x80000000);

        // Title
        drawContext.drawText(textRenderer, "§6§lGOJO ABILITIES", x + 5, y + 5, 0xFFFFFF, true);

        // Energy bar
        int energyBarWidth = (int) ((currentEnergy / maxEnergy) * 260);
        drawContext.fill(x + 10, y + 25, x + 10 + energyBarWidth, y + 35, 0xFF00FFFF);
        drawContext.drawText(textRenderer, "§bEnergy: " + String.format("%.0f/%.0f", currentEnergy, maxEnergy), x + 10, y + 40, 0xFFFFFF, true);

        // Active abilities section
        int yPos = y + 55;
        drawContext.drawText(textRenderer, "§e§lACTIVE ABILITIES:", x + 10, yPos, 0xFFFF00, true);
        yPos += 15;
        
        if (infinityActive) {
            drawContext.drawText(textRenderer, "§b[I] Infinity: ACTIVE", x + 10, yPos, 0x00FFFF, true);
            yPos += 12;
        }
        if (sixEyesActive) {
            drawContext.drawText(textRenderer, "§6[P] Six Eyes: ACTIVE", x + 10, yPos, 0xFFFF00, true);
            yPos += 12;
        }
        if (blueAmplifiedActive) {
            drawContext.drawText(textRenderer, "§9[U] Blue Amplified: ACTIVE", x + 10, yPos, 0x9999FF, true);
            yPos += 12;
        }
        if (reverseCursedActive) {
            drawContext.drawText(textRenderer, "§c[R] Reverse Cursed: ACTIVE", x + 10, yPos, 0xFF9999, true);
            yPos += 12;
        }
        if (maxBlueActive) {
            drawContext.drawText(textRenderer, "§1[Shift+J] MAX Blue: ACTIVE", x + 10, yPos, 0x5555FF, true);
            yPos += 12;
        }
        
        if (yPos == y + 70) { // No active abilities
            drawContext.drawText(textRenderer, "§7None", x + 10, yPos, 0xAAAAAA, true);
            yPos += 12;
        }

        // Cooldowns section
        yPos += 5;
        drawContext.drawText(textRenderer, "§c§lCOOLDOWNS:", x + 10, yPos, 0xFF5555, true);
        yPos += 12;
        
        // Update cooldowns (simulate countdown)
        if (blueCooldown > 0) blueCooldown--;
        if (redCooldown > 0) redCooldown--;
        if (hollowPurpleCooldown > 0) hollowPurpleCooldown--;
        if (blueAmplifiedCooldown > 0) blueAmplifiedCooldown--;
        if (maxBlueCooldown > 0) maxBlueCooldown--;
        if (domainCooldown > 0) domainCooldown--;
        if (sixEyesCooldown > 0) sixEyesCooldown--;
        
        boolean hasCooldowns = false;
        if (blueCooldown > 0) {
            int seconds = blueCooldown / 20;
            drawContext.drawText(textRenderer, "§7[J] Blue: " + seconds + "s", x + 10, yPos, 0xAAAAAA, true);
            yPos += 12;
            hasCooldowns = true;
        }
        if (redCooldown > 0) {
            int seconds = redCooldown / 20;
            drawContext.drawText(textRenderer, "§7[K] Red: " + seconds + "s", x + 10, yPos, 0xAAAAAA, true);
            yPos += 12;
            hasCooldowns = true;
        }
        if (hollowPurpleCooldown > 0) {
            int seconds = hollowPurpleCooldown / 20;
            drawContext.drawText(textRenderer, "§7[L] Hollow Purple: " + seconds + "s", x + 10, yPos, 0xAAAAAA, true);
            yPos += 12;
            hasCooldowns = true;
        }
        if (blueAmplifiedCooldown > 0) {
            int seconds = blueAmplifiedCooldown / 20;
            drawContext.drawText(textRenderer, "§7[U] Blue Amplified: " + seconds + "s", x + 10, yPos, 0xAAAAAA, true);
            yPos += 12;
            hasCooldowns = true;
        }
        if (maxBlueCooldown > 0) {
            int seconds = maxBlueCooldown / 20;
            drawContext.drawText(textRenderer, "§7[Shift+J] MAX Blue: " + seconds + "s", x + 10, yPos, 0xAAAAAA, true);
            yPos += 12;
            hasCooldowns = true;
        }
        if (domainCooldown > 0) {
            int seconds = domainCooldown / 20;
            drawContext.drawText(textRenderer, "§7[O] Domain: " + seconds + "s", x + 10, yPos, 0xAAAAAA, true);
            yPos += 12;
            hasCooldowns = true;
        }
        if (sixEyesCooldown > 0) {
            int seconds = sixEyesCooldown / 20;
            drawContext.drawText(textRenderer, "§7[P] Six Eyes: " + seconds + "s", x + 10, yPos, 0xAAAAAA, true);
            yPos += 12;
            hasCooldowns = true;
        }
        
        if (!hasCooldowns) {
            drawContext.drawText(textRenderer, "§aAll Ready", x + 10, yPos, 0x55FF55, true);
            yPos += 12;
        }

        // Available abilities section
        yPos += 5;
        drawContext.drawText(textRenderer, "§a§lAVAILABLE:", x + 10, yPos, 0x55FF55, true);
        yPos += 12;
        
        // Show available abilities (not on cooldown and not active)
        if (!infinityActive && blueCooldown <= 0) {
            drawContext.drawText(textRenderer, "§a[I] Infinity (50)", x + 10, yPos, 0x55FF55, true);
            yPos += 12;
        }
        if (!sixEyesActive && sixEyesCooldown <= 0) {
            drawContext.drawText(textRenderer, "§a[P] Six Eyes (30)", x + 10, yPos, 0x55FF55, true);
            yPos += 12;
        }
        if (blueCooldown <= 0) {
            drawContext.drawText(textRenderer, "§a[J] Blue (75)", x + 10, yPos, 0x55FF55, true);
            yPos += 12;
        }
        if (redCooldown <= 0) {
            drawContext.drawText(textRenderer, "§a[K] Red (75)", x + 10, yPos, 0x55FF55, true);
            yPos += 12;
        }
        if (hollowPurpleCooldown <= 0) {
            drawContext.drawText(textRenderer, "§a[L] Hollow Purple (150)", x + 10, yPos, 0x55FF55, true);
            yPos += 12;
        }
        if (domainCooldown <= 0) {
            drawContext.drawText(textRenderer, "§a[O] Domain (300)", x + 10, yPos, 0x55FF55, true);
            yPos += 12;
        }
        if (blueAmplifiedCooldown <= 0) {
            drawContext.drawText(textRenderer, "§a[U] Blue Amplified (50)", x + 10, yPos, 0x55FF55, true);
            yPos += 12;
        }
        if (reverseCursedActive) {
            drawContext.drawText(textRenderer, "§a[R] Reverse Cursed (100)", x + 10, yPos, 0x55FF55, true);
            yPos += 12;
        }

        // Current move indicator
        if (!activeAbility.equals("NONE")) {
            drawContext.drawText(textRenderer, "§dCurrent: " + activeAbility, x + 10, y + height - 20, 0xFF55FF, true);
        }
    }
    
    public static void showHUDOverlay() {
        showHUD = true;
        System.out.println("HUD Overlay Activated - Real overlay on screen!");
    }
    
    public static void hideHUDOverlay() {
        showHUD = false;
        System.out.println("HUD Overlay Hidden");
    }
    
    public static void updateEnergy(float current, float max) {
        currentEnergy = current;
        maxEnergy = max;
        // Auto-show HUD when energy is updated (Gojo mode activated)
        if (!showHUD && current > 0) {
            showHUDOverlay();
        }
    }
    
    public static void updateStatus(boolean infinity, boolean sixEyes, boolean blueAmplified, boolean reverseCursed, boolean maxBlue) {
        infinityActive = infinity;
        sixEyesActive = sixEyes;
        blueAmplifiedActive = blueAmplified;
        reverseCursedActive = reverseCursed;
        // Note: maxBlue is tracked but not shown in HUD per user request
    }
    
    public static boolean isInfinityActive() { return infinityActive; }
    public static boolean isSixEyesActive() { return sixEyesActive; }
    public static boolean isBlueAmplifiedActive() { return blueAmplifiedActive; }
    public static boolean isReverseCursedActive() { return reverseCursedActive; }
    public static boolean isHUDVisible() { return showHUD; }

    public static String getActiveAbility() {
        return activeAbility;
    }

    public static float getCurrentEnergy() {
        return currentEnergy;
    }

    public static float getMaxEnergy() {
        return maxEnergy;
    }
}
