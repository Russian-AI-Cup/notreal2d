package com.codegame.codeseries.notreal2d;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codeforces.commons.math.NumberUtil;
import com.codeforces.commons.pair.LongPair;
import com.codegame.codeseries.notreal2d.bodylist.BodyList;
import com.codegame.codeseries.notreal2d.bodylist.SimpleBodyList;
import com.codegame.codeseries.notreal2d.collision.*;
import com.codegame.codeseries.notreal2d.listener.CollisionListener;
import com.codegame.codeseries.notreal2d.provider.MomentumTransferFactorProvider;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.*;

import static com.codeforces.commons.math.Math.*;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
@SuppressWarnings("WeakerAccess")
public class World {
    private static final Logger logger = Logger.getLogger(World.class);

    @SuppressWarnings("ConstantConditions")
    private static final CollisionInfo NULL_COLLISION_INFO = new CollisionInfo(null, null, null, null, 0.0D, 0.0D);

    private final int iterationCountPerStep;
    private final int stepCountPerTimeUnit;
    private final double updateFactor;

    private final double epsilon;
    private final double squaredEpsilon;

    private final BodyList bodyList;
    private final MomentumTransferFactorProvider momentumTransferFactorProvider;

    private final Map<String, ColliderEntry> colliderEntryByName = new HashMap<>();
    private final SortedSet<ColliderEntry> colliderEntries = new TreeSet<>(ColliderEntry.comparator);

    private final Map<String, CollisionListenerEntry> collisionListenerEntryByName = new HashMap<>();
    private final SortedSet<CollisionListenerEntry> collisionListenerEntries = new TreeSet<>(CollisionListenerEntry.comparator);

    public World() {
        this(Defaults.ITERATION_COUNT_PER_STEP);
    }

    public World(int iterationCountPerStep) {
        this(iterationCountPerStep, Defaults.STEP_COUNT_PER_TIME_UNIT);
    }

    public World(int iterationCountPerStep, int stepCountPerTimeUnit) {
        this(iterationCountPerStep, stepCountPerTimeUnit, Defaults.EPSILON);
    }

    public World(int iterationCountPerStep, int stepCountPerTimeUnit, double epsilon) {
        this(iterationCountPerStep, stepCountPerTimeUnit, epsilon, new SimpleBodyList());
    }

    public World(int iterationCountPerStep, int stepCountPerTimeUnit, double epsilon, BodyList bodyList) {
        this(iterationCountPerStep, stepCountPerTimeUnit, epsilon, bodyList, null);
    }

    public World(int iterationCountPerStep, int stepCountPerTimeUnit, double epsilon, BodyList bodyList,
                 @Nullable MomentumTransferFactorProvider momentumTransferFactorProvider) {
        if (iterationCountPerStep < 1) {
            throw new IllegalArgumentException("Argument 'iterationCountPerStep' is zero or negative.");
        }

        if (stepCountPerTimeUnit < 1) {
            throw new IllegalArgumentException("Argument 'stepCountPerTimeUnit' is zero or negative.");
        }

        if (Double.isNaN(epsilon) || Double.isInfinite(epsilon) || epsilon < 1.0E-100D || epsilon > 1.0D) {
            throw new IllegalArgumentException("Argument 'epsilon' should be between 1.0E-100 and 1.0.");
        }

        if (bodyList == null) {
            throw new IllegalArgumentException("Argument 'bodyList' is null.");
        }

        this.stepCountPerTimeUnit = stepCountPerTimeUnit;
        this.iterationCountPerStep = iterationCountPerStep;
        this.updateFactor = 1.0D / (stepCountPerTimeUnit * iterationCountPerStep);
        this.epsilon = epsilon;
        this.squaredEpsilon = epsilon * epsilon;
        this.bodyList = bodyList;
        this.momentumTransferFactorProvider = momentumTransferFactorProvider;

        registerCollider(new ArcAndArcCollider(epsilon));
        registerCollider(new ArcAndCircleCollider(epsilon));
        registerCollider(new CircleAndCircleCollider(epsilon));
        registerCollider(new LineAndArcCollider(epsilon));
        registerCollider(new LineAndCircleCollider(epsilon));
        registerCollider(new LineAndLineCollider(epsilon));
        registerCollider(new LineAndRectangleCollider(epsilon));
        registerCollider(new RectangleAndArcCollider(epsilon));
        registerCollider(new RectangleAndCircleCollider(epsilon));
        registerCollider(new RectangleAndRectangleCollider(epsilon));
    }

