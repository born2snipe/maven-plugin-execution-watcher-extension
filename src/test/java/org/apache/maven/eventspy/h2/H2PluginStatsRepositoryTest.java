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
import org.apache.maven.execution.*;
import org.apache.maven.project.MavenProject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static junit.framework.Assert.assertEquals;

public class H2PluginStatsRepositoryTest {
    private H2PluginStatsRepository repository;
    private JdbcTemplate jdbc;
    private PluginStats pluginStats;
    private MavenSession session;

    @Before
    public void setUp() throws Exception {
        File directory = setupH2Directory();

        H2DatabaseManager databaseManager = new H2DatabaseManager(directory);

        repository = new H2PluginStatsRepository(directory);
        repository.setH2DatabaseManager(databaseManager);
        repository.initialize(null);

        jdbc = new JdbcTemplate(databaseManager.load());

        session = new MavenSession(null, null, new DefaultMavenExecutionRequest(), new DefaultMavenExecutionResult());
        session.getRequest().setStartTime(new Date());
        session.setProjects(new ArrayList<MavenProject>());
        session.getRequest().setGoals(Arrays.asList("clean", "verify"));

        pluginStats = new PluginStats();
        pluginStats.project = project("groupId", "artifactId", "1.0");
        pluginStats.plugin = plugin("groupId", "artifactId", "1.0", "clean");
        pluginStats.timestamp = new Date();
        pluginStats.executionId = "execution-1";
        pluginStats.executionHashCode = 123;
        pluginStats.type = PluginStats.Type.START;
        pluginStats.session = session;
    }

    @Test
    public void saveBuildFinished_failingBuild() {
        MavenProject project = mavenProject("1", "1", "1");
        MavenProject otherProject = mavenProject("2", "2", "2");
        session.setProjects(Arrays.asList(project, otherProject));
        session.getResult().addBuildSummary(new BuildFailure(otherProject, 0, null));
        session.getResult().addBuildSummary(new BuildSuccess(project, 0));

        repository.saveBuildStarted(session, "build-data");
        repository.saveBuildFinished(session);

        assertEndOfBuild(session, false);
    }

    @Test
    public void saveBuildFinished_passingBuild() {
        MavenProject project = mavenProject("1", "1", "1");
        MavenProject otherProject = mavenProject("2", "2", "2");
        session.setProjects(Arrays.asList(project, otherProject));
        session.getResult().addBuildSummary(new BuildSuccess(project, 0));
        session.getResult().addBuildSummary(new BuildSuccess(otherProject, 0));

        repository.saveBuildStarted(session, "build-data");
        repository.saveBuildFinished(session);

        assertEndOfBuild(session, true);
    }

    @Test
    public void saveBuildStarted_shouldInsertTheProjectIfTheVersionIsDifferent() {
        insertProject("1", "1", "0");

        session.setProjects(Arrays.asList(mavenProject("1", "1", "1")));

        repository.saveBuildStarted(session, "build-data");

        assertProject("1", "1", "1");
    }

    @Test
    public void saveBuildStarted_shouldNotInsertDuplicateProjects() {
        insertProject("1", "1", "1");

        session.setProjects(Arrays.asList(mavenProject("1", "1", "1")));

        repository.saveBuildStarted(session, "build-data");

        assertProject("1", "1", "1");
    }


    @Test
    public void saveBuildStarted() {
        session.setProjects(Arrays.asList(mavenProject("1", "1", "1"), mavenProject("2", "2", "2")));

        repository.saveBuildStarted(session, "build-data");

        assertProject("1", "1", "1");
        assertProject("2", "2", "2");
        assertStartOfBuild(session, "build-data");
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

        assertProject("groupId", "artifactId", "1.0");
        assertPlugin("groupId", "artifactId", "1.0");
    }

    @Test
    public void save_start() {
        insertBuild();

        repository.save(pluginStats);

        assertProject("groupId", "artifactId", "1.0");
        assertPlugin("groupId", "artifactId", "1.0");
        assertExecution("execution-1", "clean", PluginStats.Type.START);
    }

    private void insertBuild() {
        jdbc.update("insert into build (id) values (?)", session.getRequest().getStartTime().getTime());
    }

    private void assertExecution(String executionId, String goal, PluginStats.Type type) {
        long buildId = session.getRequest().getStartTime().getTime();
        assertEquals("we should have insert an execution", 1, jdbc.queryForInt(
                "select count(1) from plugin_execution where execution_id = ? and goal = ? and result = ? and start_time is not null and build_id = ?",
                executionId, goal, type.name(), buildId
        ));
    }


    private void assertPlugin(String groupId, String artifactId, String version) {
        assertEquals("we should have saved the plugin", 1, jdbc.queryForInt(
                "select count(1) from plugin where group_id = ? and artifact_id = ? and version = ?",
                groupId, artifactId, version
        ));
    }

    private void assertProject(String groupId, String artifactId, String version) {
        assertEquals("we should have saved the project", 1,
                jdbc.queryForInt("select count(1) from project where group_id = ? " +
                        "and artifact_id = ? and version =?", groupId, artifactId, version));
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

    private void insertProject(String groupId, String artifactId, String version) {
        jdbc.update("insert into project (group_id, artifact_id, version) values (?,?,?)", groupId, artifactId, version);
    }

    private void assertStartOfBuild(MavenSession session, String buildData) {
        String goals = "";
        for (String goal : session.getRequest().getGoals()) {
            goals += goal + " ";
        }

        MavenProject topLevelProject = session.getTopLevelProject();
        long projectId = jdbc.queryForLong("select id from project where group_id=? and artifact_id=? and version=?",
                topLevelProject.getGroupId(), topLevelProject.getArtifactId(), topLevelProject.getVersion());

        Date startTime = session.getRequest().getStartTime();
        assertEquals(1, jdbc.queryForInt("select count(1) from build where id = ? and start_time = ? and passed is null and goals = ? and top_level_project_id = ? and data = ?",
                startTime.getTime(),
                startTime,
                goals.trim(),
                projectId,
                buildData
        ));
    }

    private MavenProject mavenProject(String groupId, String artifactId, String version) {
        MavenProject project = new MavenProject();
        project.setGroupId(groupId);
        project.setArtifactId(artifactId);
        project.setVersion(version);
        return project;
    }

    private void assertEndOfBuild(MavenSession session, boolean passed) {
        Date startTime = session.getRequest().getStartTime();
        assertEquals(1, jdbc.queryForInt("select count(1) from build where id =? and end_time is not null and passed = ?", startTime.getTime(), passed ? 1 : 0));
    }
}
