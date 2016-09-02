package com.codegame.codeseries.notreal2d;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codeforces.commons.text.StringUtil;
import com.codegame.codeseries.notreal2d.form.Form;
import com.codegame.codeseries.notreal2d.provider.ConstantMovementFrictionProvider;
import com.codegame.codeseries.notreal2d.provider.MovementFrictionProvider;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.codeforces.commons.math.Math.pow;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
public class Body {
    private static final AtomicLong idGenerator = new AtomicLong();

    /**
     * Unique ID.
     */
    private final long id = idGenerator.incrementAndGet();

    /**
     * The name of this body.
     */
    private String name;

    /**
     * The form (shape and size) of this body.
     */
    private Form form;

    /**
     * The mass of this body.
     */
    private double mass;

    /**
     * The inverted mass of this body. Used to speed up some calculations.
     */
    private double invertedMass;

    /**
     * The relative loss of the speed per time unit. Should be in range [0, 1].
     */
    private double movementAirFrictionFactor;

    /**
     * The relative loss of the angular speed per time unit. Should be in range [0, 1].
     */
    private double rotationAirFrictionFactor;

    /**
     * The provider of the absolute loss of the speed.
     */
    private MovementFrictionProvider movementFrictionProvider = new ConstantMovementFrictionProvider(0.0D);

    /**
     * The absolute loss of the angular speed per time unit.
     */
    private double rotationFrictionFactor;

    /**
     * The momentum transfer factor of this body. Should be in range [0, 1].
     * <p/>
     * If two bodies collide, the resulting momentum transfer can be calculated as the product of their momentum
     * transfer factors. This is a default behaviour.
     * <p/>
     * However it can be {@link World#momentumTransferFactorProvider overridden}.
     */
    private double momentumTransferFactor = 1.0D;

    /**
     * The surface friction factor of this body. Should be in range [0, 1].
     * <p/>
     * If two bodies collide, the resulting surface friction is proportional to the square root of the product of their
     * surface friction factors.
     */
    private double surfaceFrictionFactor;

    private final DynamicState currentState = new DynamicState();
    private DynamicState beforeStepState;
    private DynamicState beforeIterationState;

    private double lastMovementAirFrictionFactor;
    private double lastMovementUpdateFactor;
    private Double lastMovementTransferFactor;

    private double lastRotationAirFrictionFactor;
    private double lastRotationUpdateFactor;
    private Double lastRotationTransferFactor;

    private Map<String, Object> attributeByName;

    private final int hashCode = Long.hashCode(id);

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Form getForm() {
        return form;
    }

    public void setForm(Form form) {
        this.form = form;
    }

    public double getMass() {
        return mass;
    }

    public void setMass(double mass) {
        if (Double.isNaN(mass) || mass == Double.NEGATIVE_INFINITY || mass <= 0.0D) {
            throw new IllegalArgumentException(this + ": argument 'mass' should be positive.");
        }

        this.mass = mass;

        if (Double.isInfinite(mass)) {
            this.invertedMass = 0.0D;
        } else {
            this.invertedMass = 1.0D / mass;
        }
    }

    @Contract(pure = true)
    public boolean isStatic() {
        return Double.isInfinite(mass);
    }

    public double getInvertedMass() {
        return invertedMass;
    }

    public double getAngularMass() {
        if (Double.isNaN(mass) || mass == Double.NEGATIVE_INFINITY || mass <= 0.0D) {
            throw new IllegalStateException(this + ": field 'mass' should be positive.");
        }

        if (Double.isInfinite(mass)) {
            return Double.POSITIVE_INFINITY;
        } else {
            return form.getAngularMass(mass);
        }
    }

    public double getInvertedAngularMass() {
        double angularMass = getAngularMass();

        if (Double.isInfinite(angularMass)) {
            return 0.0D;
        } else {
            return 1.0D / angularMass;
        }
    }

    public double getMovementAirFrictionFactor() {
        return movementAirFrictionFactor;
    }

    public void setMovementAirFrictionFactor(double movementAirFrictionFactor) {
        if (Double.isNaN(movementAirFrictionFactor) || Double.isInfinite(movementAirFrictionFactor)
                || movementAirFrictionFactor < 0.0D || movementAirFrictionFactor > 1.0D) {
            throw new IllegalArgumentException(String.format(
                    "%s: argument 'movementAirFrictionFactor' should be between 0.0 and 1.0 both inclusive but got %s.",
                    this, movementAirFrictionFactor
            ));
        }

        this.movementAirFrictionFactor = movementAirFrictionFactor;
    }

