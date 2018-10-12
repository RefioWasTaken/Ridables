package net.pl3x.bukkit.ridables.entity;

import net.minecraft.server.v1_13_R2.AxisAlignedBB;
import net.minecraft.server.v1_13_R2.ControllerMove;
import net.minecraft.server.v1_13_R2.Entity;
import net.minecraft.server.v1_13_R2.EntityGhast;
import net.minecraft.server.v1_13_R2.EntityHuman;
import net.minecraft.server.v1_13_R2.EntityPlayer;
import net.minecraft.server.v1_13_R2.EnumHand;
import net.minecraft.server.v1_13_R2.MathHelper;
import net.minecraft.server.v1_13_R2.SoundEffects;
import net.minecraft.server.v1_13_R2.World;
import net.pl3x.bukkit.ridables.Ridables;
import net.pl3x.bukkit.ridables.configuration.Config;
import net.pl3x.bukkit.ridables.configuration.Lang;
import net.pl3x.bukkit.ridables.entity.ai.AIFindNearestPlayer;
import net.pl3x.bukkit.ridables.entity.ai.ghast.AIGhastFireballAttack;
import net.pl3x.bukkit.ridables.entity.ai.ghast.AIGhastLookAround;
import net.pl3x.bukkit.ridables.entity.ai.ghast.AIGhastRandomFly;
import net.pl3x.bukkit.ridables.entity.controller.ControllerWASDFlying;
import net.pl3x.bukkit.ridables.entity.controller.LookController;
import net.pl3x.bukkit.ridables.entity.projectile.CustomFireball;
import org.bukkit.craftbukkit.v1_13_R2.entity.CraftPlayer;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class RidableGhast extends EntityGhast implements RidableEntity {
    private int spacebarCooldown = 0;

    public RidableGhast(World world) {
        super(world);
        moveController = new GhastWASDController(this);
        lookController = new LookController(this);
        initAI();
    }

    public RidableType getType() {
        return RidableType.GHAST;
    }

    // initAI - override vanilla AI
    protected void n() {
    }

    private void initAI() {
        goalSelector.a(5, new AIGhastRandomFly(this));
        goalSelector.a(7, new AIGhastLookAround(this));
        goalSelector.a(7, new AIGhastFireballAttack(this));
        targetSelector.a(1, new AIFindNearestPlayer(this));
    }

    // canBeRiddenInWater
    public boolean aY() {
        return Config.GHAST_RIDABLE_IN_WATER;
    }

    protected void mobTick() {
        if (spacebarCooldown > 0) {
            spacebarCooldown--;
        }
        super.mobTick();
    }

    public float getSpeed() {
        return Config.GHAST_SPEED;
    }

    // processInteract
    public boolean a(EntityHuman entityhuman, EnumHand enumhand) {
        if (passengers.isEmpty() && !entityhuman.isPassenger() && !entityhuman.isSneaking()) {
            return enumhand == EnumHand.MAIN_HAND && tryRide(entityhuman, entityhuman.b(enumhand));
        }
        return passengers.isEmpty() && super.a(entityhuman, enumhand);
    }

    // removePassenger
    public boolean removePassenger(Entity passenger) {
        return dismountPassenger(passenger.getBukkitEntity()) && super.removePassenger(passenger);
    }

    public boolean onSpacebar() {
        if (spacebarCooldown == 0) {
            EntityPlayer rider = getRider();
            if (rider != null) {
                return shoot(rider);
            }
        }
        return false;
    }

    public boolean shoot(EntityPlayer rider) {
        spacebarCooldown = Config.GHAST_SHOOT_COOLDOWN;

        if (rider == null) {
            return false;
        }

        CraftPlayer player = (CraftPlayer) ((Entity) rider).getBukkitEntity();
        if (!hasShootPerm(player)) {
            Lang.send(player, Lang.SHOOT_NO_PERMISSION);
            return false;
        }

        Vector direction = player.getEyeLocation().getDirection()
                .normalize().multiply(25).add(new Vector(0, 2.5, 0)).normalize().multiply(25);

        a(SoundEffects.ENTITY_GHAST_WARN, 1.0F, 1.0F);

        new BukkitRunnable() {
            @Override
            public void run() {
                CustomFireball fireball = new CustomFireball(world, RidableGhast.this, rider,
                        direction.getX(), direction.getY(), direction.getZ(),
                        Config.GHAST_SHOOT_SPEED, Config.GHAST_SHOOT_DAMAGE, Config.GHAST_SHOOT_GRIEF);
                world.addEntity(fireball);

                a(SoundEffects.ENTITY_GHAST_SHOOT, 1.0F, 1.0F);
            }
        }.runTaskLater(Ridables.getInstance(), 10);

        return true;
    }

    static class GhastWASDController extends ControllerWASDFlying {
        private RidableGhast ghast;
        private int courseChangeCooldown;

        public GhastWASDController(RidableGhast ghast) {
            super(ghast);
            this.ghast = ghast;
        }

        public void tick() {
            if (h == ControllerMove.Operation.MOVE_TO) {
                double x = b - ghast.locX;
                double y = c - ghast.locY;
                double z = d - ghast.locZ;
                double distance = x * x + y * y + z * z;
                if (courseChangeCooldown-- <= 0) {
                    courseChangeCooldown += ghast.getRandom().nextInt(5) + 2;
                    distance = (double) MathHelper.sqrt(distance);
                    if (isNotColliding(b, c, d, distance)) {
                        ghast.motX += x / distance * 0.1D;
                        ghast.motY += y / distance * 0.1D;
                        ghast.motZ += z / distance * 0.1D;
                    } else {
                        h = ControllerMove.Operation.WAIT;
                    }
                }
            }
        }

        private boolean isNotColliding(double x, double y, double z, double distance) {
            double stepX = (x - ghast.locX) / distance;
            double stepY = (y - ghast.locY) / distance;
            double stepZ = (z - ghast.locZ) / distance;
            AxisAlignedBB aabb = ghast.getBoundingBox();
            for (int i = 1; (double) i < distance; ++i) {
                if (!ghast.world.getCubes(ghast, aabb = aabb.d(stepX, stepY, stepZ))) { // offset
                    return false;
                }
            }
            return true;
        }
    }
}
