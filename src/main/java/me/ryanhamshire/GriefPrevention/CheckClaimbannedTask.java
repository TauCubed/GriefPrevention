package me.ryanhamshire.GriefPrevention;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class CheckClaimbannedTask implements Runnable {

    @Override
    public void run() {
        for (Player p : Bukkit.getOnlinePlayers()) {
            Claim claim = GriefPrevention.instance.dataStore.getClaimAt(p.getLocation(), false, null);
            if (claim != null) {
                if (claim.checkBanned(p.getUniqueId())) {
                    p.eject();
                    GriefPrevention.ejectPlayerFromBannedClaim(p);
                    GriefPrevention.sendMessage(p, TextMode.Err, Messages.BannedFromClaim);
                }
            }
        }
    }

}
