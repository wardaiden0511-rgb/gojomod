package com.solardragon.gojo;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.util.math.Box;
import net.minecraft.block.Blocks;
import net.minecraft.world.World;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class GojoAbilityManager {
    private static final Map<UUID, GojoPlayerData> playerDataMap = new HashMap<>();
    
    public static class GojoPlayerData {
        public float currentCursedEnergy = 1000.0f;
        public float maxCursedEnergy = 1000.0f;
        public float energyRegenerationRate = 5.0f;
        public int energyRegenerationTimer = 0;
        private static final int DOMAIN_RADIUS = 115;
        private static final int DOMAIN_HEIGHT = 40;
        private static final int DOMAIN_EXPAND_SPEED = 2; // blocks per tick

        // Ability cooldowns (in ticks)
        public int blueCooldown = 0;
        public int redCooldown = 0;
        public int hollowPurpleCooldown = 0;
        public int blueAmplifiedCooldown = 0;
        public int domainCooldown = 0;
        public int domainDuration = 0;
        public boolean domainActive = false;
        public int sixEyesCooldown = 0;
        public boolean sixEyesActive = false;
        public boolean infinityActive = false;
        
        
        // Active ability tracking
        public boolean blueAmplifiedActive = false;
        public boolean reverseCursedActive = false;
        public boolean maxBlueActive = false;
        public String activeAbility = "NONE";
        
        // MAX Blue cooldown
        public int maxBlueCooldown = 0;
        
        // Maintenance timers
        public int infinityMaintenanceTimer = 0;
        public int sixEyesMaintenanceTimer = 0;
        
        // Domain block storage for restoration
        public Map<BlockPos, net.minecraft.block.BlockState> originalBlocks;

        public GojoPlayerData() {
            originalBlocks = new HashMap<>();
        }
    }
    
    public static GojoPlayerData getPlayerData(PlayerEntity player) {
        return playerDataMap.computeIfAbsent(player.getUuid(), k -> new GojoPlayerData());
    }
    
    public static boolean hasEnoughCursedEnergy(GojoPlayerData data, float amount) {
        return data.currentCursedEnergy >= amount;
    }
    
    public static boolean consumeCursedEnergy(PlayerEntity player, GojoPlayerData data, float amount) {
        if (data.currentCursedEnergy >= amount) {
            data.currentCursedEnergy -= amount;
            
            // Sync energy to client GUI only
            syncPlayerDataToClient(player);
            return true;
        }
        return false;
    }
    
    public static void regenerateCursedEnergy(PlayerEntity player, GojoPlayerData data) {
        data.currentCursedEnergy = Math.min(data.currentCursedEnergy + data.energyRegenerationRate, data.maxCursedEnergy);
        
        // Sync energy to client GUI only
        syncPlayerDataToClient(player);
    }
    
    public static void activateBlueAmplifiedPunches(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);
        if (data.blueAmplifiedCooldown > 0) {
            player.sendMessage(Text.literal("Blue Amplified Punches on cooldown! (" + data.blueAmplifiedCooldown / 20 + "s)"), true);
            return;
        }
        
        if (!hasEnoughCursedEnergy(data, 50)) {
            player.sendMessage(Text.literal("Not enough cursed energy! (Requires 50)"), true);
            return;
        }
        
        if (!consumeCursedEnergy(player, data, 50)) {
            return;
        }
        
        // Set as active ability
        data.blueAmplifiedActive = true;
        data.activeAbility = "BLUE_AMPLIFIED";
        
        data.blueAmplifiedCooldown = 300; // 15 seconds cooldown
        
        World world = player.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            Vec3d lookVec = player.getRotationVector();
            Vec3d startPos = player.getEyePos();
            
            // Create enhanced blue amplified punch effect with improved cursor following and dust particles
            for (int i = 0; i < 60; i++) {
                double progress = i / 60.0;
                Vec3d currentPos = startPos.add(lookVec.multiply(i * 0.35));
                
                // Add rapid punch effect with oscillation
                double punch = Math.sin(progress * Math.PI * 12) * 0.15;
                Vec3d punchVec = new Vec3d(
                    Math.cos(progress * Math.PI * 24) * punch,
                    Math.sin(progress * Math.PI * 16) * 0.1,
                    Math.sin(progress * Math.PI * 24) * punch
                );
                Vec3d finalPos = currentPos.add(punchVec);
                
                // Main cyan dust trail
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.0f, 0.8f, 1.0f), 1.4f), // Brighter CYAN
                    finalPos.x, finalPos.y, finalPos.z,
                    10, 0.4, 0.4, 0.4, 0.1
                );
                
                // Electric blue particles
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.2f, 0.6f, 1.0f), 1.1f), // Electric blue
                    finalPos.x, finalPos.y, finalPos.z,
                    6, 0.3, 0.3, 0.3, 0.07
                );
                
                // White impact particles
                if (i % 2 == 0) {
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 1.6f), // White
                        finalPos.x, finalPos.y, finalPos.z,
                        4, 0.2, 0.2, 0.2, 0.04
                    );
                }
                
                // Create punching vortex effect
                for (int angle = 0; angle < 360; angle += 60) {
                    double radians = Math.toRadians(angle + progress * 1440);
                    double vortexRadius = 0.6 + Math.sin(progress * Math.PI * 8) * 0.3;
                    Vec3d vortexPos = finalPos.add(
                        Math.cos(radians) * vortexRadius,
                        Math.sin(radians * 3) * 0.2,
                        Math.sin(radians) * vortexRadius
                    );
                    
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(0.1f, 0.4f, 0.8f), 0.8f), // Dark blue
                        vortexPos.x, vortexPos.y, vortexPos.z,
                        2, 0.1, 0.1, 0.1, 0.03
                    );
                }
                
                // Add electric spark effects
                if (i % 4 == 0) {
                    for (int spark = 0; spark < 3; spark++) {
                        Vec3d sparkPos = finalPos.add(
                            (Math.random() - 0.5) * 0.8,
                            (Math.random() - 0.5) * 0.8,
                            (Math.random() - 0.5) * 0.8
                        );
                        
                        serverWorld.spawnParticles(
                            new DustParticleEffect(new Vector3f(0.8f, 0.9f, 1.0f), 1.3f), // Light blue
                            sparkPos.x, sparkPos.y, sparkPos.z,
                            1, 0.05, 0.05, 0.05, 0.02
                        );
                    }
                }
            }
            
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_GENERIC_EXPLODE,
                SoundCategory.PLAYERS, 1.0f, 1.5f);
        }
        
        syncPlayerDataToClient(player);
    }
    
    public static void activateInfinity(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);
        
        // Toggle Infinity
        data.infinityActive = !data.infinityActive;
        data.activeAbility = data.infinityActive ? "INFINITY" : "NONE";
        
        String status = data.infinityActive ? "§a§lInfinity ACTIVATED - You are now invincible to physical attacks!" : "§7Infinity DEACTIVATED";
        player.sendMessage(Text.literal(status), true);
        
        syncPlayerDataToClient(player);
    }
    
    public static void activateSixEyes(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);
        
        // Toggle Six Eyes
        data.sixEyesActive = !data.sixEyesActive;
        data.activeAbility = data.sixEyesActive ? "SIX_EYES" : "NONE";
        
        String status = data.sixEyesActive ? "§6§lSix Eyes ACTIVATED - Enhanced perception enabled!" : "§7Six Eyes DEACTIVATED";
        player.sendMessage(Text.literal(status), true);
        
        syncPlayerDataToClient(player);
    }
    
    public static void activateBlue(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);
        if (data.blueCooldown > 0) {
            player.sendMessage(Text.literal("Blue on cooldown! (" + data.blueCooldown / 20 + "s)"), true);
            return;
        }
        
        if (!hasEnoughCursedEnergy(data, 75)) {
            player.sendMessage(Text.literal("Not enough cursed energy! (Requires 75)"), true);
            return;
        }
        
        if (!consumeCursedEnergy(player, data, 75)) {
            return;
        }
        
        data.blueCooldown = 300; // 15 seconds cooldown
        
        World world = player.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            Vec3d lookVec = player.getRotationVector();
            Vec3d startPos = player.getEyePos();
            Vec3d targetPos = startPos.add(lookVec.multiply(6.0)); // 6 blocks in front of player
            
            // Create a blue orb that follows the cursor
            for (int frame = 0; frame < 60; frame++) { // 3 seconds of animation
                double progress = frame / 60.0;
                
                // Orb follows cursor with smooth movement
                Vec3d currentLookVec = player.getRotationVector();
                Vec3d currentTargetPos = player.getEyePos().add(currentLookVec.multiply(6.0));
                
                // Smooth interpolation between positions
                Vec3d orbPos = targetPos.lerp(currentTargetPos, Math.min(progress * 2, 1.0));
                
                // Add slight floating animation
                double floatOffset = Math.sin(progress * Math.PI * 4) * 0.2;
                Vec3d finalOrbPos = orbPos.add(0, floatOffset, 0);
                
                // Create the blue orb particles
                for (int radius = 0; radius < 2; radius++) {
                    for (int angle = 0; angle < 360; angle += 30) {
                        double radians = Math.toRadians(angle + frame * 6); // Rotating effect
                        Vec3d particleOffset = new Vec3d(
                            Math.cos(radians) * radius * 0.8,
                            Math.sin(radians * 2) * 0.3,
                            Math.sin(radians) * radius * 0.8
                        );
                        Vec3d particlePos = finalOrbPos.add(particleOffset);
                        
                        // Main cyan orb particles
                        serverWorld.spawnParticles(
                            new DustParticleEffect(new Vector3f(0.0f, 0.8f, 1.0f), 1.5f), // Bright cyan
                            particlePos.x, particlePos.y, particlePos.z,
                            3, 0.15, 0.15, 0.15, 0.05
                        );
                        
                        // Electric blue particles
                        if (radius == 0) { // Core particles
                            serverWorld.spawnParticles(
                                new DustParticleEffect(new Vector3f(0.2f, 0.6f, 1.0f), 1.2f), // Electric blue
                                particlePos.x, particlePos.y, particlePos.z,
                                2, 0.1, 0.1, 0.1, 0.03
                            );
                        }
                        
                        // White spark particles
                        if (angle % 60 == 0) { // Occasional sparks
                            serverWorld.spawnParticles(
                                new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 2.0f), // White
                                particlePos.x, particlePos.y, particlePos.z,
                                1, 0.05, 0.05, 0.05, 0.02
                            );
                        }
                    }
                }
                
                // Create attraction effect around the orb
                for (Entity entity : world.getOtherEntities(player, new Box(finalOrbPos.add(-8, -8, -8), finalOrbPos.add(8, 8, 8)), entity -> true)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        Vec3d entityPos = entity.getPos();
                        double distance = entityPos.distanceTo(finalOrbPos);
                        
                        if (distance < 8 && distance > 0.5) {
                            // Calculate pull force
                            double pullStrength = (8 - distance) / 8 * 2.0;
                            Vec3d pullDirection = finalOrbPos.subtract(entityPos).normalize().multiply(pullStrength);
                            
                            // Apply pull velocity
                            entity.addVelocity(pullDirection.x, pullDirection.y * 0.5, pullDirection.z);
                            
                            // Add blue swirl particles around pulled entities
                            serverWorld.spawnParticles(
                                new DustParticleEffect(new Vector3f(0.0f, 0.8f, 1.0f), 0.8f),
                                entity.getX(), entity.getY() + 1, entity.getZ(),
                                2, 0.2, 0.2, 0.2, 0.04
                            );
                        }
                    }
                }
                
                // Update target position for next frame
                targetPos = currentTargetPos;
            }
            
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0f, 0.8f);
        }
        
        syncPlayerDataToClient(player);
    }
    
    public static void activateMaxBlue(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);
        
        if (data.maxBlueCooldown > 0) {
            player.sendMessage(Text.literal("MAX Blue on cooldown! (" + data.maxBlueCooldown / 20 + "s)"), true);
            return;
        }
        
        if (!hasEnoughCursedEnergy(data, 150)) {
            player.sendMessage(Text.literal("Not enough cursed energy! (Requires 150)"), true);
            return;
        }
        
        if (!consumeCursedEnergy(player, data, 150)) {
            return;
        }
        
        // Set as active ability
        data.maxBlueActive = true;
        data.activeAbility = "MAX_BLUE";
        
        data.maxBlueCooldown = 600; // 30 seconds cooldown
        
        World world = player.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            Vec3d playerPos = player.getPos();
            Vec3d targetPos = player.getEyePos().add(player.getRotationVector().multiply(10.0)); // 10 blocks away
            
            // Create a massive stationary blue orb - 10x10x10 version
            for (int frame = 0; frame < 100; frame++) { // 5 seconds of animation
                double progress = frame / 100.0;
                
                // Pulsing effect for the orb
                double pulse = 1.0 + Math.sin(progress * Math.PI * 6) * 0.2;
                
                // Create the massive blue orb
                for (int radius = 0; radius < 5; radius++) { // Bigger radius for 10x10x10 orb
                    for (int angle = 0; angle < 360; angle += 15) { // More angles for denser orb
                        double radians = Math.toRadians(angle + frame * 3); // Slower rotation for bigger orb
                        Vec3d orbPos = targetPos.add(
                            Math.cos(radians) * radius * pulse,
                            0,
                            Math.sin(radians) * radius * pulse
                        );
                        
                        // Create massive blue particle sphere - 10x10x10 total size
                        for (int y = -4; y <= 4; y++) { // Bigger height for 10x10x10
                            Vec3d particlePos = orbPos.add(0, y, 0);
                            double distance = Math.sqrt(radius*radius + y*y);
                            
                            if (distance <= 5.0 * pulse) { // Bigger distance for massive orb
                                // Main blue orb particles - INTENSE CORE
                                int particleCount = (distance < 2) ? 40 : 25; // Very dense particles in core
                                serverWorld.spawnParticles(
                                    new DustParticleEffect(new Vector3f(0.0f, 0.8f, 1.0f), 3.0f), // Larger particles
                                    particlePos.x, particlePos.y, particlePos.z,
                                    particleCount, 0.4, 0.4, 0.4, 0.12
                                );
                                
                                // Electric blue particles - VERY DENSE
                                serverWorld.spawnParticles(
                                    new DustParticleEffect(new Vector3f(0.2f, 0.6f, 1.0f), 2.2f), // Larger
                                    particlePos.x, particlePos.y, particlePos.z,
                                    20, 0.3, 0.3, 0.3, 0.1
                                );
                                
                                // White core particles - BRIGHT CENTER
                                if (distance < 2) { // Bigger bright core
                                    serverWorld.spawnParticles(
                                        new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 3.5f), // Brighter
                                        particlePos.x, particlePos.y, particlePos.z,
                                        15, 0.15, 0.15, 0.15, 0.06
                                    );
                                }
                                
                                // Lightning effect particles
                                if (distance < 3 && angle % 30 == 0) { // Lightning in inner core
                                    serverWorld.spawnParticles(
                                        new DustParticleEffect(new Vector3f(0.8f, 0.9f, 1.0f), 2.8f), // Light blue
                                        particlePos.x, particlePos.y, particlePos.z,
                                        3, 0.2, 0.2, 0.2, 0.08
                                    );
                                }
                            }
                        }
                    }
                }
                
                // Create MASSIVE gravitational pull effect - HUGE RANGE, BIG ORB
                for (Entity entity : world.getOtherEntities(player, new Box(targetPos.add(-50, -50, -50), targetPos.add(50, 50, 50)), entity -> true)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        Vec3d entityPos = entity.getPos();
                        double distance = entityPos.distanceTo(targetPos);
                        
                        if (distance < 50 && distance > 2) { // Keep 50 blocks pull range
                            // Calculate pull force - VERY STRONG for big orb
                            double pullStrength = (50 - distance) / 50 * 8.0; // Increased to 8.0 for massive effect
                            Vec3d pullDirection = targetPos.subtract(entityPos).normalize().multiply(pullStrength);
                            
                            // Apply STRONG pull velocity with strong upward component
                            entity.addVelocity(pullDirection.x, pullDirection.y * 1.5, pullDirection.z);
                            
                            // Add blue swirl particles around pulled entities
                            serverWorld.spawnParticles(
                                new DustParticleEffect(new Vector3f(0.0f, 0.8f, 1.0f), 1.5f),
                                entity.getX(), entity.getY() + 1, entity.getZ(),
                                8, 0.3, 0.3, 0.3, 0.08
                            );
                            
                            // Add electric sparks on pulled entities
                            if (distance < 30) {
                                serverWorld.spawnParticles(
                                    new DustParticleEffect(new Vector3f(0.8f, 0.9f, 1.0f), 1.2f),
                                    entity.getX(), entity.getY() + 1, entity.getZ(),
                                    5, 0.2, 0.2, 0.2, 0.06
                                );
                            }
                        }
                    }
                }
                
                // Create massive swirling vortex particles around big orb
                for (int i = 0; i < 200; i++) {
                    double vortexProgress = i / 200.0;
                    double radius = 8 + Math.sin(vortexProgress * Math.PI * 8) * 3; // Bigger vortex
                    double height = Math.sin(vortexProgress * Math.PI * 12) * 3; // Taller vortex
                    double angle = vortexProgress * Math.PI * 12; // Fast rotation
                    
                    Vec3d vortexPos = targetPos.add(
                        Math.cos(angle) * radius,
                        height,
                        Math.sin(angle) * radius
                    );
                    
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(0.1f, 0.4f, 0.8f), 2.0f),
                        vortexPos.x, vortexPos.y, vortexPos.z,
                        4, 0.15, 0.15, 0.15, 0.06
                    );
                }
                
                // Create massive attraction field particles showing the huge pull range
                for (int ring = 1; ring <= 10; ring++) {
                    double ringRadius = ring * 5; // Rings every 5 blocks to show 50-block range
                    for (int angle = 0; angle < 360; angle += 15) {
                        double radians = Math.toRadians(angle);
                        Vec3d ringPos = targetPos.add(
                            Math.cos(radians) * ringRadius,
                            0,
                            Math.sin(radians) * ringRadius
                        );
                        
                        // Make outer rings more subtle to show range but not clutter
                        float particleSize = (ring > 5) ? 0.8f : 1.2f;
                        int particleCount = (ring > 5) ? 3 : 6;
                        
                        serverWorld.spawnParticles(
                            new DustParticleEffect(new Vector3f(0.5f, 0.9f, 1.0f), particleSize),
                            ringPos.x, ringPos.y, ringPos.z,
                            particleCount, 0.2, 0.2, 0.2, 0.04
                        );
                    }
                }
            }
            
            // Create massive explosion at the end
            world.createExplosion(null, targetPos.x, targetPos.y, targetPos.z, 15.0f,
                World.ExplosionSourceType.MOB);
            
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_SHOOT,
                SoundCategory.PLAYERS, 3.0f, 0.5f);
        }
        
        syncPlayerDataToClient(player);
    }
    
    public static void activateRed(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);
        if (data.redCooldown > 0) {
            player.sendMessage(Text.literal("Red on cooldown! (" + data.redCooldown / 20 + "s)"), true);
            return;
        }
        
        boolean isMaxVersion = player.isSneaking();
        int energyCost = isMaxVersion ? 200 : 75;
        if (!hasEnoughCursedEnergy(data, energyCost)) {
            player.sendMessage(Text.literal("Not enough cursed energy! (Requires " + energyCost + ")"), true);
            return;
        }
        
        if (!consumeCursedEnergy(player, data, energyCost)) {
            return;
        }
        
        data.redCooldown = isMaxVersion ? 600 : 300; // 30s or 15s cooldown
        
        World world = player.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            Vec3d lookVec = player.getRotationVector();
            Vec3d startPos = player.getEyePos();
            
            if (isMaxVersion) {
                // MAX RED - Massive destruction beam with enhanced orange dust particles
                for (int i = 0; i < 120; i++) {
                    double progress = i / 120.0;
                    Vec3d currentPos = startPos.add(lookVec.multiply(i * 0.6));
                    
                    // Add explosive spread effect
                    double spread = progress * 0.3;
                    Vec3d spreadVec = new Vec3d(
                        (Math.random() - 0.5) * spread,
                        (Math.random() - 0.5) * spread,
                        (Math.random() - 0.5) * spread
                    );
                    Vec3d finalPos = currentPos.add(spreadVec);
                    
                    // Main orange dust trail
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(1.0f, 0.5f, 0.0f), 1.5f), // Brighter ORANGE
                        finalPos.x, finalPos.y, finalPos.z,
                        12, 0.4, 0.4, 0.4, 0.12
                    );
                    
                    // Secondary red particles for intensity
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(1.0f, 0.2f, 0.0f), 1.0f), // Deep red
                        finalPos.x, finalPos.y, finalPos.z,
                        6, 0.3, 0.3, 0.3, 0.08
                    );
                    
                    // Yellow core for explosive effect
                    if (i % 2 == 0) {
                        serverWorld.spawnParticles(
                            new DustParticleEffect(new Vector3f(1.0f, 1.0f, 0.0f), 1.8f), // Yellow
                            finalPos.x, finalPos.y, finalPos.z,
                            3, 0.2, 0.2, 0.2, 0.05
                        );
                    }
                    
                    // Create explosive ring effect
                    for (int angle = 0; angle < 360; angle += 30) {
                        double radians = Math.toRadians(angle + progress * 720);
                        Vec3d ringPos = finalPos.add(
                            Math.cos(radians) * (0.8 + progress * 0.5),
                            Math.sin(radians * 3) * 0.3,
                            Math.sin(radians) * (0.8 + progress * 0.5)
                        );
                        
                        serverWorld.spawnParticles(
                            new DustParticleEffect(new Vector3f(0.8f, 0.3f, 0.0f), 0.7f), // Dark orange
                            ringPos.x, ringPos.y, ringPos.z,
                            2, 0.1, 0.1, 0.1, 0.03
                        );
                    }
                    
                    // Create explosions along the path
                    if (i % 8 == 0) {
                        world.createExplosion(null, finalPos.x, finalPos.y, finalPos.z, 2.5f, 
                            World.ExplosionSourceType.MOB);
                    }
                }
            } else {
                // Normal Red - Enhanced orb projectile with improved dust particles
                for (int i = 0; i < 40; i++) {
                    double progress = i / 40.0;
                    Vec3d currentPos = startPos.add(lookVec.multiply(i * 0.5));
                    
                    // Add pulsing effect
                    double pulse = Math.sin(progress * Math.PI * 6) * 0.2;
                    Vec3d pulseVec = lookVec.crossProduct(new Vec3d(0, 1, 0)).normalize().multiply(pulse);
                    Vec3d finalPos = currentPos.add(pulseVec);
                    
                    // Main orange dust orb
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(1.0f, 0.5f, 0.0f), 1.3f), // ORANGE
                        finalPos.x, finalPos.y, finalPos.z,
                        10, 0.3, 0.3, 0.3, 0.1
                    );
                    
                    // Red glow particles
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(1.0f, 0.2f, 0.0f), 0.9f), // Red
                        finalPos.x, finalPos.y, finalPos.z,
                        5, 0.2, 0.2, 0.2, 0.06
                    );
                    
                    // White sparks
                    if (i % 4 == 0) {
                        serverWorld.spawnParticles(
                            new DustParticleEffect(new Vector3f(1.0f, 0.8f, 0.4f), 1.6f), // Light orange
                            finalPos.x, finalPos.y, finalPos.z,
                            3, 0.15, 0.15, 0.15, 0.04
                        );
                    }
                }
                
                // Create explosion at the end
                Vec3d endPos = startPos.add(lookVec.multiply(12.0));
                world.createExplosion(null, endPos.x, endPos.y, endPos.z, 3.0f, 
                    World.ExplosionSourceType.MOB);
            }
            
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_DRAGON_FIREBALL_EXPLODE,
                SoundCategory.PLAYERS, 1.0f, 0.7f);
        }
        
        syncPlayerDataToClient(player);
    }
    
    public static void activateHollowPurple(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);
        if (data.hollowPurpleCooldown > 0) {
            player.sendMessage(Text.literal("Hollow Purple on cooldown! (" + data.hollowPurpleCooldown / 20 + "s)"), true);
            return;
        }
        
        if (!hasEnoughCursedEnergy(data, 150)) {
            player.sendMessage(Text.literal("Not enough cursed energy! (Requires 150)"), true);
            return;
        }
        
        if (!consumeCursedEnergy(player, data, 150)) {
            return;
        }
        
        data.hollowPurpleCooldown = 900; // 45 seconds cooldown
        
        World world = player.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            Vec3d lookVec = player.getRotationVector();
            Vec3d startPos = player.getEyePos();
            
            // Enhanced wind-up animation: Blue to the right, Red to the left with improved effects
            Vec3d rightOffset = lookVec.crossProduct(new Vec3d(0, 1, 0)).normalize().multiply(2.5);
            Vec3d leftOffset = rightOffset.multiply(-1.0);
            
            // Spawn enhanced blue particles to the right
            for (int i = 0; i < 30; i++) {
                double progress = i / 30.0;
                Vec3d bluePos = startPos.add(rightOffset).add(lookVec.multiply(i * 0.15));
                
                // Add swirling effect
                double swirl = Math.sin(progress * Math.PI * 4) * 0.3;
                Vec3d swirlVec = new Vec3d(
                    Math.cos(progress * Math.PI * 8) * swirl,
                    Math.sin(progress * Math.PI * 6) * 0.2,
                    Math.sin(progress * Math.PI * 8) * swirl
                );
                Vec3d finalBluePos = bluePos.add(swirlVec);
                
                // Main cyan dust
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.0f, 0.8f, 1.0f), 1.4f), // Brighter CYAN
                    finalBluePos.x, finalBluePos.y, finalBluePos.z,
                    6, 0.2, 0.2, 0.2, 0.08
                );
                
                // Light blue glow
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.5f, 0.9f, 1.0f), 0.9f), // Light blue
                    finalBluePos.x, finalBluePos.y, finalBluePos.z,
                    3, 0.15, 0.15, 0.15, 0.05
                );
            }
            
            // Spawn enhanced red particles to the left
            for (int i = 0; i < 30; i++) {
                double progress = i / 30.0;
                Vec3d redPos = startPos.add(leftOffset).add(lookVec.multiply(i * 0.15));
                
                // Add swirling effect
                double swirl = Math.cos(progress * Math.PI * 4) * 0.3;
                Vec3d swirlVec = new Vec3d(
                    Math.sin(progress * Math.PI * 8) * swirl,
                    Math.cos(progress * Math.PI * 6) * 0.2,
                    Math.cos(progress * Math.PI * 8) * swirl
                );
                Vec3d finalRedPos = redPos.add(swirlVec);
                
                // Main orange dust
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(1.0f, 0.5f, 0.0f), 1.4f), // Brighter ORANGE
                    finalRedPos.x, finalRedPos.y, finalRedPos.z,
                    6, 0.2, 0.2, 0.2, 0.08
                );
                
                // Red glow
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(1.0f, 0.2f, 0.0f), 0.9f), // Deep red
                    finalRedPos.x, finalRedPos.y, finalRedPos.z,
                    3, 0.15, 0.15, 0.15, 0.05
                );
            }
            
            // Enhanced combination phase: Blue and Red combine to form Purple
            Vec3d combinePos = startPos.add(lookVec.multiply(3.5));
            for (int i = 0; i < 40; i++) {
                double progress = i / 40.0;
                Vec3d blueToCombine = startPos.add(rightOffset).add(lookVec.multiply(i * 0.12));
                Vec3d redToCombine = startPos.add(leftOffset).add(lookVec.multiply(i * 0.12));
                
                // Enhanced blue particles moving toward center
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.0f, 0.8f, 1.0f), 1.2f), // CYAN
                    blueToCombine.x, blueToCombine.y, blueToCombine.z,
                    4, 0.1, 0.1, 0.1, 0.04
                );
                
                // Enhanced red particles moving toward center
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(1.0f, 0.5f, 0.0f), 1.2f), // ORANGE
                    redToCombine.x, redToCombine.y, redToCombine.z,
                    4, 0.1, 0.1, 0.1, 0.04
                );
                
                // Purple particles forming at center with growing intensity
                if (i >= 15) {
                    double intensity = (i - 15) / 25.0;
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(1.0f, 0.0f, 1.0f), (float)(1.0f + intensity * 0.8f)), // Growing MAGENTA
                        combinePos.x, combinePos.y, combinePos.z,
                        6 + (int)(intensity * 8), 0.15, 0.15, 0.15, 0.08
                    );
                    
                    // White core particles at center
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 1.5f), // White
                        combinePos.x, combinePos.y, combinePos.z,
                        2, 0.1, 0.1, 0.1, 0.03
                    );
                }
            }
            
            // Enhanced launch phase: Shoot the purple orb straight (no cursor following)
            Vec3d shootDirection = lookVec; // Lock in initial direction
            for (int i = 0; i < 100; i++) {
                double progress = i / 100.0;
                Vec3d currentPos = combinePos.add(shootDirection.multiply(i * 0.6));
                
                // Add spiraling effect
                double spiral = Math.sin(progress * Math.PI * 8) * 0.4;
                Vec3d spiralVec = new Vec3d(
                    Math.cos(progress * Math.PI * 16) * spiral,
                    Math.sin(progress * Math.PI * 12) * 0.2,
                    Math.sin(progress * Math.PI * 16) * spiral
                );
                Vec3d finalPos = currentPos.add(spiralVec);
                
                // Main purple dust trail
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(1.0f, 0.0f, 1.0f), 1.6f), // Brighter MAGENTA
                    finalPos.x, finalPos.y, finalPos.z,
                    10, 0.3, 0.3, 0.3, 0.12
                );
                
                // Secondary violet particles
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.8f, 0.0f, 1.0f), 1.1f), // Violet
                    finalPos.x, finalPos.y, finalPos.z,
                    5, 0.2, 0.2, 0.2, 0.07
                );
                
                // Pink glow particles
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(1.0f, 0.5f, 0.8f), 0.9f), // Pink
                    finalPos.x, finalPos.y, finalPos.z,
                    3, 0.15, 0.15, 0.15, 0.05
                );
                
                // White core particles
                if (i % 3 == 0) {
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 1.8f), // White
                        finalPos.x, finalPos.y, finalPos.z,
                        2, 0.1, 0.1, 0.1, 0.03
                    );
                }
                
                // Create destructive ring effect
                for (int angle = 0; angle < 360; angle += 20) {
                    double radians = Math.toRadians(angle + progress * 1080);
                    Vec3d ringPos = finalPos.add(
                        Math.cos(radians) * (1.0 + progress * 0.8),
                        Math.sin(radians * 2) * 0.4,
                        Math.sin(radians) * (1.0 + progress * 0.8)
                    );
                    
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(0.6f, 0.0f, 0.8f), 0.8f), // Dark purple
                        ringPos.x, ringPos.y, ringPos.z,
                        2, 0.1, 0.1, 0.1, 0.03
                    );
                }
                
                // Create explosions along the path
                if (i % 12 == 0) {
                    world.createExplosion(null, finalPos.x, finalPos.y, finalPos.z, 5.0f, 
                        World.ExplosionSourceType.MOB);
                }
            }
            
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_WITHER_SHOOT,
                SoundCategory.PLAYERS, 2.0f, 0.5f);
        }
        
        syncPlayerDataToClient(player);
    }
    
    public static void activateDomainExpansion(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);
        if (data.domainCooldown > 0) {
            player.sendMessage(Text.literal("Domain Expansion on cooldown! (" + data.domainCooldown / 20 + "s)"), true);
            return;
        }
        
        if (!hasEnoughCursedEnergy(data, 300)) {
            player.sendMessage(Text.literal("Not enough cursed energy! (Requires 300)"), true);
            return;
        }
        
        if (!consumeCursedEnergy(player, data, 300)) {
            return;
        }
        
        data.domainActive = true;
        data.domainDuration = 600;
        data.domainCooldown = 1800;

        player.sendMessage(Text.literal("§d§lDOMAIN EXPANSION: §fUNLIMITED VOID"), true);

        World world = player.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            Vec3d playerPos = player.getPos();
            
            // Store original blocks for restoration
            if (data.originalBlocks == null) {
                data.originalBlocks = new HashMap<>();
            }
            
            // Create enhanced Domain Expansion structure
            for (int x = -7; x <= 7; x++) {
                for (int z = -7; z <= 7; z++) {
                    double distance = Math.sqrt(x*x + z*z);
                    
                    // Floor layer - create pattern with different materials
                    for (int y = 0; y <= 0; y++) {
                        Vec3d floorPos = playerPos.add(x, y, z);
                        BlockPos blockPos = new BlockPos((int)floorPos.x, (int)floorPos.y, (int)floorPos.z);
                        
                        if (!world.getBlockState(blockPos).isAir()) {
                            data.originalBlocks.put(blockPos, world.getBlockState(blockPos));
                            
                            // Create pattern: black concrete with obsidian borders
                            if (distance <= 6.5) {
                                if (x == -7 || x == 7 || z == -7 || z == 7) {
                                    world.setBlockState(blockPos, Blocks.OBSIDIAN.getDefaultState()); // Border
                                } else if (x % 2 == 0 && z % 2 == 0) {
                                    world.setBlockState(blockPos, Blocks.BLACK_CONCRETE.getDefaultState()); // Main floor
                                } else {
                                    world.setBlockState(blockPos, Blocks.GRAY_CONCRETE.getDefaultState()); // Pattern
                                }
                            }
                        }
                    }
                    
                    // Create dome structure with different layers
                    for (int y = 1; y <= 12; y++) {
                        double sphereRadius = 7 - (y * 0.3); // Tapering dome
                        
                        if (distance <= sphereRadius) {
                            Vec3d wallPos = playerPos.add(x, y, z);
                            BlockPos wallBlockPos = new BlockPos((int)wallPos.x, (int)wallPos.y, (int)wallPos.z);
                            
                            if (!world.getBlockState(wallBlockPos).isAir()) {
                                data.originalBlocks.put(wallBlockPos, world.getBlockState(wallBlockPos));
                                
                                // Create layered structure
                                if (y <= 3) {
                                    // Lower walls - black concrete
                                    world.setBlockState(wallBlockPos, Blocks.BLACK_CONCRETE.getDefaultState());
                                } else if (y <= 6) {
                                    // Middle layer - obsidian
                                    world.setBlockState(wallBlockPos, Blocks.OBSIDIAN.getDefaultState());
                                } else if (y <= 9) {
                                    // Upper layer - black concrete with glass windows
                                    if (x % 3 == 0 || z % 3 == 0) {
                                        world.setBlockState(wallBlockPos, Blocks.BLACK_STAINED_GLASS.getDefaultState());
                                    } else {
                                        world.setBlockState(wallBlockPos, Blocks.BLACK_CONCRETE.getDefaultState());
                                    }
                                } else {
                                    // Top layer - obsidian cap
                                    world.setBlockState(wallBlockPos, Blocks.OBSIDIAN.getDefaultState());
                                }
                            }
                        }
                    }
                    
                    // Add decorative pillars at cardinal directions
                    if ((x == 0 && (z == -6 || z == 6)) || (z == 0 && (x == -6 || x == 6))) {
                        for (int y = 1; y <= 10; y++) {
                            Vec3d pillarPos = playerPos.add(x, y, z);
                            BlockPos pillarBlockPos = new BlockPos((int)pillarPos.x, (int)pillarPos.y, (int)pillarPos.z);
                            
                            if (!world.getBlockState(pillarBlockPos).isAir()) {
                                data.originalBlocks.put(pillarBlockPos, world.getBlockState(pillarBlockPos));
                                world.setBlockState(pillarBlockPos, Blocks.OBSIDIAN.getDefaultState());
                            }
                        }
                    }
                }
            }
            
            // Create enhanced void visual effects
            for (int x = -10; x <= 10; x++) {
                for (int z = -10; z <= 10; z++) {
                    for (int y = 0; y <= 12; y++) {
                        Vec3d particlePos = playerPos.add(x, y, z);
                        double distance = Math.sqrt(x*x + z*z);

                        // Void particles on dome surface
                        double domeRadius = 7 - (y * 0.3);
                        if (distance >= domeRadius - 0.5 && distance <= domeRadius + 0.5) {
                            serverWorld.spawnParticles(
                                ParticleTypes.PORTAL,
                                particlePos.x, particlePos.y, particlePos.z,
                                2, 0.1, 0.1, 0.1, 0.03
                            );
                            
                            // Add purple dust particles for mystical effect
                            serverWorld.spawnParticles(
                                new DustParticleEffect(new Vector3f(0.5f, 0.0f, 1.0f), 0.8f),
                                particlePos.x, particlePos.y, particlePos.z,
                                1, 0.05, 0.05, 0.05, 0.02
                            );
                        }

                        // Darkness particles inside domain
                        if (distance < domeRadius - 0.5) {
                            if (Math.random() < 0.1) { // Less dense particles
                                serverWorld.spawnParticles(
                                    ParticleTypes.SMOKE,
                                    particlePos.x, particlePos.y, particlePos.z,
                                    1, 0.0, 0.0, 0.0, 0.01
                                );
                            }
                        }
                        
                        // Special effects at pillar locations
                        if ((x == 0 && (z == -6 || z == 6)) || (z == 0 && (x == -6 || x == 6))) {
                            if (y <= 10) {
                                serverWorld.spawnParticles(
                                    new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 1.0f), // White
                                    particlePos.x, particlePos.y, particlePos.z,
                                    3, 0.2, 0.2, 0.2, 0.05
                                );
                            }
                        }
                    }
                }
            }
            
            // Add floating particles inside the domain
            for (int i = 0; i < 50; i++) {
                Vec3d randomPos = playerPos.add(
                    (Math.random() - 0.5) * 12,
                    Math.random() * 8,
                    (Math.random() - 0.5) * 12
                );
                
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.8f, 0.0f, 1.0f), 0.6f), // Purple
                    randomPos.x, randomPos.y, randomPos.z,
                    1, 0.0, 0.0, 0.0, 0.02
                );
            }

            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_ENDER_DRAGON_DEATH,
                SoundCategory.PLAYERS, 3.0f, 0.3f);
        }
        
        syncPlayerDataToClient(player);
    }
    
    public static void activateReverseCursedTechnique(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);
        
        if (!hasEnoughCursedEnergy(data, 100)) {
            player.sendMessage(Text.literal("Not enough cursed energy! (Requires 100)"), true);
            return;
        }
        
        if (!consumeCursedEnergy(player, data, 100)) {
            return;
        }
        
        // Set as active ability
        data.reverseCursedActive = true;
        data.activeAbility = "REVERSE_CURSED";
        
        // Heal the player
        player.heal(20.0f);
        
        World world = player.getWorld();
        if (world instanceof ServerWorld) {
            ServerWorld serverWorld = (ServerWorld) world;
            Vec3d playerPos = player.getPos();
            
            // Create green healing particles
            for (int i = 0; i < 30; i++) {
                Vec3d particlePos = playerPos.add(
                    (Math.random() - 0.5) * 2.0,
                    Math.random() * 2.0,
                    (Math.random() - 0.5) * 2.0
                );
                
                serverWorld.spawnParticles(
                    new DustParticleEffect(new Vector3f(0.0f, 1.0f, 0.0f), 1.0f), // GREEN
                    particlePos.x, particlePos.y, particlePos.z,
                    2, 0.1, 0.1, 0.1, 0.05
                );
            }
            
            world.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
                SoundCategory.PLAYERS, 1.0f, 1.0f);
        }
        
        syncPlayerDataToClient(player);
    }
    
    public static void toggleInfinity(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);
        
        if (data.infinityActive) {
            data.infinityActive = false;
            player.sendMessage(Text.literal("§7Infinity deactivated"), true);
        } else {
            if (!hasEnoughCursedEnergy(data, 50)) {
                player.sendMessage(Text.literal("Not enough cursed energy! (Requires 50)"), true);
                return;
            }
            data.infinityActive = true;
            player.sendMessage(Text.literal("§bInfinity activated"), true);
        }
        
        syncPlayerDataToClient(player);
    }
    
    public static void toggleSixEyes(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);
        
        if (data.sixEyesActive) {
            data.sixEyesActive = false;
            player.sendMessage(Text.literal("§7Six Eyes deactivated"), true);
        } else {
            if (!hasEnoughCursedEnergy(data, 30)) {
                player.sendMessage(Text.literal("Not enough cursed energy! (Requires 30)"), true);
                return;
            }
            data.sixEyesActive = true;
            player.sendMessage(Text.literal("§6Six Eyes activated - Wall penetration enabled"), true);
        }
        
        syncPlayerDataToClient(player);
    }
    
    public static void tick(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);

        // Cooldowns
        if (data.blueCooldown > 0) data.blueCooldown--;
        if (data.redCooldown > 0) data.redCooldown--;
        if (data.hollowPurpleCooldown > 0) data.hollowPurpleCooldown--;
        if (data.blueAmplifiedCooldown > 0) data.blueAmplifiedCooldown--;
        if (data.maxBlueCooldown > 0) data.maxBlueCooldown--;
        if (data.domainCooldown > 0) data.domainCooldown--;
        if (data.sixEyesCooldown > 0) data.sixEyesCooldown--;

        // Energy regeneration
        data.energyRegenerationTimer++;
        if (data.energyRegenerationTimer >= 20) { // Regenerate every second
            regenerateCursedEnergy(player, data);
            data.energyRegenerationTimer = 0;
        }

        // Infinity maintenance cost (separate timer)
        if (data.infinityActive) {
            data.infinityMaintenanceTimer++;
            if (data.infinityMaintenanceTimer >= 40) { // Every 2 seconds
                consumeCursedEnergy(player, data, 5.0f); // 5 energy every 2 seconds
                data.infinityMaintenanceTimer = 0;
            }
            if (data.currentCursedEnergy <= 0) {
                toggleInfinity(player); // Auto-disable if out of energy
            }
            
            // Apply proximity-based slow effect to nearby entities
            World world = player.getWorld();
            if (world instanceof ServerWorld) {
                ServerWorld serverWorld = (ServerWorld) world;
                Vec3d playerPos = player.getPos();
                
                // Check all entities within 20 blocks
                for (Entity entity : world.getOtherEntities(player, new Box(playerPos.add(-20, -20, -20), playerPos.add(20, 20, 20)), entity -> true)) {
                    if (entity instanceof LivingEntity && entity != player) {
                        Vec3d entityPos = entity.getPos();
                        double distance = entityPos.distanceTo(playerPos);
                        
                        if (distance < 20) { // Within 20 blocks
                            // Calculate slow effect based on distance
                            // Closer = slower movement
                            double slowFactor;
                            if (distance <= 3) {
                                slowFactor = 0.0; // Completely frozen at 3 blocks or closer
                            } else if (distance <= 6) {
                                slowFactor = 0.1; // 90% slow at 3-6 blocks
                            } else if (distance <= 10) {
                                slowFactor = 0.3; // 70% slow at 6-10 blocks
                            } else if (distance <= 15) {
                                slowFactor = 0.6; // 40% slow at 10-15 blocks
                            } else {
                                slowFactor = 0.8; // 20% slow at 15-20 blocks
                            }
                            
                            // Apply slow effect to entity movement
                            LivingEntity livingEntity = (LivingEntity) entity;
                            
                            // Slow down current velocity
                            Vec3d currentVelocity = livingEntity.getVelocity();
                            Vec3d slowedVelocity = currentVelocity.multiply(slowFactor);
                            livingEntity.setVelocity(slowedVelocity);
                            
                            // Apply additional movement restriction for very close entities
                            if (distance <= 3) {
                                livingEntity.addVelocity(0, 0, 0); // Zero out movement
                                livingEntity.setVelocity(0, 0, 0); // Complete stop
                            }
                            
                            // Add visual effects based on slow intensity
                            int particleCount = (distance <= 6) ? 10 : (distance <= 15) ? 5 : 2;
                            float particleSize = (distance <= 6) ? 2.0f : (distance <= 15) ? 1.5f : 1.0f;
                            
                            // White/blue particles to show infinity effect
                            serverWorld.spawnParticles(
                                new DustParticleEffect(new Vector3f(0.8f, 0.9f, 1.0f), particleSize), // Light blue-white
                                entity.getX(), entity.getY() + 1, entity.getZ(),
                                particleCount, 0.3, 0.3, 0.3, 0.08
                            );
                            
                            // Add extra particles for very close entities
                            if (distance <= 3) {
                                serverWorld.spawnParticles(
                                    new DustParticleEffect(new Vector3f(1.0f, 1.0f, 1.0f), 2.5f), // Bright white
                                    entity.getX(), entity.getY() + 1, entity.getZ(),
                                    15, 0.2, 0.2, 0.2, 0.1
                                );
                                
                                // Create barrier effect around frozen entities
                                for (int angle = 0; angle < 360; angle += 45) {
                                    double radians = Math.toRadians(angle);
                                    Vec3d barrierPos = entityPos.add(
                                        Math.cos(radians) * 1.5,
                                        1,
                                        Math.sin(radians) * 1.5
                                    );
                                    
                                    serverWorld.spawnParticles(
                                        new DustParticleEffect(new Vector3f(0.5f, 0.8f, 1.0f), 1.5f), // Cyan
                                        barrierPos.x, barrierPos.y, barrierPos.z,
                                        3, 0.1, 0.1, 0.1, 0.05
                                    );
                                }
                            }
                            
                            // Slow projectiles too
                            if (entity instanceof net.minecraft.entity.projectile.ProjectileEntity) {
                                net.minecraft.entity.projectile.ProjectileEntity projectile = (net.minecraft.entity.projectile.ProjectileEntity) entity;
                                Vec3d projectileVelocity = projectile.getVelocity();
                                projectile.setVelocity(projectileVelocity.multiply(slowFactor * 0.5)); // Even slower for projectiles
                                
                                // Add particles to slowed projectiles
                                serverWorld.spawnParticles(
                                    new DustParticleEffect(new Vector3f(0.8f, 0.9f, 1.0f), 1.0f),
                                    projectile.getX(), projectile.getY(), projectile.getZ(),
                                    3, 0.1, 0.1, 0.1, 0.03
                                );
                            }
                        }
                    }
                }
                
                // Create infinity field particles around player
                for (int i = 0; i < 20; i++) {
                    double angle = (i / 20.0) * Math.PI * 2;
                    double radius = 2 + Math.sin(player.age * 0.1 + i) * 0.5;
                    Vec3d particlePos = playerPos.add(
                        Math.cos(angle) * radius,
                        1,
                        Math.sin(angle) * radius
                    );
                    
                    serverWorld.spawnParticles(
                        new DustParticleEffect(new Vector3f(0.9f, 0.95f, 1.0f), 1.2f), // Very light blue-white
                        particlePos.x, particlePos.y, particlePos.z,
                        2, 0.1, 0.1, 0.1, 0.04
                    );
                }
            }
        }

        // Six Eyes maintenance cost (separate timer)
        if (data.sixEyesActive) {
            data.sixEyesMaintenanceTimer++;
            if (data.sixEyesMaintenanceTimer >= 60) { // Every 3 seconds
                consumeCursedEnergy(player, data, 3.0f); // 3 energy every 3 seconds
                data.sixEyesMaintenanceTimer = 0;
            }
            if (data.currentCursedEnergy <= 0) {
                toggleSixEyes(player); // Auto-disable if out of energy
            }
        }

        // Domain duration
        if (data.domainActive) {
            data.domainDuration--;
            if (data.domainDuration <= 0) {
                data.domainActive = false;
                data.activeAbility = "NONE";
                player.sendMessage(Text.literal("§7Domain Expansion ended"), true);
                syncPlayerDataToClient(player);
            }
        }
    }
    
    public static void onCombatEvent(PlayerEntity player, Entity target) {
        GojoPlayerData data = getPlayerData(player);
        if (data.infinityActive && target instanceof LivingEntity) {
            // Infinity prevents taking damage and reflects some back
            ((LivingEntity) target).damage(player.getWorld().getDamageSources().thrown(player, player), 10.0f);
            
            if (player.getWorld() instanceof ServerWorld) {
                ((ServerWorld) player.getWorld()).spawnParticles(
                    ParticleTypes.END_ROD,
                    target.getX(), target.getY(), target.getZ(),
                    20, 0.5, 0.5, 0.5, 0.1
                );
            }
        }
    }
    
    public static void restoreDomainBlocks(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);
        World world = player.getWorld();
        
        if (data.originalBlocks != null && !data.originalBlocks.isEmpty()) {
            for (Map.Entry<BlockPos, net.minecraft.block.BlockState> entry : data.originalBlocks.entrySet()) {
                world.setBlockState(entry.getKey(), entry.getValue());
            }
            data.originalBlocks.clear();
        }
    }
    
    public static void activateAbilityByKey(PlayerEntity player, String ability) {
        GojoPlayerData data = getPlayerData(player);
        
        switch (ability) {
            case "BLUE":
                activateBlue(player);
                break;
            case "RED":
                activateRed(player);
                break;
            case "HOLLOW_PURPLE":
                activateHollowPurple(player);
                break;
            case "DOMAIN":
                activateDomainExpansion(player);
                break;
            case "BLUE_AMPLIFIED":
                activateBlueAmplifiedPunches(player);
                break;
            case "MAX_BLUE":
                activateMaxBlue(player);
                break;
            case "REVERSE_CURSED":
                activateReverseCursedTechnique(player);
                break;
        }
    }
    
    public static void syncPlayerDataToClient(PlayerEntity player) {
        GojoPlayerData data = getPlayerData(player);
        
        // Sync data to client GUI only (no chat messages)
        if (player instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
            GojoNetworking.sendEnergySync(serverPlayer, data.currentCursedEnergy, data.maxCursedEnergy);
            GojoNetworking.sendStatusSync(serverPlayer, data.infinityActive, data.sixEyesActive, data.blueAmplifiedActive, data.reverseCursedActive, data.activeAbility);
        }
    }
}

