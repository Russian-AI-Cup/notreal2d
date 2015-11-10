package com.codegame.codeseries.notreal2d.collision;

import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.form.Shape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 26.06.2015
 */
public class ArcAndArcCollider extends ColliderBase {
    public ArcAndArcCollider(double epsilon) {
        super(epsilon);
    }

    @Override
    protected boolean matchesOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        return bodyA.getForm().getShape() == Shape.ARC && bodyB.getForm().getShape() == Shape.ARC;
    }

    @Nullable
    @Override
    protected CollisionInfo collideOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        return null; // TODO
    }
}
