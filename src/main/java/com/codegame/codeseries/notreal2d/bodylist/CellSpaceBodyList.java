package com.codegame.codeseries.notreal2d.bodylist;

import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.math.NumberUtil;
import com.codeforces.commons.pair.IntPair;
import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.listener.PositionListenerAdapter;
import com.google.common.collect.UnmodifiableIterator;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

import static com.codeforces.commons.math.Math.floor;
import static com.codeforces.commons.math.Math.sqr;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
@NotThreadSafe
public class CellSpaceBodyList extends BodyListBase {
    private static final int MIN_FAST_X = -1000;
    private static final int MAX_FAST_X = 1000;
    private static final int MIN_FAST_Y = -1000;
    private static final int MAX_FAST_Y = 1000;

    private static final int MAX_FAST_CELL_BODY_ID = 9999;

    private final Set<Body> bodies = new HashSet<>();
    private final Map<Long, Body> bodyById = new HashMap<>();

    private final int[] fastCellXByBodyId = new int[MAX_FAST_CELL_BODY_ID + 1];
    private final int[] fastCellYByBodyId = new int[MAX_FAST_CELL_BODY_ID + 1];
    private final Point2D[] fastCellLeftTopByBodyId = new Point2D[MAX_FAST_CELL_BODY_ID + 1];
    private final Point2D[] fastCellRightBottomByBodyId = new Point2D[MAX_FAST_CELL_BODY_ID + 1];

    private final Body[][][] bodiesByCellXY = new Body[MAX_FAST_X - MIN_FAST_X + 1][MAX_FAST_Y - MIN_FAST_Y + 1][];
    private final Map<IntPair, Body[]> bodiesByCell = new HashMap<>();
    private final Set<Body> cellExceedingBodies = new HashSet<>();

    private double cellSize;
    private final double maxCellSize;

    public CellSpaceBodyList(double initialCellSize, double maxCellSize) {
        this.cellSize = initialCellSize;
        this.maxCellSize = maxCellSize;
    }

    @Override
    public void addBody(@Nonnull Body body) {
        validateBody(body);

        if (bodies.contains(body)) {
            throw new IllegalStateException(body + " is already added.");
        }

        double radius = body.getForm().getCircumcircleRadius();
        double diameter = 2.0D * radius;

        if (diameter > cellSize && diameter <= maxCellSize) {
            cellSize = diameter;
            rebuildIndexes();
        }

        bodies.add(body);
        bodyById.put(body.getId(), body);
        addBodyToIndexes(body);

        body.getCurrentState().registerPositionListener(new PositionListenerAdapter() {
            @Override
            public void afterChangePosition(@Nonnull Point2D oldPosition, @Nonnull Point2D newPosition) {
                if (diameter > cellSize) {
                    return;
                }

                int oldCellX;
                int oldCellY;

                int newCellX;
                int newCellY;

                if (body.getId() >= 0 && body.getId() <= MAX_FAST_CELL_BODY_ID) {
                    @SuppressWarnings("NumericCastThatLosesPrecision") int bodyId = (int) body.getId();
                    Point2D cellLeftTop = fastCellLeftTopByBodyId[bodyId];
                    Point2D cellRightBottom = fastCellRightBottomByBodyId[bodyId];

                    Point2D position = body.getPosition();

                    if (position.getX() >= cellLeftTop.getX() && position.getY() >= cellLeftTop.getY()
                            && position.getX() < cellRightBottom.getX() && position.getY() < cellRightBottom.getY()) {
                        return;
                    }

                    oldCellX = getCellX(oldPosition.getX());
                    oldCellY = getCellY(oldPosition.getY());

                    newCellX = getCellX(newPosition.getX());
                    newCellY = getCellY(newPosition.getY());
                } else {
                    oldCellX = getCellX(oldPosition.getX());
                    oldCellY = getCellY(oldPosition.getY());

                    newCellX = getCellX(newPosition.getX());
                    newCellY = getCellY(newPosition.getY());

                    if (oldCellX == newCellX && oldCellY == newCellY) {
                        return;
                    }
                }

                removeBodyFromIndexes(body, oldCellX, oldCellY);
                addBodyToIndexes(body, newCellX, newCellY);
            }
        }, getClass().getSimpleName() + "Listener");
    }

    @Override
    public void removeBody(@Nonnull Body body) {
        validateBody(body);

        if (bodyById.remove(body.getId()) == null) {
            throw new IllegalStateException("Can't find " + body + '.');
        }

        bodies.remove(body);
        removeBodyFromIndexes(body);
    }

