package com.codegame.codeseries.notreal2d.listener;

import com.codeforces.commons.geometry.Point2D;

import javax.annotation.Nonnull;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 26.08.2015
 */
public class PositionListenerAdapter implements PositionListener {
    @Override
    public boolean beforeChangePosition(@Nonnull Point2D oldPosition, @Nonnull Point2D newPosition) {
        return true;
    }

    @Override
    public void afterChangePosition(@Nonnull Point2D oldPosition, @Nonnull Point2D newPosition) {
        // No operations.
    }
}
