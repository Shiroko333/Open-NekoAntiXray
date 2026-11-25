package sakuyaayane.nekoantixray;

import java.io.FileInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.GZIPInputStream;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RealSeedCommand implements CommandExecutor {
   private static final long BYTE_MASK = 255L;

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (!sender.hasPermission("nekoantixray.realseed")) {
         sender.sendMessage(ChatColor.RED + "没有权限: nekoantixray.realseed");
         return true;
      } else {
         World world = this.resolveTargetWorld(sender, args);
         if (world == null) {
            sender.sendMessage(ChatColor.RED + "找不到世界");
            return true;
         } else {
            Long originalSeed = this.resolveOriginalSeed(world);
            if (originalSeed == null) {
               sender.sendMessage(ChatColor.RED + "无法获取世界的原始种子");
               return true;
            } else {
               long worldSeedXor = NekoAntiXray.getWorldSeedXor();
               int structureSaltXor = NekoAntiXray.getStructureSaltXor();
               long effectiveSeed = originalSeed ^ worldSeedXor;
               sender.sendMessage(ChatColor.GOLD + "============== " + ChatColor.YELLOW + "真实世界种子信息" + ChatColor.GOLD + " ==============");
               String worldName = world.getName();
               sender.sendMessage(ChatColor.GRAY + "世界: " + ChatColor.WHITE + worldName);
               sender.sendMessage("");
               sender.sendMessage(ChatColor.YELLOW + "实际生成种子 (Effective Seed): " + ChatColor.GREEN + effectiveSeed);
               sender.sendMessage(ChatColor.DARK_GRAY + "└─ 用于史莱姆区块计算、种子查找器等");
               sender.sendMessage("");
               sender.sendMessage(ChatColor.GRAY + "原始种子 (Original): " + ChatColor.WHITE + originalSeed);
               sender.sendMessage(ChatColor.GRAY + "世界XOR值: " + ChatColor.WHITE + worldSeedXor);
               sender.sendMessage(ChatColor.GRAY + "结构XOR值: " + ChatColor.WHITE + structureSaltXor);
               sender.sendMessage("");
               sender.sendMessage(ChatColor.DARK_GRAY + "提示: 使用 " + ChatColor.GREEN + effectiveSeed + ChatColor.DARK_GRAY + " 来:");
               sender.sendMessage(ChatColor.DARK_GRAY + "  • 在种子查找器中查找结构");
               sender.sendMessage(ChatColor.DARK_GRAY + "  • 确定史莱姆区块位置");
               sender.sendMessage(ChatColor.DARK_GRAY + "  • 预测世界生成特征");
               sender.sendMessage(ChatColor.GOLD + "===============================================");
               return true;
            }
         }
      }
   }

   private Long resolveOriginalSeed(World world) {
      Long levelDatSeed = this.readSeedFromLevelDat(world);
      if (levelDatSeed != null) {
         return levelDatSeed;
      }

      try {
         return world.getSeed();
      } catch (Throwable ignored) {
         return this.readSeedViaNmsReflection(world);
      }
   }

   private World resolveTargetWorld(CommandSender sender, String[] args) {
      if (args.length > 0) {
         World world = Bukkit.getWorld(args[0]);
         if (world != null) {
            return world;
         }
      }

      if (sender instanceof Player) {
         return ((Player)sender).getWorld();
      } else {
         return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
      }
   }

   private Long readSeedFromLevelDat(World world) {
      try {
         Path worldFolder = world.getWorldFolder().toPath();
         Path levelDatPath = worldFolder.resolve("level.dat");
         if (!Files.exists(levelDatPath)) {
            return null;
         }

         GZIPInputStream gzipInputStream = new GZIPInputStream(new FileInputStream(levelDatPath.toFile()));

         byte[] nbtBytes;
         try {
            nbtBytes = gzipInputStream.readAllBytes();
         } catch (Throwable readEx) {
            try {
               gzipInputStream.close();
            } catch (Throwable closeEx) {
               readEx.addSuppressed(closeEx);
            }

            throw readEx;
         }

         gzipInputStream.close();
         String latin1Content = new String(nbtBytes, StandardCharsets.ISO_8859_1);
         String[] worldGenTags = new String[]{"WorldGenSettings", "seed"};
         int tagIndex = this.findEarliestTagIndex(latin1Content, worldGenTags);
         if (tagIndex >= 0) {
            Long worldGenSeed = this.readBigEndianLongAfterTag(nbtBytes, latin1Content, tagIndex, "seed");
            if (worldGenSeed != null) {
               return worldGenSeed;
            }
         }

         tagIndex = latin1Content.indexOf("RandomSeed");
         if (tagIndex >= 0) {
            Long randomSeed = this.readBigEndianLongAfterTag(nbtBytes, latin1Content, tagIndex, "RandomSeed");
            if (randomSeed != null) {
               return randomSeed;
            }
         }

         tagIndex = latin1Content.indexOf("WorldSeed");
         if (tagIndex >= 0) {
            Long worldSeed = this.readBigEndianLongAfterTag(nbtBytes, latin1Content, tagIndex, "WorldSeed");
            if (worldSeed != null) {
               return worldSeed;
            }
         }
      } catch (Throwable ignored) {
      }

      return null;
   }

   private int findEarliestTagIndex(String content, String[] tags) {
      int earliestIndex = -1;

      for (String tag : tags) {
         int foundIndex = content.indexOf(tag);
         if (foundIndex >= 0 && (earliestIndex < 0 || foundIndex < earliestIndex)) {
            earliestIndex = foundIndex;
         }
      }

      return earliestIndex;
   }

   private Long readBigEndianLongAfterTag(byte[] nbtBytes, String latin1Content, int startIndex, String tagName) {
      int tagIndex = latin1Content.indexOf(tagName, startIndex);
      if (tagIndex < 0) {
         return null;
      }

      int dataStart = tagIndex + tagName.length();
      int searchLimit = Math.min(latin1Content.length(), dataStart + 512);
      if (dataStart + 8 > searchLimit) {
         return null;
      }

      long seedValue = 0L;

      for (int i = 0; i < 8; i++) {
         seedValue = seedValue << 8 | nbtBytes[dataStart + i] & 255L;
      }

      return seedValue;
   }

   private Long readSeedViaNmsReflection(World world) {
      try {
         Class<?> targetClass = world.getClass();
         Class<?>[] emptyParams = new Class[0];
         Object nmsWorld = targetClass.getMethod("getHandle", emptyParams).invoke(world);
         Object levelData = null;

         for (String methodName : new String[]{"getLevelData", "q_", "r_", "levelData"}) {
            try {
               targetClass = nmsWorld.getClass();
               emptyParams = new Class[0];
               levelData = targetClass.getMethod(methodName, emptyParams).invoke(nmsWorld);
               break;
            } catch (Throwable methodEx) {
            }
         }

         if (levelData == null) {
            for (String fieldName : new String[]{"levelData", "Q", "R"}) {
               try {
                  targetClass = nmsWorld.getClass();
                  Field field = targetClass.getDeclaredField(fieldName);
                  field.setAccessible(true);
                  levelData = field.get(nmsWorld);
                  break;
               } catch (Throwable fieldEx) {
               }
            }
         }

         if (levelData == null) {
            return null;
         }

         for (String seedMethodName : new String[]{"getSeed", "getLevelSeed", "m", "n"}) {
            try {
               targetClass = levelData.getClass();
               emptyParams = new Class[0];
               Object seedResult = targetClass.getMethod(seedMethodName, emptyParams).invoke(levelData);
               if (seedResult instanceof Long) {
                  return (Long)seedResult;
               }
            } catch (Throwable seedEx) {
            }
         }

         for (String optionsMethodName : new String[]{"worldGenOptions", "getWorldGenOptions", "getWorldOptions", "options"}) {
            try {
               targetClass = levelData.getClass();
               emptyParams = new Class[0];
               Method optionsMethod = targetClass.getMethod(optionsMethodName, emptyParams);
               Object worldGenOptions = optionsMethod.invoke(levelData);
               if (worldGenOptions != null) {
                  for (String optionSeedMethodName : new String[]{"seed", "getSeed", "f", "g"}) {
                     try {
                        targetClass = worldGenOptions.getClass();
                        emptyParams = new Class[0];
                        Object optionSeedResult = targetClass.getMethod(optionSeedMethodName, emptyParams).invoke(worldGenOptions);
                        if (optionSeedResult instanceof Long) {
                           return (Long)optionSeedResult;
                        }
                     } catch (Throwable optionSeedEx) {
                     }
                  }
               }
            } catch (Throwable optionsEx) {
            }
         }
      } catch (Throwable reflectionEx) {
      }

      return null;
   }
}