    public double getRotationAirFrictionFactor() {
        return rotationAirFrictionFactor;
    }

    public void setRotationAirFrictionFactor(double rotationAirFrictionFactor) {
        if (Double.isNaN(rotationAirFrictionFactor) || Double.isInfinite(rotationAirFrictionFactor)
                || rotationAirFrictionFactor < 0.0D || rotationAirFrictionFactor > 1.0D) {
            throw new IllegalArgumentException(String.format(
                    "%s: argument 'rotationAirFrictionFactor' should be between 0.0 and 1.0 both inclusive but got %s.",
                    this, rotationAirFrictionFactor
            ));
        }

        this.rotationAirFrictionFactor = rotationAirFrictionFactor;
    }

    public MovementFrictionProvider getMovementFrictionProvider() {
        return movementFrictionProvider;
    }

    public void setMovementFrictionProvider(MovementFrictionProvider movementFrictionProvider) {
        if (movementFrictionProvider == null) {
            throw new IllegalArgumentException(String.format("%s: argument 'movementFrictionProvider' is null.", this));
        }

        this.movementFrictionProvider = movementFrictionProvider;
    }

    public void setMovementFrictionFactor(double movementFrictionFactor) {
        setMovementFrictionProvider(new ConstantMovementFrictionProvider(movementFrictionFactor));
    }

    public void applyFriction(double updateFactor) {
        movementFrictionProvider.applyFriction(this, updateFactor);
    }

    public double getRotationFrictionFactor() {
        return rotationFrictionFactor;
    }

    public void setRotationFrictionFactor(double rotationFrictionFactor) {
        if (Double.isNaN(rotationFrictionFactor) || rotationFrictionFactor < 0.0D) {
            throw new IllegalArgumentException(String.format(
                    "%s: argument 'rotationFrictionFactor' should be zero or positive but got %s.",
                    this, rotationFrictionFactor
            ));
        }

        this.rotationFrictionFactor = rotationFrictionFactor;
    }

    public double getMomentumTransferFactor() {
        return momentumTransferFactor;
    }

    public void setMomentumTransferFactor(double momentumTransferFactor) {
        if (Double.isNaN(momentumTransferFactor) || Double.isInfinite(momentumTransferFactor)
                || momentumTransferFactor < 0.0D || momentumTransferFactor > 1.0D) {
            throw new IllegalArgumentException(String.format(
                    "%s: argument 'momentumTransferFactor' should be between 0.0 and 1.0 both inclusive but got %s.",
                    this, momentumTransferFactor
            ));
        }

        this.momentumTransferFactor = momentumTransferFactor;
    }

    public double getSurfaceFrictionFactor() {
        return surfaceFrictionFactor;
    }

    public void setSurfaceFrictionFactor(double surfaceFrictionFactor) {
        if (Double.isNaN(surfaceFrictionFactor) || Double.isInfinite(surfaceFrictionFactor)
                || surfaceFrictionFactor < 0.0D || surfaceFrictionFactor > 1.0D) {
            throw new IllegalArgumentException(String.format(
                    "%s: argument 'surfaceFrictionFactor' should be between 0.0 and 1.0 both inclusive but got %s.",
                    this, surfaceFrictionFactor
            ));
        }

        this.surfaceFrictionFactor = surfaceFrictionFactor;
    }

    public DynamicState getCurrentState() {
        return currentState;
    }

    public DynamicState getBeforeStepState() {
        return beforeStepState;
    }

    public void saveBeforeStepState() {
        this.beforeStepState = new DynamicState(currentState);
    }

    public DynamicState getBeforeIterationState() {
        return beforeIterationState;
    }

    public void saveBeforeIterationState() {
        this.beforeIterationState = new DynamicState(currentState);
    }

    public Point2D getPosition() {
        return currentState.getPosition();
    }

    public void setPosition(Point2D position) {
        currentState.setPosition(position);
    }

    public void setPosition(double x, double y) {
        Point2D position = currentState.getPosition();
        if (position == null) {
            currentState.setPosition(new Point2D(x, y));
        } else {
            position.setX(x);
            position.setY(y);
        }
    }

    public double getX() {
        Point2D position = currentState.getPosition();
        return position == null ? 0.0D : position.getX();
    }

