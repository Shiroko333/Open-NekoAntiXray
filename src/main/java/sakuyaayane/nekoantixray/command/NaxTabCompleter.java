package sakuyaayane.nekoantixray.command;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

public class NaxTabCompleter implements TabCompleter {
   private final File dataFolder;
   private static final List<String> NAX_SUBCOMMANDS = Arrays.asList(
      "reload",
      "ban",
      "showhwid",
      "resetviolation",
      "replay",
      "togglemassexcavation",
      "record",
      "internalbypass",
      "cleanup",
      "realseed",
      "secureseed",
      "announcement",
      "orestats",
      "oretop",
      "oretrack",
      "diagnose",
      "stats",
      "debugchunks",
      "gui",
      "vl",
      "decoy",
      "resend",
      "report",
      "summary",
      "probeguard",
      "help"
   );
   private static final List<String> REPLAY_ACTIONS = Arrays.asList("stop", "pause", "forward", "backward", "tp");
   private static final List<String> INTERNAL_BYPASS_ACTIONS = Arrays.asList("grant", "remove", "list", "clear", "info", "extend");
   private static final List<String> TIME_RANGE_PRESETS = Arrays.asList("30m", "1h", "6h", "12h", "1d", "3d", "7d", "all");
   private static final List<String> DECOY_ORE_MATERIALS = Arrays.asList(
      "DIAMOND_ORE",
      "DEEPSLATE_DIAMOND_ORE",
      "EMERALD_ORE",
      "DEEPSLATE_EMERALD_ORE",
      "GOLD_ORE",
      "DEEPSLATE_GOLD_ORE",
      "IRON_ORE",
      "DEEPSLATE_IRON_ORE",
      "COAL_ORE",
      "DEEPSLATE_COAL_ORE",
      "LAPIS_ORE",
      "DEEPSLATE_LAPIS_ORE",
      "REDSTONE_ORE",
      "DEEPSLATE_REDSTONE_ORE",
      "COPPER_ORE",
      "DEEPSLATE_COPPER_ORE",
      "NETHER_QUARTZ_ORE",
      "NETHER_GOLD_ORE",
      "ANCIENT_DEBRIS",
      "EMERALD_ORE"
   );

   public NaxTabCompleter(File dataFolder) {
      this.dataFolder = dataFolder;
   }

