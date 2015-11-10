package com.codegame.codeseries.notreal2d.collision;

import com.codeforces.commons.geometry.Line2D;
import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.form.CircularForm;
import com.codegame.codeseries.notreal2d.form.LinearForm;
import com.codegame.codeseries.notreal2d.form.Shape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.codeforces.commons.math.Math.max;
import static com.codeforces.commons.math.Math.min;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 08.06.2015
 */
public class LineAndCircleCollider extends ColliderBase {
    public LineAndCircleCollider(double epsilon) {
        super(epsilon);
    }

    @Override
    protected boolean matchesOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        return bodyA.getForm().getShape() == Shape.LINE && bodyB.getForm().getShape() == Shape.CIRCLE;
    }

    @Nullable
    @Override
    protected CollisionInfo collideOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        LinearForm linearFormA = (LinearForm) bodyA.getForm();
        CircularForm circularFormB = (CircularForm) bodyB.getForm();

        Point2D point1A = linearFormA.getPoint1(bodyA.getPosition(), bodyA.getAngle(), epsilon);
        Point2D point2A = linearFormA.getPoint2(bodyA.getPosition(), bodyA.getAngle(), epsilon);

        return collideOneWay(bodyA, bodyB, point1A, point2A, circularFormB, epsilon);
    }

    @SuppressWarnings("OverlyLongMethod")
    @Nullable
    static CollisionInfo collideOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB,
                                       @Nonnull Point2D point1A, @Nonnull Point2D point2A,
                                       @Nonnull CircularForm circularFormB, double epsilon) {
        Line2D lineA = Line2D.getLineByTwoPoints(point1A, point2A);

        double distanceFromB = lineA.getDistanceFrom(bodyB.getPosition());
        double radiusB = circularFormB.getRadius();

        if (distanceFromB > radiusB) {
            return null;
        }

        double leftA = min(point1A.getX(), point2A.getX());
        double topA = min(point1A.getY(), point2A.getY());
        double rightA = max(point1A.getX(), point2A.getX());
        double bottomA = max(point1A.getY(), point2A.getY());

        Point2D projectionOfB = lineA.getProjectionOf(bodyB.getPosition());

        boolean projectionOfBBelongsToA = (projectionOfB.getX() > leftA - epsilon)
                && (projectionOfB.getX() < rightA + epsilon)
                && (projectionOfB.getY() > topA - epsilon)
                && (projectionOfB.getY() < bottomA + epsilon);

        if (projectionOfBBelongsToA) {
            Vector2D collisionNormalB;

            if (distanceFromB >= epsilon) {
                collisionNormalB = new Vector2D(bodyB.getPosition(), projectionOfB).normalize();
            } else {
                Vector2D unitNormalA = lineA.getUnitNormal();
                Vector2D relativeVelocityB = bodyB.getVelocity().copy().subtract(bodyA.getVelocity());

                if (relativeVelocityB.getLength() >= epsilon) {
                    collisionNormalB = relativeVelocityB.dotProduct(unitNormalA) >= epsilon
                            ? unitNormalA : unitNormalA.negate();
                } else if (bodyB.getVelocity().getLength() >= epsilon) {
                    collisionNormalB = bodyB.getVelocity().dotProduct(unitNormalA) >= epsilon
                            ? unitNormalA : unitNormalA.negate();
                } else {
                    collisionNormalB = unitNormalA;
                }
            }

            return new CollisionInfo(bodyA, bodyB, projectionOfB, collisionNormalB, radiusB - distanceFromB, epsilon);
        }

        double distanceToPoint1A = bodyB.getDistanceTo(point1A);
        double distanceToPoint2A = bodyB.getDistanceTo(point2A);

        Point2D nearestPointA;
        double distanceToNearestPointA;

        if (distanceToPoint1A < distanceToPoint2A) {
            nearestPointA = point1A;
            distanceToNearestPointA = distanceToPoint1A;
        } else {
            nearestPointA = point2A;
            distanceToNearestPointA = distanceToPoint2A;
        }

        if (distanceToNearestPointA > radiusB) {
            return null;
        }

        return new CollisionInfo(
                bodyA, bodyB, nearestPointA, new Vector2D(bodyB.getPosition(), nearestPointA).normalize(),
                radiusB - distanceToNearestPointA, epsilon
        );
    }
}
