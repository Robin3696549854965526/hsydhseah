package com.example.thormod;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
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

    private static final DustParticleOptions RED = new DustParticleOptions(0xD91414, 1.3F);
    private static final DustParticleOptions PURPLE = new DustParticleOptions(0x7B2CFF, 1.0F);

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

    /** Ein Partikel an einer Position relativ zum Spieler (lx = rechts, lz = vorne). */
    private static void dot(ServerLevel sl, Player p, double lx, double ly, double lz, DustParticleOptions o) {
        double yaw = Math.toRadians(p.yBodyRot);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);
        double rx = -Math.cos(yaw), rz = -Math.sin(yaw);
        sl.sendParticles(o,
                p.getX() + rx * lx + fx * lz,
                p.getY() + ly,
                p.getZ() + rz * lx + fz * lz,
                1, 0, 0, 0, 0);
    }

    /** Erzeugt mehrere rote Ringer / Aura-Schichten übereinander (wie auf dem Bild). */
    private static void ringAura(ServerLevel sl, Player p) {
        double radius = 1.0; // Radius des Kreises um den Spieler
        // Mehrere Ringe übereinander von Fußhöhe (0.1) bis Kopfhöhe (1.9)
        for (double yOffset = 0.1; yOffset <= 1.9; yOffset += 0.45) {
            int points = 16; // Anzahl der Partikelpunkte pro Ringkreis
            for (int i = 0; i < points; i++) {
                double angle = i * (2 * Math.PI / points);
                double lx = Math.cos(angle) * radius;
                double lz = Math.sin(angle) * radius;
                dot(sl, p, lx, yOffset, lz, RED);
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
                // Ring-Aura alle 2 Ticks aktualisieren
                if (player.tickCount % 2 == 0) {
                    ringAura(sl, player);
                }
                // lila Nebel am Koerper
                for (int i = 0; i < 4; i++) {
                    double lx = (sl.getRandom().nextDouble() - 0.5) * 0.8;
                    double ly = 0.2 + sl.getRandom().nextDouble() * 1.6;
                    double lz = (sl.getRandom().nextDouble() - 0.5) * 0.8;
                    dot(sl, player, lx, ly, lz, PURPLE);
                }
            }
        }
    }
}
