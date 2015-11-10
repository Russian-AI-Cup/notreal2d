package com.codegame.codeseries.notreal2d.form;

import javax.annotation.Nonnull;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 01.07.2015
 */
public abstract class ThinForm extends Form {
    private final boolean endpointCollisionEnabled;

    protected ThinForm(@Nonnull Shape shape, boolean endpointCollisionEnabled) {
        super(shape);

        this.endpointCollisionEnabled = endpointCollisionEnabled;
    }

    public boolean isEndpointCollisionEnabled() {
        return endpointCollisionEnabled;
    }
}
