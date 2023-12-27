/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.wicket.markup.html.form;

import org.apache.wicket.util.tester.WicketTestCase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

/**
 */
class TestFormHasError extends WicketTestCase
{

	// Existing tests are now helper methods for the parameterized test
    private void executeTest(String linkId) {
        tester.startPage(FormHasErrorPage.class);
        tester.assertRenderedPage(FormHasErrorPage.class);
        tester.clickLink(linkId);
        tester.dumpPage();
    }

    // Parameterized test method
    @ParameterizedTest
    @MethodSource("linkIdsProvider")
    void testFormHasError(String linkId) {
        executeTest(linkId);
    }

    // Provide linkIds for parameterized test
    private static Stream<String> linkIdsProvider() {
        return Stream.of("form:submitForm", "form:submitFormComponent", "form:submitComponent");
    }
}