    public void setX(double x) {
        Point2D position = currentState.getPosition();
        if (position == null) {
            currentState.setPosition(new Point2D(x, 0.0D));
        } else {
            position.setX(x);
        }
    }

    public double getY() {
        Point2D position = currentState.getPosition();
        return position == null ? 0.0D : position.getY();
    }

    public void setY(double y) {
        Point2D position = currentState.getPosition();
        if (position == null) {
            currentState.setPosition(new Point2D(0.0D, y));
        } else {
            position.setY(y);
        }
    }

    public Vector2D getVelocity() {
        return currentState.getVelocity();
    }

    public void setVelocity(Vector2D velocity) {
        currentState.setVelocity(velocity);
    }

    public void setVelocity(double x, double y) {
        Vector2D velocity = currentState.getVelocity();
        if (velocity == null) {
            currentState.setVelocity(new Vector2D(x, y));
        } else {
            velocity.setX(x);
            velocity.setY(y);
        }
    }

    public double getVelocityX() {
        Vector2D velocity = currentState.getVelocity();
        return velocity == null ? 0.0D : velocity.getX();
    }

    public void setVelocityX(double x) {
        Vector2D velocity = currentState.getVelocity();
        if (velocity == null) {
            currentState.setVelocity(new Vector2D(x, 0.0D));
        } else {
            velocity.setX(x);
        }
    }

    public double getVelocityY() {
        Vector2D velocity = currentState.getVelocity();
        return velocity == null ? 0.0D : velocity.getY();
    }

    public void setVelocityY(double y) {
        Vector2D velocity = currentState.getVelocity();
        if (velocity == null) {
            currentState.setVelocity(new Vector2D(0.0D, y));
        } else {
            velocity.setY(y);
        }
    }

    public Vector2D getMedianVelocity() {
        return currentState.getMedianVelocity();
    }

    public void setMedianVelocity(Vector2D medianVelocity) {
        currentState.setMedianVelocity(medianVelocity);
    }

    public void setMedianVelocity(double x, double y) {
        Vector2D medianVelocity = currentState.getMedianVelocity();
        if (medianVelocity == null) {
            currentState.setMedianVelocity(new Vector2D(x, y));
        } else {
            medianVelocity.setX(x);
            medianVelocity.setY(y);
        }
    }

    public double getMedianVelocityX() {
        Vector2D medianVelocity = currentState.getMedianVelocity();
        return medianVelocity == null ? 0.0D : medianVelocity.getX();
    }

    public void setMedianVelocityX(double x) {
        Vector2D medianVelocity = currentState.getMedianVelocity();
        if (medianVelocity == null) {
            currentState.setMedianVelocity(new Vector2D(x, 0.0D));
        } else {
            medianVelocity.setX(x);
        }
    }

    public double getMedianVelocityY() {
        Vector2D medianVelocity = currentState.getMedianVelocity();
        return medianVelocity == null ? 0.0D : medianVelocity.getY();
    }

    public void setMedianVelocityY(double y) {
        Vector2D medianVelocity = currentState.getMedianVelocity();
        if (medianVelocity == null) {
            currentState.setMedianVelocity(new Vector2D(0.0D, y));
        } else {
            medianVelocity.setY(y);
        }
    }

    public Vector2D getForce() {
        return currentState.getForce();
    }

    public void setForce(Vector2D force) {
        currentState.setForce(force);
    }

    public void setForce(double x, double y) {
        Vector2D force = currentState.getForce();
        if (force == null) {
            currentState.setForce(new Vector2D(x, y));
        } else {
            force.setX(x);
            force.setY(y);
        }
    }

    public double getForceX() {
        Vector2D force = currentState.getForce();
        return force == null ? 0.0D : force.getX();
    }

    public void setForceX(double x) {
        Vector2D force = currentState.getForce();
        if (force == null) {
            currentState.setForce(new Vector2D(x, 0.0D));
        } else {
            force.setX(x);
        }
    }

    public double getForceY() {
        Vector2D force = currentState.getForce();
        return force == null ? 0.0D : force.getY();
    }

    public void setForceY(double y) {
        Vector2D force = currentState.getForce();
        if (force == null) {
            currentState.setForce(new Vector2D(0.0D, y));
        } else {
            force.setY(y);
        }
    }

    public double getAngle() {
        return currentState.getAngle();
    }

    public void setAngle(double angle) {
        currentState.setAngle(angle);
    }