    public int getIterationCountPerStep() {
        return iterationCountPerStep;
    }

    public int getStepCountPerTimeUnit() {
        return stepCountPerTimeUnit;
    }

    public double getEpsilon() {
        return epsilon;
    }

    public void addBody(@Nonnull Body body) {
        if (body.getForm() == null || body.getMass() == 0.0D) {
            throw new IllegalArgumentException("Specify form and mass of 'body' before adding to the world.");
        }

        bodyList.addBody(body);
    }

    public void removeBody(@Nonnull Body body) {
        bodyList.removeBody(body);
    }

    public void removeBody(long id) {
        bodyList.removeBody(id);
    }

    public void removeBodyQuietly(@Nullable Body body) {
        bodyList.removeBodyQuietly(body);
    }

    public void removeBodyQuietly(long id) {
        bodyList.removeBodyQuietly(id);
    }

    public boolean hasBody(@Nonnull Body body) {
        return bodyList.hasBody(body);
    }

    public boolean hasBody(long id) {
        return bodyList.hasBody(id);
    }

    public Body getBody(long id) {
        return bodyList.getBody(id);
    }

    public boolean isColliding(@Nonnull Body body) {
        return getCollisionInfo(body) != null;
    }

    public List<Body> getBodies() {
        return bodyList.getBodies();
    }

    @Nullable
    public CollisionInfo getCollisionInfo(@Nonnull Body body) {
        if (!bodyList.hasBody(body)) {
            return null;
        }

        for (Body otherBody : bodyList.getPotentialIntersections(body)) {
            if (body.isStatic() && otherBody.isStatic()) {
                continue;
            }

            for (ColliderEntry colliderEntry : colliderEntries) {
                if (colliderEntry.collider.matches(body, otherBody)) {
                    return colliderEntry.collider.collide(body, otherBody);
                }
            }
        }

        return null;
    }

    @Nonnull
    public List<CollisionInfo> getCollisionInfos(@Nonnull Body body) {
        if (!bodyList.hasBody(body)) {
            return Collections.emptyList();
        }

        List<CollisionInfo> collisionInfos = new ArrayList<>();

        for (Body otherBody : bodyList.getPotentialIntersections(body)) {
            if (body.isStatic() && otherBody.isStatic()) {
                continue;
            }

            for (ColliderEntry colliderEntry : colliderEntries) {
                if (colliderEntry.collider.matches(body, otherBody)) {
                    CollisionInfo collisionInfo = colliderEntry.collider.collide(body, otherBody);
                    if (collisionInfo != null) {
                        collisionInfos.add(collisionInfo);
                    }
                    break;
                }
            }
        }

        return Collections.unmodifiableList(collisionInfos);
    }

    public void proceed() {
        List<Body> bodies = new ArrayList<>(getBodies());

        for (Body body : bodies) {
            if (!hasBody(body)) {
                continue;
            }

            body.normalizeAngle();
            body.saveBeforeStepState();
        }

        for (int i = 1; i <= iterationCountPerStep; ++i) {
            for (Body body : bodies) {
                if (!hasBody(body)) {
                    continue;
                }

                body.saveBeforeIterationState();
                updateState(body);
                body.normalizeAngle();
            }

            Map<LongPair, CollisionInfo> collisionInfoByBodyIdsPair = new HashMap<>();

            for (Body body : bodies) {
                if (body.isStatic() || !hasBody(body)) {
                    continue;
                }

                for (Body otherBody : bodyList.getPotentialIntersections(body)) {
                    if (!hasBody(body)) {
                        break;
                    }

                    if (hasBody(otherBody)) {
                        collide(body, otherBody, collisionInfoByBodyIdsPair);
                    }
                }
            }
        }

        for (Body body : bodies) {
            if (!hasBody(body)) {
                continue;
            }

            body.setForce(0.0D, 0.0D);
            body.setTorque(0.0D);
        }
    }

