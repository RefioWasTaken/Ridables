package net.pl3x.bukkit.ridables.entity;

import net.minecraft.server.v1_13_R1.AxisAlignedBB;
import net.minecraft.server.v1_13_R1.ControllerLook;
import net.minecraft.server.v1_13_R1.ControllerMove;
import net.minecraft.server.v1_13_R1.Entity;
import net.minecraft.server.v1_13_R1.EntityHuman;
import net.minecraft.server.v1_13_R1.EntityLiving;
import net.minecraft.server.v1_13_R1.EntityPlayer;
import net.minecraft.server.v1_13_R1.EntityShulker;
import net.minecraft.server.v1_13_R1.EntityShulkerBullet;
import net.minecraft.server.v1_13_R1.EnumDifficulty;
import net.minecraft.server.v1_13_R1.EnumDirection;
import net.minecraft.server.v1_13_R1.EnumHand;
import net.minecraft.server.v1_13_R1.GenericAttributes;
import net.minecraft.server.v1_13_R1.IMonster;
import net.minecraft.server.v1_13_R1.PathfinderGoal;
import net.minecraft.server.v1_13_R1.PathfinderGoalHurtByTarget;
import net.minecraft.server.v1_13_R1.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_13_R1.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_13_R1.PathfinderGoalRandomLookaround;
import net.minecraft.server.v1_13_R1.SoundEffects;
import net.minecraft.server.v1_13_R1.World;
import net.pl3x.bukkit.ridables.configuration.Config;
import net.pl3x.bukkit.ridables.configuration.Lang;
import net.pl3x.bukkit.ridables.entity.controller.BlankLookController;
import net.pl3x.bukkit.ridables.entity.controller.ControllerWASD;
import net.pl3x.bukkit.ridables.entity.projectile.EntityCustomShulkerBullet;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_13_R1.entity.CraftPlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

public class EntityRidableShulker extends EntityShulker implements RidableEntity {
    private ControllerMove aiController;
    private ControllerWASD wasdController;
    private ControllerLook defaultLookController;
    private BlankLookController blankLookController;
    private EntityPlayer rider;
    private boolean isOpen = true;
    private int shootCooldown = 0;
    private int spacebarCooldown = 0;

    public EntityRidableShulker(World world) {
        super(world);
        aiController = moveController;
        wasdController = new ControllerWASD(this);
        defaultLookController = lookController;
        blankLookController = new BlankLookController(this);
    }

    public RidableType getType() {
        return RidableType.SHULKER;
    }

    public boolean isActionableItem(ItemStack itemstack) {
        return false;
    }

    public boolean aY() {
        return true; // dont eject passengers when in water
    }

    protected void mobTick() {
        if (spacebarCooldown > 0) {
            spacebarCooldown--;
        }
        if (shootCooldown > 0) {
            shootCooldown--;
        }
        EntityPlayer rider = updateRider();
        if (rider != null) {
            setGoalTarget(null, null, false);
            setRotation(rider.yaw, rider.pitch);
            useWASDController();
            updatePeek();
        }
        super.mobTick();
    }

    public void setRotation(float newYaw, float newPitch) {
        setYawPitch(lastYaw = yaw = newYaw, pitch = newPitch * 0.5F);
        aS = aQ = yaw;
    }

    public float getJumpPower() {
        return 0;
    }

    public float getSpeed() {
        return (float) getAttributeInstance(GenericAttributes.MOVEMENT_SPEED).getValue();
    }

    public EntityPlayer getRider() {
        return rider;
    }

    public EntityPlayer updateRider() {
        if (passengers == null || passengers.isEmpty()) {
            rider = null;
        } else {
            Entity entity = passengers.get(0);
            rider = entity instanceof EntityPlayer ? (EntityPlayer) entity : null;
        }
        return rider;
    }

    public void useAIController() {
        if (moveController != aiController) {
            moveController = aiController;
            lookController = defaultLookController;
        }
    }

    public void useWASDController() {
        if (moveController != wasdController) {
            moveController = wasdController;
            lookController = blankLookController;
        }
    }

    public boolean onSpacebar() {
        if (spacebarCooldown == 0) {
            spacebarCooldown = 20;
            setOpen(!isOpen());
            return true;
        }
        return false;
    }

    public boolean onClick(org.bukkit.entity.Entity entity, EnumHand hand) {
        handleClick();
        return true;
    }

    public boolean onClick(Block block, BlockFace blockFace, EnumHand hand) {
        handleClick();
        return true;
    }

    public boolean onClick(EnumHand hand) {
        handleClick();
        return true;
    }

    private void handleClick() {
        shoot();
    }

    private void updatePeek() {
        byte peekTick = (byte) (isOpen ? Config.SHULKER_PEEK_HEIGHT : 0);
        if (dB() != peekTick) {
            a(peekTick);
        }
    }

    public void setOpen(boolean open) {
        this.isOpen = open;
    }

    public boolean isOpen() {
        return isOpen;
    }

