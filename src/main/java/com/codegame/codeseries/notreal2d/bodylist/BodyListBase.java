package com.codegame.codeseries.notreal2d.bodylist;

import com.codegame.codeseries.notreal2d.Body;
import org.jetbrains.annotations.Contract;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 26.08.2015
 */
public abstract class BodyListBase implements BodyList {
    @Contract("null -> fail")
    protected static void validateBody(Body body) {
        if (body == null) {
            throw new IllegalArgumentException("Argument 'body' is null.");
        }
    }
}