    private void collide(@Nonnull Body body, @Nonnull Body otherBody,
                         @Nonnull Map<LongPair, CollisionInfo> collisionInfoByBodyIdsPair) {
        Body bodyA;
        Body bodyB;

        if (body.getId() > otherBody.getId()) {
            bodyA = otherBody;
            bodyB = body;
        } else {
            bodyA = body;
            bodyB = otherBody;
        }

        LongPair bodyIdsPair = new LongPair(bodyA.getId(), bodyB.getId());

        CollisionInfo collisionInfo = collisionInfoByBodyIdsPair.get(bodyIdsPair);
        if (collisionInfo != null) {
            return;
        }

        for (CollisionListenerEntry collisionListenerEntry : collisionListenerEntries) {
            if (!collisionListenerEntry.listener.beforeStartingCollision(bodyA, bodyB)) {
                collisionInfoByBodyIdsPair.put(bodyIdsPair, NULL_COLLISION_INFO);
                return;
            }

            if (!hasBody(bodyA) || !hasBody(bodyB)) {
                return;
            }
        }

        for (ColliderEntry colliderEntry : colliderEntries) {
            if (colliderEntry.collider.matches(bodyA, bodyB)) {
                collisionInfo = colliderEntry.collider.collide(bodyA, bodyB);
                break;
            }
        }

        if (collisionInfo == null) {
            collisionInfoByBodyIdsPair.put(bodyIdsPair, NULL_COLLISION_INFO);
        } else {
            collisionInfoByBodyIdsPair.put(bodyIdsPair, collisionInfo);
            resolveCollision(collisionInfo);
        }
    }

    private void resolveCollision(@Nonnull CollisionInfo collisionInfo) {
        Body bodyA = collisionInfo.getBodyA();
        Body bodyB = collisionInfo.getBodyB();

        if (bodyA.isStatic() && bodyB.isStatic()) {
            throw new IllegalArgumentException("Both " + bodyA + " and " + bodyB + " are static.");
        }

        for (CollisionListenerEntry collisionListenerEntry : collisionListenerEntries) {
            if (!collisionListenerEntry.listener.beforeResolvingCollision(collisionInfo)) {
                return;
            }

            if (!hasBody(bodyA) || !hasBody(bodyB)) {
                return;
            }
        }

        logCollision(collisionInfo);

        Vector3D collisionNormalB = toVector3D(collisionInfo.getNormalB());

        Vector3D vectorAC = toVector3D(bodyA.getCenterOfMass(), collisionInfo.getPoint());
        Vector3D vectorBC = toVector3D(bodyB.getCenterOfMass(), collisionInfo.getPoint());

        Vector3D angularVelocityPartAC = toVector3DZ(bodyA.getAngularVelocity()).crossProduct(vectorAC);
        Vector3D angularVelocityPartBC = toVector3DZ(bodyB.getAngularVelocity()).crossProduct(vectorBC);

        Vector3D velocityAC = toVector3D(bodyA.getVelocity()).add(angularVelocityPartAC);
        Vector3D velocityBC = toVector3D(bodyB.getVelocity()).add(angularVelocityPartBC);

        Vector3D relativeVelocityC = velocityAC.subtract(velocityBC);
        double normalRelativeVelocityLengthC = -relativeVelocityC.dotProduct(collisionNormalB);

        if (normalRelativeVelocityLengthC > -epsilon) {
            resolveImpact(bodyA, bodyB, collisionNormalB, vectorAC, vectorBC, relativeVelocityC);
            resolveSurfaceFriction(bodyA, bodyB, collisionNormalB, vectorAC, vectorBC, relativeVelocityC);
        }

        if (collisionInfo.getDepth() >= epsilon) {
            pushBackBodies(bodyA, bodyB, collisionInfo);
        }

        bodyA.normalizeAngle();
        bodyB.normalizeAngle();

        for (CollisionListenerEntry collisionListenerEntry : collisionListenerEntries) {
            collisionListenerEntry.listener.afterResolvingCollision(collisionInfo);
        }
    }

