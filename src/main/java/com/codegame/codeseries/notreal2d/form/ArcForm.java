package com.codegame.codeseries.notreal2d.form;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.text.StringUtil;
import com.codegame.codeseries.notreal2d.util.GeometryUtil;

import javax.annotation.Nonnull;

import static com.codeforces.commons.math.Math.DOUBLE_PI;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 26.06.2015
 */
public class ArcForm extends ThinForm {
    private final double radius;
    private final double angle;
    private final double sector;

    public ArcForm(double radius, double angle, double sector, boolean endpointCollisionEnabled) {
        super(Shape.ARC, endpointCollisionEnabled);

        if (Double.isNaN(radius) || Double.isInfinite(radius) || radius <= 0.0D) {
            throw new IllegalArgumentException(String.format(
                    "Argument 'radius' should be a positive finite number but got %s.", radius
            ));
        }

        if (Double.isNaN(angle) || Double.isInfinite(angle)) {
            throw new IllegalArgumentException(String.format(
                    "Argument 'angle' should be a finite number but got %s.", angle
            ));
        }

        if (Double.isNaN(sector) || Double.isInfinite(sector) || sector <= 0.0D || sector > DOUBLE_PI) {
            throw new IllegalArgumentException(String.format(
                    "Argument 'sector' should be between 0.0 exclusive and 2 * PI inclusive but got %s.", sector
            ));
        }

        this.angle = GeometryUtil.normalizeAngle(angle);
        this.radius = radius;
        this.sector = sector;
    }

    public ArcForm(double radius, double angle, double sector) {
        this(radius, angle, sector, true);
    }

    public double getRadius() {
        return radius;
    }

    public double getAngle() {
        return angle;
    }

    public double getSector() {
        return sector;
    }

    @Override
    public double getCircumcircleRadius() {
        return radius;
    }

    @Nonnull
    @Override
    public Point2D getCenterOfMass(@Nonnull Point2D position, double angle) {
        return position; // TODO just a method stub, does not really return a center of mass
    }

    @Override
    public double getAngularMass(double mass) {
        if (Double.isInfinite(mass) && mass != Double.NEGATIVE_INFINITY) {
            return mass;
        }

        throw new IllegalArgumentException("Arc form is only supported for static bodies.");
    }

    @Override
    public String toString() {
        return StringUtil.toString(this, false, "radius", "angle", "sector");
    }
}
