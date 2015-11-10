package com.codegame.codeseries.notreal2d.provider;

import com.codegame.codeseries.notreal2d.Body;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 28.08.2015
 */
public interface MomentumTransferFactorProvider {
    /**
     * Calculates and returns momentum transfer factor used to resolve collision of two bodies. {@code null} result
     * means that physics engine should use a product of
     * {@link Body#getMomentumTransferFactor() momentum transfer factors} of two bodies.
     * <p>
     * A momentum transfer factor should be between 0.0 and 1.0 both inclusive.
     *
     * @param bodyA first body to get factor
     * @param bodyB second body to get factor
     * @return momentum transfer factor of two bodies or {@code null} to use default strategy
     */
    @Nullable
    Double getFactor(@Nonnull Body bodyA, @Nonnull Body bodyB);
}
