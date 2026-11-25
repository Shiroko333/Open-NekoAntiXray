package sakuyaayane.nekoantixray;

import java.util.HashSet;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class BanInventoryCleaner {
   private final Set<Material> oreMaterials;
   private final NekoAntiXray plugin;
   private boolean enabled;

   public BanInventoryCleaner(NekoAntiXray plugin) {
      this.plugin = plugin;
      this.enabled = plugin.getConfigManager().isBanInventoryCleanEnabled();
      this.oreMaterials = this.buildOreMaterialSet();
   }

   public boolean isEnabled() {
      return this.enabled;
   }

   public void reloadConfig() {
      this.enabled = this.plugin.getConfigManager().isBanInventoryCleanEnabled();
      this.plugin.logDebug("BanInventoryCleaner 配置已重新加载, enabled=" + this.enabled);
   }

   private Set buildOreMaterialSet() {
      Set<Material> materials = new HashSet<>();
      this.addMaterialIfExists(materials, "IRON_ORE");
      this.addMaterialIfExists(materials, "DEEPSLATE_IRON_ORE");
      this.addMaterialIfExists(materials, "GOLD_ORE");
      this.addMaterialIfExists(materials, "DEEPSLATE_GOLD_ORE");
      this.addMaterialIfExists(materials, "DIAMOND_ORE");
      this.addMaterialIfExists(materials, "DEEPSLATE_DIAMOND_ORE");
      this.addMaterialIfExists(materials, "EMERALD_ORE");
      this.addMaterialIfExists(materials, "DEEPSLATE_EMERALD_ORE");
      this.addMaterialIfExists(materials, "LAPIS_ORE");
      this.addMaterialIfExists(materials, "DEEPSLATE_LAPIS_ORE");
      this.addMaterialIfExists(materials, "REDSTONE_ORE");
      this.addMaterialIfExists(materials, "DEEPSLATE_REDSTONE_ORE");
      this.addMaterialIfExists(materials, "COPPER_ORE");
      this.addMaterialIfExists(materials, "DEEPSLATE_COPPER_ORE");
      this.addMaterialIfExists(materials, "COAL_ORE");
      this.addMaterialIfExists(materials, "DEEPSLATE_COAL_ORE");
      this.addMaterialIfExists(materials, "NETHER_GOLD_ORE");
      this.addMaterialIfExists(materials, "NETHER_QUARTZ_ORE");
      this.addMaterialIfExists(materials, "ANCIENT_DEBRIS");
      this.addMaterialIfExists(materials, "IRON_INGOT");
      this.addMaterialIfExists(materials, "RAW_IRON");
      this.addMaterialIfExists(materials, "GOLD_INGOT");
      this.addMaterialIfExists(materials, "RAW_GOLD");
      this.addMaterialIfExists(materials, "DIAMOND");
      this.addMaterialIfExists(materials, "EMERALD");
      this.addMaterialIfExists(materials, "LAPIS_LAZULI");
      this.addMaterialIfExists(materials, "REDSTONE");
      this.addMaterialIfExists(materials, "COPPER_INGOT");
      this.addMaterialIfExists(materials, "RAW_COPPER");
      this.addMaterialIfExists(materials, "COAL");
      this.addMaterialIfExists(materials, "QUARTZ");
      this.addMaterialIfExists(materials, "NETHERITE_SCRAP");
      this.addMaterialIfExists(materials, "NETHERITE_INGOT");
      this.addMaterialIfExists(materials, "IRON_BLOCK");
      this.addMaterialIfExists(materials, "GOLD_BLOCK");
      this.addMaterialIfExists(materials, "DIAMOND_BLOCK");
      this.addMaterialIfExists(materials, "EMERALD_BLOCK");
      this.addMaterialIfExists(materials, "LAPIS_BLOCK");
      this.addMaterialIfExists(materials, "REDSTONE_BLOCK");
      this.addMaterialIfExists(materials, "COPPER_BLOCK");
      this.addMaterialIfExists(materials, "COAL_BLOCK");
      this.addMaterialIfExists(materials, "QUARTZ_BLOCK");
      this.addMaterialIfExists(materials, "NETHERITE_BLOCK");
      return materials;
   }

   private void addMaterialIfExists(Set materials, String materialName) {
      try {
         Material material = Material.valueOf(materialName);
         materials.add(material);
      } catch (IllegalArgumentException ex) {
         this.plugin.logDebug("材质 " + materialName + " 在当前服务器版本中不存在，已跳过");
      }
   }

   public void cleanPlayerInventory(Player player) {
      if (this.enabled) {
         PlayerInventory inventory = player.getInventory();
         int removedCount = 0;

         for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && this.oreMaterials.contains(item.getType())) {
               removedCount += item.getAmount();
               inventory.setItem(i, null);
            }
         }

         for (ItemStack armorItem : inventory.getArmorContents()) {
            if (armorItem != null && this.oreMaterials.contains(armorItem.getType())) {
               removedCount += armorItem.getAmount();
               inventory.remove(armorItem);
            }
         }

         ItemStack itemInOffHand = inventory.getItemInOffHand();
         if (itemInOffHand != null && this.oreMaterials.contains(itemInOffHand.getType())) {
            removedCount += itemInOffHand.getAmount();
            inventory.setItemInOffHand(null);
         }

         if (removedCount > 0) {
            this.plugin.logDebug("已从玩家 " + player.getName() + " 的背包中清除 " + removedCount + " 个矿物物品");
         }
      }
   }
}
