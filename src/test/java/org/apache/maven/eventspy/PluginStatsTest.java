/**
 *
 * Copyright to the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */

package org.apache.maven.eventspy;

import org.apache.maven.execution.ExecutionEvent;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class PluginStatsTest {
    @Test
    public void types_shouldMapToMavensTypes() {
        assertEquals(PluginStats.Type.START, PluginStats.Type.valueOf(ExecutionEvent.Type.MojoStarted));
        assertEquals(PluginStats.Type.SUCCEED, PluginStats.Type.valueOf(ExecutionEvent.Type.MojoSucceeded));
        assertEquals(PluginStats.Type.FAILED, PluginStats.Type.valueOf(ExecutionEvent.Type.MojoFailed));
        assertNull(PluginStats.Type.valueOf((ExecutionEvent.Type)null));
    }
}
