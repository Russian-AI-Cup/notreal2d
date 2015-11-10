package com.codegame.codeseries.notreal2d.collision;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.form.CircularForm;
import com.codegame.codeseries.notreal2d.form.Shape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 08.06.2015
 */
public class CircleAndCircleCollider extends ColliderBase {
    public CircleAndCircleCollider(double epsilon) {
        super(epsilon);
    }

    @Override
    protected boolean matchesOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        return bodyA.getForm().getShape() == Shape.CIRCLE && bodyB.getForm().getShape() == Shape.CIRCLE;
    }

    @Nullable
    @Override
    protected CollisionInfo collideOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        CircularForm circularFormA = (CircularForm) bodyA.getForm();
        CircularForm circularFormB = (CircularForm) bodyB.getForm();

        double radiusA = circularFormA.getRadius();
        double radiusB = circularFormB.getRadius();
        double distance = bodyA.getPosition().getDistanceTo(bodyB.getPosition());

        if (distance > radiusA + radiusB) {
            return null;
        }

        Vector2D collisionNormalB;
        Point2D collisionPoint;

        if (distance >= epsilon) {
            Vector2D vectorBA = new Vector2D(bodyB.getPosition(), bodyA.getPosition());
            collisionNormalB = vectorBA.copy().normalize();
            collisionPoint = bodyB.getPosition().copy().add(vectorBA.copy().multiply(radiusB / (radiusA + radiusB)));
        } else {
            Vector2D relativeVelocityB = bodyB.getVelocity().copy().subtract(bodyA.getVelocity());

            if (relativeVelocityB.getLength() >= epsilon) {
                collisionNormalB = relativeVelocityB.normalize();
            } else if (bodyB.getVelocity().getLength() >= epsilon) {
                collisionNormalB = bodyB.getVelocity().copy().normalize();
            } else {
                collisionNormalB = new Vector2D(1.0D, 0.0D);
            }

            collisionPoint = bodyB.getPosition().copy();
        }

        return new CollisionInfo(bodyA, bodyB, collisionPoint, collisionNormalB, radiusA + radiusB - distance, epsilon);
    }
}
