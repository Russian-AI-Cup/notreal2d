package com.codegame.codeseries.notreal2d.provider;

import com.codegame.codeseries.notreal2d.Body;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 04.06.2015
 */
public interface MovementFrictionProvider {
    void applyFriction(Body body, double updateFactor);
}
