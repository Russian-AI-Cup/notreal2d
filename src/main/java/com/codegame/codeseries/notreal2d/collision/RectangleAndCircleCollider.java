package com.codegame.codeseries.notreal2d.collision;

import com.codeforces.commons.geometry.Line2D;
import com.codeforces.commons.geometry.Point2D;
import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.form.CircularForm;
import com.codegame.codeseries.notreal2d.form.RectangularForm;
import com.codegame.codeseries.notreal2d.form.Shape;
import com.codegame.codeseries.notreal2d.util.GeometryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 19.06.2015
 */
public class RectangleAndCircleCollider extends ColliderBase {
    public RectangleAndCircleCollider(double epsilon) {
        super(epsilon);
    }

    @Override
    protected boolean matchesOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        return bodyA.getForm().getShape() == Shape.RECTANGLE && bodyB.getForm().getShape() == Shape.CIRCLE;
    }

    @SuppressWarnings("OverlyLongMethod")
    @Nullable
    @Override
    protected CollisionInfo collideOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        RectangularForm rectangularFormA = (RectangularForm) bodyA.getForm();
        CircularForm circularFormB = (CircularForm) bodyB.getForm();

        Point2D[] pointsA = rectangularFormA.getPoints(bodyA.getPosition(), bodyA.getAngle(), epsilon);
        int pointACount = pointsA.length;

        if (!GeometryUtil.isPointOutsideConvexPolygon(bodyB.getPosition(), pointsA, epsilon)) {
            double minDistanceFromB = Double.POSITIVE_INFINITY;
            Line2D nearestLineA = null;

            for (int pointAIndex = 0; pointAIndex < pointACount; ++pointAIndex) {
                Point2D point1A = pointsA[pointAIndex];
                Point2D point2A = pointsA[pointAIndex == pointACount - 1 ? 0 : pointAIndex + 1];

                Line2D lineA = Line2D.getLineByTwoPoints(point1A, point2A);
                double distanceFromB = lineA.getDistanceFrom(bodyB.getPosition());

                if (distanceFromB < minDistanceFromB) {
                    minDistanceFromB = distanceFromB;
                    nearestLineA = lineA;
                }
            }

            if (nearestLineA != null) {
                return new CollisionInfo(
                        bodyA, bodyB, bodyB.getPosition(), nearestLineA.getUnitNormal().negate(),
                        circularFormB.getRadius() - nearestLineA.getSignedDistanceFrom(bodyB.getPosition()), epsilon
                );
            }
        }

        CollisionInfo collisionInfo = null;

        for (int pointAIndex = 0; pointAIndex < pointACount; ++pointAIndex) {
            Point2D point1A = pointsA[pointAIndex];
            Point2D point2A = pointsA[pointAIndex == pointACount - 1 ? 0 : pointAIndex + 1];

            CollisionInfo lineCollisionInfo = LineAndCircleCollider.collideOneWay(
                    bodyA, bodyB, point1A, point2A, circularFormB, epsilon
            );

            if (lineCollisionInfo != null
                    && (collisionInfo == null || lineCollisionInfo.getDepth() > collisionInfo.getDepth())) {
                collisionInfo = lineCollisionInfo;
            }
        }

        return collisionInfo;
    }
}
