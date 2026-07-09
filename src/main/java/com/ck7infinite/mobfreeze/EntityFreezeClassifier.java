package com.ck7infinite.mobfreeze;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decide si un mob es elegible para ser gestionado por el freeze de IA.
 * El resultado depende solo del EntityType (whitelist/blacklist por tipo y por mod id),
 * asi que se cachea por EntityType en vez de recalcularse en cada tick por instancia.
 */
public final class EntityFreezeClassifier {

    private static final Map<EntityType<?>, Boolean> ELIGIBILITY_CACHE = new ConcurrentHashMap<>();

    private EntityFreezeClassifier() {
    }

    public static void invalidateCache() {
        ELIGIBILITY_CACHE.clear();
    }

    public static boolean isEligible(Mob mob) {
        if (!MobFreezeConfig.isEnabled()) {
            return false;
        }
        if (MobFreezeConfig.requireHostile() && !(mob instanceof Enemy)) {
            return false;
        }
        return ELIGIBILITY_CACHE.computeIfAbsent(mob.getType(), EntityFreezeClassifier::computeEligibility);
    }

    private static boolean computeEligibility(EntityType<?> type) {
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (id == null) {
            return false;
        }
        String modId = id.getNamespace();
        String typeId = id.toString();

        if (MobFreezeConfig.modIdBlacklist().contains(modId)) {
            return false;
        }
        if (MobFreezeConfig.entityTypeBlacklist().contains(typeId)) {
            return false;
        }

        Set<String> modWhitelist = MobFreezeConfig.modIdWhitelist();
        if (!modWhitelist.isEmpty() && !modWhitelist.contains(modId)) {
            return false;
        }

        Set<String> typeWhitelist = MobFreezeConfig.entityTypeWhitelist();
        if (!typeWhitelist.isEmpty() && !typeWhitelist.contains(typeId)) {
            return false;
        }

        return true;
    }
}
