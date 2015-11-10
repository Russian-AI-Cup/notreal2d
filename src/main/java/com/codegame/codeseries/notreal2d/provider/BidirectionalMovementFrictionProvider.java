package com.codegame.codeseries.notreal2d.provider;

import com.codeforces.commons.geometry.Vector2D;
import com.codegame.codeseries.notreal2d.Body;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 04.06.2015
 */
public class BidirectionalMovementFrictionProvider implements MovementFrictionProvider {
    private final double lengthwiseMovementFrictionFactor;
    private final double crosswiseMovementFrictionFactor;

    public BidirectionalMovementFrictionProvider(
            double lengthwiseMovementFrictionFactor, double crosswiseMovementFrictionFactor) {
        if (lengthwiseMovementFrictionFactor < 0.0D) {
            throw new IllegalArgumentException(
                    "Argument 'lengthwiseMovementFrictionFactor' should be zero or positive."
            );
        }

        if (crosswiseMovementFrictionFactor < 0.0D) {
            throw new IllegalArgumentException(
                    "Argument 'crosswiseMovementFrictionFactor' should be zero or positive."
            );
        }

        this.lengthwiseMovementFrictionFactor = lengthwiseMovementFrictionFactor;
        this.crosswiseMovementFrictionFactor = crosswiseMovementFrictionFactor;
    }

    public double getLengthwiseMovementFrictionFactor() {
        return lengthwiseMovementFrictionFactor;
    }

    public double getCrosswiseMovementFrictionFactor() {
        return crosswiseMovementFrictionFactor;
    }

    @Override
    public void applyFriction(Body body, double updateFactor) {
        double velocityLength = body.getVelocity().getLength();
        if (velocityLength <= 0.0D) {
            return;
        }

        double lengthwiseVelocityChange = lengthwiseMovementFrictionFactor * updateFactor;
        double crosswiseVelocityChange = crosswiseMovementFrictionFactor * updateFactor;

        Vector2D lengthwiseUnitVector = new Vector2D(1.0D, 0.0D).rotate(body.getAngle());
        Vector2D crosswiseUnitVector = new Vector2D(0.0D, 1.0D).rotate(body.getAngle());

        double lengthwiseVelocityPart = body.getVelocity().dotProduct(lengthwiseUnitVector);

        if (lengthwiseVelocityPart >= 0.0D) {
            lengthwiseVelocityPart -= lengthwiseVelocityChange;
            if (lengthwiseVelocityPart < 0.0D) {
                lengthwiseVelocityPart = 0.0D;
            }
        } else {
            lengthwiseVelocityPart += lengthwiseVelocityChange;
            if (lengthwiseVelocityPart > 0.0D) {
                lengthwiseVelocityPart = 0.0D;
            }
        }

        double crosswiseVelocityPart = body.getVelocity().dotProduct(crosswiseUnitVector);

        if (crosswiseVelocityPart >= 0.0D) {
            crosswiseVelocityPart -= crosswiseVelocityChange;
            if (crosswiseVelocityPart < 0.0D) {
                crosswiseVelocityPart = 0.0D;
            }
        } else {
            crosswiseVelocityPart += crosswiseVelocityChange;
            if (crosswiseVelocityPart > 0.0D) {
                crosswiseVelocityPart = 0.0D;
            }
        }

        body.setVelocity(
                lengthwiseUnitVector.copy().multiply(lengthwiseVelocityPart)
                        .add(crosswiseUnitVector.copy().multiply(crosswiseVelocityPart))
        );
    }
}
