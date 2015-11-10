package com.codegame.codeseries.notreal2d.listener;

import com.codeforces.commons.geometry.Point2D;

import javax.annotation.Nonnull;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 26.08.2015
 */
public interface PositionListener {
    /**
     * Physics engine iterates over all registered position listeners in some order and invokes this method before
     * changing position. If any listener returns {@code false}, it cancels all remaining method calls and the change
     * itself.
     * <p>
     * Any {@code oldPosition} changes in the method will be ignored.
     * Any {@code newPosition} changes in the method will be saved and used to update associated object after last
     * iteration (all listeners should return {@code true}).
     *
     * @param oldPosition current position
     * @param newPosition next position
     * @return {@code true} iff physics engine should continue to change position
     */
    boolean beforeChangePosition(@Nonnull Point2D oldPosition, @Nonnull Point2D newPosition);

    /**
     * Physics engine iterates over all registered position listeners in some order and invokes this method after
     * changing position.
     * <p>
     * Any {@code oldPosition} changes in the method will be ignored.
     * Any {@code newPosition} changes in the method will be ignored.
     *
     * @param oldPosition previous position
     * @param newPosition current position
     */
    void afterChangePosition(@Nonnull Point2D oldPosition, @Nonnull Point2D newPosition);
}
