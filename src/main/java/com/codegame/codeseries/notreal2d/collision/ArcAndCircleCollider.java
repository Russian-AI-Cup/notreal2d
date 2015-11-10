package com.codegame.codeseries.notreal2d.collision;

import com.codeforces.commons.geometry.Line2D;
import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.form.ArcForm;
import com.codegame.codeseries.notreal2d.form.CircularForm;
import com.codegame.codeseries.notreal2d.form.Shape;
import com.codegame.codeseries.notreal2d.util.GeometryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.codeforces.commons.math.Math.*;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 26.06.2015
 */
public class ArcAndCircleCollider extends ColliderBase {
    public ArcAndCircleCollider(double epsilon) {
        super(epsilon);
    }

    @Override
    protected boolean matchesOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        return bodyA.getForm().getShape() == Shape.ARC && bodyB.getForm().getShape() == Shape.CIRCLE;
    }

    @SuppressWarnings({"OverlyComplexMethod", "OverlyLongMethod"})
    @Nullable
    @Override
    protected CollisionInfo collideOneWay(@Nonnull Body bodyA, @Nonnull Body bodyB) {
        ArcForm arcFormA = (ArcForm) bodyA.getForm();
        CircularForm circularFormB = (CircularForm) bodyB.getForm();

        double radiusA = arcFormA.getRadius();
        double radiusB = circularFormB.getRadius();
        double distance = bodyA.getPosition().getDistanceTo(bodyB.getPosition());

        if (distance > radiusA + radiusB) {
            return null;
        }

        if (distance < abs(radiusA - radiusB)) {
            return null;
        }

        bodyA.normalizeAngle();
        bodyB.normalizeAngle();

        double startAngleA = bodyA.getAngle() + arcFormA.getAngle();
        double finishAngleA = startAngleA + arcFormA.getSector();

        CollisionInfo endpointCollisionInfo = collideWithEndpoints(
                bodyA, bodyB, radiusA, radiusB, distance, startAngleA, finishAngleA
        );

        if (endpointCollisionInfo != null) {
            return endpointCollisionInfo;
        }

        if (distance >= epsilon) {
            double d = sqrt(
                    (distance + radiusA + radiusB) * (distance + radiusA - radiusB)
                            * (distance - radiusA + radiusB) * (-distance + radiusA + radiusB)
            ) / 4.0D;

            double squaredDistance = bodyA.getPosition().getSquaredDistanceTo(bodyB.getPosition());
            double coordinateNumeratorFactor = sqr(radiusA) - sqr(radiusB);

            double xBase = (bodyA.getX() + bodyB.getX()) / 2.0D
                    + (bodyB.getX() - bodyA.getX()) * coordinateNumeratorFactor / (2.0D * squaredDistance);
            double yBase = (bodyA.getY() + bodyB.getY()) / 2.0D
                    + (bodyB.getY() - bodyA.getY()) * coordinateNumeratorFactor / (2.0D * squaredDistance);

            double xOffset = 2.0D * (bodyA.getY() - bodyB.getY()) * d / squaredDistance;
            double yOffset = 2.0D * (bodyA.getX() - bodyB.getX()) * d / squaredDistance;

            Point2D collisionPoint = new Point2D(xBase, yBase);

            if (abs(xOffset) < epsilon && abs(yOffset) < epsilon) {
                double intersectionPointAngleA = new Vector2D(bodyA.getPosition(), collisionPoint).getAngle();
                if (intersectionPointAngleA < startAngleA) {
                    intersectionPointAngleA += DOUBLE_PI;
                }

                if (intersectionPointAngleA >= startAngleA && intersectionPointAngleA <= finishAngleA) {
                    return new CollisionInfo(
                            bodyA, bodyB, collisionPoint,
                            new Vector2D(bodyB.getPosition(), collisionPoint).normalize(),
                            radiusB - bodyB.getDistanceTo(collisionPoint), epsilon
                    );
                }
            } else {
                Point2D intersectionPoint1 = collisionPoint.copy().add(xOffset, -yOffset);
                Point2D intersectionPoint2 = collisionPoint.copy().add(-xOffset, yOffset);

                double intersectionPoint1AngleA = new Vector2D(bodyA.getPosition(), intersectionPoint1).getAngle();
                if (intersectionPoint1AngleA < startAngleA) {
                    intersectionPoint1AngleA += DOUBLE_PI;
                }

                double intersectionPoint2AngleA = new Vector2D(bodyA.getPosition(), intersectionPoint2).getAngle();
                if (intersectionPoint2AngleA < startAngleA) {
                    intersectionPoint2AngleA += DOUBLE_PI;
                }

                if (intersectionPoint1AngleA >= startAngleA && intersectionPoint1AngleA <= finishAngleA
                        && intersectionPoint2AngleA >= startAngleA && intersectionPoint2AngleA <= finishAngleA) {
                    if (distance > radiusA - epsilon) {
                        return new CollisionInfo(
                                bodyA, bodyB, collisionPoint,
                                new Vector2D(bodyB.getPosition(), bodyA.getPosition()).normalize(),
                                radiusA + radiusB - distance, epsilon
                        );
                    } else {
                        return new CollisionInfo(
                                bodyA, bodyB, collisionPoint,
                                new Vector2D(bodyA.getPosition(), bodyB.getPosition()).normalize(),
                                distance + radiusB - radiusA, epsilon
                        );
                    }
                }
            }

            return null;
        } else {
            return collideSameCenter(bodyB, bodyA, arcFormA, radiusA, startAngleA, finishAngleA, radiusB);
        }
    }

    @Nullable
    private CollisionInfo collideWithEndpoints(
            @Nonnull Body bodyA, @Nonnull Body bodyB, double radiusA, double radiusB, double distance,
            double startAngleA, double finishAngleA) {
        Point2D point1A = bodyA.getPosition().copy().add(new Vector2D(radiusA, 0.0D).setAngle(startAngleA));
        Point2D point2A = bodyA.getPosition().copy().add(new Vector2D(radiusA, 0.0D).setAngle(finishAngleA));

        double distanceToPoint1A = bodyB.getDistanceTo(point1A);
        double distanceToPoint2A = bodyB.getDistanceTo(point2A);

        if (distanceToPoint1A <= radiusB && distanceToPoint2A <= radiusB) {
            Point2D collisionPoint = new Point2D(
                    (point1A.getX() + point2A.getX()) / 2.0D, (point1A.getY() + point2A.getY()) / 2.0D
            );

            Vector2D collisionNormalB;
            Line2D normalLineB;

            if (bodyB.getDistanceTo(collisionPoint) >= epsilon) {
                collisionNormalB = new Vector2D(bodyB.getPosition(), collisionPoint).normalize();
                normalLineB = Line2D.getLineByTwoPoints(bodyB.getPosition(), collisionPoint);
            } else {
                collisionNormalB = new Vector2D(bodyB.getPosition(), bodyA.getPosition()).normalize();
                normalLineB = Line2D.getLineByTwoPoints(bodyB.getPosition(), bodyA.getPosition());
            }

            Point2D projectionOfPoint1A = normalLineB.getProjectionOf(point1A, epsilon);
            double distanceFromPoint1A = normalLineB.getDistanceFrom(point1A);
            double depth1 = sqrt(sqr(radiusB) - sqr(distanceFromPoint1A)) - bodyB.getDistanceTo(projectionOfPoint1A);

            Point2D projectionOfPoint2A = normalLineB.getProjectionOf(point2A, epsilon);
            double distanceFromPoint2A = normalLineB.getDistanceFrom(point2A);
            double depth2 = sqrt(sqr(radiusB) - sqr(distanceFromPoint2A)) - bodyB.getDistanceTo(projectionOfPoint2A);

            return new CollisionInfo(bodyA, bodyB, collisionPoint, collisionNormalB, max(depth1, depth2), epsilon);
        }

        if (distanceToPoint1A <= radiusB) {
            if (distanceToPoint1A >= epsilon) {
                return new CollisionInfo(
                        bodyA, bodyB, point1A, new Vector2D(bodyB.getPosition(), point1A).normalize(),
                        radiusB - distanceToPoint1A, epsilon
                );
            } else {
                return new CollisionInfo(
                        bodyA, bodyB, point1A, new Vector2D(bodyB.getPosition(), bodyA.getPosition()).normalize(),
                        radiusA + radiusB - distance, epsilon
                );
            }
        }

        if (distanceToPoint2A <= radiusB) {
            if (distanceToPoint2A >= epsilon) {
                return new CollisionInfo(
                        bodyA, bodyB, point2A, new Vector2D(bodyB.getPosition(), point2A).normalize(),
                        radiusB - distanceToPoint2A, epsilon
                );
            } else {
                return new CollisionInfo(
                        bodyA, bodyB, point2A, new Vector2D(bodyB.getPosition(), bodyA.getPosition()).normalize(),
                        radiusA + radiusB - distance, epsilon
                );
            }
        }

        return null;
    }

    @Nullable
    private CollisionInfo collideSameCenter(
            @Nonnull Body bodyB, @Nonnull Body bodyA, ArcForm arcFormA,
            double radiusA, double startAngleA, double finishAngleA, double radiusB) {
        if (radiusB >= radiusA) {
            Vector2D relativeVelocityB = bodyB.getVelocity().copy().subtract(bodyA.getVelocity());
            Vector2D collisionNormalB;

            if (relativeVelocityB.getLength() >= epsilon
                    && GeometryUtil.isAngleBetween(relativeVelocityB.getAngle(), startAngleA, finishAngleA)) {
                collisionNormalB = relativeVelocityB.normalize();
            } else if (bodyB.getVelocity().getLength() >= epsilon
                    && GeometryUtil.isAngleBetween(bodyB.getVelocity().getAngle(), startAngleA, finishAngleA)) {
                collisionNormalB = bodyB.getVelocity().copy().normalize();
            } else {
                collisionNormalB = new Vector2D(1.0D, 0.0D).setAngle(
                        bodyA.getAngle() + arcFormA.getAngle() + arcFormA.getSector() / 2.0D
                );
            }

            return new CollisionInfo(
                    bodyA, bodyB, bodyB.getPosition().copy(), collisionNormalB, radiusB - radiusA, epsilon
            );
        } else {
            return null;
        }
    }
}
