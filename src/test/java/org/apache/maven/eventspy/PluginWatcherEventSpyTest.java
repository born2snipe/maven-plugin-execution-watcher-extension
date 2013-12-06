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

import co.leantechniques.maven.PluginStats;
import co.leantechniques.maven.PluginStatsFactory;
import co.leantechniques.maven.PluginStatsRepository;
import co.leantechniques.maven.PluginStatsRepositoryProvider;
import org.apache.maven.execution.ExecutionEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openide.util.Lookup;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PluginWatcherEventSpyTest {
    @Mock
    private PluginStatsRepositoryProvider pluginStatsRepositoryProvider;
    @Mock
    PluginStatsFactory pluginStatsFactory;
    @Mock
    EventSpy.Context context;
    @Mock
    private PluginStatsRepository statsRepository;
    @Mock
    private Lookup lookup;
    @InjectMocks
    private PluginWatcherEventSpy spy = new PluginWatcherEventSpy();
    private ExecutionEventBuilder executionEventBuilder;

    @Before
    public void setUp() throws Exception {
        System.getProperties().remove("plugin.execution.watcher.build.data");

        executionEventBuilder = new ExecutionEventBuilder();

        when(pluginStatsRepositoryProvider.provide()).thenReturn(statsRepository);
    }

    @Test
    public void close_shouldNotifyTheRepositoryTheBuildIsComplete() throws Exception {
        spy.close();

        verify(statsRepository).finished();
    }

    @Test
    public void onEvent_shouldIgnoreTypesThatAreNotMojoRelated() throws Exception {
        expectPluginStatsToBeNotSaved(ExecutionEvent.Type.ForkedProjectFailed);
        expectPluginStatsToBeNotSaved(ExecutionEvent.Type.MojoSkipped);
    }

    @Test
    public void onEvent_shouldStoreWhenTheExecutionEventTypeIsMojoRelated() throws Exception {
        expectPluginStatsToBeSaved(ExecutionEvent.Type.MojoSucceeded);
        expectPluginStatsToBeSaved(ExecutionEvent.Type.MojoStarted);
        expectPluginStatsToBeSaved(ExecutionEvent.Type.MojoFailed);
    }

    @Test
    public void onEvent_shouldStoreWhenASessionStarts_withAdditionalBuildData() throws Exception {
        System.setProperty("plugin.execution.watcher.build.data", "build-data");

        executionEventBuilder.expectEventType(ExecutionEvent.Type.SessionStarted);
        ExecutionEvent event = executionEventBuilder.toEvent();

        spy.onEvent(event);

        verify(statsRepository).saveBuildStarted(event.getSession(), "build-data");
    }

    @Test
    public void onEvent_shouldStoreWhenASessionStarts() throws Exception {
        executionEventBuilder.expectEventType(ExecutionEvent.Type.SessionStarted);
        ExecutionEvent event = executionEventBuilder.toEvent();

        spy.onEvent(event);

        verify(statsRepository).saveBuildStarted(event.getSession(), null);
    }

    @Test
    public void onEvent_shouldStoreWhenASessionEnds() throws Exception {
        executionEventBuilder.expectEventType(ExecutionEvent.Type.SessionEnded);
        ExecutionEvent event = executionEventBuilder.toEvent();

        spy.onEvent(event);

        verify(statsRepository).saveBuildFinished(event.getSession());
    }

    @Test
    public void onEvent_shouldIgnoreAnythingOtherThanExecutionEvents() throws Exception {
        spy.onEvent("test");

        verifyZeroInteractions(statsRepository);
    }

    @Test
    public void init_shouldUseTheProvidedStatRepositoryFound() throws Exception {
        spy.init(context);

        verify(statsRepository).initialize(context);
    }

    private void expectPluginStatsToBeSaved(ExecutionEvent.Type expectedType) throws Exception {
        PluginStats stats = new PluginStats();

        executionEventBuilder.expectEventType(expectedType);
        ExecutionEvent event = executionEventBuilder.toEvent();

        when(pluginStatsFactory.build(event)).thenReturn(stats);

        spy.onEvent(event);

        verify(statsRepository).save(stats);
    }

    private void expectPluginStatsToBeNotSaved(ExecutionEvent.Type expectedType) throws Exception {
        executionEventBuilder.expectEventType(expectedType);

        spy.onEvent(executionEventBuilder.toEvent());

        verifyZeroInteractions(statsRepository, pluginStatsFactory);
    }
}
