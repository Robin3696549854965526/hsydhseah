package com.example.thormod;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ToolMaterial;

import java.util.function.Function;

public class ThorMod implements ModInitializer {
    public static final String MODID = "thormod";

    public static Item THOR_HAMMER;
    public static Item KATANA;

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties props) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MODID, name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(props.setId(key)));
    }

    @Override
    public void onInitialize() {
        THOR_HAMMER = register("thor_hammer", ThorHammerItem::new,
                new Item.Properties().stacksTo(1).rarity(Rarity.EPIC));

        KATANA = register("katana", KatanaItem::new,
                new Item.Properties().sword(ToolMaterial.NETHERITE, KatanaItem.BONUS_DAMAGE, KatanaItem.ATTACK_SPEED).rarity(Rarity.EPIC));

        // Creative-Tab "Kampf"
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT).register(output -> {
            output.accept(THOR_HAMMER);
            output.accept(KATANA);
        });

        // Flug + Schild jeden Tick aktualisieren
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            for (Player player : server.getPlayerList().getPlayers()) {
                PlayerEffects.tick(player);
            }
        });

        // Kein Schaden mit aktivem Schild, kein Fallschaden mit Hammer in der Hand
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            if (entity instanceof Player p) {
                if (PlayerEffects.isShielded(p)) return false;
                if (source.is(DamageTypeTags.IS_FALL) && PlayerEffects.holdingHammer(p)) return false;
            }
            return true;
        });

        // Tod verhindern, solange das Schild aktiv ist (z.B. bei /kill)
        ServerLivingEntityEvents.ALLOW_DEATH.register((entity, source, amount) -> {
            if (entity instanceof Player p && PlayerEffects.isShielded(p)) {
                p.setHealth(p.getMaxHealth());
                return false;
            }
            return true;
        });
    }
}
