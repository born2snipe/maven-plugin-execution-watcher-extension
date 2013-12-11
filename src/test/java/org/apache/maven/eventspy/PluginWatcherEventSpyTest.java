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

import co.leantechniques.maven.BuildInformation;
import co.leantechniques.maven.BuildInformationRepository;
import co.leantechniques.maven.BuildInformationRepositoryProvider;
import co.leantechniques.maven.scm.CodeRevision;
import co.leantechniques.maven.scm.CodeRevisionProvider;
import org.apache.maven.execution.ExecutionEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.openide.util.Lookup;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import static junit.framework.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PluginWatcherEventSpyTest {
    @Mock
    private BuildInformationRepositoryProvider buildInformationRepositoryProvider;
    @Mock
    EventSpy.Context context;
    @Mock
    private BuildInformationRepository statsRepository;
    @Mock
    private Lookup lookup;
    @Mock
    private CodeRevisionProvider codeRevisionProvider;
    @InjectMocks
    private PluginWatcherEventSpy spy = new PluginWatcherEventSpy();
    private ExecutionEventBuilder executionEventBuilder;
    private File baseDirectory;

    @Before
    public void setUp() throws Exception {
        System.getProperties().remove("plugin.execution.watcher.build.data");
        baseDirectory = new File("base-dir").getCanonicalFile();

        executionEventBuilder = new ExecutionEventBuilder();
        executionEventBuilder.withProject("1", "1", "1");
        executionEventBuilder.withBaseDirectory(baseDirectory);

        when(buildInformationRepositoryProvider.provide()).thenReturn(statsRepository);
    }

    @Test
    public void onEvent_shouldNotInitializeTheBuildInformationWhenTheProjectListIsEmpty() throws Exception {
        executionEventBuilder = new ExecutionEventBuilder();
        executionEventBuilder.withBuildStarting();
        ExecutionEvent event = executionEventBuilder.toEvent();
        event.getSession().setProjects(new ArrayList());

        spy.onEvent(event);

        assertNull(spy.getCurrentBuildInformation());
    }

    @Test
    public void onEvent_shouldNotInitializeTheBuildInformationWhenTheProjectListIsNull() throws Exception {
        executionEventBuilder = new ExecutionEventBuilder();
        executionEventBuilder.withBuildStarting();
        ExecutionEvent event = executionEventBuilder.toEvent();

        spy.onEvent(event);

        assertNull(spy.getCurrentBuildInformation());
    }

    @Test
    public void onEvent_shouldStoreTheBuildDataAsPartOfTheBuildInformation() throws Exception {
        System.setProperty(PluginWatcherEventSpy.BUILD_DATA_KEY, "build-data");

        executionEventBuilder.withBuildStarting();
        spy.onEvent(executionEventBuilder.toEvent());

        assertEquals("build-data", spy.getCurrentBuildInformation().getUserSpecifiedBuildData());
    }

    @Test
    public void onEvent_shouldNotStoreTheBuildInformationWhenTheBuildFails() throws Exception {
        executionEventBuilder = new ExecutionEventBuilder();
        executionEventBuilder.withProject("1", "1", "1").withFailure();

        executionEventBuilder.withBuildStarting();
        spy.onEvent(executionEventBuilder.toEvent());

        executionEventBuilder.withBuildFinished();
        spy.onEvent(executionEventBuilder.toEvent());

        verify(statsRepository, never()).save(spy.getCurrentBuildInformation());
    }

    @Test
    public void onEvent_shouldSetTheEndTimeForTheBuild() throws Exception {
        executionEventBuilder.withBuildStarting();
        spy.onEvent(executionEventBuilder.toEvent());

        executionEventBuilder.withBuildFinished();
        spy.onEvent(executionEventBuilder.toEvent());

        assertNotNull(spy.getCurrentBuildInformation().getEndTime());
    }

    @Test
    public void onEvent_shouldStoreTheBuildInformationWhenTheSessionFinishes() throws Exception {
        executionEventBuilder.withBuildStarting();
        spy.onEvent(executionEventBuilder.toEvent());

        executionEventBuilder.withBuildFinished();
        spy.onEvent(executionEventBuilder.toEvent());

        verify(statsRepository).save(spy.getCurrentBuildInformation());
    }

    @Test
    public void onEvent_shouldNotReinitializeTheBuildInfoOnEveryEventBeingProcessed() throws Exception {
        executionEventBuilder.withBuildStarting();
        spy.onEvent(executionEventBuilder.toEvent());
        BuildInformation info = spy.getCurrentBuildInformation();

        executionEventBuilder.withBuildFinished();
        spy.onEvent(executionEventBuilder.toEvent());
        assertSame(info, spy.getCurrentBuildInformation());
    }

    @Test
    public void onEvent_shouldStoreTheStartOfTheBuildInformationInMemory() throws Exception {
        executionEventBuilder.withBuildStarting();

        spy.onEvent(executionEventBuilder.toEvent());

        assertNotNull(spy.getCurrentBuildInformation());
    }

    @Test
    public void onEvent_shouldCaptureTheScmVersionOnStartOfBuild() throws Exception {
        CodeRevision codeRevision = new CodeRevision("git", "revision");
        when(codeRevisionProvider.determineRevisionOf(baseDirectory)).thenReturn(codeRevision);
        executionEventBuilder.withBuildStarting();

        spy.onEvent(executionEventBuilder.toEvent());

        BuildInformation buildInformation = spy.getCurrentBuildInformation();
        assertSame(codeRevision, buildInformation.getCodeRevision());
    }

    @Test
    public void close_shouldNotifyTheRepositoryTheBuildIsComplete() throws Exception {
        spy.close();

        verify(statsRepository).cleanUp();
    }

    @Test
    public void onEvent_shouldIgnoreTypesThatAreNotMojoRelated() throws Exception {
        expectPluginStatsToBeNotSaved(ExecutionEvent.Type.ForkedProjectFailed);
        expectPluginStatsToBeNotSaved(ExecutionEvent.Type.MojoSkipped);
    }

    @Test
    public void onEvent_shouldStoreWhenTheExecutionEventTypeIsMojoSucceeded() throws Exception {
        expectPluginStatsToBeSaved(ExecutionEvent.Type.MojoSucceeded);
    }

    @Test
    public void onEvent_shouldStoreWhenTheExecutionEventTypeIsMojoStarted() throws Exception {
        expectPluginStatsToBeSaved(ExecutionEvent.Type.MojoStarted);
    }

    @Test
    public void onEvent_shouldStoreWhenTheExecutionEventTypeIsMojoFailed() throws Exception {
        expectPluginStatsToBeSaved(ExecutionEvent.Type.MojoFailed);
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
        executionEventBuilder.expectEventType(expectedType);
        ExecutionEvent event = executionEventBuilder.toEvent();

        spy.onEvent(event);

        BuildInformation buildInformation = spy.getCurrentBuildInformation();
        assertEquals(Arrays.asList(event), buildInformation.getMavenEvents());
    }

    private void expectPluginStatsToBeNotSaved(ExecutionEvent.Type expectedType) throws Exception {
        executionEventBuilder.expectEventType(expectedType);

        spy.onEvent(executionEventBuilder.toEvent());

        assertEquals(0, spy.getCurrentBuildInformation().getMavenEvents().size());
    }
}
