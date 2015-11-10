package com.codegame.codeseries.notreal2d.collision;

import com.codeforces.commons.geometry.Line2D;
import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.form.Form;
import com.codegame.codeseries.notreal2d.form.RectangularForm;
import com.codegame.codeseries.notreal2d.form.Shape;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 19.06.2015
 */
public class RectangleAndRectangleCollider extends ColliderBase {
    public RectangleAndRectangleCollider(double epsilon) {
        super(epsilon);
    }

    @Override
    protected boolean matchesOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        return bodyA.getForm().getShape() == Shape.RECTANGLE && bodyB.getForm().getShape() == Shape.RECTANGLE;
    }

    @Nullable
    @Override
    protected CollisionInfo collideOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        RectangularForm rectangularFormA = (RectangularForm) bodyA.getForm();
        RectangularForm rectangularFormB = (RectangularForm) bodyB.getForm();

        Point2D[] pointsA = rectangularFormA.getPoints(bodyA.getPosition(), bodyA.getAngle(), epsilon);
        Point2D[] pointsB = rectangularFormB.getPoints(bodyB.getPosition(), bodyB.getAngle(), epsilon);

        CollisionInfo collisionInfoA = collideOneWay(bodyA, bodyB, pointsA, pointsB);
        if (collisionInfoA == null) {
            return null;
        }

        CollisionInfo collisionInfoB = collideOneWay(bodyB, bodyA, pointsB, pointsA);
        if (collisionInfoB == null) {
            return null;
        }

        if (collisionInfoB.getDepth() < collisionInfoA.getDepth()) {
            return new CollisionInfo(
                    bodyA, bodyB, collisionInfoB.getPoint(), collisionInfoB.getNormalB().negate(),
                    collisionInfoB.getDepth(), epsilon
            );
        } else {
            return collisionInfoA;
        }
    }

    @SuppressWarnings("OverlyLongMethod")
    @Nullable
    private CollisionInfo collideOneWay(
            @Nonnull Body bodyA, @Nonnull Body bodyB, @Nonnull Point2D[] pointsA, @Nonnull Point2D[] pointsB) {
        int pointACount = pointsA.length;
        int pointBCount = pointsB.length;

        double minDepth = Double.POSITIVE_INFINITY;
        Point2D bestIntersectionPoint = null;
        Vector2D bestCollisionNormalB = null;

        for (int pointAIndex = 0; pointAIndex < pointACount; ++pointAIndex) {
            Point2D point1A = pointsA[pointAIndex];
            Point2D point2A = pointsA[pointAIndex == pointACount - 1 ? 0 : pointAIndex + 1];

            Line2D lineA = Line2D.getLineByTwoPoints(point1A, point2A);

            if (lineA.getSignedDistanceFrom(bodyA.getPosition()) > -epsilon) {
                throw new IllegalStateException(String.format("%s of %s is too small, " +
                                "does not represent a convex polygon, or its points are going in wrong order.",
                        Form.toString(bodyA.getForm()), bodyA
                ));
            }

            double minDistanceFromB = Double.POSITIVE_INFINITY;
            Point2D intersectionPoint = null;
            Vector2D collisionNormalB = null;

            for (int pointBIndex = 0; pointBIndex < pointBCount; ++pointBIndex) {
                Point2D pointB = pointsB[pointBIndex];
                double distanceFromPointB = lineA.getSignedDistanceFrom(pointB);

                if (distanceFromPointB < minDistanceFromB) {
                    minDistanceFromB = distanceFromPointB;
                    intersectionPoint = pointB;
                    collisionNormalB = lineA.getUnitNormalFrom(bodyA.getPosition(), epsilon).negate();
                }
            }

            if (minDistanceFromB > 0.0D) {
                return null;
            }

            double depth = -minDistanceFromB;
            if (depth < minDepth) {
                minDepth = depth;
                bestIntersectionPoint = intersectionPoint;
                bestCollisionNormalB = collisionNormalB;
            }
        }

        if (bestIntersectionPoint == null || bestCollisionNormalB == null) {
            return null;
        }

        return new CollisionInfo(bodyA, bodyB, bestIntersectionPoint, bestCollisionNormalB, minDepth, epsilon);
    }
}
