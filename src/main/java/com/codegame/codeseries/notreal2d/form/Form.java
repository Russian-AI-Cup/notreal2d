package com.codegame.codeseries.notreal2d.form;

import com.codeforces.commons.geometry.Point2D;
import com.codegame.codeseries.notreal2d.Body;
import com.google.common.base.Preconditions;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static com.codeforces.commons.math.Math.abs;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
public abstract class Form {
    @Nonnull
    private final Shape shape;

    protected Form(@Nonnull Shape shape) {
        Preconditions.checkNotNull(shape, "Argument 'shape' is null.");

        this.shape = shape;
    }

    @Nonnull
    public Shape getShape() {
        return shape;
    }

    public abstract double getCircumcircleRadius();

    @Nonnull
    public abstract Point2D getCenterOfMass(@Nonnull Point2D position, double angle);

    @Nonnull
    public final Point2D getCenterOfMass(@Nonnull Body body) {
        return getCenterOfMass(body.getPosition(), body.getAngle());
    }

    public abstract double getAngularMass(double mass);

    @Override
    public abstract String toString();

    @Nonnull
    public static String toString(@Nullable Form form) {
        return form == null ? "Form {null}" : form.toString();
    }

    @Contract(pure = true)
    protected static double normalizeSinCos(double value, double epsilon) {
        return abs(value) < epsilon ? 0.0D
                : abs(1.0D - value) < epsilon ? 1.0D
                : abs(-1.0D - value) < epsilon ? -1.0D
                : value;
    }
}
