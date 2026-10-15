package com.example.thormod;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Verwaltet Flug (Hammer), Schild (Katana) und Katana-Verzauberungen pro Spieler. */
public class PlayerEffects {
    private static final Set<UUID> SHIELD_ACTIVE = new HashSet<>();
    private static final Set<UUID> GRANTED_FLIGHT = new HashSet<>();

    public static boolean holdingHammer(Player p) {
        return p.getMainHandItem().is(ThorMod.THOR_HAMMER) || p.getOffhandItem().is(ThorMod.THOR_HAMMER);
    }

    public static boolean holdingKatana(Player p) {
        return p.getMainHandItem().is(ThorMod.KATANA) || p.getOffhandItem().is(ThorMod.KATANA);
    }

    public static boolean isShielded(Player p) {
        return SHIELD_ACTIVE.contains(p.getUUID());
    }

    /** Schild bleibt aktiv, bis das Katana nicht mehr in der Hand ist. */
    public static void activateShield(Player p) {
        SHIELD_ACTIVE.add(p.getUUID());
    }

    /** Katana im Inventar: unzerstoerbar + Schaerfe 10 + Pluenderung 10. */
    private static void upgradeKatanas(Player player) {
        var lookup = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        Holder<Enchantment> sharpness = lookup.getOrThrow(Enchantments.SHARPNESS);
        Holder<Enchantment> looting = lookup.getOrThrow(Enchantments.LOOTING);

        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (!stack.is(ThorMod.KATANA)) continue;

            if (stack.getDamageValue() > 0) {
                stack.setDamageValue(0);
            }

            if (EnchantmentHelper.getItemEnchantmentLevel(sharpness, stack) < 10
                    || EnchantmentHelper.getItemEnchantmentLevel(looting, stack) < 10) {
                EnchantmentHelper.updateEnchantments(stack, enchants -> {
                    enchants.set(sharpness, 10);
                    enchants.set(looting, 10);
                });
            }
        }
    }

    public static void tick(Player player) {
        UUID id = player.getUUID();

        upgradeKatanas(player);

        // ---- Fliegen mit Thors Hammer ----
        boolean holding = holdingHammer(player);
        if (holding && !player.getAbilities().mayfly) {
            player.getAbilities().mayfly = true;
            GRANTED_FLIGHT.add(id);
            player.onUpdateAbilities();
        } else if (!holding && GRANTED_FLIGHT.remove(id)) {
            if (!player.isCreative() && !player.isSpectator()) {
                player.getAbilities().mayfly = false;
                player.getAbilities().flying = false;
                player.onUpdateAbilities();
            }
        }

        // ---- Katana-Schild: aus, sobald das Katana nicht mehr gehalten wird ----
        if (SHIELD_ACTIVE.contains(id) && !holdingKatana(player)) {
            SHIELD_ACTIVE.remove(id);
        }

        if (SHIELD_ACTIVE.contains(id)) {
            player.setHealth(player.getMaxHealth());
            player.clearFire();
            player.setAirSupply(player.getMaxAirSupply());

            if (player.level() instanceof ServerLevel sl) {
                double r = 1.6;
                for (int i = 0; i < 12; i++) {
                    double theta = sl.getRandom().nextDouble() * Math.PI * 2;
                    double phi = Math.acos(2 * sl.getRandom().nextDouble() - 1);
                    double x = player.getX() + r * Math.sin(phi) * Math.cos(theta);
                    double y = player.getY() + 1.0 + r * Math.cos(phi) * 0.8;
                    double z = player.getZ() + r * Math.sin(phi) * Math.sin(theta);
                    sl.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0, 0, 0, 0);
                }
            }
        }
    }
}
