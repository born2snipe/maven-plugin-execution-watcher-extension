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

import co.leantechniques.maven.PluginStats;
import org.apache.maven.eventspy.MavenSessionBuilder;
import org.apache.maven.execution.MavenSession;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.Date;

public class H2PluginStatsRepositoryTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private H2PluginStatsRepository repository;
    private PluginStats pluginStats;
    private MavenSessionBuilder sessionBuilder;
    private H2TestRepository testRepository;

    @Before
    public void setUp() throws Exception {
        File directory = temporaryFolder.newFolder("test-db");

        H2DatabaseManager databaseManager = new H2DatabaseManager(directory);

        repository = new H2PluginStatsRepository(directory);
        repository.setH2DatabaseManager(databaseManager);
        repository.initialize(null);

        testRepository = new H2TestRepository(databaseManager.load());

        sessionBuilder = new MavenSessionBuilder();
        sessionBuilder.withGoals("clean", "verify");

        pluginStats = new PluginStats();
        pluginStats.project = project("groupId", "artifactId", "1.0");
        pluginStats.plugin = plugin("groupId", "artifactId", "1.0", "clean");
        pluginStats.timestamp = new Date();
        pluginStats.executionId = "execution-1";
        pluginStats.executionHashCode = 123;
        pluginStats.type = PluginStats.Type.START;
        pluginStats.session = sessionBuilder.toSession();
    }

    @After
    public void tearDown() throws Exception {
        repository.finished();
    }

    @Test
    public void saveBuildFinished_failingBuild() {
        sessionBuilder.withProject("1", "1", "1").withSuccess();
        sessionBuilder.withProject("2", "2", "2").withFailure();
        MavenSession session = sessionBuilder.toSession();

        repository.saveBuildStarted(session, "build-data");
        repository.saveBuildFinished(session);

        testRepository.assertEndOfBuild(session, false);
    }

    @Test
    public void saveBuildFinished_passingBuild() {
        sessionBuilder.withProject("1", "1", "1").withSuccess();
        sessionBuilder.withProject("2", "2", "2").withSuccess();
        MavenSession session = sessionBuilder.toSession();

        repository.saveBuildStarted(session, "build-data");
        repository.saveBuildFinished(session);

        testRepository.assertEndOfBuild(session, true);
    }

    @Test
    public void saveBuildStarted_shouldInsertTheProjectIfTheVersionIsDifferent() {
        testRepository.insertProject("1", "1", "0");

        sessionBuilder.withProject("1", "1", "1");

        repository.saveBuildStarted(sessionBuilder.toSession(), "build-data");

        testRepository.assertProject("1", "1", "1");
    }

    @Test
    public void saveBuildStarted_shouldNotInsertDuplicateProjects() {
        testRepository.insertProject("1", "1", "1");

        sessionBuilder.withProject("1", "1", "1");

        repository.saveBuildStarted(sessionBuilder.toSession(), "build-data");

        testRepository.assertProject("1", "1", "1");
    }


    @Test
    public void saveBuildStarted() {
        sessionBuilder.withProject("1", "1", "1");
        sessionBuilder.withProject("2", "2", "2");
        MavenSession session = sessionBuilder.toSession();

        repository.saveBuildStarted(session, "build-data");

        testRepository.assertProject("1", "1", "1");
        testRepository.assertProject("2", "2", "2");
        testRepository.assertStartOfBuild(session, "build-data");
    }

    @Test
    public void save_shouldUpdateTheExistingRecordWhenTheTypeIs_SUCCEED() {
        insertBuild();

        repository.save(pluginStats);

        pluginStats.type = PluginStats.Type.SUCCEED;
        repository.save(pluginStats);

        assertExecution("execution-1", "clean", PluginStats.Type.SUCCEED);
    }

    @Test
    public void save_shouldUpdateTheExistingRecordWhenTheTypeIs_FAILED() {
        insertBuild();

        repository.save(pluginStats);

        pluginStats.type = PluginStats.Type.FAILED;
        repository.save(pluginStats);

        assertExecution("execution-1", "clean", PluginStats.Type.FAILED);
    }

    @Test
    public void save_shouldInsertOnlyOneProjectWhenCalledMultipleTimes() {
        insertBuild();

        repository.save(pluginStats);
        repository.save(pluginStats);

        testRepository.assertProject("groupId", "artifactId", "1.0");
        testRepository.assertPlugin("groupId", "artifactId", "1.0");
    }

    @Test
    public void save_start() {
        insertBuild();

        repository.save(pluginStats);

        testRepository.assertProject("groupId", "artifactId", "1.0");
        testRepository.assertPlugin("groupId", "artifactId", "1.0");
        assertExecution("execution-1", "clean", PluginStats.Type.START);
    }

    private void insertBuild() {
        testRepository.insertBuild(sessionBuilder.toSession());
    }

    private void assertExecution(String executionId, String goal, PluginStats.Type type) {
        testRepository.assertExecution(sessionBuilder.toSession(), executionId, goal, type);
    }

    private PluginStats.Project project(String groupId, String artifactId, String version) {
        PluginStats.Project project = new PluginStats.Project();
        project.groupId = groupId;
        project.artifactId = artifactId;
        project.version = version;
        return project;
    }

    private PluginStats.Plugin plugin(String groupId, String artifactId, String version, String goal) {
        PluginStats.Plugin plugin = new PluginStats.Plugin();
        plugin.groupId = groupId;
        plugin.artifactId = artifactId;
        plugin.version = version;
        plugin.goal = goal;
        return plugin;
    }

}
