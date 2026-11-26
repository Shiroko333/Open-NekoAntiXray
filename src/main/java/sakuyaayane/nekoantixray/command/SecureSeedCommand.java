package sakuyaayane.nekoantixray.command;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.craftbukkit.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.World;
import sakuyaayane.nekoantixray.NekoAntiXray;
import sakuyaayane.nekoantixray.seed.AsyncChunkSeedProtection;
import sakuyaayane.nekoantixray.seed.SecureSeedStatusFormatter;
import sakuyaayane.nekoantixray.seed.WorldSeedResolver;

public class SecureSeedCommand implements CommandExecutor, TabCompleter {
   private final NekoAntiXray plugin;

   public SecureSeedCommand(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.secureseed")) {
         sender.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
         return true;
      }

      if (args.length == 0) {
         this.sendHelp(sender);
         return true;
      }

      String subcommand = args[0].toLowerCase();
      switch (subcommand) {
         case "status":
            this.showStatus(sender);
            break;
         case "info":
            String targetWorldName = args.length > 1 ? args[1] : this.resolveDefaultWorldName(sender);
            this.showWorldInfo(sender, targetWorldName);
            break;
         case "generate":
            this.generateNewSeed(sender);
            break;
         case "stats":
            this.showStatistics(sender);
            break;
         case "reload":
            this.reloadSecureSystem(sender);
            break;
         case "test":
            if (args.length > 2) {
               this.testSlimeChunk(sender, args[1], args[2]);
            } else {
               sender.sendMessage(ChatColor.RED + "Usage: /secureseed test <chunkX> <chunkZ>");
            }
            break;
         case "teststructure":
            if (args.length > 3) {
               String chunkXArg = args[1];
               String chunkZArg = args[2];
               String structureType = args[3];
               this.testStructureProtection(sender, chunkXArg, chunkZArg, structureType);
            } else {
               sender.sendMessage(ChatColor.RED + "Usage: /secureseed teststructure <chunkX> <chunkZ> <structureType>");
               sender.sendMessage(ChatColor.YELLOW + "Structure types: desert_pyramid, jungle_pyramid, swamp_hut, igloo, shipwreck, pillager_outpost, monument");
            }
            break;
         default:
            this.sendHelp(sender);
      }

