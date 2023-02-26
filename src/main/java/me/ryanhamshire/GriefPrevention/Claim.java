/*
    GriefPrevention Server Plugin for Minecraft
    Copyright (C) 2012 Ryan Hamshire

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package me.ryanhamshire.GriefPrevention;

import me.ryanhamshire.GriefPrevention.events.ClaimPermissionCheckEvent;
import me.ryanhamshire.GriefPrevention.util.BoundingBox;
import org.bukkit.*;
import org.bukkit.World.Environment;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

//represents a player claim
//creating an instance doesn't make an effective claim
//only claims which have been added to the datastore have any effect
public class Claim
{
    public static final int _2D_HEIGHT = 32_000_000;

    //two locations, which together define the boundaries of the claim
    //note that the upper Y value is always ignored, because claims ALWAYS extend up to the sky
    //Location lesserBoundaryCorner;
    //Location greaterBoundaryCorner;

    // the world the claim resides in
    World world;

    // the bounds of the claim
    BoundingBox bounds;

    //modification date.  this comes from the file timestamp during load, and is updated with runtime changes
    public Date modifiedDate;

    //id number.  unique to this claim, never changes.
    Long id = null;

    //ownerID.  for admin claims, this is NULL
    //use getOwnerName() to get a friendly name (will be "an administrator" for admin claims)
    public UUID ownerID;

    //list of players who (beyond the claim owner) have permission to grant permissions in this claim
    public ArrayList<String> managers = new ArrayList<>();

    //permissions for this claim, see ClaimPermission class
    private HashMap<String, ClaimPermission> playerIDToClaimPermissionMap = new HashMap<>();

    //whether or not this claim is in the data store
    //if a claim instance isn't in the data store, it isn't "active" - players can't interract with it
    //why keep this?  so that claims which have been removed from the data store can be correctly
    //ignored even though they may have references floating around
    public boolean inDataStore = false;

    public boolean areExplosivesAllowed = false;

    //parent claim
    //only used for claim subdivisions.  top level claims have null here
    public Claim parent = null;

    // intended for subclaims - they inherit no permissions
    private boolean inheritNothing = false;

    //children (subdivisions)
    //note subdivisions themselves never have children
    public ArrayList<Claim> children = new ArrayList<>();

    //playerIds who have been banned from this claim
    public HashSet<UUID> bannedPlayerIds = new HashSet<>();

    //information about a siege involving this claim.  null means no siege is impacting this claim
    public SiegeData siegeData = null;

    //following a siege, buttons/levers are unlocked temporarily.  this represents that state
    public boolean doorsOpen = false;

    //whether or not this is an administrative claim
    //administrative claims are created and maintained by players with the griefprevention.adminclaims permission.
    public boolean isAdminClaim()
    {
        return this.getOwnerID() == null;
    }

    //accessor for ID
    public Long getID()
    {
        return this.id;
    }

    //basic constructor, just notes the creation time
    //see above declarations for other defaults
    Claim()
    {
        this.modifiedDate = Calendar.getInstance().getTime();
    }

    //players may only siege someone when he's not in an admin claim
    //and when he has some level of permission in the claim
    public boolean canSiege(Player defender)
    {
        if (this.isAdminClaim()) return false;

        return this.checkPermission(defender, ClaimPermission.Access, null) == null;
    }

    //removes any lava above sea level in a claim
    //exclusionClaim is another claim indicating an sub-area to be excluded from this operation
    //it may be null
    public void removeSurfaceFluids(Claim exclusionClaim)
    {
        //don't do this for administrative claims
        if (this.isAdminClaim()) return;

        //don't do it for very large claims
        if (this.getArea() > 10000) return;

        //only in creative mode worlds
        if (!GriefPrevention.instance.creativeRulesApply(world)) return;

        Location lesser = this.getLesserBoundaryCorner();
        Location greater = this.getGreaterBoundaryCorner();

        if (lesser.getWorld().getEnvironment() == Environment.NETHER) return;  //don't clean up lava in the nether

        int seaLevel = 0;  //clean up all fluids in the end

        //respect sea level in normal worlds
        if (lesser.getWorld().getEnvironment() == Environment.NORMAL)
            seaLevel = GriefPrevention.instance.getSeaLevel(lesser.getWorld());

        for (int x = lesser.getBlockX(); x <= greater.getBlockX(); x++)
        {
            for (int z = lesser.getBlockZ(); z <= greater.getBlockZ(); z++)
            {
                for (int y = seaLevel - 1; y <= lesser.getWorld().getMaxHeight(); y++)
                {
                    //dodge the exclusion claim
                    Block block = lesser.getWorld().getBlockAt(x, y, z);
                    if (exclusionClaim != null && exclusionClaim.contains(block.getLocation(), true, false)) continue;

                    if (block.getType() == Material.LAVA || block.getType() == Material.WATER)
                    {
                        block.setType(Material.AIR);
                    }
                }
            }
        }
    }

    //determines whether or not a claim has surface lava
    //used to warn players when they abandon their claims about automatic fluid cleanup
    boolean hasSurfaceFluids()
    {
        Location lesser = this.getLesserBoundaryCorner();
        Location greater = this.getGreaterBoundaryCorner();

        //don't bother for very large claims, too expensive
        if (this.getArea() > 10000) return false;

        int seaLevel = 0;  //clean up all fluids in the end

        //respect sea level in normal worlds
        if (lesser.getWorld().getEnvironment() == Environment.NORMAL)
            seaLevel = GriefPrevention.instance.getSeaLevel(lesser.getWorld());

        for (int x = lesser.getBlockX(); x <= greater.getBlockX(); x++)
        {
            for (int z = lesser.getBlockZ(); z <= greater.getBlockZ(); z++)
            {
                for (int y = seaLevel - 1; y <= lesser.getWorld().getMaxHeight(); y++)
                {
                    //dodge the exclusion claim
                    Block block = lesser.getWorld().getBlockAt(x, y, z);

                    if (block.getType() == Material.WATER || block.getType() == Material.LAVA)
                    {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    Claim(Location lesserBoundaryCorner, Location greaterBoundaryCorner, UUID ownerID, List<String> builderIDs, List<String> containerIDs, List<String> accessorIDs, List<String> managerIDs, boolean inheritNothing, Long id) {
        this(lesserBoundaryCorner.getWorld(), new BoundingBox(lesserBoundaryCorner, greaterBoundaryCorner), ownerID, builderIDs, containerIDs, accessorIDs, managerIDs, inheritNothing, id);
    }

    //main constructor.  note that only creating a claim instance does nothing - a claim must be added to the data store to be effective
    Claim(World world, BoundingBox bounds, UUID ownerID, List<String> builderIDs, List<String> containerIDs, List<String> accessorIDs, List<String> managerIDs, boolean inheritNothing, Long id)
    {
        //modification date
        this.modifiedDate = Calendar.getInstance().getTime();

        //id
        this.id = id;

        //store world
        this.world = world;

        //store bounds
        this.bounds = bounds;

        //owner
        this.ownerID = ownerID;

        //other permissions
        for (String builderID : builderIDs)
        {
            this.setPermission(builderID, ClaimPermission.Build);
        }

        for (String containerID : containerIDs)
        {
            this.setPermission(containerID, ClaimPermission.Inventory);
        }

        for (String accessorID : accessorIDs)
        {
            this.setPermission(accessorID, ClaimPermission.Access);
        }

        for (String managerID : managerIDs)
        {
            if (managerID != null && !managerID.isEmpty())
            {
                this.managers.add(managerID);
            }
        }

        this.inheritNothing = inheritNothing;
    }

    Claim(Location lesserBoundaryCorner, Location greaterBoundaryCorner, UUID ownerID, List<String> builderIDs, List<String> containerIDs, List<String> accessorIDs, List<String> managerIDs, Long id)
    {
        this(lesserBoundaryCorner, greaterBoundaryCorner, ownerID, builderIDs, containerIDs, accessorIDs, managerIDs, false, id);
    }

    //produces a copy of a claim.
    public Claim(Claim claim) {
        this.modifiedDate = claim.modifiedDate;
        this.world = claim.getWorld();
        this.bounds = claim.bounds.clone();
        this.id = claim.id;
        this.ownerID = claim.ownerID;
        this.managers = new ArrayList<>(claim.managers);
        this.playerIDToClaimPermissionMap = new HashMap<>(claim.playerIDToClaimPermissionMap);
        this.inDataStore = false; //since it's a copy of a claim, not in datastore!
        this.areExplosivesAllowed = claim.areExplosivesAllowed;
        this.parent = claim.parent;
        this.inheritNothing = claim.inheritNothing;
        this.children = new ArrayList<>(claim.children);
        this.siegeData = claim.siegeData;
        this.doorsOpen = claim.doorsOpen;
        this.bannedPlayerIds = claim.bannedPlayerIds;
    }

    //measurements.  all measurements are in blocks
    public int getArea()
    {
        int claimLength = this.bounds.getLength() + 1;
        int claimWidth = this.bounds.getWidth() + 1;

        return claimLength * claimWidth;
    }

    // backwards, use getBounds getLength
    @Deprecated(forRemoval = true)
    public int getWidth() {
        return bounds.getLength() + 1;
    }

    // backwards and not anything to do with Y, use getBounds getLength
    @Deprecated(forRemoval = true)
    public int getHeight() {
        return bounds.getWidth() + 1;
    }

    public boolean getSubclaimRestrictions()
    {
        return inheritNothing;
    }

    public void setSubclaimRestrictions(boolean inheritNothing)
    {
        this.inheritNothing = inheritNothing;
    }

    // returns true if the location is near this claim by howNear
    public boolean isNear(Location location, int howNear) {
        BoundingBox bounds = parent == null ? this.bounds : parent.bounds;
        return bounds.clone().expand(howNear).contains(location);
    }

    /**
     * @deprecated Check {@link ClaimPermission#Edit} with {@link #checkPermission(Player, ClaimPermission, Event)}.
     * @param player the Player
     * @return the denial message, or null if the action is allowed
     */
    @Deprecated
    public @Nullable String allowEdit(@NotNull Player player)
    {
        Supplier<String> supplier = checkPermission(player, ClaimPermission.Edit, null);
        return supplier != null ? supplier.get() : null;
    }

    private static final Set<Material> PLACEABLE_FARMING_BLOCKS = EnumSet.of(
            Material.PUMPKIN_STEM,
            Material.WHEAT,
            Material.MELON_STEM,
            Material.CARROTS,
            Material.POTATOES,
            Material.NETHER_WART,
            Material.BEETROOTS,
            Material.COCOA,
            Material.GLOW_BERRIES,
            Material.CAVE_VINES,
            Material.CAVE_VINES_PLANT);

    private static boolean placeableForFarming(Material material)
    {
        return PLACEABLE_FARMING_BLOCKS.contains(material);
    }

    /**
     * @deprecated Check {@link ClaimPermission#Build} with {@link #checkPermission(Player, ClaimPermission, Event)}.
     * @param player the Player
     * @return the denial message, or null if the action is allowed
     */
    @Deprecated
    //build permission check
    public @Nullable String allowBuild(@NotNull Player player, @NotNull Material material)
    {
        Supplier<String> supplier = checkPermission(player, ClaimPermission.Build, new CompatBuildBreakEvent(material, false));
        return supplier != null ? supplier.get() : null;
    }

    public static class CompatBuildBreakEvent extends Event
    {
        private final Material material;
        private final boolean isBreak;

        private CompatBuildBreakEvent(Material material, boolean isBreak)
        {
            this.material = material;
            this.isBreak = isBreak;
        }

        public Material getMaterial()
        {
            return material;
        }

        public boolean isBreak()
        {
            return isBreak;
        }

        @Override
        public @NotNull HandlerList getHandlers()
        {
            return new HandlerList();
        }

    }

    public boolean hasExplicitPermission(@NotNull UUID uuid, @NotNull ClaimPermission level)
    {
        if (uuid.equals(this.getOwnerID())) return true;

        if (level == ClaimPermission.Manage) return this.managers.contains(uuid.toString());

        return level.isGrantedBy(this.playerIDToClaimPermissionMap.get(uuid.toString()));
    }

    public boolean hasExplicitPermission(@NotNull Player player, @NotNull ClaimPermission level)
    {
        // Check explicit ClaimPermission for UUID
        if (this.hasExplicitPermission(player.getUniqueId(), level)) return true;

        // Special case managers - a separate list is used.
        if (level == ClaimPermission.Manage)
        {
            for (String node : this.managers)
            {
                // Ensure valid permission format for permissions - [permission.node]
                if (node.length() < 3 || node.charAt(0) != '[' || node.charAt(node.length() - 1) != ']') continue;
                // Check if player has node
                if (player.hasPermission(node.substring(1, node.length() - 1))) return true;
            }
            return false;
        }

        // Check permission-based ClaimPermission
        for (Map.Entry<String, ClaimPermission> stringToPermission : this.playerIDToClaimPermissionMap.entrySet())
        {
            String node = stringToPermission.getKey();
            // Ensure valid permission format for permissions - [permission.node]
            if (node.length() < 3 || node.charAt(0) != '[' || node.charAt(node.length() - 1) != ']') continue;

            // Check if level is high enough and player has node
            if (level.isGrantedBy(stringToPermission.getValue())
                    && player.hasPermission(node.substring(1, node.length() - 1)))
                return true;
        }

        return false;
    }

    /**
     * Check whether a Player has a certain level of trust.
     *
     * @param player the Player being checked for permissions
     * @param permission the ClaimPermission level required
     * @param event the Event triggering the permission check
     * @return the denial message or null if permission is granted
     */
    public @Nullable Supplier<String> checkPermission(
            @NotNull Player player,
            @NotNull ClaimPermission permission,
            @Nullable Event event)
    {
        return checkPermission(player, permission, event, null);
    }

    /**
     * Checks if a player is banned from this claim
     * @param who the player to check
     * @return true if banned, false otherwise
     */
    public boolean checkBanned(Player who) {
        return checkBanned(who.getUniqueId());
    }

    /**
     * Checks if a player UUID is banned from this claim
     * @param uid the player UUID to check
     * @return true if banned, false otherwise
     */
    public boolean checkBanned(UUID uid) {
        if (bannedPlayerIds.contains(uid)) {
            return true;
        } else if (!inheritNothing && parent != null) {
            return parent.checkBanned(uid);
        }
        return false;
    }

    /**
     * Check whether a Player has a certain level of trust. For internal use; allows changing default message.
     *
     * @param player the Player being checked for permissions
     * @param permission the ClaimPermission level required
     * @param event the Event triggering the permission check
     * @param denialOverride a message overriding the default denial for clarity
     * @return the denial message or null if permission is granted
     */
    @Nullable Supplier<String> checkPermission(
            @NotNull Player player,
            @NotNull ClaimPermission permission,
            @Nullable Event event,
            @Nullable Supplier<String> denialOverride)
    {
        return callPermissionCheck(new ClaimPermissionCheckEvent(player, this, permission, event), denialOverride);
    }

    /**
     * Check whether a UUID has a certain level of trust.
     *
     * @param uuid the UUID being checked for permissions
     * @param permission the ClaimPermission level required
     * @param event the Event triggering the permission check
     * @return the denial reason or null if permission is granted
     */
    public @Nullable Supplier<String> checkPermission(
            @NotNull UUID uuid,
            @NotNull ClaimPermission permission,
            @Nullable Event event)
    {
        return callPermissionCheck(new ClaimPermissionCheckEvent(uuid, this, permission, event), null);
    }

    /**
     * Helper method for calling a ClaimPermissionCheckEvent.
     *
     * @param event the ClaimPermissionCheckEvent to call
     * @param denialOverride a message overriding the default denial for clarity
     * @return the denial reason or null if permission is granted
     */
    private @Nullable Supplier<String> callPermissionCheck(
            @NotNull ClaimPermissionCheckEvent event,
            @Nullable Supplier<String> denialOverride)
    {
        // Set denial message (if any) using default behavior.
        Supplier<String> defaultDenial = getDefaultDenial(event.getCheckedPlayer(), event.getCheckedUUID(),
                event.getRequiredPermission(), event.getTriggeringEvent());
        // If permission is denied and a clarifying override is provided, use override.
        if (defaultDenial != null && denialOverride != null) {
            defaultDenial = denialOverride;
        }

        event.setDenialReason(defaultDenial);

        Bukkit.getPluginManager().callEvent(event);

        return event.getDenialReason();
    }

    /**
     * Get the default reason for denial of a ClaimPermission.
     *
     * @param player the Player being checked for permissions
     * @param uuid the UUID being checked for permissions
     * @param permission the ClaimPermission required
     * @param event the Event triggering the permission check
     * @return the denial reason or null if permission is granted
     */
    private @Nullable Supplier<String> getDefaultDenial(
            @Nullable Player player,
            @NotNull UUID uuid,
            @NotNull ClaimPermission permission,
            @Nullable Event event)
    {
        if (player != null)
        {
            // Admin claims need adminclaims permission only.
            if (this.isAdminClaim())
            {
                if (player.hasPermission("griefprevention.adminclaims")) return null;
            }

            // Anyone with deleteclaims permission can edit non-admin claims at any time.
            else if (permission == ClaimPermission.Edit && player.hasPermission("griefprevention.deleteclaims"))
                return null;
        }

        // Claim owner and admins in ignoreclaims mode have access.
        if (uuid.equals(this.getOwnerID())
                || GriefPrevention.instance.dataStore.getPlayerData(uuid).ignoreClaims
                && hasBypassPermission(player, permission))
            return null;

        // Look for explicit individual permission.
        if (player != null)
        {
            if (this.hasExplicitPermission(player, permission)) return null;
        }
        else
        {
            if (this.hasExplicitPermission(uuid, permission)) return null;
        }

        // Check for public permission.
        if (permission.isGrantedBy(this.playerIDToClaimPermissionMap.get("public"))) return null;

        // Special building-only rules.
        if (permission == ClaimPermission.Build)
        {
            // No building while in PVP.
            PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(uuid);
            if (playerData.inPvpCombat())
            {
                return () -> GriefPrevention.instance.dataStore.getMessage(Messages.NoBuildPvP);
            }

            // Allow farming crops with container trust.
            Material material = null;
            if (event instanceof BlockBreakEvent || event instanceof BlockPlaceEvent)
                material = ((BlockEvent) event).getBlock().getType();

            if (material != null && placeableForFarming(material)
                    && this.getDefaultDenial(player, uuid, ClaimPermission.Inventory, event) == null)
                return null;
        }

        // Permission inheritance for subdivisions.
        if (this.parent != null && !inheritNothing) {
            ClaimPermission parentPerm = permission;
            // if permissiontrusted in parent claim allow edits
            if (parentPerm == ClaimPermission.Edit) parentPerm = ClaimPermission.Manage;
            return this.parent.getDefaultDenial(player, uuid, parentPerm, event);
        }

        // Catch-all error message for all other cases.
        return () ->
        {
            String reason = GriefPrevention.instance.dataStore.getMessage(permission.getDenialMessage(), this.getOwnerName());
            if (hasBypassPermission(player, permission))
                reason += "  " + GriefPrevention.instance.dataStore.getMessage(Messages.IgnoreClaimsAdvertisement);
            return reason;
        };
    }

    /**
     * Check if the {@link Player} has bypass permissions for a {@link ClaimPermission}. Owner-exclusive edit actions
     * require {@code griefprevention.deleteclaims}. All other actions require {@code griefprevention.ignoreclaims}.
     *
     * @param player the {@code Player}
     * @param permission the {@code ClaimPermission} whose bypass permission is being checked
     * @return whether the player has the bypass node
     */
    @Contract("null, _ -> false")
    private boolean hasBypassPermission(@Nullable Player player, @NotNull ClaimPermission permission)
    {
        if (player == null) return false;

        if (permission == ClaimPermission.Edit) return player.hasPermission("griefprevention.deleteclaims");

        return player.hasPermission("griefprevention.ignoreclaims");
    }

    /**
     * @deprecated Check {@link ClaimPermission#Build} with {@link #checkPermission(Player, ClaimPermission, Event)}.
     * @param player the Player
     * @return the denial message, or null if the action is allowed
     */
    @Deprecated
    public @Nullable String allowBreak(@NotNull Player player, @NotNull Material material)
    {
        Supplier<String> supplier = checkPermission(player, ClaimPermission.Build, new CompatBuildBreakEvent(material, true));
        return supplier != null ? supplier.get() : null;
    }

    /**
     * @deprecated Check {@link ClaimPermission#Access} with {@link #checkPermission(Player, ClaimPermission, Event)}.
     * @param player the Player
     * @return the denial message, or null if the action is allowed
     */
    @Deprecated
    public @Nullable String allowAccess(@NotNull Player player)
    {
        Supplier<String> supplier = checkPermission(player, ClaimPermission.Access, null);
        return supplier != null ? supplier.get() : null;
    }

    /**
     * @deprecated Check {@link ClaimPermission#Inventory} with {@link #checkPermission(Player, ClaimPermission, Event)}.
     * @param player the Player
     * @return the denial message, or null if the action is allowed
     */
    @Deprecated
    public @Nullable String allowContainers(@NotNull Player player)
    {
        Supplier<String> supplier = checkPermission(player, ClaimPermission.Inventory, null);
        return supplier != null ? supplier.get() : null;
    }

    /**
     * @deprecated Check {@link ClaimPermission#Manage} with {@link #checkPermission(Player, ClaimPermission, Event)}.
     * @param player the Player
     * @return the denial message, or null if the action is allowed
     */
    @Deprecated
    public @Nullable String allowGrantPermission(@NotNull Player player)
    {
        Supplier<String> supplier = checkPermission(player, ClaimPermission.Manage, null);
        return supplier != null ? supplier.get() : null;
    }

    @Contract("null -> null")
    public @Nullable ClaimPermission getPermission(@Nullable String playerID)
    {
        if (playerID == null || playerID.isEmpty()) return null;

        return this.playerIDToClaimPermissionMap.get(playerID.toLowerCase());
    }

    //grants a permission for a player or the public
    public void setPermission(@Nullable String playerID, @Nullable ClaimPermission permissionLevel)
    {
        if (permissionLevel == ClaimPermission.Edit) throw new IllegalArgumentException("Cannot add editors!");

        if (playerID == null || playerID.isEmpty()) return;

        if (permissionLevel == null)
            dropPermission(playerID);
        else if (permissionLevel == ClaimPermission.Manage)
            this.managers.add(playerID.toLowerCase());
        else
            this.playerIDToClaimPermissionMap.put(playerID.toLowerCase(), permissionLevel);
    }

    //revokes a permission for a player or the public
    public void dropPermission(@NotNull String playerID)
    {
        playerID = playerID.toLowerCase();
        this.playerIDToClaimPermissionMap.remove(playerID);
        this.managers.remove(playerID);

        for (Claim child : this.children)
        {
            child.dropPermission(playerID);
        }
    }

    //clears all permissions (except owner of course)
    public void clearPermissions()
    {
        this.playerIDToClaimPermissionMap.clear();
        this.managers.clear();

        for (Claim child : this.children)
        {
            child.clearPermissions();
        }
    }

    //gets ALL permissions
    //useful for  making copies of permissions during a claim resize and listing all permissions in a claim
    public void getPermissions(ArrayList<String> builders, ArrayList<String> containers, ArrayList<String> accessors, ArrayList<String> managers)
    {
        //loop through all the entries in the hash map
        for (Map.Entry<String, ClaimPermission> entry : this.playerIDToClaimPermissionMap.entrySet())
        {
            //build up a list for each permission level
            if (entry.getValue() == ClaimPermission.Build)
            {
                builders.add(entry.getKey());
            }
            else if (entry.getValue() == ClaimPermission.Inventory)
            {
                containers.add(entry.getKey());
            }
            else
            {
                accessors.add(entry.getKey());
            }
        }

        //managers are handled a little differently
        managers.addAll(this.managers);
    }

    // the claims current bounds, DO NOT MODIFY
    public BoundingBox getBounds() {
        return bounds;
    }

    //returns the world this claim resides in
    public World getWorld() {
        return world;
    }

    // creates a location representing the lesser corner of the getBounds
    public Location getLesserBoundaryCorner() {
        return new Location(world, bounds.getMinX(), bounds.getMinY(), bounds.getMinZ());
    }

    // creates a location representing the greater corner of the getBounds
    public Location getGreaterBoundaryCorner() {
        return new Location(world, bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ());
    }

    //returns a friendly owner name (for admin claims, returns "an administrator" as the owner)
    public String getOwnerName()
    {
        if (this.parent != null)
            return this.parent.getOwnerName();

        if (this.ownerID == null)
            return GriefPrevention.instance.dataStore.getMessage(Messages.OwnerNameForAdminClaims);

        return GriefPrevention.lookupPlayerName(this.ownerID);
    }

    public UUID getOwnerID()
    {
        if (this.parent != null)
        {
            return this.parent.ownerID;
        }
        return this.ownerID;
    }

    //whether or not a location is in a claim
    //ignoreHeight = true means location UNDER the claim will return TRUE
    //excludeSubdivisions = true means that locations inside subdivisions of the claim will return FALSE
    public boolean contains(Location location, boolean ignoreHeight, boolean excludeSubdivisions)
    {
        //not in the same world implies false
        if (!Objects.equals(location.getWorld(), world)) return false;

        int x = location.getBlockX();
        int z = location.getBlockZ();

        // If we're ignoring height, use 2D containment check.
        if (ignoreHeight && !bounds.contains2d(x, z))
        {
            return false;
        }
        // Otherwise use full containment check.
        else if (!ignoreHeight && !bounds.contains(x, location.getBlockY(), z))
        {
            return false;
        }

        //code to exclude subdivisions in this check
        else if (excludeSubdivisions)
        {
            //search all subdivisions to see if the location is in any of them
            for (Claim child : this.children)
            {
                //if we find such a subdivision, return false
                if (child.contains(location, ignoreHeight, true))
                {
                    return false;
                }
            }
        }

        //otherwise yes
        return true;
    }

    public boolean isCorner(Block block) {
        return isCorner(block.getX(), block.getY(), block.getZ());
    }

    public boolean isCorner(int x, int y, int z) {
        return (x == bounds.getMinX() || x == bounds.getMaxX()) && (z == bounds.getMinZ() || z == bounds.getMaxZ())
                && (!is3D() || y == bounds.getMinY() || y == bounds.getMaxY());
    }

    public boolean is3D() {
        return bounds.getMaxY() != _2D_HEIGHT;
    }

    //whether or not two claims overlap
    //used internally to prevent overlaps when creating claims
    boolean overlaps(Claim otherClaim)
    {
        if (!Objects.equals(world, otherClaim.getWorld())) return false;

        if (is3D() && otherClaim.is3D()) {
            return bounds.intersects(otherClaim.bounds);
        } else {
            return bounds.intersects2d(otherClaim.bounds);
        }
    }

    public boolean isInside(Block block) {
        return isInside(block.getX(), block.getY(), block.getZ());
    }

    public boolean isInside(int x, int y, int z) {
        return bounds.contains(x, y, z);
    }

    //whether more entities may be added to a claim
    public String allowMoreEntities(boolean remove)
    {
        if (this.parent != null) return this.parent.allowMoreEntities(remove);

        //this rule only applies to creative mode worlds
        if (!GriefPrevention.instance.creativeRulesApply(world)) return null;

        //admin claims aren't restricted
        if (this.isAdminClaim()) return null;

        //don't apply this rule to very large claims
        if (this.getArea() > 10000) return null;

        //determine maximum allowable entity count, based on claim size
        int maxEntities = this.getArea() / 50;
        if (maxEntities == 0) return GriefPrevention.instance.dataStore.getMessage(Messages.ClaimTooSmallForEntities);

        //count current entities (ignoring players)
        int totalEntities = 0;
        ArrayList<Chunk> chunks = this.getChunks();
        for (Chunk chunk : chunks)
        {
            Entity[] entities = chunk.getEntities();
            for (Entity entity : entities)
            {
                if (!(entity instanceof Player) && this.contains(entity.getLocation(), false, false))
                {
                    totalEntities++;
                    if (remove && totalEntities > maxEntities) entity.remove();
                }
            }
        }

        if (totalEntities >= maxEntities)
            return GriefPrevention.instance.dataStore.getMessage(Messages.TooManyEntitiesInClaim);

        return null;
    }

    public String allowMoreActiveBlocks()
    {
        if (this.parent != null) return this.parent.allowMoreActiveBlocks();

        //determine maximum allowable entity count, based on claim size
        int maxActives = this.getArea() / 100;
        if (maxActives == 0)
            return GriefPrevention.instance.dataStore.getMessage(Messages.ClaimTooSmallForActiveBlocks);

        //count current actives
        int totalActives = 0;
        ArrayList<Chunk> chunks = this.getChunks();
        for (Chunk chunk : chunks)
        {
            BlockState[] actives = chunk.getTileEntities();
            for (BlockState active : actives)
            {
                if (BlockEventHandler.isActiveBlock(active))
                {
                    if (this.contains(active.getLocation(), false, false))
                    {
                        totalActives++;
                    }
                }
            }
        }

        if (totalActives >= maxActives)
            return GriefPrevention.instance.dataStore.getMessage(Messages.TooManyActiveBlocksInClaim);

        return null;
    }

    //implements a strict ordering of claims, used to keep the claims collection sorted for faster searching
    boolean greaterThan(Claim otherClaim)
    {
        Location thisCorner = this.getLesserBoundaryCorner();
        Location otherCorner = otherClaim.getLesserBoundaryCorner();

        if (thisCorner.getBlockX() > otherCorner.getBlockX()) return true;

        if (thisCorner.getBlockX() < otherCorner.getBlockX()) return false;

        if (thisCorner.getBlockZ() > otherCorner.getBlockZ()) return true;

        if (thisCorner.getBlockZ() < otherCorner.getBlockZ()) return false;

        return thisCorner.getWorld().getName().compareTo(otherCorner.getWorld().getName()) < 0;
    }


    long getPlayerInvestmentScore()
    {
        //decide which blocks will be considered player placed
        Location lesserBoundaryCorner = this.getLesserBoundaryCorner();
        Set<Material> playerBlocks = RestoreNatureProcessingTask.getPlayerBlocks(world.getEnvironment(), lesserBoundaryCorner.getBlock().getBiome());

        //scan the claim for player placed blocks
        double score = 0;

        boolean creativeMode = GriefPrevention.instance.creativeRulesApply(world);

        for (int x = this.bounds.getMinX(); x <= this.bounds.getMaxX(); x++)
        {
            for (int z = this.bounds.getMinZ(); z <= this.bounds.getMaxZ(); z++)
            {
                int y = this.bounds.getMinY();
                for (; y < GriefPrevention.instance.getSeaLevel(this.world) - 5; y++)
                {
                    Block block = this.world.getBlockAt(x, y, z);
                    if (playerBlocks.contains(block.getType()))
                    {
                        if (block.getType() == Material.CHEST && !creativeMode)
                        {
                            score += 10;
                        }
                        else
                        {
                            score += .5;
                        }
                    }
                }

                for (; y < this.world.getMaxHeight(); y++)
                {
                    Block block = this.world.getBlockAt(x, y, z);
                    if (playerBlocks.contains(block.getType()))
                    {
                        if (block.getType() == Material.CHEST && !creativeMode)
                        {
                            score += 10;
                        }
                        else if (creativeMode && (block.getType() == Material.LAVA))
                        {
                            score -= 10;
                        }
                        else
                        {
                            score += 1;
                        }
                    }
                }
            }
        }

        return (long) score;
    }

    public ArrayList<Chunk> getChunks()
    {
        ArrayList<Chunk> chunks = new ArrayList<>();

        World world = this.getLesserBoundaryCorner().getWorld();
        Chunk lesserChunk = this.getLesserBoundaryCorner().getChunk();
        Chunk greaterChunk = this.getGreaterBoundaryCorner().getChunk();

        for (int x = lesserChunk.getX(); x <= greaterChunk.getX(); x++)
        {
            for (int z = lesserChunk.getZ(); z <= greaterChunk.getZ(); z++)
            {
                chunks.add(world.getChunkAt(x, z));
            }
        }

        return chunks;
    }

    ArrayList<Long> getChunkHashes()
    {
        return DataStore.getChunkHashes(this);
    }

    @Override
    public String toString() {
        return "Claim{" +
                "lesserBoundaryCorner=%d,%d,%d".formatted(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ()) +
                ", greaterBoundaryCorner=%d,%d,%d".formatted(bounds.getMaxX(), bounds.getMaxY(), bounds.getMaxZ()) +
                ", world=" + world.getName() +
                ", id=" + id +
                ", ownerID=" + ownerID +
                ", inheritNothing=" + inheritNothing +
                (parent == null ? ", children" + children : ", parentId=" + parent.id) +
                '}';
    }

}
