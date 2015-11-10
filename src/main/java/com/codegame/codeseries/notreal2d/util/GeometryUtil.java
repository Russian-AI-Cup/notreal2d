package com.codegame.codeseries.notreal2d.util;

import com.codeforces.commons.geometry.Line2D;
import com.codeforces.commons.geometry.Point2D;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

import static com.codeforces.commons.math.Math.DOUBLE_PI;
import static com.codeforces.commons.math.Math.PI;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 29.06.2015
 */
public final class GeometryUtil {
    private GeometryUtil() {
        throw new UnsupportedOperationException();
    }

    @Contract(pure = true)
    public static double normalizeAngle(double angle) {
        while (angle > PI) {
            angle -= DOUBLE_PI;
        }

        while (angle < -PI) {
            angle += DOUBLE_PI;
        }

        return angle;
    }

    @Contract(pure = true)
    public static boolean isAngleBetween(double angle, double startAngle, double finishAngle) {
        while (finishAngle < startAngle) {
            finishAngle += DOUBLE_PI;
        }

        while (finishAngle - DOUBLE_PI > startAngle) {
            finishAngle -= DOUBLE_PI;
        }

        while (angle < startAngle) {
            angle += DOUBLE_PI;
        }

        while (angle - DOUBLE_PI > startAngle) {
            angle -= DOUBLE_PI;
        }

        return angle >= startAngle && angle <= finishAngle;
    }

    public static boolean isPointInsideConvexPolygon(
            @Nonnull Point2D point, @Nonnull Point2D[] polygonVertexes, double epsilon) {
        for (int vertexIndex = 0, vertexCount = polygonVertexes.length; vertexIndex < vertexCount; ++vertexIndex) {
            Point2D vertex1 = polygonVertexes[vertexIndex];
            Point2D vertex2 = polygonVertexes[vertexIndex == vertexCount - 1 ? 0 : vertexIndex + 1];

            Line2D polygonEdge = Line2D.getLineByTwoPoints(vertex1, vertex2);

            if (polygonEdge.getSignedDistanceFrom(point) > -epsilon) {
                return false;
            }
        }

        return true;
    }

    public static boolean isPointInsideConvexPolygon(@Nonnull Point2D point, @Nonnull Point2D[] polygonVertexes) {
        return isPointInsideConvexPolygon(point, polygonVertexes, 0.0D);
    }

    public static boolean isPointOutsideConvexPolygon(
            @Nonnull Point2D point, @Nonnull Point2D[] polygonVertexes, double epsilon) {
        for (int vertexIndex = 0, vertexCount = polygonVertexes.length; vertexIndex < vertexCount; ++vertexIndex) {
            Point2D vertex1 = polygonVertexes[vertexIndex];
            Point2D vertex2 = polygonVertexes[vertexIndex == vertexCount - 1 ? 0 : vertexIndex + 1];

            Line2D polygonEdge = Line2D.getLineByTwoPoints(vertex1, vertex2);

            if (polygonEdge.getSignedDistanceFrom(point) >= epsilon) {
                return true;
            }
        }

        return false;
    }

    public static boolean isPointOutsideConvexPolygon(@Nonnull Point2D point, @Nonnull Point2D[] polygonVertexes) {
        return isPointOutsideConvexPolygon(point, polygonVertexes, 0.0D);
    }
}
