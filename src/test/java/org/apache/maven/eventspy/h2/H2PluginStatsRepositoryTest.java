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

import org.apache.maven.eventspy.PluginStats;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.util.Date;

import static junit.framework.Assert.assertEquals;

public class H2PluginStatsRepositoryTest {
    private H2PluginStatsRepository repository;
    private JdbcTemplate jdbc;
    private PluginStats pluginStats;

    @Before
    public void setUp() throws Exception {
        File directory = setupH2Directory();

        H2DatabaseManager databaseManager = new H2DatabaseManager(directory);

        repository = new H2PluginStatsRepository(directory);
        repository.setH2DatabaseManager(databaseManager);
        repository.initialize(null);

        jdbc = new JdbcTemplate(databaseManager.load());

        pluginStats = new PluginStats();
        pluginStats.project = project("groupId", "artifactId");
        pluginStats.plugin = plugin("groupId", "artifactId", "1.0", "clean");
        pluginStats.timestamp = new Date();
        pluginStats.executionId = "execution-1";
        pluginStats.executionHashCode = 123;
        pluginStats.type = PluginStats.Type.START;
    }

    @Test
    public void save_shouldUpdateTheExistingRecordWhenTheTypeIs_SUCCEED() {
        repository.save(pluginStats);

        pluginStats.type = PluginStats.Type.SUCCEED;
        repository.save(pluginStats);

        assertExecution("execution-1", "clean", PluginStats.Type.SUCCEED);
    }

    @Test
    public void save_shouldUpdateTheExistingRecordWhenTheTypeIs_FAILED() {
        repository.save(pluginStats);

        pluginStats.type = PluginStats.Type.FAILED;
        repository.save(pluginStats);

        assertExecution("execution-1", "clean", PluginStats.Type.FAILED);
    }


    @Test
    public void save_shouldInsertOnlyOneProjectWhenCalledMultipleTimes() {
        repository.save(pluginStats);
        repository.save(pluginStats);

        assertProject("groupId", "artifactId");
        assertPlugin("groupId", "artifactId", "1.0");
    }

    @Test
    public void save_start() {
        repository.save(pluginStats);

        assertProject("groupId", "artifactId");
        assertPlugin("groupId", "artifactId", "1.0");
        assertExecution("execution-1", "clean", PluginStats.Type.START);
    }

    private void assertExecution(String executionId, String goal, PluginStats.Type type) {
        assertEquals("we should have insert an execution", 1, jdbc.queryForInt(
                "select count(1) from plugin_execution where execution_id = ? and goal = ? and result = ? and start_time is not null",
                executionId, goal, type.name()
        ));
    }

    private void assertPlugin(String groupId, String artifactId, String version) {
        assertEquals("we should have saved the plugin", 1, jdbc.queryForInt(
                "select count(1) from plugin where group_id = ? and artifact_id = ? and version = ?",
                groupId, artifactId, version
        ));
    }

    private void assertProject(String groupId, String artifactId) {
        assertEquals("we should have saved the project", 1,
                jdbc.queryForInt("select count(1) from project where group_id = ? and artifact_id = ?", groupId, artifactId));
    }

    private PluginStats.Project project(String groupId, String artifactId) {
        PluginStats.Project project = new PluginStats.Project();
        project.groupId = groupId;
        project.artifactId = artifactId;
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

    private File setupH2Directory() {
        File directory = new File(System.getProperty("java.io.tmpdir"), "at");
        directory.mkdirs();
        cleanDirectory(directory);
        assertEquals("the db directory should be empty. Are you sure the files are not locked?",
                0, directory.listFiles().length);
        return directory;
    }

    private void cleanDirectory(File directory) {
        for (File file : directory.listFiles()) file.delete();
    }
}
