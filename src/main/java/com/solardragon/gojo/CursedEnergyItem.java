package com.solardragon.gojo;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CursedEnergyItem extends Item {
    
    public enum Ability {
        INFINITY("Infinity", 50, "Toggle defensive barrier"),
        LIMITLESS("Limitless", 200, "Explosive area attack"),
        DOMAIN("Domain Expansion", 500, "Unlimited Void domain"),
        BLUE("Blue", 100, "Attraction force"),
        RED("Red", 150, "Repulsion force"),
        HOLLOW_PURPLE("Hollow Purple", 300, "Devastating beam"),
        SIX_EYES("Six Eyes", 25, "See through walls"),
        BLUE_AMPLIFIED_PUNCHES("Blue Amplified Punches", 50, "Enhanced melee attacks");
        
        private final String name;
        private final int energyCost;
        private final String description;
        
        Ability(String name, int energyCost, String description) {
            this.name = name;
            this.energyCost = energyCost;
            this.description = description;
        }
        
        public String getName() { return name; }
        public int getEnergyCost() { return energyCost; }
        public String getDescription() { return description; }
    }
    
    private static final Map<UUID, Integer> playerAbilities = new HashMap<>();
    
    public CursedEnergyItem(Settings settings) {
        super(settings);
    }
    
    @Override
    public boolean hasGlint(ItemStack stack) {
        return true; // Always has enchantment glow like Six Eyes
    }
    
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        
        if (!world.isClient) {
            // Right click - activate current ability
            activateCurrentAbility(world, player, stack);
        }
        
        return TypedActionResult.success(stack);
    }
    
    public boolean onAttackEntity(ItemStack stack, net.minecraft.entity.LivingEntity target, net.minecraft.entity.LivingEntity attacker) {
        if (!attacker.getWorld().isClient && attacker instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) attacker;
            // Left click on entity - cycle abilities
            cycleAbility(stack, player);
            return true;
        }
        return false;
    }
    
    private void cycleAbility(ItemStack stack, PlayerEntity player) {
        int current = playerAbilities.getOrDefault(player.getUuid(), 0);
        Ability[] abilities = Ability.values();
        
        current = (current + 1) % abilities.length;
        playerAbilities.put(player.getUuid(), current);
        
        Ability selected = abilities[current];
        player.sendMessage(Text.literal("§6Selected: " + selected.getName() + " (" + selected.getEnergyCost() + " energy)"), true);
        
        // Show ability description
        player.sendMessage(Text.literal("Not enough cursed energy! (Requires " + selected.getEnergyCost() + ")"), true);
        
        // Add visual effect when cycling
        if (player.getWorld() instanceof net.minecraft.server.world.ServerWorld) {
            net.minecraft.server.world.ServerWorld serverWorld = (net.minecraft.server.world.ServerWorld) player.getWorld();
            serverWorld.spawnParticles(
                net.minecraft.particle.ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0, player.getZ(),
                10, 0.5, 0.5, 0.5, 0.1
            );
        }
    }
    
    private void activateCurrentAbility(World world, PlayerEntity player, ItemStack stack) {
        int current = playerAbilities.getOrDefault(player.getUuid(), 0);
        Ability[] abilities = Ability.values();
        
        if (current >= 0 && current < abilities.length) {
            Ability selected = abilities[current];
            
            switch (selected) {
                case INFINITY:
                    GojoAbilityManager.toggleInfinity(player);
                    player.sendMessage(Text.literal("§bInfinity toggled"), true);
                    break;
                    
                case LIMITLESS:
                    GojoAbilityManager.activateBlueAmplifiedPunches(player);
                    player.sendMessage(Text.literal("§cLimitless activated!"), true);
                    break;
                    
                case BLUE_AMPLIFIED_PUNCHES:
                    GojoAbilityManager.activateBlueAmplifiedPunches(player);
                    player.sendMessage(Text.literal("§cBlue Amplified Punches activated!"), true);
                    break;
                    
                case DOMAIN:
                    GojoAbilityManager.activateDomainExpansion(player);
                    player.sendMessage(Text.literal("§5Domain Expansion: Unlimited Void!"), true);
                    break;
                    
                case BLUE:
                    GojoAbilityManager.activateBlueAmplifiedPunches(player);
                    player.sendMessage(Text.literal("§9§lBlue Orb fired!"), true);
                    break;
                    
                case RED:
                    GojoAbilityManager.activateRed(player);
                    player.sendMessage(Text.literal("§c§lEPIC RED ORB fired!"), true);
                    break;
                    
                case HOLLOW_PURPLE:
                    GojoAbilityManager.activateHollowPurple(player);
                    player.sendMessage(Text.literal("§d§lHOLLOW PURPLE - UNLEASHED!"), true);
                    break;
                    
                case SIX_EYES:
                    GojoAbilityManager.toggleSixEyes(player);
                    break;
            }
        }
    }
    
    public static Ability getCurrentAbility(PlayerEntity player) {
        int current = playerAbilities.getOrDefault(player.getUuid(), 0);
        Ability[] abilities = Ability.values();
        
        if (current >= 0 && current < abilities.length) {
            return abilities[current];
        }
        return Ability.INFINITY; // Default
    }
    
    public static int getCurrentAbilityIndex(PlayerEntity player) {
        return playerAbilities.getOrDefault(player.getUuid(), 0);
    }
    
    public static void setCurrentAbilityIndex(PlayerEntity player, int index) {
        playerAbilities.put(player.getUuid(), index);
    }
    
    public static void onPlayerDisconnect(PlayerEntity player) {
        playerAbilities.remove(player.getUuid());
    }
}
