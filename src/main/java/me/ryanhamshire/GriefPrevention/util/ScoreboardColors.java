package me.ryanhamshire.GriefPrevention.util;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Map.Entry;

public class ScoreboardColors {

    private static final Scoreboard SCOREBOARD = Bukkit.getScoreboardManager().getMainScoreboard();
    private static final Map<ChatColor, String> COLOR_TEAM_NAMES = makeColorTeamNameMap();
    private static final Map<ChatColor, Team> BACKING_SCOREBOARD_COLOR_TEAMS = createColorTeams(SCOREBOARD);
    public static final Map<ChatColor, Team> SCOREBOARD_COLOR_TEAMS = Collections.unmodifiableMap(BACKING_SCOREBOARD_COLOR_TEAMS);
    
    public static Map<ChatColor, Team> getColorTeams() {
        return SCOREBOARD_COLOR_TEAMS;
    }

    public static Team getTeamFor(ChatColor color) {
        return BACKING_SCOREBOARD_COLOR_TEAMS.get(color);
    }

    public static Map<ChatColor, Team> createColorTeams(Scoreboard board) {
        EnumMap<ChatColor, Team> teamColors = new EnumMap<>(ChatColor.class);
        for (Entry<ChatColor, String> e : COLOR_TEAM_NAMES.entrySet()) {
            Team team = board.getTeam(e.getValue());
            if (team == null) team = board.registerNewTeam(e.getValue());
            team.setColor(e.getKey());
            teamColors.put(e.getKey(), team);
        }
        return teamColors;
    }

    public static EnumMap<ChatColor, String> makeColorTeamNameMap() {
        EnumMap<ChatColor, String> colorTeamNames = new EnumMap<>(ChatColor.class);
        for (ChatColor color : ChatColor.values()) {
            if (color.isColor()) {
                colorTeamNames.put(color, "griefprevention_" + StringUtil.enumNameToCamelCase(color.name()));
            }
        }
        return colorTeamNames;
    }

    /**
     * Gets the team name for a specified color
     * @param color the color
     * @return the team name or null if the ChatColor is not a color (ie: a format)
     */
    public static @Nullable String teamNameFor(ChatColor color) {
        return COLOR_TEAM_NAMES.get(color);
    }

}
