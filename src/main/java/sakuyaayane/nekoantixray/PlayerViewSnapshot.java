package sakuyaayane.nekoantixray;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import lombok.Generated;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public final class PlayerViewSnapshot {
   private final Reference<World> world;
   private final Vector vector;
   private final Vector direction;

   public PlayerViewSnapshot(World world, Vector vector, Vector direction) {
      this.world = new WeakReference<>(world);
      this.vector = vector;
      this.direction = direction;
   }

   public PlayerViewSnapshot(PlayerViewSnapshot playerViewSnapshot) {
      this.world = playerViewSnapshot.world;
      this.vector = playerViewSnapshot.getVector().clone();
      this.direction = playerViewSnapshot.getDirection().clone();
   }

   public PlayerViewSnapshot(Location location) {
      this(location.getWorld(), location.toVector(), location.getDirection());
   }

   public World getWorld() {
      return this.world.get();
   }

   @Generated
   public Vector getVector() {
      return this.vector;
   }

   @Generated
   public Vector getDirection() {
      return this.direction;
   }
}