      return true;
   }

   @Override
   public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
      if (args.length == 1) {
         return Arrays.asList("status", "info", "generate", "stats", "reload", "test", "teststructure");
      } else if (args.length == 2 && "info".equals(args[0])) {
         return this.plugin.getServer().getWorlds().stream().map(world -> world.getName()).toList();
      } else {
         return args.length == 4 && "teststructure".equals(args[0])
            ? Arrays.asList("desert_pyramid", "jungle_pyramid", "swamp_hut", "igloo", "shipwreck", "pillager_outpost", "monument", "end_city", "dungeon")
            : null;
      }
   }

   private void sendHelp(CommandSender sender) {
      sender.sendMessage(ChatColor.GOLD + "=== Secure Seed Commands ===");
      sender.sendMessage(ChatColor.YELLOW + "/secureseed status" + ChatColor.WHITE + " - Show security status");
      sender.sendMessage(ChatColor.YELLOW + "/secureseed info [world]" + ChatColor.WHITE + " - Show world information");
      sender.sendMessage(ChatColor.YELLOW + "/secureseed generate" + ChatColor.WHITE + " - Generate new secure seed");
      sender.sendMessage(ChatColor.YELLOW + "/secureseed stats" + ChatColor.WHITE + " - Show security statistics");
      sender.sendMessage(ChatColor.YELLOW + "/secureseed reload" + ChatColor.WHITE + " - Reload secure system");
      sender.sendMessage(ChatColor.YELLOW + "/secureseed test <x> <z>" + ChatColor.WHITE + " - Test slime chunk calculation");
      sender.sendMessage(ChatColor.YELLOW + "/secureseed teststructure <x> <z> <type>" + ChatColor.WHITE + " - Test structure protection");
   }

   private void showStatus(CommandSender sender) {
      sender.sendMessage(ChatColor.GOLD + "=== Secure Anti-Seed Cracker Status ===");
      boolean secureSystemActive = SecureSeedStatusFormatter.isSecureSystemActive();
      sender.sendMessage(ChatColor.GRAY + "System Status: " + (secureSystemActive ? ChatColor.GREEN + "ACTIVE" : ChatColor.RED + "INACTIVE"));
      if (secureSystemActive) {
         String formattedStatus = SecureSeedStatusFormatter.formatSecurityStatus();
         sender.sendMessage(formattedStatus);
      } else {
         sender.sendMessage(ChatColor.RED + "Secure seed system is not active!");
         sender.sendMessage(ChatColor.YELLOW + "Run '/secureseed generate' to initialize the system.");
      }
   }

   private void showWorldInfo(CommandSender sender, String worldName) {
      sender.sendMessage(ChatColor.GOLD + "=== World Information: " + worldName + " ===");
      AsyncChunkSeedProtection.WorldProtectionConfig worldConfig = AsyncChunkSeedProtection.getWorldConfig(worldName);
      if (worldConfig == null) {
         sender.sendMessage(ChatColor.RED + "World '" + worldName + "' is not secured!");
      } else {
         sender.sendMessage(ChatColor.GRAY + "World Name: " + ChatColor.WHITE + worldName);
         WorldSeedResolver.StructureOffset structureOffset = worldConfig.getStructureOffset();
         if (structureOffset != null) {
            int offsetX = structureOffset.getOffsetX();
            int offsetZ = structureOffset.getOffsetZ();
            sender.sendMessage(ChatColor.GRAY + "Structure Offset: " + ChatColor.WHITE + "(" + offsetX + ", " + offsetZ + ")");
            String saltHex = Long.toHexString(structureOffset.getSalt()).toUpperCase();
            sender.sendMessage(ChatColor.GRAY + "Offset Salt: " + ChatColor.WHITE + saltHex);
         }

         sender.sendMessage(ChatColor.GRAY + "Security Level: " + ChatColor.GREEN + "MAXIMUM");
         sender.sendMessage(ChatColor.GRAY + "Seed Bits: " + ChatColor.AQUA + "1024");
      }
   }

   private void showStatistics(CommandSender sender) {
      sender.sendMessage(ChatColor.GOLD + "=== Security Statistics ===");

      try {
         Set<String> protectedWorlds = AsyncChunkSeedProtection.getProtectedWorlds();
         int totalSecuredWorlds = protectedWorlds.size();
         int activeSecuredWorlds = 0;

         for (World world : this.plugin.getServer().getWorlds()) {
            if (protectedWorlds.contains(world.getName())) {
               activeSecuredWorlds++;
            }
         }

         sender.sendMessage(ChatColor.GRAY + "Secured Worlds: " + ChatColor.WHITE + totalSecuredWorlds);
         sender.sendMessage(ChatColor.GRAY + "Active Secured Worlds: " + ChatColor.WHITE + activeSecuredWorlds);
         sender.sendMessage(ChatColor.GRAY + "Seed Bits: " + ChatColor.WHITE + "1024");
         sender.sendMessage(ChatColor.GRAY + "Hash Algorithm: " + ChatColor.WHITE + "BLAKE2b");
         sender.sendMessage(ChatColor.GRAY + "RNG Type: " + ChatColor.WHITE + "SecureCryptoRandom");
         sender.sendMessage(ChatColor.GRAY + "Structure Protection: " + ChatColor.WHITE + "Advanced Anti-SeedCracker");
         sender.sendMessage(ChatColor.GRAY + "Ore Protection: " + ChatColor.WHITE + "Randomized Generation");
         if (protectedWorlds.size() > 0) {
            sender.sendMessage(ChatColor.GRAY + "Configured Worlds:");

            for (String worldName : protectedWorlds) {
               AsyncChunkSeedProtection.WorldProtectionConfig worldConfig = AsyncChunkSeedProtection.getWorldConfig(worldName);
               boolean isConfigured = worldConfig != null;
               String statusIcon = isConfigured ? ChatColor.GREEN + "✓" : ChatColor.RED + "✗";
               sender.sendMessage(ChatColor.GRAY + "  - " + ChatColor.WHITE + worldName + " " + statusIcon);
            }
         }
      } catch (Exception ex) {
         sender.sendMessage(ChatColor.RED + "Failed to retrieve statistics: " + ex.getMessage());
      }
   }

   private void reloadSecureSystem(CommandSender sender) {
      sender.sendMessage(ChatColor.YELLOW + "Reloading secure seed system...");

      try {
         AsyncChunkSeedProtection.cleanup();
         this.plugin.getServer().getWorlds().forEach(world -> {
            try {
               WorldSeedResolver.setupWorldSeed(((CraftWorld)world).getHandle());
            } catch (Exception worldEx) {
               sender.sendMessage(ChatColor.RED + "Failed to reinitialize world " + world.getName() + ": " + worldEx.getMessage());
            }
         });
         sender.sendMessage(ChatColor.GREEN + "Secure seed system reloaded successfully!");
      } catch (Exception reloadEx) {
         sender.sendMessage(ChatColor.RED + "Failed to reload secure system: " + reloadEx.getMessage());
      }
   }

   private void testSlimeChunk(CommandSender sender, String chunkXArg, String chunkZArg) {
      try {
         int chunkX = Integer.parseInt(chunkXArg);
         int chunkZ = Integer.parseInt(chunkZArg);
         String worldName = this.resolveDefaultWorldName(sender);
         boolean slimeChunk = SecureSeedStatusFormatter.isSlimeChunk(this.plugin.getServer().getWorld(worldName), chunkX, chunkZ);
         sender.sendMessage(ChatColor.GRAY + "Chunk (" + chunkX + ", " + chunkZ + ") in world " + worldName + ":");
         sender.sendMessage(ChatColor.GRAY + "Is Slime Chunk: " + (slimeChunk ? ChatColor.GREEN + "YES" : ChatColor.RED + "NO"));
      } catch (NumberFormatException parseEx) {
         sender.sendMessage(ChatColor.RED + "Invalid coordinates. Please use integers.");
      } catch (Exception ex) {
         sender.sendMessage(ChatColor.RED + "Error testing slime chunk: " + ex.getMessage());
      }
   }

   private String resolveDefaultWorldName(CommandSender sender) {
      return sender instanceof Player ? ((Player)sender).getWorld().getName() : this.plugin.getServer().getWorlds().get(0).getName();
   }

   private void generateNewSeed(CommandSender sender) {
      sender.sendMessage(ChatColor.YELLOW + "Generating new secure seed...");

      try {
         long[] seedWords = WorldSeedResolver.generateRandomSeed();
         String seedText = WorldSeedResolver.formatSeedToString(seedWords);
         String displaySeed = seedText.length() > 32 ? seedText.substring(0, 32) + "..." : seedText;
         sender.sendMessage(ChatColor.GREEN + "New secure seed generated successfully!");
         sender.sendMessage(ChatColor.GRAY + "Seed Hash: " + ChatColor.WHITE + displaySeed);
         sender.sendMessage(ChatColor.GRAY + "Bits: " + ChatColor.AQUA + "1024");
         sender.sendMessage(ChatColor.YELLOW + "Note: Restart the server to apply the new seed to new worlds.");
      } catch (Exception ex) {
         sender.sendMessage(ChatColor.RED + "Failed to generate secure seed: " + ex.getMessage());
      }
   }

   private void testStructureProtection(CommandSender sender, String chunkXArg, String chunkZArg, String structureType) {
      try {
         int chunkX = Integer.parseInt(chunkXArg);
         int chunkZ = Integer.parseInt(chunkZArg);
         String worldName = this.resolveDefaultWorldName(sender);
         sender.sendMessage(ChatColor.GOLD + "=== Structure Protection Test ===");
         sender.sendMessage(ChatColor.GRAY + "Chunk: " + ChatColor.WHITE + "(" + chunkX + ", " + chunkZ + ")");
         sender.sendMessage(ChatColor.GRAY + "World: " + ChatColor.WHITE + worldName);
         sender.sendMessage(ChatColor.GRAY + "Structure Type: " + ChatColor.WHITE + structureType);
         sender.sendMessage("");
         sender.sendMessage(ChatColor.YELLOW + "结构落点保护由 ASM 注入实现 (setLargeFeatureWithSalt XOR)。");
         sender.sendMessage(ChatColor.GRAY + "新生成区块的结构落点会偏离真种子，使 SeedcrackerX 的");
         sender.sendMessage(ChatColor.GRAY + "结构种子 LIFTING 反推出错误的世界种子。");
         sender.sendMessage(ChatColor.GRAY + "开关: " + ChatColor.WHITE + "config.yml -> secure-seed.structure-placement-xor-enabled");
      } catch (NumberFormatException parseEx) {
         sender.sendMessage(ChatColor.RED + "Invalid coordinates. Please use integers.");
      } catch (Exception ex) {
         sender.sendMessage(ChatColor.RED + "Error testing structure protection: " + ex.getMessage());
      }
   }
}
