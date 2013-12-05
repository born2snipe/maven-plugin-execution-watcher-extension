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

import org.apache.maven.eventspy.EventSpy;
import org.apache.maven.eventspy.PluginStats;
import org.apache.maven.eventspy.PluginStatsRepository;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.io.File;
import java.util.Date;

public class H2PluginStatsRepository implements PluginStatsRepository {
    private H2DatabaseManager h2DatabaseManager;
    private Handle handle;

    public H2PluginStatsRepository(File dbLocation) {
        h2DatabaseManager = new H2DatabaseManager(dbLocation);
    }

    @Override
    public void initialize(EventSpy.Context context) {
        DBI dbi = new DBI(h2DatabaseManager.load());
        handle = dbi.open();
    }

    @Override
    public void finished() {
        if (handle != null) {
            handle.close();
        }
    }

    @Override
    public void save(PluginStats pluginStats) {
        long buildId = findBuild(pluginStats);
        long projectId = findOrCreateProject(pluginStats);
        long pluginId = findOrCreatePlugin(pluginStats);

        if (pluginStats.type == PluginStats.Type.START) {
            handle.createStatement("insert into plugin_execution (project_id, plugin_id, goal, execution_id, execution_hashcode, start_time, result, build_id) values (?,?,?,?,?,?,?,?)")
                    .bind(0, projectId)
                    .bind(1, pluginId)
                    .bind(2, pluginStats.plugin.goal)
                    .bind(3, pluginStats.executionId)
                    .bind(4, pluginStats.executionHashCode)
                    .bind(5, pluginStats.timestamp)
                    .bind(6, pluginStats.type.name())
                    .bind(7, buildId)
                    .execute();
        } else {
            handle.createStatement("update plugin_execution set end_time=?,result=? where execution_hashcode=? and build_id=?")
                    .bind(0, pluginStats.timestamp)
                    .bind(1, pluginStats.type.name())
                    .bind(2, pluginStats.executionHashCode)
                    .bind(3, buildId)
                    .execute();

        }
    }

    @Override
    public void saveBuildStarted(MavenSession session, String additionalBuildData) {
        for (MavenProject project : session.getProjects()) {
            if (projectDoesNotExist(project.getGroupId(), project.getArtifactId(), project.getVersion())) {
                insertProject(project.getGroupId(), project.getArtifactId(), project.getVersion());
            }
        }

        String goals = "";
        for (String goal : session.getRequest().getGoals()) {
            goals += goal + " ";
        }
        Date startTime = session.getRequest().getStartTime();
        MavenProject topLevelProject = session.getTopLevelProject();
        long projectId = findProject(topLevelProject.getGroupId(), topLevelProject.getArtifactId(), topLevelProject.getVersion());

        handle.createStatement("insert into build (id, start_time, goals, top_level_project_id, data) values (?,?,?,?,?)")
                .bind(0, startTime.getTime())
                .bind(1, startTime)
                .bind(2, goals.trim())
                .bind(3, projectId)
                .bind(4, additionalBuildData)
                .execute();
    }

    @Override
    public void saveBuildFinished(MavenSession session) {
        boolean passing = true;
        for (MavenProject project : session.getProjects()) {
            if (session.getResult().getBuildSummary(project) instanceof BuildFailure) {
                passing = false;
                break;
            }
        }

        Date startTime = session.getRequest().getStartTime();
        handle.createStatement("update build set end_time = ?, passed = ? where id = ?")
                .bind(0, new Date())
                .bind(1, passing ? 1 : 0)
                .bind(2, startTime.getTime())
                .execute();
    }

    private long findOrCreatePlugin(PluginStats pluginStats) {
        String groupId = pluginStats.plugin.groupId;
        String artifactId = pluginStats.plugin.artifactId;
        String version = pluginStats.plugin.version;

        if (pluginDoesNotExist(groupId, artifactId, version)) {
            handle.createStatement("insert into plugin (group_id, artifact_id, version) values (?,?,?)")
                    .bind(0, groupId)
                    .bind(1, artifactId)
                    .bind(2, version)
                    .execute();
        }

        return handle.createQuery("select id from plugin where group_id = ? and artifact_id = ? and version = ?")
                .bind(0, groupId)
                .bind(1, artifactId)
                .bind(2, version)
                .mapTo(Long.class)
                .first();
    }

    private boolean pluginDoesNotExist(String groupId, String artifactId, String version) {
        return handle.createQuery("select count(1) from plugin where group_id = ? and artifact_id = ? and version = ?")
                .bind(0, groupId)
                .bind(1, artifactId)
                .bind(2, version)
                .mapTo(Integer.class)
                .first() == 0;
    }

    private long findOrCreateProject(PluginStats pluginStats) {
        String groupId = pluginStats.project.groupId;
        String artifactId = pluginStats.project.artifactId;
        String version = pluginStats.project.version;

        if (projectDoesNotExist(groupId, artifactId, version)) {
            insertProject(groupId, artifactId, version);
        }
        return findProject(groupId, artifactId, version);
    }

    private long findProject(String groupId, String artifactId, String version) {
        return handle.createQuery("select id from project where group_id = ? and artifact_id = ? and version = ?")
                .bind(0, groupId)
                .bind(1, artifactId)
                .bind(2, version)
                .mapTo(Long.class)
                .first();
    }

    private void insertProject(String groupId, String artifactId, String version) {
        handle.createStatement("insert into project (group_id, artifact_id, version) values (?,?,?)")
                .bind(0, groupId)
                .bind(1, artifactId)
                .bind(2, version)
                .execute();
    }

    private boolean projectDoesNotExist(String groupId, String artifactId, String version) {
        return handle.createQuery("select count(1) from project where group_id = ? and artifact_id = ? and version = ?")
                .bind(0, groupId)
                .bind(1, artifactId)
                .bind(2, version)
                .mapTo(Integer.class)
                .first() == 0;
    }

    private long findBuild(PluginStats pluginStats) {
        return pluginStats.session.getRequest().getStartTime().getTime();
    }

    public void setH2DatabaseManager(H2DatabaseManager h2DatabaseManager) {
        this.h2DatabaseManager = h2DatabaseManager;
    }

}
