package com.codegame.codeseries.notreal2d.collision;

import com.codegame.codeseries.notreal2d.Body;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 08.06.2015
 */
public interface Collider {
    boolean matches(@Nonnull Body bodyA, @Nonnull Body bodyB);

    @Nullable
    CollisionInfo collide(@Nonnull Body bodyA, @Nonnull Body bodyB);
}
