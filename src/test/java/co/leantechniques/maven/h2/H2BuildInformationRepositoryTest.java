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

package co.leantechniques.maven.h2;

import co.leantechniques.maven.BuildInformation;
import co.leantechniques.maven.scm.CodeRevision;
import org.apache.maven.eventspy.ExecutionEventBuilder;
import org.apache.maven.eventspy.MavenSessionBuilder;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;

public class H2BuildInformationRepositoryTest extends AbstractDatabaseTest {
    private H2BuildInformationRepository repository;
    private MavenSessionBuilder sessionBuilder;
    private H2TestRepository testRepository;
    private CodeRevision codeRevision;

    @Before
    public void setUp() throws Exception {
        H2DatabaseManager databaseManager = new H2DatabaseManager();

        repository = new H2BuildInformationRepository();
        repository.setH2DatabaseManager(databaseManager);
        repository.initialize(null);

        testRepository = new H2TestRepository(databaseManager.load());

        sessionBuilder = new MavenSessionBuilder();
        sessionBuilder.withGoals("clean", "verify");
        sessionBuilder.withMavenVersion("maven-version");
        sessionBuilder.withJavaVersion("java-version");
        sessionBuilder.withComputerName("computer-name");
        sessionBuilder.withOSName("os-name");
        sessionBuilder.withOSArch("os-arch");
        sessionBuilder.withUsername("username");
        codeRevision = new CodeRevision("git", "revision");
    }

    @After
    public void tearDown() throws Exception {
        repository.cleanUp();
    }

    @Test
    public void save_shouldSaveAllThePluginExecutionsForEachProject() {
        ExecutionEventBuilder builder = new ExecutionEventBuilder(sessionBuilder);
        builder.withProject("1", "1", "1");
        builder.withPlugin("plugin-1", "plugin-1", "plugin-1", "goal-1").starting();

        ExecutionEvent event = builder.toEvent();
        MavenSession session = event.getSession();

        BuildInformation buildInformation = new BuildInformation(session, null, codeRevision);
        buildInformation.addMavenEvent(event);

        repository.save(buildInformation);

        testRepository.assertExecution(session, "plugin-1:plugin-1:plugin-1:goal-1", "goal-1");
    }

    @Test
    public void save_shouldSaveAllTheProjectsInTheBuild() {
        ExecutionEventBuilder builder = new ExecutionEventBuilder(sessionBuilder);
        builder.withProject("1", "1", "1");
        builder.withProject("2", "2", "2");

        ExecutionEvent event = builder.toEvent();
        MavenSession session = event.getSession();

        repository.save(new BuildInformation(session, null, codeRevision));

        testRepository.assertProject("1", "1", "1");
        testRepository.assertProject("2", "2", "2");
    }

    @Test
    public void save_shouldNotInsertTheProjectMultipleTimes() {
        ExecutionEventBuilder builder = new ExecutionEventBuilder(sessionBuilder);
        builder.withProject("1", "1", "1");
        builder.withProject("1", "1", "1");

        ExecutionEvent event = builder.toEvent();
        MavenSession session = event.getSession();

        repository.save(new BuildInformation(session, null, codeRevision));

        testRepository.assertProject("1", "1", "1");
    }

    @Test
    public void save_shouldSaveAllTheBuildInformation() {
        ExecutionEventBuilder builder = new ExecutionEventBuilder(sessionBuilder);
        builder.withProject("1", "1", "1");

        ExecutionEvent event = builder.toEvent();
        MavenSession session = event.getSession();

        BuildInformation info = new BuildInformation(session, null, codeRevision);
        info.setEndTime(new Date());

        repository.save(info);

        testRepository.assertEndOfBuild(session);
        testRepository.assertProject("1", "1", "1");
        testRepository.assertMachineInfoStored(session);
    }

}
