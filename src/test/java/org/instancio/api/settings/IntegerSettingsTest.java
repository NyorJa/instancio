package org.instancio.api.settings;

import org.instancio.Instancio;
import org.instancio.pojo.basic.IntegerHolder;
import org.instancio.pojo.collections.lists.ListInteger;
import org.instancio.settings.Setting;
import org.instancio.settings.Settings;
import org.instancio.testsupport.tags.SettingsTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SettingsTag
class IntegerSettingsTest {

    private static final int MIN_VALUE = 10000;
    private static final int MAX_VALUE = 10002;

    private static final Settings settings = Settings.create()
            .set(Setting.INTEGER_MIN, MIN_VALUE)
            .set(Setting.INTEGER_MAX, MAX_VALUE)
            // increase collection size for bigger sample
            .set(Setting.COLLECTION_MIN_SIZE, MIN_VALUE)
            .set(Setting.COLLECTION_MAX_SIZE, MAX_VALUE)
            .lock();


    @Test
    @DisplayName("Override MIN and MAX")
    void minMax() {
        final Settings overrides = settings.merge(Settings.create().set(Setting.STRING_NULLABLE, true));
        final Set<Integer> results = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            final IntegerHolder result = Instancio.of(IntegerHolder.class).withSettings(overrides).create();
            assertThat(result.getPrimitive()).isBetween(MIN_VALUE, MAX_VALUE);
            assertThat(result.getWrapper()).isBetween(MIN_VALUE, MAX_VALUE);
        }
    }

    @Test
    @DisplayName("Override nullable to true - generates null in Integer fields")
    void nullable() {
        final Settings overrides = settings.merge(Settings.create().set(Setting.INTEGER_NULLABLE, true));
        final Set<Integer> results = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            final IntegerHolder result = Instancio.of(IntegerHolder.class).withSettings(overrides).create();
            results.add(result.getWrapper());
        }
        assertThat(results).containsNull();
    }

    @Test
    @DisplayName("Override nullable to true - does not generate null in collection elements")
    void integerIsNotNullInCollections() {
        final Settings overrides = settings.merge(Settings.create().set(Setting.STRING_NULLABLE, true));
        final ListInteger result = Instancio.of(ListInteger.class).withSettings(overrides).create();
        assertThat(result.getList()).doesNotContainNull();
    }


}