    @SuppressWarnings("Duplicates")
    private void resolveImpact(
            @Nonnull Body bodyA, @Nonnull Body bodyB, @Nonnull Vector3D collisionNormalB,
            @Nonnull Vector3D vectorAC, @Nonnull Vector3D vectorBC, @Nonnull Vector3D relativeVelocityC) {
        Double momentumTransferFactor;

        if (momentumTransferFactorProvider == null
                || (momentumTransferFactor = momentumTransferFactorProvider.getFactor(bodyA, bodyB)) == null) {
            momentumTransferFactor = bodyA.getMomentumTransferFactor() * bodyB.getMomentumTransferFactor();
        }

        Vector3D denominatorPartA = vectorAC.crossProduct(collisionNormalB)
                .scalarMultiply(bodyA.getInvertedAngularMass()).crossProduct(vectorAC);
        Vector3D denominatorPartB = vectorBC.crossProduct(collisionNormalB)
                .scalarMultiply(bodyB.getInvertedAngularMass()).crossProduct(vectorBC);

        double denominator = bodyA.getInvertedMass() + bodyB.getInvertedMass()
                + collisionNormalB.dotProduct(denominatorPartA.add(denominatorPartB));

        double impulseChange = -1.0D * (1.0D + momentumTransferFactor) * relativeVelocityC.dotProduct(collisionNormalB)
                / denominator;

        if (abs(impulseChange) < epsilon) {
            return;
        }

        if (!bodyA.isStatic()) {
            Vector3D velocityChangeA = collisionNormalB.scalarMultiply(impulseChange * bodyA.getInvertedMass());
            Vector3D newVelocityA = toVector3D(bodyA.getVelocity()).add(velocityChangeA);
            bodyA.setVelocity(newVelocityA.getX(), newVelocityA.getY());

            Vector3D angularVelocityChangeA = vectorAC.crossProduct(collisionNormalB.scalarMultiply(impulseChange))
                    .scalarMultiply(bodyA.getInvertedAngularMass());
            Vector3D newAngularVelocityA = toVector3DZ(bodyA.getAngularVelocity()).add(angularVelocityChangeA);
            bodyA.setAngularVelocity(newAngularVelocityA.getZ());
        }

        if (!bodyB.isStatic()) {
            Vector3D velocityChangeB = collisionNormalB.scalarMultiply(impulseChange * bodyB.getInvertedMass());
            Vector3D newVelocityB = toVector3D(bodyB.getVelocity()).subtract(velocityChangeB);
            bodyB.setVelocity(newVelocityB.getX(), newVelocityB.getY());

            Vector3D angularVelocityChangeB = vectorBC.crossProduct(collisionNormalB.scalarMultiply(impulseChange))
                    .scalarMultiply(bodyB.getInvertedAngularMass());
            Vector3D newAngularVelocityB = toVector3DZ(bodyB.getAngularVelocity()).subtract(angularVelocityChangeB);
            bodyB.setAngularVelocity(newAngularVelocityB.getZ());
        }
    }

    @SuppressWarnings("Duplicates")
    private void resolveSurfaceFriction(
            @Nonnull Body bodyA, @Nonnull Body bodyB, @Nonnull Vector3D collisionNormalB,
            @Nonnull Vector3D vectorAC, @Nonnull Vector3D vectorBC, @Nonnull Vector3D relativeVelocityC) {
        Vector3D tangent = relativeVelocityC
                .subtract(collisionNormalB.scalarMultiply(relativeVelocityC.dotProduct(collisionNormalB)));

        if (tangent.getNormSq() < squaredEpsilon) {
            return;
        }

        tangent = tangent.normalize();

        double surfaceFriction = sqrt(bodyA.getSurfaceFrictionFactor() * bodyB.getSurfaceFrictionFactor())
                * SQRT_2 * abs(relativeVelocityC.dotProduct(collisionNormalB)) / relativeVelocityC.getNorm();

        if (surfaceFriction < epsilon) {
            return;
        }

        Vector3D denominatorPartA = vectorAC.crossProduct(tangent)
                .scalarMultiply(bodyA.getInvertedAngularMass()).crossProduct(vectorAC);
        Vector3D denominatorPartB = vectorBC.crossProduct(tangent)
                .scalarMultiply(bodyB.getInvertedAngularMass()).crossProduct(vectorBC);

        double denominator = bodyA.getInvertedMass() + bodyB.getInvertedMass()
                + tangent.dotProduct(denominatorPartA.add(denominatorPartB));

        double impulseChange = -1.0D * surfaceFriction * relativeVelocityC.dotProduct(tangent)
                / denominator;

        if (abs(impulseChange) < epsilon) {
            return;
        }

        if (!bodyA.isStatic()) {
            Vector3D velocityChangeA = tangent.scalarMultiply(impulseChange * bodyA.getInvertedMass());
            Vector3D newVelocityA = toVector3D(bodyA.getVelocity()).add(velocityChangeA);
            bodyA.setVelocity(newVelocityA.getX(), newVelocityA.getY());

            Vector3D angularVelocityChangeA = vectorAC.crossProduct(tangent.scalarMultiply(impulseChange))
                    .scalarMultiply(bodyA.getInvertedAngularMass());
            Vector3D newAngularVelocityA = toVector3DZ(bodyA.getAngularVelocity()).add(angularVelocityChangeA);
            bodyA.setAngularVelocity(newAngularVelocityA.getZ());
        }

        if (!bodyB.isStatic()) {
            Vector3D velocityChangeB = tangent.scalarMultiply(impulseChange * bodyB.getInvertedMass());
            Vector3D newVelocityB = toVector3D(bodyB.getVelocity()).subtract(velocityChangeB);
            bodyB.setVelocity(newVelocityB.getX(), newVelocityB.getY());

            Vector3D angularVelocityChangeB = vectorBC.crossProduct(tangent.scalarMultiply(impulseChange))
                    .scalarMultiply(bodyB.getInvertedAngularMass());
            Vector3D newAngularVelocityB = toVector3DZ(bodyB.getAngularVelocity()).subtract(angularVelocityChangeB);
            bodyB.setAngularVelocity(newAngularVelocityB.getZ());
        }
    }