    @Override
    public void removeBody(long id) {
        Body body;

        if ((body = bodyById.remove(id)) == null) {
            throw new IllegalStateException("Can't find Body {id=" + id + "}.");
        }

        bodies.remove(body);
        removeBodyFromIndexes(body);
    }

    @Override
    public void removeBodyQuietly(@Nullable Body body) {
        if (body == null) {
            return;
        }

        if (bodyById.remove(body.getId()) == null) {
            return;
        }

        bodies.remove(body);
        removeBodyFromIndexes(body);
    }

    @Override
    public void removeBodyQuietly(long id) {
        Body body;

        if ((body = bodyById.remove(id)) == null) {
            return;
        }

        bodies.remove(body);
        removeBodyFromIndexes(body);
    }

    @Override
    public boolean hasBody(@Nonnull Body body) {
        validateBody(body);

        return bodies.contains(body);
    }

    @Override
    public boolean hasBody(long id) {
        return bodyById.containsKey(id);
    }

    @Override
    public Body getBody(long id) {
        return bodyById.get(id);
    }

    @Override
    public List<Body> getBodies() {
        return new UnmodifiableCollectionWrapperList<>(bodies);
    }

    /**
     * May not find all potential intersections for bodies whose size exceeds cell size.
     */
    @Override
    public List<Body> getPotentialIntersections(@Nonnull Body body) {
        validateBody(body);

        if (!bodies.contains(body)) {
            throw new IllegalStateException("Can't find " + body + '.');
        }

        List<Body> potentialIntersections = new ArrayList<>();

        if (!cellExceedingBodies.isEmpty()) {
            for (Body otherBody : cellExceedingBodies) {
                addPotentialIntersection(body, otherBody, potentialIntersections);
            }
        }

        int cellX;
        int cellY;

        if (body.getId() >= 0 && body.getId() <= MAX_FAST_CELL_BODY_ID) {
            @SuppressWarnings("NumericCastThatLosesPrecision") int bodyId = (int) body.getId();
            cellX = fastCellXByBodyId[bodyId];
            cellY = fastCellYByBodyId[bodyId];
        } else {
            cellX = getCellX(body.getX());
            cellY = getCellY(body.getY());
        }

        for (int cellOffsetX = -1; cellOffsetX <= 1; ++cellOffsetX) {
            for (int cellOffsetY = -1; cellOffsetY <= 1; ++cellOffsetY) {
                Body[] cellBodies = getCellBodies(cellX + cellOffsetX, cellY + cellOffsetY);
                addPotentialIntersections(body, cellBodies, potentialIntersections);
            }
        }

        return Collections.unmodifiableList(potentialIntersections);
    }

    private static void addPotentialIntersections(
            @Nonnull Body body, @Nullable Body[] bodies, @Nonnull List<Body> potentialIntersections) {
        if (bodies == null) {
            return;
        }

        for (int bodyIndex = 0, bodyCount = bodies.length; bodyIndex < bodyCount; ++bodyIndex) {
            addPotentialIntersection(body, bodies[bodyIndex], potentialIntersections);
        }
    }

    private static void addPotentialIntersection(
            @Nonnull Body body, @Nonnull Body otherBody, @Nonnull List<Body> potentialIntersections) {
        if (otherBody.equals(body)) {
            return;
        }

        if (body.isStatic() && otherBody.isStatic()) {
            return;
        }

        if (sqr(otherBody.getForm().getCircumcircleRadius() + body.getForm().getCircumcircleRadius())
                < otherBody.getSquaredDistanceTo(body)) {
            return;
        }

        potentialIntersections.add(otherBody);
    }

    private void rebuildIndexes() {
        for (int cellX = MIN_FAST_X; cellX <= MAX_FAST_X; ++cellX) {
            for (int cellY = MIN_FAST_Y; cellY <= MAX_FAST_Y; ++cellY) {
                bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y] = null;
            }
        }

        bodiesByCell.clear();
        cellExceedingBodies.clear();

