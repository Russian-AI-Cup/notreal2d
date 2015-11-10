package com.codegame.codeseries.notreal2d.bodylist;

import com.codegame.codeseries.notreal2d.Body;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
public interface BodyList {
    void addBody(@Nonnull Body body);

    void removeBody(@Nonnull Body body);
    void removeBody(long id);
    void removeBodyQuietly(@Nullable Body body);
    void removeBodyQuietly(long id);

    boolean hasBody(@Nonnull Body body);
    boolean hasBody(long id);

    Body getBody(long id);
    List<Body> getBodies();

    List<Body> getPotentialIntersections(@Nonnull Body body);
}