   @Override
   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (command.getName().equalsIgnoreCase("nax")) {
         return this.completeNaxCommand(args);
      } else {
         return command.getName().equalsIgnoreCase("nekoantixrayupdate") ? this.completeUpdateCommand(args) : Collections.emptyList();
      }
   }

   private List completeNaxCommand(String[] args) {
      List<String> completions = new ArrayList<>();
      if (args.length == 1) {
         StringUtil.copyPartialMatches(args[0], NAX_SUBCOMMANDS, completions);
      } else if (args.length > 1) {
         switch (args[0].toLowerCase()) {
            case "ban":
            case "resetviolation":
            case "orestats":
            case "oretrack":
               if (args.length == 2) {
                  for (Player player : Bukkit.getOnlinePlayers()) {
                     if (StringUtil.startsWithIgnoreCase(player.getName(), args[1])) {
                        completions.add(player.getName());
                     }
                  }
               }
               break;
            case "replay":
               return this.completeReplaySubCommand(args);
            case "record":
               return this.completeRecordSubCommand(args);
            case "internalbypass":
               return this.completeInternalBypassSubCommand(args);
            case "realseed":
               if (args.length == 2) {
                  for (World world : Bukkit.getWorlds()) {
                     if (StringUtil.startsWithIgnoreCase(world.getName(), args[1])) {
                        completions.add(world.getName());
                     }
                  }
               }
               break;
            case "vl":
               if (args.length == 2) {
                  for (Player player : Bukkit.getOnlinePlayers()) {
                     if (StringUtil.startsWithIgnoreCase(player.getName(), args[1])) {
                        completions.add(player.getName());
                     }
                  }
               }
               break;
            case "resend":
               if (args.length == 2) {
                  for (Player player : Bukkit.getOnlinePlayers()) {
                     if (StringUtil.startsWithIgnoreCase(player.getName(), args[1])) {
                        completions.add(player.getName());
                     }
                  }
               } else if (args.length == 3) {
                  StringUtil.copyPartialMatches(args[2], Arrays.asList("2", "4", "8", "16", "32", "cancel"), completions);
               }
               break;
            case "report":
               if (args.length == 2) {
                  for (Player player : Bukkit.getOnlinePlayers()) {
                     if (StringUtil.startsWithIgnoreCase(player.getName(), args[1])) {
                        completions.add(player.getName());
                     }
                  }

                  StringUtil.copyPartialMatches(args[1], TIME_RANGE_PRESETS, completions);
               } else if (args.length == 3) {
                  StringUtil.copyPartialMatches(args[2], TIME_RANGE_PRESETS, completions);
               }
               break;
            case "summary":
               if (args.length == 2) {
                  StringUtil.copyPartialMatches(args[1], TIME_RANGE_PRESETS, completions);
               }
               break;
            case "help":
               if (args.length == 2) {
                  StringUtil.copyPartialMatches(args[1], NAX_SUBCOMMANDS, completions);
                  StringUtil.copyPartialMatches(args[1], Arrays.asList("1", "2", "3", "4"), completions);
               }
               break;
            case "decoy":
               if (args.length == 2) {
                  for (Player player : Bukkit.getOnlinePlayers()) {
                     if (StringUtil.startsWithIgnoreCase(player.getName(), args[1])) {
                        completions.add(player.getName());
                     }
                  }

                  StringUtil.copyPartialMatches(args[1], DECOY_ORE_MATERIALS, completions);
               } else if (args.length == 3) {
                  if (Bukkit.getPlayerExact(args[1]) != null) {
                     StringUtil.copyPartialMatches(args[2], DECOY_ORE_MATERIALS, completions);
                  } else {
                     StringUtil.copyPartialMatches(args[2], Arrays.asList("8", "16", "24", "32", "48", "64"), completions);
                  }
               } else if (args.length == 4) {
                  StringUtil.copyPartialMatches(args[3], Arrays.asList("8", "16", "24", "32", "48", "64"), completions);
               }
         }
      }

      Collections.sort(completions);
      return completions;
   }

   private List completeReplaySubCommand(String[] args) {
      List<String> completions = new ArrayList<>();
      if (args.length == 2) {
         StringUtil.copyPartialMatches(args[1], REPLAY_ACTIONS, completions);
      }

      Collections.sort(completions);
      return completions;
   }

   private List completeRecordSubCommand(String[] args) {
      List<String> completions = new ArrayList<>();
      if (args.length == 2) {
         for (Player player : Bukkit.getOnlinePlayers()) {
            if (StringUtil.startsWithIgnoreCase(player.getName(), args[1])) {
               completions.add(player.getName());
            }
         }
      } else if (args.length == 3) {
         List<String> recordActions = Arrays.asList("start", "stop");
         StringUtil.copyPartialMatches(args[2], recordActions, completions);
      } else if (args.length == 4 && args[2].equalsIgnoreCase("start")) {
         List<String> recordLabels = Arrays.asList("cheater", "legit");
         StringUtil.copyPartialMatches(args[3], recordLabels, completions);
      }

      Collections.sort(completions);
      return completions;
   }

   private List completeInternalBypassSubCommand(String[] args) {
      List<String> completions = new ArrayList<>();
      if (args.length == 2) {
         StringUtil.copyPartialMatches(args[1], INTERNAL_BYPASS_ACTIONS, completions);
      } else if (args.length == 3) {
         String action = args[1].toLowerCase();
         if ("grant".equals(action) || "remove".equals(action) || "info".equals(action) || "extend".equals(action)) {
            for (Player player : Bukkit.getOnlinePlayers()) {
               if (StringUtil.startsWithIgnoreCase(player.getName(), args[2])) {
                  completions.add(player.getName());
               }
            }
         }
      } else if (args.length == 4 && ("grant".equals(args[1].toLowerCase()) || "extend".equals(args[1].toLowerCase()))) {
         List<String> durationPresets = Arrays.asList("60", "300", "600", "1800", "3600");
         StringUtil.copyPartialMatches(args[3], durationPresets, completions);
      }

      Collections.sort(completions);
      return completions;
   }

   private List completeUpdateCommand(String[] args) {
      return args.length == 1
         ? StringUtil.copyPartialMatches(args[0], Arrays.asList("force", "check"), new ArrayList<String>())
         : Collections.emptyList();
   }
}