        bodies.forEach(this::addBodyToIndexes);
    }

    private void addBodyToIndexes(@Nonnull Body body) {
        double radius = body.getForm().getCircumcircleRadius();
        double diameter = 2.0D * radius;

        if (diameter > cellSize) {
            if (!cellExceedingBodies.add(body)) {
                throw new IllegalStateException("Can't add Body {id=" + body.getId() + "} to index.");
            }
        } else {
            addBodyToIndexes(body, getCellX(body.getX()), getCellY(body.getY()));
        }
    }

    private void addBodyToIndexes(@Nonnull Body body, int cellX, int cellY) {
        if (cellX >= MIN_FAST_X && cellX <= MAX_FAST_X && cellY >= MIN_FAST_Y && cellY <= MAX_FAST_Y) {
            Body[] cellBodies = bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y];
            cellBodies = addBodyToCell(cellBodies, body);
            bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y] = cellBodies;
        } else {
            IntPair cell = new IntPair(cellX, cellY);
            Body[] cellBodies = bodiesByCell.get(cell);
            cellBodies = addBodyToCell(cellBodies, body);
            bodiesByCell.put(cell, cellBodies);
        }

        if (body.getId() >= 0 && body.getId() <= MAX_FAST_CELL_BODY_ID) {
            @SuppressWarnings("NumericCastThatLosesPrecision") int bodyId = (int) body.getId();
            fastCellXByBodyId[bodyId] = cellX;
            fastCellYByBodyId[bodyId] = cellY;
            fastCellLeftTopByBodyId[bodyId] = new Point2D(cellX * cellSize, cellY * cellSize);
            fastCellRightBottomByBodyId[bodyId] = fastCellLeftTopByBodyId[bodyId].copy().add(cellSize, cellSize);
        }
    }

    private void removeBodyFromIndexes(@Nonnull Body body) {
        double radius = body.getForm().getCircumcircleRadius();
        double diameter = 2.0D * radius;

        if (diameter > cellSize) {
            if (!cellExceedingBodies.remove(body)) {
                throw new IllegalStateException("Can't remove Body {id=" + body.getId() + "} from index.");
            }
        } else {
            removeBodyFromIndexes(body, getCellX(body.getX()), getCellY(body.getY()));
        }
    }

    private void removeBodyFromIndexes(@Nonnull Body body, int cellX, int cellY) {
        if (cellX >= MIN_FAST_X && cellX <= MAX_FAST_X && cellY >= MIN_FAST_Y && cellY <= MAX_FAST_Y) {
            Body[] cellBodies = bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y];
            cellBodies = removeBodyFromCell(cellBodies, body);
            bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y] = cellBodies;
        } else {
            IntPair cell = new IntPair(cellX, cellY);
            Body[] cellBodies = bodiesByCell.get(cell);
            cellBodies = removeBodyFromCell(cellBodies, body);

            if (cellBodies == null) {
                bodiesByCell.remove(cell);
            } else {
                bodiesByCell.put(cell, cellBodies);
            }
        }
    }

    @Nonnull
    private static Body[] addBodyToCell(@Nullable Body[] cellBodies, @Nonnull Body body) {
        if (cellBodies == null) {
            return new Body[]{body};
        }

        int bodyIndex = ArrayUtils.indexOf(cellBodies, body);
        if (bodyIndex != ArrayUtils.INDEX_NOT_FOUND) {
            throw new IllegalStateException("Can't add Body {id=" + body.getId() + "} to index.");
        }

        int bodyCount = cellBodies.length;
        Body[] newCellBodies = new Body[bodyCount + 1];
        System.arraycopy(cellBodies, 0, newCellBodies, 0, bodyCount);
        newCellBodies[bodyCount] = body;
        return newCellBodies;
    }

    @Nullable
    private static Body[] removeBodyFromCell(@Nonnull Body[] cellBodies, @Nonnull Body body) {
        int bodyIndex = ArrayUtils.indexOf(cellBodies, body);
        if (bodyIndex == ArrayUtils.INDEX_NOT_FOUND) {
            throw new IllegalStateException("Can't remove Body {id=" + body.getId() + "} from index.");
        }

        int bodyCount = cellBodies.length;
        if (bodyCount == 1) {
            return null;
        }

        Body[] newCellBodies = new Body[bodyCount - 1];
        System.arraycopy(cellBodies, 0, newCellBodies, 0, bodyIndex);
        System.arraycopy(cellBodies, bodyIndex + 1, newCellBodies, bodyIndex, bodyCount - bodyIndex - 1);
        return newCellBodies;
    }

    @Nullable
    private Body[] getCellBodies(int cellX, int cellY) {
        if (cellX >= MIN_FAST_X && cellX <= MAX_FAST_X && cellY >= MIN_FAST_Y && cellY <= MAX_FAST_Y) {
            return bodiesByCellXY[cellX - MIN_FAST_X][cellY - MIN_FAST_Y];
        } else {
            return bodiesByCell.get(new IntPair(cellX, cellY));
        }
    }

    private int getCellX(double x) {
        return NumberUtil.toInt(floor(x / cellSize));
    }

    private int getCellY(double y) {
        return NumberUtil.toInt(floor(y / cellSize));
    }

    private static final class UnmodifiableCollectionWrapperList<E> implements List<E> {
        private final Collection<E> collection;

        private UnmodifiableCollectionWrapperList(Collection<E> collection) {
            this.collection = collection;
        }

        @Override
        public int size() {
            return collection.size();
        }

        @Override
        public boolean isEmpty() {
            return collection.isEmpty();
        }

        @Override
        public boolean contains(Object object) {
            return collection.contains(object);
        }

        @Nonnull
        @Override
        public Iterator<E> iterator() {
            Iterator<E> iterator = collection.iterator();

            return new UnmodifiableIterator<E>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public E next() {
                    return iterator.next();
                }
            };
        }

        @Nonnull
        @Override
        public Object[] toArray() {
            return collection.toArray();
        }

        @SuppressWarnings("SuspiciousToArrayCall")
        @Nonnull
        @Override
        public <T> T[] toArray(@Nonnull T[] array) {
            return collection.toArray(array);
        }

        @Contract("_ -> fail")
        @Override
        public boolean add(E element) {
            throw new UnsupportedOperationException();
        }

        @Contract("_ -> fail")
        @Override
        public boolean remove(Object object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(@Nonnull Collection<?> collection) {
            return this.collection.containsAll(collection);
        }

        @Contract("_ -> fail")
        @Override
        public boolean addAll(@Nonnull Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        @Contract("_, _ -> fail")
        @Override
        public boolean addAll(int index, @Nonnull Collection<? extends E> collection) {
            throw new UnsupportedOperationException();
        }

        @Contract("_ -> fail")
        @Override
        public boolean removeAll(@Nonnull Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Contract("_ -> fail")
        @Override
        public boolean retainAll(@Nonnull Collection<?> collection) {
            throw new UnsupportedOperationException();
        }

        @Contract(" -> fail")
        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public E get(int index) {
            if (collection instanceof List) {
                return ((List<E>) collection).get(index);
            }

            if (index < 0 || index >= collection.size()) {
                throw new IndexOutOfBoundsException("Illegal index: " + index + ", size: " + collection.size() + '.');
            }

            Iterator<E> iterator = collection.iterator();

            for (int i = 0; i < index; ++i) {
                iterator.next();
            }

            return iterator.next();
        }

        @Contract("_, _ -> fail")
        @Override
        public E set(int index, E element) {
            throw new UnsupportedOperationException();
        }

        @Contract("_, _ -> fail")
        @Override
        public void add(int index, E element) {
            throw new UnsupportedOperationException();
        }

        @Contract("_ -> fail")
        @Override
        public E remove(int index) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int indexOf(Object o) {
            Iterator<E> iterator = collection.iterator();
            int index = 0;

            if (o == null) {
                while (iterator.hasNext()) {
                    if (iterator.next() == null) {
                        return index;
                    }
                    ++index;
                }
            } else {
                while (iterator.hasNext()) {
                    if (o.equals(iterator.next())) {
                        return index;
                    }
                    ++index;
                }
            }

            return -1;
        }

        @Override
        public int lastIndexOf(Object o) {
            if (collection instanceof List) {
                return ((List) collection).lastIndexOf(o);
            }

            Iterator<E> iterator = collection.iterator();
            int index = 0;
            int lastIndex = -1;

            if (o == null) {
                while (iterator.hasNext()) {
                    if (iterator.next() == null) {
                        lastIndex = index;
                    }
                    ++index;
                }
            } else {
                while (iterator.hasNext()) {
                    if (o.equals(iterator.next())) {
                        lastIndex = index;
                    }
                    ++index;
                }
            }

            return lastIndex;
        }

        @Nonnull
        @Override
        public ListIterator<E> listIterator() {
            return collection instanceof List
                    ? Collections.unmodifiableList((List<E>) collection).listIterator()
                    : Collections.unmodifiableList(new ArrayList<>(collection)).listIterator();
        }

        @Nonnull
        @Override
        public ListIterator<E> listIterator(int index) {
            return collection instanceof List
                    ? Collections.unmodifiableList((List<E>) collection).listIterator(index)
                    : Collections.unmodifiableList(new ArrayList<>(collection)).listIterator(index);
        }

        @Nonnull
        @Override
        public List<E> subList(int fromIndex, int toIndex) {
            return collection instanceof List
                    ? Collections.unmodifiableList(((List<E>) collection).subList(fromIndex, toIndex))
                    : Collections.unmodifiableList(new ArrayList<>(collection)).subList(fromIndex, toIndex);
        }
    }
}
