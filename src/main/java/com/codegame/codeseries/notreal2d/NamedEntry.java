package com.codegame.codeseries.notreal2d;

import com.codeforces.commons.text.StringUtil;
import org.jetbrains.annotations.Contract;

import javax.annotation.Nonnull;

/**
 * @author Maxim Shipko (sladethe@gmail.com)
 *         Date: 26.08.2015
 */
class NamedEntry {
    public final String name;

    NamedEntry(String name) {
        this.name = name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        NamedEntry namedEntry = (NamedEntry) o;

        return name.equals(namedEntry.name);
    }

    @Contract("null -> fail")
    static void validateName(@Nonnull String name) {
        if (StringUtil.isBlank(name)) {
            throw new IllegalArgumentException("Argument 'name' is blank.");
        }

        if (!StringUtil.trim(name).equals(name)) {
            throw new IllegalArgumentException(
                    "Argument 'name' should not contain neither leading nor trailing whitespace characters."
            );
        }
    }
}