    private void updateState(@Nonnull Body body) {
        updatePosition(body);
        updateAngle(body);
    }

    private void updatePosition(@Nonnull Body body) {
        if (body.getVelocity().getSquaredLength() > 0.0D) {
            body.getPosition().add(body.getVelocity().copy().multiply(updateFactor));
        }

        if (body.getForce().getSquaredLength() > 0.0D) {
            body.getVelocity().add(body.getForce().copy().multiply(body.getInvertedMass()).multiply(updateFactor));
        }

        if (body.getMovementAirFrictionFactor() >= 1.0D) {
            body.setVelocity(body.getMedianVelocity().copy());
        } else if (body.getMovementAirFrictionFactor() > 0.0D) {
            body.applyMovementAirFriction(updateFactor);

            if (body.getVelocity().nearlyEquals(body.getMedianVelocity(), epsilon)) {
                body.setVelocity(body.getMedianVelocity().copy());
            }
        }

        body.getVelocity().subtract(body.getMedianVelocity());
        body.applyFriction(updateFactor);
        body.getVelocity().add(body.getMedianVelocity());
    }

    private void updateAngle(@Nonnull Body body) {
        body.setAngle(body.getAngle() + body.getAngularVelocity() * updateFactor);
        body.setAngularVelocity(
                body.getAngularVelocity() + body.getTorque() * body.getInvertedAngularMass() * updateFactor
        );

        if (body.getRotationAirFrictionFactor() >= 1.0D) {
            body.setAngularVelocity(body.getMedianAngularVelocity());
        } else if (body.getRotationAirFrictionFactor() > 0.0D) {
            body.applyRotationAirFriction(updateFactor);

            if (NumberUtil.nearlyEquals(body.getAngularVelocity(), body.getMedianAngularVelocity(), epsilon)) {
                body.setAngularVelocity(body.getMedianAngularVelocity());
            }
        }

        double angularVelocity = body.getAngularVelocity() - body.getMedianAngularVelocity();

        if (abs(angularVelocity) > 0.0D) {
            double rotationFrictionFactor = body.getRotationFrictionFactor() * updateFactor;

            if (rotationFrictionFactor >= abs(angularVelocity)) {
                body.setAngularVelocity(body.getMedianAngularVelocity());
            } else if (rotationFrictionFactor > 0.0D) {
                if (angularVelocity > 0.0D) {
                    body.setAngularVelocity(angularVelocity - rotationFrictionFactor + body.getMedianAngularVelocity());
                } else {
                    body.setAngularVelocity(angularVelocity + rotationFrictionFactor + body.getMedianAngularVelocity());
                }
            }
        }
    }

    private void pushBackBodies(@Nonnull Body bodyA, @Nonnull Body bodyB, @Nonnull CollisionInfo collisionInfo) {
        if (bodyA.isStatic()) {
            bodyB.getPosition().subtract(collisionInfo.getNormalB().multiply(collisionInfo.getDepth() + epsilon));
        } else if (bodyB.isStatic()) {
            bodyA.getPosition().add(collisionInfo.getNormalB().multiply(collisionInfo.getDepth() + epsilon));
        } else {
            Vector2D normalOffset = collisionInfo.getNormalB().multiply(0.5D * (collisionInfo.getDepth() + epsilon));
            bodyA.getPosition().add(normalOffset);
            bodyB.getPosition().subtract(normalOffset);
        }
    }

    public void registerCollider(@Nonnull Collider collider, @Nonnull String name, double priority) {
        NamedEntry.validateName(name);

        if (colliderEntryByName.containsKey(name)) {
            throw new IllegalArgumentException("Collider '" + name + "' is already registered.");
        }

        ColliderEntry colliderEntry = new ColliderEntry(name, priority, collider);
        colliderEntryByName.put(name, colliderEntry);
        colliderEntries.add(colliderEntry);
    }

