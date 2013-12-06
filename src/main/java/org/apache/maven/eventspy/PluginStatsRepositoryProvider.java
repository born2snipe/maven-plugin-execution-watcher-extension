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

import co.leantechniques.maven.PluginStatsRepository;
import co.leantechniques.maven.h2.H2PluginStatsRepository;
import org.openide.util.Lookup;

public class PluginStatsRepositoryProvider {
    private Lookup lookup;

    public PluginStatsRepositoryProvider() {
        this(Lookup.getDefault());
    }

    public PluginStatsRepositoryProvider(Lookup lookup) {
        this.lookup = lookup;
    }

    public PluginStatsRepository provide() {
        PluginStatsRepository pluginStatsRepository = lookup.lookup(PluginStatsRepository.class);
        if (pluginStatsRepository == null) {
            pluginStatsRepository = new H2PluginStatsRepository();
        }
        return pluginStatsRepository;
    }
}
