package sakuyaayane.nekoantixray.replay;

import java.io.Serializable;
import lombok.Generated;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

public class ReplayFrame implements Serializable {
   private static final long serialVersionUID = 1L;
   public long timestamp;
   public String action;
   public double posX;
   public double posY;
   public double posZ;
   public float yaw;
   public float pitch;
   public String worldName;
   public String blockType;
   private static final String UNKNOWN_WORLD_NAME = "unknown";

   public ReplayFrame(long timestamp, String action, Location location, Material material) {
      this.timestamp = timestamp;
      this.action = action;
      this.posX = location.getX();
      this.posY = location.getY();
      this.posZ = location.getZ();
      this.yaw = location.getYaw();
      this.pitch = location.getPitch();
      this.worldName = location.getWorld() != null ? location.getWorld().getName() : "unknown";
      this.blockType = material != null ? material.name() : null;
   }

   public Location getLocation(World world) {
      return new Location(world, this.posX, this.posY, this.posZ, this.yaw, this.pitch);
   }

   public Location getCorrectLocation() {
      World world = Bukkit.getWorld(this.worldName);
      return world == null ? null : new Location(world, this.posX, this.posY, this.posZ, this.yaw, this.pitch);
   }

   @Generated
   public static ReplayFrame.ReplayFrameBuilder builder() {
      return new ReplayFrame.ReplayFrameBuilder();
   }

   @Generated
   public long getTimestamp() {
      return this.timestamp;
   }

   @Generated
   public String getAction() {
      return this.action;
   }

   @Generated
   public double getX() {
      return this.posX;
   }

   @Generated
   public double getY() {
      return this.posY;
   }

   @Generated
   public double getZ() {
      return this.posZ;
   }

   @Generated
   public float getYaw() {
      return this.yaw;
   }

   @Generated
   public float getPitch() {
      return this.pitch;
   }

   @Generated
   public String getWorldName() {
      return this.worldName;
   }

   @Generated
   public String getBlockType() {
      return this.blockType;
   }

   @Generated
   public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
   }

   @Generated
   public void setAction(String action) {
      this.action = action;
   }

   @Generated
   public void setX(double posX) {
      this.posX = posX;
   }

   @Generated
   public void setY(double posY) {
      this.posY = posY;
   }

   @Generated
   public void setZ(double posZ) {
      this.posZ = posZ;
   }

   @Generated
   public void setYaw(float yaw) {
      this.yaw = yaw;
   }

   @Generated
   public void setPitch(float pitch) {
      this.pitch = pitch;
   }

   @Generated
   public void setWorldName(String worldName) {
      this.worldName = worldName;
   }

   @Generated
   public void setBlockType(String blockType) {
      this.blockType = blockType;
   }

   @Generated
   public ReplayFrame() {
   }

   @Generated
   public ReplayFrame(long timestamp, String action, double posX, double posY, double posZ, float yaw, float pitch, String worldName, String blockType) {
      this.timestamp = timestamp;
      this.action = action;
      this.posX = posX;
      this.posY = posY;
      this.posZ = posZ;
      this.yaw = yaw;
      this.pitch = pitch;
      this.worldName = worldName;
      this.blockType = blockType;
   }

   @Generated
   public static class ReplayFrameBuilder {
      @Generated
      private long timestamp;
      @Generated
      private String action;
      @Generated
      private double posX;
      @Generated
      private double posY;
      @Generated
      private double posZ;
      @Generated
      private float yaw;
      @Generated
      private float pitch;
      @Generated
      private String worldName;
      @Generated
      private String blockType;

      @Generated
      ReplayFrameBuilder() {
      }

      @Generated
      public ReplayFrame.ReplayFrameBuilder timestamp(long timestamp) {
         this.timestamp = timestamp;
         return this;
      }

      @Generated
      public ReplayFrame.ReplayFrameBuilder action(String action) {
         this.action = action;
         return this;
      }

      @Generated
      public ReplayFrame.ReplayFrameBuilder withX(double posX) {
         this.posX = posX;
         return this;
      }

      @Generated
      public ReplayFrame.ReplayFrameBuilder withY(double posY) {
         this.posY = posY;
         return this;
      }

      @Generated
      public ReplayFrame.ReplayFrameBuilder withZ(double posZ) {
         this.posZ = posZ;
         return this;
      }

      @Generated
      public ReplayFrame.ReplayFrameBuilder yaw(float yaw) {
         this.yaw = yaw;
         return this;
      }

      @Generated
      public ReplayFrame.ReplayFrameBuilder pitch(float pitch) {
         this.pitch = pitch;
         return this;
      }

      @Generated
      public ReplayFrame.ReplayFrameBuilder worldName(String worldName) {
         this.worldName = worldName;
         return this;
      }

      @Generated
      public ReplayFrame.ReplayFrameBuilder blockType(String blockType) {
         this.blockType = blockType;
         return this;
      }

      @Generated
      public ReplayFrame build() {
         return new ReplayFrame(this.timestamp, this.action, this.posX, this.posY, this.posZ, this.yaw, this.pitch, this.worldName, this.blockType);
      }

      @Generated
      @Override
      public String toString() {
         return "ReplayFrame.ReplayFrameBuilder(timestamp="
            + this.timestamp
            + ", action="
            + this.action
            + ", x="
            + this.posX
            + ", y="
            + this.posY
            + ", z="
            + this.posZ
            + ", yaw="
            + this.yaw
            + ", pitch="
            + this.pitch
            + ", worldName="
            + this.worldName
            + ", blockType="
            + this.blockType
            + ")";
      }
   }
}
