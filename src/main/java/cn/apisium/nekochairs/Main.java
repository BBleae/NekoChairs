package cn.apisium.nekochairs;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Stairs;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.permissions.PermissionDefault;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.java.annotation.permission.Permission;
import org.bukkit.plugin.java.annotation.plugin.ApiVersion;
import org.bukkit.plugin.java.annotation.plugin.Description;
import org.bukkit.plugin.java.annotation.plugin.Plugin;
import org.bukkit.plugin.java.annotation.plugin.Website;
import org.bukkit.plugin.java.annotation.plugin.author.Author;
import org.spigotmc.event.entity.EntityDismountEvent;

import java.util.Collection;
import java.util.HashSet;

import static org.bukkit.event.block.Action.RIGHT_CLICK_BLOCK;

@Plugin(name = "NekoChairs", version = "1.0")
@Description("Make some chairs in Minecraft!")
@Author("Shirasawa")
@Website("https://apisium.cn")
@Permission(name = "nekochairs.use", defaultValue = PermissionDefault.TRUE)
@ApiVersion(ApiVersion.Target.v1_18)
public class Main extends JavaPlugin implements Listener {
    private final String NAME = "$$Chairs$$";
    private final HashSet<Entity> armorStandList = new HashSet<>();

    @Override
    public void onEnable() {
        getServer().getScheduler().runTaskTimer(this, () -> getServer().getWorlds().forEach(w -> w.getEntitiesByClasses(ArmorStand.class).forEach(this::check)), 100, 100);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        armorStandList.forEach(it -> {
            it.getPassengers().forEach(Entity::leaveVehicle);
            it.remove();
        });
        armorStandList.clear();
    }

    @EventHandler
    public void onPlayerQuit(final PlayerQuitEvent e) {
        final Entity t = e.getPlayer().getVehicle();
        if (t instanceof ArmorStand) {
            e.getPlayer().leaveVehicle();
            check(t);
        }
    }

    @EventHandler
    void onPlayerInteract(final PlayerInteractEvent e) {
        final Block b = e.getClickedBlock();
        final Player p = e.getPlayer();
        if (p.getGameMode() == GameMode.SPECTATOR || e.getItem() != null || e.getAction() != RIGHT_CLICK_BLOCK || b == null || !p.hasPermission("nekochairs.use") || b.getType().data != Stairs.class)
            return;
        final Stairs data = (Stairs) b.getBlockData();
        if (data.getHalf() == Bisected.Half.TOP) return;
        final Location l = b.getLocation().clone().add(0.5, -1.18, 0.5);
        final Collection<ArmorStand> entities = l.getNearbyEntitiesByType(ArmorStand.class, 0.5, 0.5, 0.5);
        int i = entities.size();
        if (i > 0) {
            for (ArmorStand it : entities) if (!check(it)) i--;
            if (i > 0) return;
        }
        l.setYaw(switch (data.getFacing()) {
            case SOUTH -> 180;
            case EAST -> 90;
            case WEST -> 270;
            default -> 0;
        });

        final ArmorStand a = (ArmorStand) b.getWorld().spawnEntity(l, EntityType.ARMOR_STAND);
        a.setAI(false);
        a.customName(Component.text(NAME));
        a.setCanMove(false);
        a.setBasePlate(false);
        a.setCanTick(false);
        a.setVisible(false);
        a.setCanPickupItems(false);
        a.addPassenger(p);
        armorStandList.add(a);
    }

    @EventHandler
    void onEntityDismount(final EntityDismountEvent e) {
        final Entity l = e.getDismounted();
        final TextComponent name = (TextComponent) l.customName();
        if (l instanceof ArmorStand && name != null && name.content().equals(NAME)) leaveChair(l, e.getEntity());
    }

    private void leaveChair(final Entity l, final Entity p) {
        armorStandList.remove(l);
        l.remove();
        getServer().getScheduler().runTaskLater(this, () -> {
            final Entity p2 = p == null ? l.getPassengers().get(0) : p;
            if (p2 == null) return;
            p2.teleport(p2.getLocation().add(0.0, 0.5, 0.0));
        }, 1);
    }

    private boolean check(final Entity it) {
        final TextComponent name = (TextComponent) it.customName();
        if (it.getPassengers().size() != 0) {
        final Entity p = it.getPassengers().get(0);
        if (name != null && name.content().equals(NAME)) {
            if (p != null && p.getVehicle() == it && it.getLocation().clone().add(-0.5, 1.18, -0.5).getBlock().getType().data == Stairs.class)
                return true;
            it.remove();
            armorStandList.remove(it);
        } else {
            return false;
        }
        }
        return false;
    }
}
