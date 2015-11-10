package com.codegame.codeseries.notreal2d.bodylist;

import com.codegame.codeseries.notreal2d.Body;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;

import static com.codeforces.commons.math.Math.sqr;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
@NotThreadSafe
public class SimpleBodyList extends BodyListBase {
    private final List<Body> bodies = new LinkedList<>();

    @Override
    public void addBody(@Nonnull Body body) {
        validateBody(body);

        if (bodies.contains(body)) {
            throw new IllegalStateException(body + " is already added.");
        }

        bodies.add(body);
    }

    @Override
    public void removeBody(@Nonnull Body body) {
        validateBody(body);

        for (Iterator<Body> bodyIterator = bodies.iterator(); bodyIterator.hasNext(); ) {
            if (bodyIterator.next().equals(body)) {
                bodyIterator.remove();
                return;
            }
        }

        throw new IllegalStateException("Can't find " + body + '.');
    }

    @Override
    public void removeBody(long id) {
        for (Iterator<Body> bodyIterator = bodies.iterator(); bodyIterator.hasNext(); ) {
            if (bodyIterator.next().getId() == id) {
                bodyIterator.remove();
                return;
            }
        }

        throw new IllegalStateException("Can't find Body {id=" + id + "}.");
    }

    @Override
    public void removeBodyQuietly(@Nullable Body body) {
        if (body == null) {
            return;
        }

        for (Iterator<Body> bodyIterator = bodies.iterator(); bodyIterator.hasNext(); ) {
            if (bodyIterator.next().equals(body)) {
                bodyIterator.remove();
                return;
            }
        }
    }

    @Override
    public void removeBodyQuietly(long id) {
        for (Iterator<Body> bodyIterator = bodies.iterator(); bodyIterator.hasNext(); ) {
            if (bodyIterator.next().getId() == id) {
                bodyIterator.remove();
                return;
            }
        }
    }

    @Override
    public boolean hasBody(@Nonnull Body body) {
        validateBody(body);

        return bodies.contains(body);
    }

    @Override
    public boolean hasBody(long id) {
        for (Body body : bodies) {
            if (body.getId() == id) {
                return true;
            }
        }

        return false;
    }

    @Nullable
    @Override
    public Body getBody(long id) {
        for (Body body : bodies) {
            if (body.getId() == id) {
                return body;
            }
        }

        return null;
    }

    @Override
    public List<Body> getBodies() {
        return Collections.unmodifiableList(bodies);
    }

    @Override
    public List<Body> getPotentialIntersections(@Nonnull Body body) {
        validateBody(body);

        List<Body> potentialIntersections = new ArrayList<>();
        boolean exists = false;

        for (Body otherBody : bodies) {
            if (otherBody.equals(body)) {
                exists = true;
                continue;
            }

            if (body.isStatic() && otherBody.isStatic()) {
                continue;
            }

            if (sqr(otherBody.getForm().getCircumcircleRadius() + body.getForm().getCircumcircleRadius())
                    < otherBody.getSquaredDistanceTo(body)) {
                continue;
            }

            potentialIntersections.add(otherBody);
        }

        if (!exists) {
            throw new IllegalStateException("Can't find " + body + '.');
        }

        return Collections.unmodifiableList(potentialIntersections);
    }
}
