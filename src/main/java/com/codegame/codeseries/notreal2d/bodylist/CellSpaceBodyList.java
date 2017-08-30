package com.codegame.codeseries.notreal2d.bodylist;

import com.codeforces.commons.codec.PackUtil;
import com.codeforces.commons.collection.CollectionUtil;
import com.codeforces.commons.geometry.Point2D;
import com.codeforces.commons.math.NumberUtil;
import com.codegame.codeseries.notreal2d.Body;
import com.codegame.codeseries.notreal2d.listener.PositionListenerAdapter;
import com.google.common.collect.UnmodifiableIterator;
import gnu.trove.map.TLongObjectMap;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import static com.codeforces.commons.math.Math.*;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 * Date: 02.06.2015
 */
@NotThreadSafe
public class CellSpaceBodyList extends BodyListBase {
    private static final int MIN_FAST_X = -1000;
    private static final int MAX_FAST_X = 1000;
    private static final int MIN_FAST_Y = -1000;
    private static final int MAX_FAST_Y = 1000;

    private static final int FAST_COLUMN_COUNT = MAX_FAST_X - MIN_FAST_X + 1;
    private static final int FAST_ROW_COUNT = MAX_FAST_Y - MIN_FAST_Y + 1;

    private static final int MAX_FAST_BODY_ID = 9999;

    private final TLongObjectMap<Body> bodyById = CollectionUtil.newTLongObjectMap();

    private final Body[] fastBodies = new Body[MAX_FAST_BODY_ID + 1];
    private final int[] fastCellXByBodyId = new int[MAX_FAST_BODY_ID + 1];
    private final int[] fastCellYByBodyId = new int[MAX_FAST_BODY_ID + 1];
    private final Point2D[] fastCellLeftTopByBodyId = new Point2D[MAX_FAST_BODY_ID + 1];
    private final Point2D[] fastCellRightBottomByBodyId = new Point2D[MAX_FAST_BODY_ID + 1];

    private final Body[][] bodiesByCellXY = new Body[FAST_COLUMN_COUNT * FAST_ROW_COUNT][];
    private final TLongObjectMap<Body[]> bodiesByCell = CollectionUtil.newTLongObjectMap();
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
        long id = body.getId();

        if (hasBody(id)) {
            throw new IllegalStateException(body + " is already added.");
        }

        double radius = body.getForm().getCircumcircleRadius();
        double diameter = 2.0D * radius;

        if (diameter > cellSize && diameter <= maxCellSize) {
            cellSize = diameter;
            rebuildIndexes();
        }

        bodyById.put(id, body);
        addBodyToIndexes(body);

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            @SuppressWarnings("NumericCastThatLosesPrecision") int fastId = (int) id;
            fastBodies[fastId] = body;

