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

package org.apache.maven.eventspy.h2;

import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.eventspy.PluginStats;
import org.apache.maven.eventspy.PluginStatsRepository;

public class H2PluginStatsRepository implements PluginStatsRepository {
    @Override
    public void initialize(EventSpy.Context context) {

    }

    @Override
    public void save(PluginStats pluginStats) {
        PluginStats.Plugin plugin = pluginStats.plugin;
        String plugInfo = plugin.groupId + ":" + plugin.artifactId + ":" + plugin.version + ":" + plugin.goal;
        System.out.println("SAVING -- " + plugInfo);
    }
}
