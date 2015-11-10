package com.codegame.codeseries.notreal2d.collision;

import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.form.Form;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.07.2015
 */
public abstract class ColliderBase implements Collider {
    @SuppressWarnings("ProtectedField")
    protected final double epsilon;

    protected ColliderBase(double epsilon) {
        this.epsilon = epsilon;
    }

    @Override
    public final boolean matches(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        return matchesOneWay(bodyA, bodyB) || matchesOneWay(bodyB, bodyA);
    }

    @Nullable
    @Override
    public final CollisionInfo collide(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        if (matchesOneWay(bodyA, bodyB)) {
            return collideOneWay(bodyA, bodyB);
        }

        if (matchesOneWay(bodyB, bodyA)) {
            CollisionInfo collisionInfo = collideOneWay(bodyB, bodyA);
            return collisionInfo == null ? null : new CollisionInfo(
                    bodyA, bodyB, collisionInfo.getPoint(), collisionInfo.getNormalB().negate(),
                    collisionInfo.getDepth(), epsilon
            );
        }

        throw new IllegalArgumentException(String.format(
                "Unsupported %s of %s or %s of %s.",
                Form.toString(bodyA.getForm()), bodyA, Form.toString(bodyB.getForm()), bodyB
        ));
    }

    protected abstract boolean matchesOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB);

    @Nullable
    protected abstract CollisionInfo collideOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB);
}
