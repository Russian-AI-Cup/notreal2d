package com.codegame.codeseries.notreal2d;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 02.06.2015
 */
public final class Defaults {
    public static final double EPSILON = 1.0E-7D;

    public static final int ITERATION_COUNT_PER_STEP = 10;
    public static final int STEP_COUNT_PER_TIME_UNIT = 60;

    private Defaults() {
        throw new UnsupportedOperationException();
    }
}
