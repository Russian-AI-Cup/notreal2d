package com.codegame.codeseries.notreal2d.listener;

import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.collision.CollisionInfo;

import javax.annotation.Nonnull;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 11.06.2015
 */
public class CollisionListenerAdapter implements CollisionListener {
    @Override
    public boolean beforeStartingCollision(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        return true;
    }

    @Override
    public boolean beforeResolvingCollision(@Nonnull CollisionInfo collisionInfo) {
        return true;
    }

    @Override
    public void afterResolvingCollision(@Nonnull CollisionInfo collisionInfo) {
        // No operations.
    }
}