    public void registerCollider(@Nonnull Collider collider, @Nonnull String name) {
        registerCollider(collider, name, 0.0D);
    }

    private void registerCollider(@Nonnull Collider collider) {
        registerCollider(collider, collider.getClass().getSimpleName());
    }

    public void unregisterCollider(@Nonnull String name) {
        NamedEntry.validateName(name);

        ColliderEntry colliderEntry = colliderEntryByName.remove(name);
        if (colliderEntry == null) {
            throw new IllegalArgumentException("Collider '" + name + "' is not registered.");
        }

        colliderEntries.remove(colliderEntry);
    }

    public boolean hasCollider(@Nonnull String name) {
        NamedEntry.validateName(name);
        return colliderEntryByName.containsKey(name);
    }

    public void registerCollisionListener(@Nonnull CollisionListener listener, @Nonnull String name, double priority) {
        NamedEntry.validateName(name);

        if (collisionListenerEntryByName.containsKey(name)) {
            throw new IllegalArgumentException("Listener '" + name + "' is already registered.");
        }

        CollisionListenerEntry collisionListenerEntry = new CollisionListenerEntry(name, priority, listener);
        collisionListenerEntryByName.put(name, collisionListenerEntry);
        collisionListenerEntries.add(collisionListenerEntry);
    }

    public void registerCollisionListener(@Nonnull CollisionListener listener, @Nonnull String name) {
        registerCollisionListener(listener, name, 0.0D);
    }

    private void registerCollisionListener(@Nonnull CollisionListener listener) {
        registerCollisionListener(listener, listener.getClass().getSimpleName());
    }

    public void unregisterCollisionListener(@Nonnull String name) {
        NamedEntry.validateName(name);

        CollisionListenerEntry collisionListenerEntry = collisionListenerEntryByName.remove(name);
        if (collisionListenerEntry == null) {
            throw new IllegalArgumentException("Listener '" + name + "' is not registered.");
        }

        collisionListenerEntries.remove(collisionListenerEntry);
    }

    public boolean hasCollisionListener(@Nonnull String name) {
        NamedEntry.validateName(name);
        return collisionListenerEntryByName.containsKey(name);
    }

    private static void logCollision(CollisionInfo collisionInfo) {
        if (collisionInfo.getDepth() >= collisionInfo.getBodyA().getForm().getCircumcircleRadius() * 0.25D
                || collisionInfo.getDepth() >= collisionInfo.getBodyB().getForm().getCircumcircleRadius() * 0.25D) {
            if (logger.isEnabledFor(Level.WARN)) {
                logger.warn("Resolving collision (big depth) " + collisionInfo + '.');
            }
        } else {
            if (logger.isDebugEnabled()) {
                logger.debug("Resolving collision " + collisionInfo + '.');
            }
        }
    }

    @Nonnull
    private static Vector3D toVector3DZ(double z) {
        return new Vector3D(0.0D, 0.0D, z);
    }

    @Nonnull
    private static Vector3D toVector3D(@Nonnull Vector2D vector) {
        return new Vector3D(vector.getX(), vector.getY(), 0.0D);
    }

    @Nonnull
    private static Vector3D toVector3D(@Nonnull Point2D point1, @Nonnull Point2D point2) {
        return toVector3D(new Vector2D(point1, point2));
    }

    @SuppressWarnings("PublicField")
    private static final class ColliderEntry extends NamedEntry {
        private static final Comparator<ColliderEntry> comparator = (colliderEntryA, colliderEntryB) -> {
            int comparisonResult = Double.compare(colliderEntryB.priority, colliderEntryA.priority);
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            return colliderEntryA.name.compareTo(colliderEntryB.name);
        };

        public final double priority;
        public final Collider collider;

        private ColliderEntry(String name, double priority, Collider collider) {
            super(name);

            this.priority = priority;
            this.collider = collider;
        }
    }

    @SuppressWarnings("PublicField")
    private static final class CollisionListenerEntry extends NamedEntry {
        private static final Comparator<CollisionListenerEntry> comparator = (listenerEntryA, listenerEntryB) -> {
            int comparisonResult = Double.compare(listenerEntryB.priority, listenerEntryA.priority);
            if (comparisonResult != 0) {
                return comparisonResult;
            }

            return listenerEntryA.name.compareTo(listenerEntryB.name);
        };

        public final double priority;
        public final CollisionListener listener;

        private CollisionListenerEntry(String name, double priority, CollisionListener listener) {
            super(name);

            this.priority = priority;
            this.listener = listener;
        }
    }
}