    public boolean shoot() {
        if (shootCooldown > 0 || !isOpen()) {
            return false;
        }

        shootCooldown = Config.SHULKER_SHOOT_COOLDOWN;

        if (rider == null) {
            return false;
        }

        CraftPlayer player = rider.getBukkitEntity();
        if (!player.hasPermission("allow.shoot.shulker")) {
            Lang.send(player, Lang.SHOOT_NO_PERMISSION);
            return false;
        }

        Vector target = player.getEyeLocation().getDirection().normalize().multiply(25);

        EntityCustomShulkerBullet bullet = new EntityCustomShulkerBullet(world, this, rider, null, dz().k());
        bullet.shoot(target.getX(), target.getY(), target.getZ(), Config.SHULKER_SHOOT_SPEED, 5.0F);
        world.addEntity(bullet);

        a(SoundEffects.ENTITY_SHULKER_SHOOT, 2.0F, (random.nextFloat() - random.nextFloat()) * 0.2F + 1.0F);
        return true;
    }

    protected boolean l() {
        return getRider() != null || super.l();
    }

    protected void n() {
        goalSelector.a(1, new PathfinderGoalLookAtPlayer(this, EntityHuman.class, 8.0F));
        goalSelector.a(4, new AIAttack(this));
        goalSelector.a(7, new AIPeek(this));
        goalSelector.a(8, new PathfinderGoalRandomLookaround(this));
        targetSelector.a(1, new PathfinderGoalHurtByTarget(this, true));
        targetSelector.a(2, new AIAttackNearest(this));
        targetSelector.a(3, new AIDefenseAttack(this));
    }

    static class AIDefenseAttack extends PathfinderGoalNearestAttackableTarget<EntityLiving> {
        AIDefenseAttack(EntityRidableShulker shulker) {
            super(shulker, EntityLiving.class, 10, true, false, (e) -> e instanceof IMonster);
        }

        public boolean a() {
            return ((EntityRidableShulker) e).getRider() == null && e.be() != null && super.a();
        }

        protected AxisAlignedBB a(double d0) {
            EnumDirection dir = ((EntityShulker) e).dz();
            return dir.k() == EnumDirection.EnumAxis.X ? e.getBoundingBox().grow(4.0D, d0, d0) : (dir.k() == EnumDirection.EnumAxis.Z ? e.getBoundingBox().grow(d0, d0, 4.0D) : e.getBoundingBox().grow(d0, 4.0D, d0));
        }
    }

    class AIAttackNearest extends PathfinderGoalNearestAttackableTarget<EntityHuman> {
        AIAttackNearest(EntityRidableShulker shulker) {
            super(shulker, EntityHuman.class, true);
        }

        public boolean a() {
            return ((EntityRidableShulker) e).getRider() == null && e.world.getDifficulty() != EnumDifficulty.PEACEFUL && super.a();
        }

        protected AxisAlignedBB a(double d0) {
            EnumDirection dir = ((EntityShulker) e).dz();
            return dir.k() == EnumDirection.EnumAxis.X ? e.getBoundingBox().grow(4.0D, d0, d0) : (dir.k() == EnumDirection.EnumAxis.Z ? e.getBoundingBox().grow(d0, d0, 4.0D) : e.getBoundingBox().grow(d0, 4.0D, d0));
        }
    }

    class AIAttack extends PathfinderGoal {
        private EntityRidableShulker shulker;
        private int attackTime;

        AIAttack(EntityRidableShulker shulker) {
            a(3); // setMutexBits
            this.shulker = shulker;
        }

        public boolean a() {
            EntityLiving target = shulker.getGoalTarget();
            return shulker.getRider() == null && (target != null && target.isAlive()) && shulker.world.getDifficulty() != EnumDifficulty.PEACEFUL;
        }

        public void c() {
            if (shulker.getRider() == null) {
                attackTime = 20;
                shulker.a(100);
            }
        }

        public void d() {
            if (shulker.getRider() == null) {
                a(0); // setMutexBits
            }
        }

        public void e() {
            if (shulker.getRider() == null && shulker.world.getDifficulty() != EnumDifficulty.PEACEFUL) {
                --attackTime;
                EntityLiving target = shulker.getGoalTarget();
                shulker.getControllerLook().a(target, 180.0F, 180.0F);
                double distance = shulker.h(target);
                if (distance < 400.0D) {
                    if (attackTime <= 0) {
                        attackTime = 20 + shulker.random.nextInt(10) * 20 / 2;
                        EntityShulkerBullet bullet = new EntityShulkerBullet(shulker.world, shulker, target, shulker.dz().k());
                        shulker.world.addEntity(bullet);
                        shulker.a(SoundEffects.ENTITY_SHULKER_SHOOT, 2.0F, (shulker.random.nextFloat() - shulker.random.nextFloat()) * 0.2F + 1.0F);
                    }
                } else {
                    shulker.setGoalTarget(null);
                }
                super.e();
            }
        }
    }

    class AIPeek extends PathfinderGoal {
        private EntityRidableShulker shulker;
        private int peekTime;

        AIPeek(EntityRidableShulker shulker) {
            this.shulker = shulker;
        }

        public boolean a() {
            return shulker.getRider() == null && shulker.getGoalTarget() == null && shulker.random.nextInt(40) == 0;
        }

        public boolean b() {
            return shulker.getRider() == null && shulker.getGoalTarget() == null && peekTime > 0;
        }

        public void c() {
            if (shulker.getRider() == null) {
                peekTime = 20 * (1 + shulker.random.nextInt(3));
                shulker.a(30);
            }
        }

        public void d() {
            if (shulker.getRider() == null && shulker.getGoalTarget() == null) {
                shulker.a(0);
            }
        }

        public void e() {
            --peekTime;
        }
    }
}