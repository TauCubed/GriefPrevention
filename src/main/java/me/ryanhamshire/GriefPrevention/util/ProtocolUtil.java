package me.ryanhamshire.GriefPrevention.util;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.reflect.FuzzyReflection;
import com.comphenix.protocol.reflect.fuzzy.FuzzyFieldContract;
import com.comphenix.protocol.reflect.fuzzy.FuzzyMethodContract;
import com.comphenix.protocol.utility.MinecraftReflection;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Basic class containing utilities to work with the minecraft protocol.
 * @author <a href="https://github.com/TauCubed">TauCubed</a>
 */
public class ProtocolUtil {

    // reflective handles
    // M method, F field
    private static Method m_craftBlockData_getState = null;
    private static Method m_nmsBlock_getId = null;

    // direct references
    private static AtomicInteger nmsEntity_entityCounter = null;

    /**
     * Gets the NMS registry state ID for the given block data.
     * @param data the block data
     * @return the NMS registry state ID.
     */
    public static int getBlockStateId(BlockData data) {
        try {
            if (m_craftBlockData_getState == null || m_nmsBlock_getId == null) {
                // use reflection to obtain the <nms.BlockState> getState method in CraftBlockData.
                Class<?> craftBlockDataClazz = MinecraftReflection.getCraftBukkitClass("block.data.CraftBlockData");
                Method M_craftBlockData_getState = craftBlockDataClazz.getMethod("getState");
                M_craftBlockData_getState.setAccessible(true);
                ProtocolUtil.m_craftBlockData_getState = M_craftBlockData_getState;

                // use fuzzy reflection to find getId method in the nms.Block class.
                // this will lookup and return the registry state ID for the given nms.BlockState reference.
                // we'll just have to hope there isn't another public static method that returns int and accepts exactly nms.BlockState in the nms.Block class.
                FuzzyReflection blockReflector = FuzzyReflection.fromClass(MinecraftReflection.getBlockClass());
                m_nmsBlock_getId = blockReflector.getMethod(FuzzyMethodContract.newBuilder()
                        .banModifier(Modifier.PRIVATE)
                        .banModifier(Modifier.PROTECTED)
                        .requireModifier(Modifier.STATIC)
                        .parameterExactArray(MinecraftReflection.getIBlockDataClass())
                        .returnTypeExact(int.class)
                        .build());
            }

            // invoke getState to get the nms.BlockState of the CraftBlockData
            Object nmsState = m_craftBlockData_getState.invoke(data);
            // invoke getId to get the nms registry state ID of the nms.BlockState
            return (int) m_nmsBlock_getId.invoke(null, nmsState);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Increments and gets the next server entity ID.
     * @return the next server entity ID.
     */
    public static int nextEntityId() {
        if (nmsEntity_entityCounter == null) {
            try {
                // the entity class has a static final AtomicInteger that is incremented each time an entity is spawned.
                // we'll use fuzzy reflection to obtain & store the reference to the object, so we can call it with no reflective overhead.
                FuzzyReflection entityReflector = FuzzyReflection.fromClass(MinecraftReflection.getEntityClass(), true);
                List<Field> possibleFields = entityReflector.getFieldList(
                        FuzzyFieldContract.newBuilder()
                                .requireModifier(Modifier.STATIC)
                                .requireModifier(Modifier.FINAL)
                                .typeDerivedOf(AtomicInteger.class)
                                .build());
                // it may be possible that there is more than one static final AtomicInteger in the entity class, in this unfortunate eventuality we want to make sure
                // that we actually have the entity counter by checking if the current value == the id of an entity we just spawned
                World world = Bukkit.getWorlds().get(0);
                fieldsLoop: for (Field check : possibleFields) {
                    check.setAccessible(true);
                    AtomicInteger possibleCounter = (AtomicInteger) check.get(null);
                    // some naughty things might be creating new entity instances on different threads, thus we need to check a few times
                    for (int i = 0; i < 1000; i++) {
                        FallingBlock fallingBlock = world.spawnFallingBlock(new Location(world, 0, world.getMinHeight() - 2, 0), Material.STONE.createBlockData());
                        if (fallingBlock.getEntityId() == possibleCounter.get()) {
                            // we've (most likely) found it!
                            nmsEntity_entityCounter = possibleCounter;
                            break fieldsLoop;
                        }
                        fallingBlock.remove();
                    }
                }
                // if the counter is still null, we didn't find it. It has likely changed between versions, perhaps now it's an AtomicLong.
                // inm this case, we'll just make one up. It might cause conflicts but at least it should work decently.
                if (nmsEntity_entityCounter == null) {
                    Logger.getLogger("GriefPrevention").log(Level.WARNING, "Failed to find nmsEntity_entityCounter. We'll create a fallback but it might cause id conflicts causing things such as ghost entities.");
                    // we'll use max value, so it will flip negative and be unlikely to cause conflicts with real entities.
                    // obviously if two plugins do this it will be a shit shoot.
                    nmsEntity_entityCounter = new AtomicInteger(Integer.MAX_VALUE);
                }
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
        // increment and get entity counter
        return nmsEntity_entityCounter.incrementAndGet();
    }

    /**
     * Fairly self-explanatory, instructs the players' client to destroy entities with the given IDs
     * @param whom the player to send the packet to.
     * @param entityIds the entity IDs to destroy
     */
    public static void destroyEntitiesFor(Player whom, List<Integer> entityIds) {
        if (!entityIds.isEmpty()) {
            PacketContainer destroyEntity = new PacketContainer(PacketType.Play.Server.ENTITY_DESTROY);
            destroyEntity.getIntLists().write(0, entityIds); // list of entityId's to destroy
            ProtocolLibrary.getProtocolManager().sendServerPacket(whom, destroyEntity);
        }
    }

}
