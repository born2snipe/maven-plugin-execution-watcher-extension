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

import co.leantechniques.maven.*;
import org.apache.maven.eventspy.EventSpy;
import org.codehaus.plexus.util.StringUtils;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.util.Calendar;
import java.util.List;

public class H2PluginStatsRepository implements PluginStatsRepository {
    private H2DatabaseManager h2DatabaseManager;
    private Handle handle;

    public H2PluginStatsRepository() {
        h2DatabaseManager = new H2DatabaseManager();
    }

    @Override
    public void initialize(EventSpy.Context context) {
        DBI dbi = new DBI(h2DatabaseManager.load());
        handle = dbi.open();

        deletePartialBuilds();
    }

    @Override
    public void cleanUp() {
        if (handle != null) {
            handle.close();
        }
    }

    @Override
    public void save(BuildInformation buildInformation) {
        insertBuild(buildInformation);
        insertProjects(buildInformation);
        insertPluginExecutions(buildInformation);
    }

    private void insertPluginExecutions(BuildInformation buildInformation) {
        for (Project project : buildInformation.getProjects()) {
            long projectId = findOrCreateProject(project);
            for (PluginExecution pluginExecution : project.getPluginExecutions()) {
                insertPluginExecutionFor(buildInformation.getId(), projectId, pluginExecution);
            }
        }
    }

    private void insertPluginExecutionFor(long buildId, long projectId, PluginExecution pluginExecution) {
        long pluginId = findOrCreatePlugin(pluginExecution);
        handle.createStatement("insert into plugin_execution (project_id, plugin_id, goal, execution_id, start_time, end_time, build_id) values (?,?,?,?,?,?,?)")
                .bind(0, projectId)
                .bind(1, pluginId)
                .bind(2, pluginExecution.goal)
                .bind(3, pluginExecution.executionId)
                .bind(4, pluginExecution.startTime)
                .bind(5, pluginExecution.endTime)
                .bind(6, buildId)
                .execute();
    }

    private long findOrCreatePlugin(Artifact artifact) {
        String groupId = artifact.groupId;
        String artifactId = artifact.artifactId;
        String version = artifact.version;

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

    private void insertProjects(BuildInformation buildInformation) {
        for (Project project : buildInformation.getProjects()) {
            findOrCreateProject(project);
        }
    }

    private void insertBuild(BuildInformation buildInformation) {
        long projectId = findOrCreateProject(buildInformation.getTopLevelProject());

        // todo - do not need to store passed, we only save passing builds now
        handle.createStatement("insert into build (id, start_time, goals, top_level_project_id, data, end_time, passed) values (?,?,?,?,?,?,1)")
                .bind(0, buildInformation.getId())
                .bind(1, buildInformation.getStartTime())
                .bind(2, StringUtils.join(buildInformation.getGoals().iterator(), " "))
                .bind(3, projectId)
                .bind(4, buildInformation.getUserSpecifiedBuildData())
                .bind(5, buildInformation.getEndTime())
                .execute();
    }

    private boolean pluginDoesNotExist(String groupId, String artifactId, String version) {
        return handle.createQuery("select count(1) from plugin where group_id = ? and artifact_id = ? and version = ?")
                .bind(0, groupId)
                .bind(1, artifactId)
                .bind(2, version)
                .mapTo(Integer.class)
                .first() == 0;
    }

    private long findOrCreateProject(Artifact project) {
        if (projectDoesNotExist(project)) {
            insertProject(project.groupId, project.artifactId, project.version);
        }
        return findProject(project.groupId, project.artifactId, project.version);
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

    @Deprecated
    private boolean projectDoesNotExist(String groupId, String artifactId, String version) {
        return handle.createQuery("select count(1) from project where group_id = ? and artifact_id = ? and version = ?")
                .bind(0, groupId)
                .bind(1, artifactId)
                .bind(2, version)
                .mapTo(Integer.class)
                .first() == 0;
    }

    private boolean projectDoesNotExist(Artifact project) {
        return handle.createQuery("select count(1) from project where group_id = ? and artifact_id = ? and version = ?")
                .bind(0, project.groupId)
                .bind(1, project.artifactId)
                .bind(2, project.version)
                .mapTo(Integer.class)
                .first() == 0;
    }

    private void deletePartialBuilds() {
        List<Long> buildIds = handle.createQuery("select id from build where end_time is null and start_time < ?")
                .bind(0, today())
                .mapTo(Long.class)
                .list();
        for (Long buildId : buildIds) {
            deleteBuildDataFor(buildId);
        }
    }

    private void deleteBuildDataFor(Long buildId) {
        handle.createStatement("delete from plugin_execution where build_id = ?").bind(0, buildId).execute();
        handle.createStatement("delete from build where id = ?").bind(0, buildId).execute();
    }

    private java.sql.Date today() {
        Calendar instance = Calendar.getInstance();
        instance.set(Calendar.HOUR_OF_DAY, 0);
        instance.set(Calendar.MINUTE, 0);
        instance.set(Calendar.SECOND, 0);
        instance.set(Calendar.MILLISECOND, 0);
        return new java.sql.Date(instance.getTimeInMillis());
    }

    public void setH2DatabaseManager(H2DatabaseManager h2DatabaseManager) {
        this.h2DatabaseManager = h2DatabaseManager;
    }

}
