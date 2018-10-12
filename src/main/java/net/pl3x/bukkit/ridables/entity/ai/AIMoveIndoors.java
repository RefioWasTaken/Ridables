package net.pl3x.bukkit.ridables.entity.ai;

import net.minecraft.server.v1_13_R2.EntityCreature;
import net.minecraft.server.v1_13_R2.PathfinderGoalMoveIndoors;
import net.pl3x.bukkit.ridables.entity.RidableEntity;

public class AIMoveIndoors extends PathfinderGoalMoveIndoors {
    private final RidableEntity ridable;

    public AIMoveIndoors(RidableEntity ridable) {
        super((EntityCreature) ridable);
        this.ridable = ridable;
    }

    // shouldExecute
    public boolean a() {
        return ridable.getRider() == null && super.a();
    }

    // shouldContinueExecuting
    public boolean b() {
        return ridable.getRider() == null && super.b();
    }
}
