package com.codegame.codeseries.notreal2d.form;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.text.StringUtil;

import javax.annotation.Nonnull;

import static com.codeforces.commons.math.Math.*;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
public class RectangularForm extends Form {
    private final double width;
    private final double height;
    private final double halfWidth;
    private final double halfHeight;
    private final double circumcircleRadius;
    private final double angularMassFactor;

    public RectangularForm(double width, double height) {
        super(Shape.RECTANGLE);

        if (Double.isNaN(width) || Double.isInfinite(width) || width <= 0.0D) {
            throw new IllegalArgumentException(String.format(
                    "Argument 'width' should be positive finite number but got %s.", width
            ));
        }

        if (Double.isNaN(height) || Double.isInfinite(height) || height <= 0.0D) {
            throw new IllegalArgumentException(String.format(
                    "Argument 'height' should be positive finite number but got %s.", height
            ));
        }

        this.width = width;
        this.height = height;
        this.halfWidth = width / 2.0D;
        this.halfHeight = height / 2.0D;
        this.circumcircleRadius = hypot(width, height) / 2.0D;
        this.angularMassFactor = sumSqr(width, height) / 12.0D;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    @Nonnull
    public Point2D[] getPoints(@Nonnull Point2D position, double angle, double epsilon) {
        if (Double.isNaN(angle) || Double.isInfinite(angle)) {
            throw new IllegalArgumentException("Argument 'angle' is not a finite number.");
        }

        if (Double.isNaN(epsilon) || Double.isInfinite(epsilon) || epsilon < 1.0E-100D || epsilon > 1.0D) {
            throw new IllegalArgumentException("Argument 'epsilon' should be between 1.0E-100 and 1.0.");
        }

        double sin = normalizeSinCos(sin(angle), epsilon);
        double cos = normalizeSinCos(cos(angle), epsilon);

        double lengthwiseXOffset = cos * halfWidth;
        double lengthwiseYOffset = sin * halfWidth;

        double crosswiseXOffset = sin * halfHeight;
        double crosswiseYOffset = -cos * halfHeight;

        return new Point2D[]{
                new Point2D(
                        position.getX() - lengthwiseXOffset + crosswiseXOffset,
                        position.getY() - lengthwiseYOffset + crosswiseYOffset
                ),
                new Point2D(
                        position.getX() + lengthwiseXOffset + crosswiseXOffset,
                        position.getY() + lengthwiseYOffset + crosswiseYOffset
                ),
                new Point2D(
                        position.getX() + lengthwiseXOffset - crosswiseXOffset,
                        position.getY() + lengthwiseYOffset - crosswiseYOffset
                ),
                new Point2D(
                        position.getX() - lengthwiseXOffset - crosswiseXOffset,
                        position.getY() - lengthwiseYOffset - crosswiseYOffset
                )
        };
    }

    @Override
    public double getCircumcircleRadius() {
        return circumcircleRadius;
    }

    @Nonnull
    @Override
    public Point2D getCenterOfMass(@Nonnull Point2D position, double angle) {
        return position;
    }

    @Override
    public double getAngularMass(double mass) {
        return mass * angularMassFactor;
    }

    @Override
    public String toString() {
        return StringUtil.toString(this, false, "width", "height");
    }
}
