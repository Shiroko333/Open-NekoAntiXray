package sakuyaayane.nekoantixray;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class TrainingRecordCommand implements CommandExecutor {
   private final TrainingDataRecorder trainingDataRecorder;

   public TrainingRecordCommand(TrainingDataRecorder trainingDataRecorder) {
      this.trainingDataRecorder = trainingDataRecorder;
   }

   @Override
   public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
      if (args.length < 2) {
         sender.sendMessage("§c用法: /nax record <player> <start|stop> [label]");
         return true;
      }

      Player player = Bukkit.getPlayer(args[0]);
      if (player == null) {
         sender.sendMessage("§c玩家 " + args[0] + " 不在线。");
         return true;
      }

      String action = args[1].toLowerCase();
      switch (action) {
         case "start":
            if (this.trainingDataRecorder.isRecordingPlayer(player)) {
               sender.sendMessage("§e已经在记录玩家 " + player.getName() + " 的数据。");
               return true;
            }

            String trainingLabel = "cheater";
            if (args.length > 2) {
               trainingLabel = args[2].equalsIgnoreCase("legit") ? "legit" : "cheater";
            }

            this.trainingDataRecorder.startRecording(player, trainingLabel);
            sender.sendMessage("§a已开始记录玩家 " + player.getName() + " 的数据，标签为: " + trainingLabel);
            break;
         case "stop":
            if (!this.trainingDataRecorder.isRecordingPlayer(player)) {
               sender.sendMessage("§e当前未记录玩家 " + player.getName() + " 的数据。");
            } else {
               this.trainingDataRecorder.stopRecording(player);
               sender.sendMessage("§a已停止记录玩家 " + player.getName() + " 的数据。");
            }
            break;
         default:
            sender.sendMessage("§c无效操作: " + args[1] + "。请使用 'start' 或 'stop'。");
      }

      return true;
   }
}
