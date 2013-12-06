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

import co.leantechniques.maven.PluginStatsFactory;
import co.leantechniques.maven.PluginStatsRepository;
import org.apache.maven.execution.ExecutionEvent;
import org.codehaus.plexus.component.annotations.Component;

@Component(role = EventSpy.class)
public class PluginWatcherEventSpy extends AbstractEventSpy {
    public static final String BUILD_DATA_KEY = "plugin.execution.watcher.build.data";

    private PluginStatsFactory pluginStatsFactory = new PluginStatsFactory();
    private PluginStatsRepositoryProvider pluginStatsRepositoryProvider = new PluginStatsRepositoryProvider();
    private PluginStatsRepository pluginStatsRepository;

    @Override
    public void init(Context context) throws Exception {
        pluginStatsRepository = pluginStatsRepositoryProvider.provide();
        pluginStatsRepository.initialize(context);
    }

    @Override
    public void onEvent(Object event) throws Exception {
        if (event instanceof ExecutionEvent) {
            ExecutionEvent executionEvent = (ExecutionEvent) event;
            if (isPluginRelated(executionEvent)) {
                pluginStatsRepository.save(pluginStatsFactory.build(executionEvent));
            } else if (isBuildStarting(executionEvent)) {
                pluginStatsRepository.saveBuildStarted(executionEvent.getSession(), System.getProperty(BUILD_DATA_KEY));
            } else if (isBuildFinished(executionEvent)) {
                pluginStatsRepository.saveBuildFinished(executionEvent.getSession());
            }
        }
    }

    @Override
    public void close() throws Exception {
        pluginStatsRepository.finished();
    }

    private boolean isBuildFinished(ExecutionEvent executionEvent) {
        return executionEvent.getType() == ExecutionEvent.Type.SessionEnded;
    }

    private boolean isBuildStarting(ExecutionEvent executionEvent) {
        return executionEvent.getType() == ExecutionEvent.Type.SessionStarted;
    }

    private boolean isPluginRelated(ExecutionEvent event) {
        ExecutionEvent.Type type = event.getType();
        switch (type) {
            case MojoSucceeded:
            case MojoFailed:
            case MojoStarted:
                return true;
        }
        return false;
    }
}
