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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openide.util.Lookup;

import static junit.framework.Assert.assertSame;
import static junit.framework.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PluginWatcherEventSpyTest {
    @Mock
    PluginStatsFactory pluginStatsFactory;
    @Mock
    EventSpy.Context context;
    @Mock
    ExecutionEvent executionEvent;
    @Mock
    private PluginStatsRepository statsRepository;
    @Mock
    private Lookup lookup;
    @InjectMocks
    private PluginWatcherEventSpy spy = new PluginWatcherEventSpy();

    @Before
    public void setUp() throws Exception {
        System.setProperty(PluginWatcherEventSpy.TURN_ON_KEY, "");
    }

    @Test
    public void onEvent_shouldDoNothingIfTHeTriggerPropertyIsNotGiven() throws Exception {
        System.getProperties().remove(PluginWatcherEventSpy.TURN_ON_KEY);

        spy.onEvent(executionEvent);

        verifyZeroInteractions(statsRepository, pluginStatsFactory);
    }

    @Test
    public void init_shouldDoNothingIfTheTriggerPropertyIsNotGiven() throws Exception {
        System.getProperties().remove(PluginWatcherEventSpy.TURN_ON_KEY);
        when(lookup.lookup(PluginStatsRepository.class)).thenReturn(statsRepository);

        spy.init(context);

        verifyZeroInteractions(statsRepository);
    }

    @Test
    public void onEvent_shouldIgnoreTypesThatAreNotMojoRelated() throws Exception {
        expectToBeNotSaved(ExecutionEvent.Type.ForkedProjectFailed);
        expectToBeNotSaved(ExecutionEvent.Type.MojoSkipped);
    }

    @Test
    public void onEvent_shouldStoreWhenTheExecutionEventTypeIsMojoRelated() throws Exception {
        expectToBeSaved(ExecutionEvent.Type.MojoSucceeded);
        expectToBeSaved(ExecutionEvent.Type.MojoStarted);
        expectToBeSaved(ExecutionEvent.Type.MojoFailed);
    }

    @Test
    public void onEvent_shouldIgnoreAnythingOtherThanExecutionEvents() throws Exception {
        spy.onEvent("test");

        verifyZeroInteractions(statsRepository);
    }

    @Test
    public void init_shouldUseTheProvidedStatRepositoryFound() throws Exception {
        when(lookup.lookup(PluginStatsRepository.class)).thenReturn(statsRepository);

        spy.init(context);

        assertSame(statsRepository, spy.getPluginStatsRepository());
        verify(statsRepository).initialize(context);
    }

    @Test
    public void init_shouldDefaultToH2WhenNothingElseIsFound() throws Exception {
        spy.init(context);

        assertTrue(spy.getPluginStatsRepository() instanceof H2PluginStatsRepository);
    }

    private void expectEventType(ExecutionEvent.Type expectedType) {
        when(executionEvent.getType()).thenReturn(expectedType);
    }

    private void expectToBeSaved(ExecutionEvent.Type expectedType) throws Exception {
        PluginStats stats = new PluginStats();

        expectEventType(expectedType);
        when(pluginStatsFactory.build(executionEvent)).thenReturn(stats);

        spy.onEvent(executionEvent);

        verify(statsRepository).save(stats);
    }

    private void expectToBeNotSaved(ExecutionEvent.Type expectedType) throws Exception {
        expectEventType(expectedType);

        spy.onEvent(executionEvent);

        verifyZeroInteractions(statsRepository, pluginStatsFactory);
    }
}
