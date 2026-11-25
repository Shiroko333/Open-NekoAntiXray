package sakuyaayane.nekoantixray;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class NaxCommandDispatcher implements CommandExecutor {
   private final NekoAntiXray plugin;
   private final Map<String, CommandExecutor> subCommands = new HashMap<>();

   public NaxCommandDispatcher(NekoAntiXray plugin) {
      this.plugin = plugin;
   }

   public List getSubCommandNames() {
      List<String> subCommandNames = new ArrayList<>(this.subCommands.keySet());
      Collections.sort(subCommandNames);
      return subCommandNames;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length == 0) {
         sender.sendMessage("§c用法: /nax <子命令> [参数] §7(输入 §f/nax help §7查看全部指令)");
         return true;
      } else {
         String subCommandName = args[0].toLowerCase();
         CommandExecutor commandExecutor = this.subCommands.get(subCommandName);
         if (commandExecutor == null) {
            sender.sendMessage("§c未知的子命令: " + subCommandName + " §7(输入 §f/nax help §7查看全部指令)");
            return true;
         } else {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return commandExecutor.onCommand(sender, command, label, subArgs);
         }
      }
   }

   public void registerSubCommand(String name, CommandExecutor commandExecutor) {
      this.subCommands.put(name.toLowerCase(), commandExecutor);
   }
}
