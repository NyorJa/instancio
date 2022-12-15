/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.instancio.generator;

import org.instancio.generator.hints.CollectionHint;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class CollectionHintTest {

    @Test
    void verifyToString() {
        assertThat(CollectionHint.builder().build())
                .hasToString("CollectionHint[generateElements=0," +
                        " nullableElements=false," +
                        " shuffle=false," +
                        " withElements=[]]");

        assertThat(CollectionHint.builder()
                .generateElements(3)
                .nullableElements(true)
                .shuffle(true)
                .withElements(Arrays.asList("foo", "bar"))
                .build())
                .hasToString("CollectionHint[generateElements=3," +
                        " nullableElements=true," +
                        " shuffle=true," +
                        " withElements=[foo, bar]]");
    }
}