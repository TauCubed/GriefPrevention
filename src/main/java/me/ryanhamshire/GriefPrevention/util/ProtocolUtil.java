package me.ryanhamshire.GriefPrevention.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.fuzzy.FuzzyFieldContract;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ProtocolUtil {

    // M method, F field
    private static Method m_craftBlockData_getState = null;
    private static Method m_nmsBlock_getId = null;

    private static AtomicInteger nmsEntity_entityCounter = null;

    public static int getBlockStateId(BlockData data) {
        try {
            if (m_craftBlockData_getState == null || m_nmsBlock_getId == null) {
                Class<?> craftBlockDataClazz = MinecraftReflection.getCraftBukkitClass("block.data.CraftBlockData");
                Method M_craftBlockData_getState = craftBlockDataClazz.getMethod("getState");
                M_craftBlockData_getState.setAccessible(true);
                ProtocolUtil.m_craftBlockData_getState = M_craftBlockData_getState;

                FuzzyReflection blockReflector = FuzzyReflection.fromClass(MinecraftReflection.getBlockClass());
                m_nmsBlock_getId = blockReflector.getMethod(FuzzyMethodContract.newBuilder()
                        .banModifier(Modifier.PRIVATE)
                        .banModifier(Modifier.PROTECTED)
                        .requireModifier(Modifier.STATIC)
                        .parameterExactArray(MinecraftReflection.getIBlockDataClass())
                        .returnTypeExact(int.class)
                        .build());
            }

            Object nmsState = m_craftBlockData_getState.invoke(data);
            return (int) m_nmsBlock_getId.invoke(null, nmsState);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static int nextEntityId() {
        if (nmsEntity_entityCounter == null) {
            try {
                FuzzyReflection entityReflector = FuzzyReflection.fromClass(MinecraftReflection.getEntityClass(), true);
                Field entityCounterField = entityReflector.getField(
                        FuzzyFieldContract.newBuilder()
                                .requireModifier(Modifier.STATIC)
                                .requireModifier(Modifier.FINAL)
                                .typeDerivedOf(AtomicInteger.class)
                                .build());
                entityCounterField.setAccessible(true);
                nmsEntity_entityCounter = (AtomicInteger) entityCounterField.get(null);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        return nmsEntity_entityCounter.incrementAndGet();
    }

    public static void destroyEntitiesFor(Player whom, List<Integer> entityIds) {
        if (!entityIds.isEmpty()) {
            PacketContainer destroyEntity = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            destroyEntity.getIntLists().write(0, entityIds);
            ProtocolLibrary.getProtocolManager().sendServerPacket(whom, destroyEntity);
        }
    }

}