            body.getCurrentState().registerPositionListener(new PositionListenerAdapter() {
                private final Lock listenerLock = new ReentrantLock();

                @Override
                public void afterChangePosition(@Nonnull Point2D oldPosition, @Nonnull Point2D newPosition) {
                    if (diameter > cellSize) {
                        return;
                    }

                    Point2D cellLeftTop = fastCellLeftTopByBodyId[fastId];
                    Point2D cellRightBottom = fastCellRightBottomByBodyId[fastId];

                    Point2D position = body.getPosition();

                    if (position.getX() >= cellLeftTop.getX() && position.getY() >= cellLeftTop.getY()
                            && position.getX() < cellRightBottom.getX() && position.getY() < cellRightBottom.getY()) {
                        return;
                    }

                    int oldCellX = getCellX(oldPosition.getX());
                    int oldCellY = getCellY(oldPosition.getY());

                    int newCellX = getCellX(newPosition.getX());
                    int newCellY = getCellY(newPosition.getY());

                    listenerLock.lock();
                    try {
                        removeBodyFromIndexes(body, oldCellX, oldCellY);
                        addBodyToIndexes(body, newCellX, newCellY);
                    } finally {
                        listenerLock.unlock();
                    }
                }
            }, getClass().getSimpleName() + "Listener");
        } else {
            body.getCurrentState().registerPositionListener(new PositionListenerAdapter() {
                private final Lock listenerLock = new ReentrantLock();

                @Override
                public void afterChangePosition(@Nonnull Point2D oldPosition, @Nonnull Point2D newPosition) {
                    if (diameter > cellSize) {
                        return;
                    }

                    int oldCellX = getCellX(oldPosition.getX());
                    int oldCellY = getCellY(oldPosition.getY());

                    int newCellX = getCellX(newPosition.getX());
                    int newCellY = getCellY(newPosition.getY());

                    if (oldCellX == newCellX && oldCellY == newCellY) {
                        return;
                    }

                    listenerLock.lock();
                    try {
                        removeBodyFromIndexes(body, oldCellX, oldCellY);
                        addBodyToIndexes(body, newCellX, newCellY);
                    } finally {
                        listenerLock.unlock();
                    }
                }
            }, getClass().getSimpleName() + "Listener");
        }
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public void removeBody(@Nonnull Body body) {
        validateBody(body);
        long id = body.getId();

        if (bodyById.remove(id) == null) {
            throw new IllegalStateException("Can't find " + body + '.');
        }

        removeBodyFromIndexes(body);

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            fastBodies[(int) id] = null;
        }
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public void removeBody(long id) {
        Body body;

        if ((body = bodyById.remove(id)) == null) {
            throw new IllegalStateException("Can't find Body {id=" + id + "}.");
        }

        removeBodyFromIndexes(body);

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            fastBodies[(int) id] = null;
        }
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public void removeBodyQuietly(@Nullable Body body) {
        if (body == null) {
            return;
        }

        long id = body.getId();

        if (bodyById.remove(id) == null) {
            return;
        }

        removeBodyFromIndexes(body);

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            fastBodies[(int) id] = null;
        }
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public void removeBodyQuietly(long id) {
        Body body;

        if ((body = bodyById.remove(id)) == null) {
            return;
        }

        removeBodyFromIndexes(body);

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            fastBodies[(int) id] = null;
        }
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public boolean hasBody(@Nonnull Body body) {
        validateBody(body);

        long id = body.getId();
        return id >= 0L && id <= MAX_FAST_BODY_ID ? fastBodies[(int) id] != null : bodyById.containsKey(id);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public boolean hasBody(long id) {
        return id >= 0L && id <= MAX_FAST_BODY_ID ? fastBodies[(int) id] != null : bodyById.containsKey(id);
    }

    @SuppressWarnings("NumericCastThatLosesPrecision")
    @Override
    public Body getBody(long id) {
        return id >= 0L && id <= MAX_FAST_BODY_ID ? fastBodies[(int) id] : bodyById.get(id);
    }

    @Override
    public List<Body> getBodies() {
        return new UnmodifiableCollectionWrapperList<>(bodyById.valueCollection());
    }

    /**
     * May not find all potential intersections for bodies whose size exceeds cell size.
     */
    @SuppressWarnings("OverlyLongMethod")
    @Override
    public List<Body> getPotentialIntersections(@Nonnull Body body) {
        validateBody(body);
        long id = body.getId();

        if (!hasBody(id)) {
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

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            @SuppressWarnings("NumericCastThatLosesPrecision") int fastId = (int) id;
            cellX = fastCellXByBodyId[fastId];
            cellY = fastCellYByBodyId[fastId];
        } else {
            cellX = getCellX(body.getX());
            cellY = getCellY(body.getY());
        }

        if (body.isStatic()) {
            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX - 1, cellY - 1), potentialIntersections);
            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX - 1, cellY), potentialIntersections);
            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX - 1, cellY + 1), potentialIntersections);

            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX, cellY - 1), potentialIntersections);
            addPotentialIntersectionsStatic(body, getCellBodies(cellX, cellY), potentialIntersections);
            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX, cellY + 1), potentialIntersections);

            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX + 1, cellY - 1), potentialIntersections);
            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX + 1, cellY), potentialIntersections);
            fastAddPotentialIntersectionsStatic(body, getCellBodies(cellX + 1, cellY + 1), potentialIntersections);
        } else {
            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX - 1, cellY - 1), potentialIntersections);
            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX - 1, cellY), potentialIntersections);
            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX - 1, cellY + 1), potentialIntersections);

            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX, cellY - 1), potentialIntersections);
            addPotentialIntersectionsNotStatic(body, getCellBodies(cellX, cellY), potentialIntersections);
            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX, cellY + 1), potentialIntersections);

            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX + 1, cellY - 1), potentialIntersections);
            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX + 1, cellY), potentialIntersections);
            fastAddPotentialIntersectionsNotStatic(body, getCellBodies(cellX + 1, cellY + 1), potentialIntersections);
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

    private static void addPotentialIntersectionsStatic(
            @Nonnull Body body, @Nullable Body[] bodies, @Nonnull List<Body> potentialIntersections) {
        if (bodies == null) {
            return;
        }

        for (int bodyIndex = 0, bodyCount = bodies.length; bodyIndex < bodyCount; ++bodyIndex) {
            addPotentialIntersectionStatic(body, bodies[bodyIndex], potentialIntersections);
        }
    }

    private static void addPotentialIntersectionStatic(
            @Nonnull Body body, @Nonnull Body otherBody, @Nonnull List<Body> potentialIntersections) {
        if (otherBody.equals(body)) {
            return;
        }

        if (otherBody.isStatic()) {
            return;
        }

        if (sqr(otherBody.getForm().getCircumcircleRadius() + body.getForm().getCircumcircleRadius())
                < otherBody.getSquaredDistanceTo(body)) {
            return;
        }

        potentialIntersections.add(otherBody);
    }

    private static void addPotentialIntersectionsNotStatic(
            @Nonnull Body body, @Nullable Body[] bodies, @Nonnull List<Body> potentialIntersections) {
        if (bodies == null) {
            return;
        }

        for (int bodyIndex = 0, bodyCount = bodies.length; bodyIndex < bodyCount; ++bodyIndex) {
            addPotentialIntersectionNotStatic(body, bodies[bodyIndex], potentialIntersections);
        }
    }

    private static void addPotentialIntersectionNotStatic(
            @Nonnull Body body, @Nonnull Body otherBody, @Nonnull List<Body> potentialIntersections) {
        if (otherBody.equals(body)) {
            return;
        }

        if (sqr(otherBody.getForm().getCircumcircleRadius() + body.getForm().getCircumcircleRadius())
                < otherBody.getSquaredDistanceTo(body)) {
            return;
        }

        potentialIntersections.add(otherBody);
    }

    private static void fastAddPotentialIntersectionsStatic(
            @Nonnull Body body, @Nullable Body[] bodies, @Nonnull List<Body> potentialIntersections) {
        if (bodies == null) {
            return;
        }

        for (int bodyIndex = 0, bodyCount = bodies.length; bodyIndex < bodyCount; ++bodyIndex) {
            fastAddPotentialIntersectionStatic(body, bodies[bodyIndex], potentialIntersections);
        }
    }

    private static void fastAddPotentialIntersectionStatic(
            @Nonnull Body body, @Nonnull Body otherBody, @Nonnull List<Body> potentialIntersections) {
        if (otherBody.isStatic()) {
            return;
        }

        if (sqr(otherBody.getForm().getCircumcircleRadius() + body.getForm().getCircumcircleRadius())
                < otherBody.getSquaredDistanceTo(body)) {
            return;
        }

        potentialIntersections.add(otherBody);
    }

    private static void fastAddPotentialIntersectionsNotStatic(
            @Nonnull Body body, @Nullable Body[] bodies, @Nonnull List<Body> potentialIntersections) {
        if (bodies == null) {
            return;
        }

        for (int bodyIndex = 0, bodyCount = bodies.length; bodyIndex < bodyCount; ++bodyIndex) {
            fastAddPotentialIntersectionNotStatic(body, bodies[bodyIndex], potentialIntersections);
        }
    }

    private static void fastAddPotentialIntersectionNotStatic(
            @Nonnull Body body, @Nonnull Body otherBody, @Nonnull List<Body> potentialIntersections) {
        if (sqr(otherBody.getForm().getCircumcircleRadius() + body.getForm().getCircumcircleRadius())
                < otherBody.getSquaredDistanceTo(body)) {
            return;
        }

        potentialIntersections.add(otherBody);
    }

    private void rebuildIndexes() {
        for (int cellY = MIN_FAST_Y; cellY <= MAX_FAST_Y; ++cellY) {
            int rowOffset = (cellY - MIN_FAST_Y) * FAST_COLUMN_COUNT;

            for (int cellX = MIN_FAST_X; cellX <= MAX_FAST_X; ++cellX) {
                bodiesByCellXY[rowOffset + cellX - MIN_FAST_X] = null;
            }
        }

        bodiesByCell.clear();
        cellExceedingBodies.clear();

        bodyById.forEachValue(body -> {
            addBodyToIndexes(body);
            return true;
        });
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
            int cellXY = (cellY - MIN_FAST_Y) * FAST_COLUMN_COUNT + cellX - MIN_FAST_X;
            Body[] cellBodies = bodiesByCellXY[cellXY];
            cellBodies = addBodyToCell(cellBodies, body);
            bodiesByCellXY[cellXY] = cellBodies;
        } else {
            @SuppressWarnings("SuspiciousNameCombination") long cell = PackUtil.packInts(cellX, cellY);
            Body[] cellBodies = bodiesByCell.get(cell);
            cellBodies = addBodyToCell(cellBodies, body);
            bodiesByCell.put(cell, cellBodies);
        }

        long id = body.getId();

        if (id >= 0L && id <= MAX_FAST_BODY_ID) {
            @SuppressWarnings("NumericCastThatLosesPrecision") int fastId = (int) id;
            fastCellXByBodyId[fastId] = cellX;
            fastCellYByBodyId[fastId] = cellY;
            fastCellLeftTopByBodyId[fastId] = new Point2D(cellX * cellSize, cellY * cellSize);
            fastCellRightBottomByBodyId[fastId] = new Point2D((cellX + 1) * cellSize, (cellY + 1) * cellSize);
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
            int cellXY = (cellY - MIN_FAST_Y) * FAST_COLUMN_COUNT + cellX - MIN_FAST_X;
            Body[] cellBodies = bodiesByCellXY[cellXY];
            cellBodies = removeBodyFromCell(cellBodies, body);
            bodiesByCellXY[cellXY] = cellBodies;
        } else {
            @SuppressWarnings("SuspiciousNameCombination") long cell = PackUtil.packInts(cellX, cellY);
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
            return new Body[] {body};
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
            return bodiesByCellXY[(cellY - MIN_FAST_Y) * FAST_COLUMN_COUNT + cellX - MIN_FAST_X];
        } else {
            @SuppressWarnings("SuspiciousNameCombination") long cell = PackUtil.packInts(cellX, cellY);
            return bodiesByCell.get(cell);
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
