package com.codegame.codeseries.notreal2d;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.geometry.Vector2D;
import com.codeforces.commons.text.StringUtil;
import com.codegame.codeseries.notreal2d.listener.PositionListener;

import javax.annotation.Nonnull;
import java.util.*;

import static com.codeforces.commons.math.Math.DOUBLE_PI;
import static com.codeforces.commons.math.Math.PI;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 31.08.2015
 */
public class StaticState {
    private ListeningPoint2D position;
    private double angle;

    private Map<String, PositionListenerEntry> positionListenerEntryByName;
    private SortedSet<PositionListenerEntry> positionListenerEntries;

    public StaticState() {
        this.position = this.new ListeningPoint2D(0.0D, 0.0D);
    }

    public StaticState(Point2D position, double angle) {
        this.position = this.new ListeningPoint2D(position);
        this.angle = angle;
    }

    public StaticState(StaticState state) {
        this.position = this.new ListeningPoint2D(state.position);
        this.angle = state.angle;
    }

    public Point2D getPosition() {
        return position;
    }

    public void setPosition(Point2D position) {
        Point2D oldPosition = this.position.copy();
        Point2D newPosition = position.copy();

        if (positionListenerEntries != null) {
            for (PositionListenerEntry positionListenerEntry : positionListenerEntries) {
                if (!positionListenerEntry.listener.beforeChangePosition(oldPosition.copy(), newPosition)) {
                    return;
                }
            }
        }

        this.position = this.new ListeningPoint2D(newPosition);

        if (positionListenerEntries != null) {
            for (PositionListenerEntry positionListenerEntry : positionListenerEntries) {
                positionListenerEntry.listener.afterChangePosition(oldPosition.copy(), newPosition.copy());
            }
        }
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public void normalizeAngle() {
        while (angle > PI) {
            angle -= DOUBLE_PI;
        }

        while (angle < -PI) {
            angle += DOUBLE_PI;
        }
    }

    public void registerPositionListener(@Nonnull PositionListener listener, @Nonnull String name, double priority) {
        NamedEntry.validateName(name);

        if (positionListenerEntryByName == null) {
            positionListenerEntryByName = new HashMap<>(1);
            positionListenerEntries = new TreeSet<>(PositionListenerEntry.comparator);
        } else if (positionListenerEntryByName.containsKey(name)) {
            throw new IllegalArgumentException("Listener '" + name + "' is already registered.");
        }

        PositionListenerEntry positionListenerEntry = new PositionListenerEntry(name, priority, listener);
        positionListenerEntryByName.put(name, positionListenerEntry);
        positionListenerEntries.add(positionListenerEntry);
    }

    public void registerPositionListener(@Nonnull PositionListener listener, @Nonnull String name) {
        registerPositionListener(listener, name, 0.0D);
    }

    private void registerPositionListener(@Nonnull PositionListener listener) {
        registerPositionListener(listener, listener.getClass().getSimpleName());
    }

    public void unregisterPositionListener(@Nonnull String name) {
        NamedEntry.validateName(name);

        PositionListenerEntry positionListenerEntry;

        if (positionListenerEntryByName == null
                || (positionListenerEntry = positionListenerEntryByName.remove(name)) == null) {
            throw new IllegalArgumentException("Listener '" + name + "' is not registered.");
        }

        positionListenerEntries.remove(positionListenerEntry);
    }

    public boolean hasPositionListener(@Nonnull String name) {
        NamedEntry.validateName(name);
        return positionListenerEntryByName != null && positionListenerEntryByName.containsKey(name);
    }

    @Override
    public String toString() {
        return StringUtil.toString(this, false);
    }

    @SuppressWarnings({"RefusedBequest", "NonStaticInnerClassInSecureContext"})
    private final class ListeningPoint2D extends Point2D {
        private ListeningPoint2D(double x, double y) {
            super(x, y);
        }

        private ListeningPoint2D(@Nonnull Point2D point) {
            super(point);
        }

        @Override
        public void setX(double x) {
            setFirst(x);
        }

        @Override
        public void setY(double y) {
            setSecond(y);
        }

        @Nonnull
        @Override
        public Point2D add(Vector2D vector) {
            Point2D oldPosition = super.copy();
            Point2D newPosition = super.copy().add(vector);

            return onChange(oldPosition, newPosition);
        }

        @Nonnull
        @Override
        public Point2D add(double x, double y) {
            Point2D oldPosition = super.copy();
            Point2D newPosition = super.copy().add(x, y);

            return onChange(oldPosition, newPosition);
        }

        @Nonnull
        @Override
        public Point2D subtract(Vector2D vector) {
            Point2D oldPosition = super.copy();
            Point2D newPosition = super.copy().subtract(vector);

            return onChange(oldPosition, newPosition);
        }

        @Nonnull
        @Override
        public Point2D subtract(double x, double y) {
            Point2D oldPosition = super.copy();
            Point2D newPosition = super.copy().subtract(x, y);

            return onChange(oldPosition, newPosition);
        }

        @Override
        public void setFirst(double first) {
            Point2D oldPosition = super.copy();
            Point2D newPosition = super.copy();
            newPosition.setFirst(first);

            onChange(oldPosition, newPosition);
        }

        @Override
        public void setSecond(double second) {
            Point2D oldPosition = super.copy();
            Point2D newPosition = super.copy();
            newPosition.setSecond(second);

            onChange(oldPosition, newPosition);
        }

        @Nonnull
        private Point2D onChange(@Nonnull Point2D oldPosition, @Nonnull Point2D newPosition) {
            if (positionListenerEntries != null) {
                for (PositionListenerEntry positionListenerEntry : positionListenerEntries) {
                    if (!positionListenerEntry.listener.beforeChangePosition(oldPosition.copy(), newPosition)) {
                        return this;
                    }
                }
            }

            super.setFirst(newPosition.getFirst());
            super.setSecond(newPosition.getSecond());

            if (positionListenerEntries != null) {
                for (PositionListenerEntry positionListenerEntry : positionListenerEntries) {
                    positionListenerEntry.listener.afterChangePosition(oldPosition.copy(), newPosition.copy());
                }
            }

            return this;
        }
    }

    @SuppressWarnings("PublicField")
    private static final class PositionListenerEntry extends NamedEntry {
        private static final Comparator<PositionListenerEntry> comparator = new Comparator<PositionListenerEntry>() {
            @Override
            public int compare(PositionListenerEntry listenerEntryA, PositionListenerEntry listenerEntryB) {
                int comparisonResult = Double.compare(listenerEntryB.priority, listenerEntryA.priority);
                if (comparisonResult != 0) {
                    return comparisonResult;
                }

                return listenerEntryA.name.compareTo(listenerEntryB.name);
            }
        };

        public final double priority;
        public final PositionListener listener;

        private PositionListenerEntry(String name, double priority, PositionListener listener) {
            super(name);

            this.priority = priority;
            this.listener = listener;
        }
    }
}
