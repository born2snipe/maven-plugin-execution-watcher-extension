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
import co.leantechniques.maven.h2.H2DatabaseManager;
import co.leantechniques.maven.h2.H2TestRepository;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Date;

public class PluginWatcherEventSpyIntegrationTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private PluginWatcherEventSpy eventSpy;
    private Date startTime = new Date();
    private H2TestRepository testRepository;
    private MavenSessionBuilder sessionBuilder;

    @Before
    public void setUp() throws Exception {
        File dbDirectory = temporaryFolder.newFolder("integration");

        System.setProperty(PluginWatcherEventSpy.DB_DIRECTORY_KEY, dbDirectory.getAbsolutePath());
        H2DatabaseManager databaseManager = new H2DatabaseManager(dbDirectory, true);
        testRepository = new H2TestRepository(databaseManager.load());

        eventSpy = new PluginWatcherEventSpy();
        sessionBuilder = new MavenSessionBuilder(startTime);

        System.setProperty(PluginWatcherEventSpy.TURN_ON_KEY, "true");
    }

    @Test
    public void shouldSupportStoringFailedBuilds() throws Exception {
        simulateFailingBuild();

        assertBuildInfoStored(false);
    }

    @Test
    public void shouldSupportStoringSuccessfulBuilds() throws Exception {
        simulateSuccessfulBuild();

        assertBuildInfoStored(true);
    }

    private void assertBuildInfoStored(boolean buildPassed) {
        MavenSession session = sessionBuilder.toSession();
        testRepository.assertPlugin("2", "2", "2");
        testRepository.assertExecution(session, "2:2:2:2", "2", PluginStats.Type.SUCCEED);
        testRepository.assertEndOfBuild(session, buildPassed);
    }

    private void simulateFailingBuild() throws Exception {
        simulateBuild(false);
    }

    private void simulateSuccessfulBuild() throws Exception {
        simulateBuild(true);
    }

    private void simulateBuild(boolean projectPassed) throws Exception {
        eventSpy.init(null);
        eventSpy.onEvent(buildStarting());
        eventSpy.onEvent(pluginStarted());
        eventSpy.onEvent(pluginWasSuccessful());
        eventSpy.onEvent(buildFinished(projectPassed));
    }

    private ExecutionEvent pluginWasSuccessful() {
        ExecutionEventBuilder eventBuilder = executionEvent(true);
        eventBuilder.withPlugin("2", "2", "2", "2").successful();
        return eventBuilder.toEvent();
    }

    private ExecutionEvent pluginStarted() {
        ExecutionEventBuilder eventBuilder = executionEvent(true);
        eventBuilder.withPlugin("2", "2", "2", "2").starting();
        return eventBuilder.toEvent();
    }

    private ExecutionEvent buildFinished(boolean projectPassed) {
        ExecutionEventBuilder eventBuilder = executionEvent(projectPassed);
        eventBuilder.withBuildFinished();
        return eventBuilder.toEvent();
    }

    private ExecutionEvent buildStarting() {
        ExecutionEventBuilder eventBuilder = executionEvent(true);
        eventBuilder.withBuildStarting();
        return eventBuilder.toEvent();
    }

    private ExecutionEventBuilder executionEvent(boolean projectPassed) {
        ExecutionEventBuilder eventBuilder = new ExecutionEventBuilder(sessionBuilder);
        if (projectPassed) {
            eventBuilder.withProject("1", "1", "1").withSuccess();
        } else {
            eventBuilder.withProject("1", "1", "1").withFailure();
        }
        return eventBuilder;
    }
}
