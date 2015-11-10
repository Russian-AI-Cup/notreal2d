package com.codegame.codeseries.notreal2d.listener;

import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.collision.CollisionInfo;

import javax.annotation.Nonnull;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 11.06.2015
 */
public interface CollisionListener {
    /**
     * Physics engine iterates over all registered collision listeners in some order and invokes this method before
     * gathering any collision information. Is is not guaranteed at this stage that bodies are really intersect. If any
     * listener returns {@code false}, it cancels all remaining method calls and the collision itself.
     *
     * @param bodyA first body to collide
     * @param bodyB second body to collide
     * @return {@code true} iff physics engine should continue to collide bodies
     */
    boolean beforeStartingCollision(@Nonnull Body bodyA, @Nonnull Body bodyB);

    /**
     * Physics engine iterates over all registered collision listeners in some order and invokes this method before
     * resolving collision. If any listener returns {@code false}, it cancels all remaining method calls and the
     * collision itself.
     *
     * @param collisionInfo collision information
     * @return {@code true} iff physics engine should continue to resolve collision
     */
    boolean beforeResolvingCollision(@Nonnull CollisionInfo collisionInfo);

    /**
     * Physics engine iterates over all registered collision listeners in some order and invokes this method after
     * resolving collision.
     *
     * @param collisionInfo collision information
     */
    void afterResolvingCollision(@Nonnull CollisionInfo collisionInfo);
}
