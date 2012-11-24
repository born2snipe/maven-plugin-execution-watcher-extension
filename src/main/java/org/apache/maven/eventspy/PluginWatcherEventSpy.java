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

import org.apache.maven.eventspy.h2.H2PluginStatsRepository;
import org.apache.maven.execution.ExecutionEvent;
import org.codehaus.plexus.component.annotations.Component;
import org.openide.util.Lookup;

@Component(role = EventSpy.class)
public class PluginWatcherEventSpy extends AbstractEventSpy {
    private Lookup lookup = Lookup.getDefault();
    private PluginStatsRepository pluginStatsRepository;
    private PluginStatsFactory pluginStatsFactory = new PluginStatsFactory();

    @Override
    public void init(Context context) throws Exception {
        pluginStatsRepository = lookup.lookup(PluginStatsRepository.class);
        if (pluginStatsRepository == null) {
            pluginStatsRepository = new H2PluginStatsRepository();
        }
        pluginStatsRepository.initialize(context);
    }

    @Override
    public void onEvent(Object event) throws Exception {
        if (event instanceof ExecutionEvent) {
            ExecutionEvent executionEvent = (ExecutionEvent) event;
            if (shouldBeSaved(executionEvent)) {
                pluginStatsRepository.save(pluginStatsFactory.build(executionEvent));
            }
        }
    }

    private boolean shouldBeSaved(ExecutionEvent event) {
        ExecutionEvent.Type type = event.getType();
        switch (type) {
            case MojoSucceeded:
            case MojoFailed:
            case MojoStarted:
                return true;
        }
        return false;
    }

    protected PluginStatsRepository getPluginStatsRepository() {
        return pluginStatsRepository;
    }
}
