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

import me.ryanhamshire.GriefPrevention.util.BoundingBox;
import me.ryanhamshire.GriefPrevention.util.SafeTeleports;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.stream.Stream;

//tries to rescue a trapped player from a claim where he doesn't have permission to save himself
//related to the /trapped slash command
//this does run in the main thread, so it's okay to make non-thread-safe calls
class PlayerRescueTask implements Runnable
{
    //original location where /trapped was used
    private final Location location;

    //rescue destination, may be decided at instantiation or at execution
    private Location destination;

    //player data
    private final Player player;

    public PlayerRescueTask(Player player, Location location, Location destination)
    {
        this.player = player;
        this.location = location;
        this.destination = destination;
    }

    @Override
    public void run()
    {
        //if he logged out, don't do anything
        if (!player.isOnline()) return;

        //he no longer has a pending /trapped slash command, so he can try to use it again now
        PlayerData playerData = GriefPrevention.instance.dataStore.getPlayerData(player.getUniqueId());
        playerData.pendingTrapped = false;

        //if the player moved three or more blocks from where he used /trapped,  him and don't save him
        if (!player.getLocation().getWorld().equals(this.location.getWorld()) || player.getLocation().distance(this.location) > 3)
        {
            GriefPrevention.sendMessage(player, TextMode.Err, Messages.RescueAbortedMoved);
            return;
        }

        //otherwise find a place to teleport him
        if (this.destination == null)
        {
            Claim current = GriefPrevention.instance.dataStore.getClaimAt(player.getLocation(), true, null);
            if (current != null) {
                Location lesser = current.getLesserBoundaryCorner();
                Location greater = current.getGreaterBoundaryCorner();
                World world = lesser.getWorld();
                lesser.setY(world.getMinHeight());
                greater.setY(world.getMaxHeight());
                BoundingBox ofClaim = new BoundingBox(lesser, greater);
                this.destination = SafeTeleports.findSafeLocationAround(world,
                        player.getBoundingBox(),
                        ofClaim,
                        world.getMinHeight(),
                        // only let a player teleport on top of the nether roof if they are already above it
                        world.getEnvironment() == World.Environment.NETHER && player.getLocation().getBlockY() < 128 ? 126 : world.getMaxHeight(),
                        10_000_000L,
                        PlayerRescueTask::isUnclaimed);
            } else {
                this.destination = findNearestSafeLocation(player, player.getLocation(), player.getWorld(), false);
            }
        }

        if (this.destination != null) {
            player.teleport(this.destination);
            //log entry, in case admins want to investigate the "trap"
            GriefPrevention.AddLogEntry("Rescued trapped player " + player.getName() + " from " + GriefPrevention.getfriendlyLocationString(this.location) + " to " + GriefPrevention.getfriendlyLocationString(this.destination) + ".");
        } else {
            // check possible fallback safe locations
            this.destination = Stream.of(player.getBedSpawnLocation(), player.getWorld().getSpawnLocation(), Bukkit.getWorlds().get(0).getSpawnLocation())
                    .filter(Objects::nonNull)
                    .distinct()
                    .map(l -> findNearestSafeLocation(player, l, l.getWorld(), !l.equals(player.getBedSpawnLocation())))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);
            if (this.destination == null) {
                GriefPrevention.sendMessage(player, TextMode.Err, Messages.TrappedWontWorkHere);
                //log entry, in case admins want to investigate the "trap"
                GriefPrevention.AddLogEntry("Couldn't rescue trapped player " + player.getName() + " from " + GriefPrevention.getfriendlyLocationString(this.location) + " and world spawn location(s) are UNSAFE!");
            } else {
                player.teleport(destination);
                //log entry, in case admins want to investigate the "trap"
                GriefPrevention.AddLogEntry("Couldn't rescue trapped player " + player.getName() + " from " + GriefPrevention.getfriendlyLocationString(this.location) + " sending them to " + GriefPrevention.getfriendlyLocationString(this.destination) + " instead.");
            }
        }
    }

    public static Location findNearestSafeLocation(Player player, Location from, World world, boolean allowClaimed) {
        return SafeTeleports.findNearestSafeLocation(from == null ? player.getLocation() : from,
                player.getBoundingBox(),
                32,
                world.getMinHeight(),
                world.getMaxHeight(),
                b -> allowClaimed || isUnclaimed(b)
        );
    }

    private static Claim lastChecked = null;
    public static boolean isUnclaimed(Block block) {
        lastChecked = GriefPrevention.instance.dataStore.getClaimAt(block.getLocation(), true, lastChecked);
        return lastChecked == null;
    }

}
