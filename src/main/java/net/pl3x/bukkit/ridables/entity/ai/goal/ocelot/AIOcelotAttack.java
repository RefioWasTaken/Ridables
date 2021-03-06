package net.pl3x.bukkit.ridables.entity.ai.goal.ocelot;

import net.minecraft.server.v1_13_R2.PathfinderGoalOcelotAttack;
import net.pl3x.bukkit.ridables.entity.animal.RidableOcelot;

public class AIOcelotAttack extends PathfinderGoalOcelotAttack {
    private final RidableOcelot ocelot;

    public AIOcelotAttack(RidableOcelot ocelot) {
        super(ocelot);
        this.ocelot = ocelot;
    }

    // shouldExecute
    @Override
    public boolean a() {
        return ocelot.getRider() == null && super.a();
    }

    // shouldContinueExecuting
    @Override
    public boolean b() {
        return ocelot.getRider() == null && super.b();
    }
}
