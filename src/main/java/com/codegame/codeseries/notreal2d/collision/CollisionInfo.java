package com.codegame.codeseries.notreal2d.collision;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codeforces.commons.text.StringUtil;
import com.codegame.codeseries.notreal2d.Body;
import org.apache.log4j.Logger;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 08.06.2015
 */
public class CollisionInfo {
    private static final Logger logger = Logger.getLogger(CollisionInfo.class);

    private final Body bodyA;
    private final Body bodyB;
    private final Point2D point;
    private final Vector2D normalB;
    private final double depth;

    public CollisionInfo(Body bodyA, Body bodyB, Point2D point, Vector2D normalB, double depth, double epsilon) {
        this.bodyA = bodyA;
        this.bodyB = bodyB;
        this.point = point;
        this.normalB = normalB;

        if (depth < 0.0D && depth > -epsilon) {
            this.depth = 0.0D;
        } else {
            this.depth = depth;
        }

        if (Double.isNaN(this.depth) || Double.isInfinite(this.depth) || this.depth < 0.0D) {
            logger.error(String.format(
                    "Argument 'depth' should be non-negative number but got %s (%s and %s).", this.depth, bodyA, bodyB
            ));
        }
    }

    public Body getBodyA() {
        return bodyA;
    }

    public Body getBodyB() {
        return bodyB;
    }

    public Point2D getPoint() {
        return point.copy();
    }

    public Vector2D getNormalB() {
        return normalB.copy();
    }

    public double getDepth() {
        return depth;
    }

    @Override
    public String toString() {
        return StringUtil.toString(this, false);
    }
}
