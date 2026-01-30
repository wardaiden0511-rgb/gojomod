package com.solardragon.gojo;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityCombatEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.command.CommandManager;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GojoMovesetMod implements ModInitializer {
    public static final String MOD_ID = "gojo_moveset";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final Identifier CURSED_ENERGY_SYNC = Identifier.of(MOD_ID, "cursed_energy_sync");
    public static final Identifier INFINITY_ACTIVE = Identifier.of(MOD_ID, "infinity_active");
    public static final Identifier LIMITLESS_ACTIVE = Identifier.of(MOD_ID, "limitless_active");
    public static final Identifier DOMAIN_EXPANSION = Identifier.of(MOD_ID, "domain_expansion");

    // Cursed energy item
    public static final Item CURSED_ENERGY_ITEM = Registry.register(
        Registries.ITEM,
        Identifier.of(MOD_ID, "cursed_energy_item"),
        new CursedEnergyItem(new Item.Settings().maxCount(1))
    );

    @Override
    public void onInitialize() {
        GojoNetworking.initializeServer();
        LOGGER.info("=== GOJO MOD INITIALIZATION START ===");

        // Register commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("gojo")
                .executes(context -> {
                    PlayerEntity player = context.getSource().getPlayer();
                    if (player != null) activateGojoMode(player);
                    return 1;
                })
                .then(CommandManager.literal("infinity")
                    .executes(ctx -> { runAbility(ctx, GojoAbilityManager::activateInfinity); return 1; }))
                .then(CommandManager.literal("sixeyes")
                    .executes(ctx -> { runAbility(ctx, GojoAbilityManager::activateSixEyes); return 1; }))
                .then(CommandManager.literal("blue")
                    .executes(ctx -> { runAbility(ctx, GojoAbilityManager::activateBlue); return 1; }))
                .then(CommandManager.literal("red")
                    .executes(ctx -> { runAbility(ctx, GojoAbilityManager::activateRed); return 1; }))
                .then(CommandManager.literal("hollowpurple")
                    .executes(ctx -> { runAbility(ctx, GojoAbilityManager::activateHollowPurple); return 1; }))
                .then(CommandManager.literal("domain")
                    .executes(ctx -> { runAbility(ctx, GojoAbilityManager::activateDomainExpansion); return 1; }))
                .then(CommandManager.literal("blueamplified")
                    .executes(ctx -> { runAbility(ctx, GojoAbilityManager::activateBlueAmplifiedPunches); return 1; }))
                .then(CommandManager.literal("reverse")
                    .executes(ctx -> { runAbility(ctx, GojoAbilityManager::activateReverseCursedTechnique); return 1; }))
                .then(CommandManager.literal("maxblue")
                    .executes(ctx -> { runAbility(ctx, GojoAbilityManager::activateMaxBlue); return 1; }))
            );
        });

        // Combat events
        ServerEntityCombatEvents.AFTER_KILLED_OTHER_ENTITY.register((world, entity, killedEntity) -> {
            if (entity instanceof PlayerEntity player) {
                GojoAbilityManager.onCombatEvent(player, killedEntity);
            }
        });

        // Server tick
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (PlayerEntity player : server.getPlayerManager().getPlayerList()) {
                GojoAbilityManager.tick(player);
            }
        });
        LOGGER.info("=== GOJO MOD INITIALIZATION COMPLETE ===");
    }

    // Activates Gojo mode for a player
    private static void activateGojoMode(PlayerEntity player) {
        GojoAbilityManager.GojoPlayerData data = GojoAbilityManager.getPlayerData(player);
        data.currentCursedEnergy = data.maxCursedEnergy;
        data.infinityActive = false;
        data.sixEyesActive = false;
        data.blueAmplifiedActive = false;
        data.reverseCursedActive = false;

        player.sendMessage(Text.literal("§d§l✦ GOJO MODE ACTIVATED ✦"), true);
        player.sendMessage(Text.literal("§7You are now Gojo Satoru - The Strongest"), true);
        player.sendMessage(Text.literal("§7Limitless Technique Unlocked"), true);

        showServerTextHUD(player);

        GojoAbilityManager.syncPlayerDataToClient(player);
        player.sendMessage(Text.literal("§6HUD Enabled - Real overlay on screen!"), true);

        LOGGER.info("Gojo mode activated for player: " + player.getName().getString());
    }

    // Helper to run abilities safely
    private static void runAbility(CommandContext<net.minecraft.server.command.ServerCommandSource> context, java.util.function.Consumer<PlayerEntity> action) {
        PlayerEntity player = context.getSource().getPlayer();
        if (player != null) action.accept(player);
    }

    // Server-side text HUD
    private static void showServerTextHUD(PlayerEntity player) {
        GojoAbilityManager.GojoPlayerData data = GojoAbilityManager.getPlayerData(player);
        player.sendMessage(Text.literal("§d=== GOJO SATORU HUD ==="), true);
        player.sendMessage(Text.literal(String.format("§bCursed Energy: §f%.0f/%.0f", data.currentCursedEnergy, data.maxCursedEnergy)), true);

        String activeAbilities = "§7Active: ";
        if (data.infinityActive) activeAbilities += "§bInfinity ";
        if (data.sixEyesActive) activeAbilities += "§6SixEyes ";
        if (data.blueAmplifiedActive) activeAbilities += "§9BlueAmplified ";
        if (data.reverseCursedActive) activeAbilities += "§aReverseCursed ";
        if (activeAbilities.equals("§7Active: ")) activeAbilities += "§cNone";

        player.sendMessage(Text.literal(activeAbilities), true);
    }
}
