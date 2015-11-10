package com.codegame.codeseries.notreal2d.provider;

import com.codegame.codeseries.notreal2d.Body;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 04.06.2015
 */
public class ConstantMovementFrictionProvider implements MovementFrictionProvider {
    private final double movementFrictionFactor;

    public ConstantMovementFrictionProvider(double movementFrictionFactor) {
        if (movementFrictionFactor < 0.0D) {
            throw new IllegalArgumentException("Argument 'movementFrictionFactor' should be zero or positive.");
        }

        this.movementFrictionFactor = movementFrictionFactor;
    }

    public double getMovementFrictionFactor() {
        return movementFrictionFactor;
    }

    @Override
    public void applyFriction(Body body, double updateFactor) {
        if (movementFrictionFactor <= 0.0D) {
            return;
        }

        double velocityLength = body.getVelocity().getLength();
        if (velocityLength <= 0.0D) {
            return;
        }

        double velocityChange = movementFrictionFactor * updateFactor;

        if (velocityChange >= velocityLength) {
            body.setVelocity(0.0D, 0.0D);
        } else if (velocityChange > 0.0D) {
            body.getVelocity().multiply(1.0D - velocityChange / velocityLength);
        }
    }
}
