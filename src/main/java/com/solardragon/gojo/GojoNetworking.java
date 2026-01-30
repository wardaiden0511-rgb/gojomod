package com.solardragon.gojo;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class GojoNetworking {
    public static final Identifier ENERGY_SYNC_ID = Identifier.of("gojo_moveset", "energy_sync");
    public static final Identifier STATUS_SYNC_ID = Identifier.of("gojo_moveset", "status_sync");
    
    public static void initializeServer() {
        // Register server networking handlers if needed
    }
    
    public static void sendEnergySync(ServerPlayerEntity player, float energy, float max) {
        // For now, we'll use a simpler approach - the GUI will be updated through direct method calls
        // This avoids networking complexity for now
    }
    
    public static void sendStatusSync(ServerPlayerEntity player, boolean infinity, boolean sixEyes, boolean blueAmplified, boolean reverseCursed, String activeAbility) {
        // For now, we'll use a simpler approach - the GUI will be updated through direct method calls
        // This avoids networking complexity for now
    }
}