    public double getAngularVelocity() {
        return currentState.getAngularVelocity();
    }

    public void setAngularVelocity(double angularVelocity) {
        currentState.setAngularVelocity(angularVelocity);
    }

    public double getMedianAngularVelocity() {
        return currentState.getMedianAngularVelocity();
    }

    public void setMedianAngularVelocity(double medianAngularVelocity) {
        currentState.setMedianAngularVelocity(medianAngularVelocity);
    }

    public double getTorque() {
        return currentState.getTorque();
    }

    public void setTorque(double torque) {
        currentState.setTorque(torque);
    }

    public double getDistanceTo(Body body) {
        return currentState.getPosition().getDistanceTo(body.currentState.getPosition());
    }

    public double getDistanceTo(Point2D point) {
        return currentState.getPosition().getDistanceTo(point);
    }

    public double getDistanceTo(double x, double y) {
        return currentState.getPosition().getDistanceTo(x, y);
    }

    public double getSquaredDistanceTo(Body body) {
        return currentState.getPosition().getSquaredDistanceTo(body.currentState.getPosition());
    }

    public double getSquaredDistanceTo(Point2D point) {
        return currentState.getPosition().getSquaredDistanceTo(point);
    }

    public double getSquaredDistanceTo(double x, double y) {
        return currentState.getPosition().getSquaredDistanceTo(x, y);
    }

    @Nonnull
    public Point2D getCenterOfMass() {
        Point2D position = currentState.getPosition();
        if (position == null) {
            throw new IllegalStateException("Can't calculate center of mass for body with no position.");
        }

        if (form == null) {
            throw new IllegalStateException("Can't calculate center of mass for body with no form.");
        }

        return form.getCenterOfMass(this);
    }

    public void normalizeAngle() {
        currentState.normalizeAngle();
    }

    @Nonnull
    public Map<String, Object> getAttributeByName() {
        return attributeByName == null
                ? Collections.<String, Object>emptyMap()
                : Collections.unmodifiableMap(attributeByName);
    }

    @Nullable
    public Object getAttribute(@Nonnull String name) {
        return attributeByName == null ? null : attributeByName.get(name);
    }

    public void setAttribute(@Nonnull String name, @Nullable Object value) {
        if (value == null) {
            if (attributeByName != null) {
                attributeByName.remove(name);
            }
        } else {
            if (attributeByName == null) {
                attributeByName = new HashMap<>();
            }

            attributeByName.put(name, value);
        }
    }

    @SuppressWarnings("FloatingPointEquality")
    void applyMovementAirFriction(double updateFactor) {
        if (lastMovementTransferFactor == null
                || movementAirFrictionFactor != lastMovementAirFrictionFactor
                || updateFactor != lastMovementUpdateFactor) {
            lastMovementAirFrictionFactor = movementAirFrictionFactor;
            lastMovementUpdateFactor = updateFactor;
            lastMovementTransferFactor = pow(1.0D - movementAirFrictionFactor, updateFactor);
        }

        getVelocity().subtract(getMedianVelocity()).multiply(lastMovementTransferFactor).add(getMedianVelocity());
    }

    @SuppressWarnings("FloatingPointEquality")
    void applyRotationAirFriction(double updateFactor) {
        if (lastRotationTransferFactor == null
                || rotationAirFrictionFactor != lastRotationAirFrictionFactor
                || updateFactor != lastRotationUpdateFactor) {
            lastRotationAirFrictionFactor = rotationAirFrictionFactor;
            lastRotationUpdateFactor = updateFactor;
            lastRotationTransferFactor = pow(1.0D - rotationAirFrictionFactor, updateFactor);
        }

        setAngularVelocity((
                getAngularVelocity() - getMedianAngularVelocity()
        ) * lastRotationTransferFactor + getMedianAngularVelocity());
    }

    @Contract(value = "null -> false", pure = true)
    public boolean equals(@Nullable Body body) {
        return body != null && id == body.id;
    }

    @Contract(pure = true)
    @Override
    public int hashCode() {
        return hashCode;
    }

    @Contract("null -> false")
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Body body = (Body) o;

        return id == body.id;
    }

    @Nonnull
    @Override
    public String toString() {
        return toString(this);
    }

    @Nonnull
    public static String toString(Body body) {
        return StringUtil.toString(
                Body.class, body, true, "id", "name", "position", "angle", "velocity", "angularVelocity"
        );
    }
}
