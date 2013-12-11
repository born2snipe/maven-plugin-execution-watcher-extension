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
import java.util.Map;

public class H2BuildInformationRepository implements BuildInformationRepository {
    private H2DatabaseManager h2DatabaseManager;

    public H2BuildInformationRepository() {
        h2DatabaseManager = new H2DatabaseManager();
    }

    @Override
    public void initialize(EventSpy.Context context) {
        execute(new Transaction() {
            public void inTransaction(Handle handle) {
                deletePartialBuilds(handle);
            }
        });
    }

    @Override
    public void cleanUp() {
        h2DatabaseManager.unload();
    }

    @Override
    public void save(final BuildInformation buildInformation) {
        execute(new Transaction() {
            public void inTransaction(Handle handle) {
                Long machineInfoId = insertMachineInfo(handle, buildInformation);
                insertBuild(handle, buildInformation, machineInfoId);
                insertProjects(handle, buildInformation);
                insertPluginExecutions(handle, buildInformation);
            }
        });
    }

    private Long insertMachineInfo(Handle handle, BuildInformation buildInformation) {
        Map<String, Object> insertResult = handle.createStatement("insert into machine_info (maven_version, java_version, computer_name, os, username, os_arch) values (?,?,?,?,?,?)")
                .bind(0, buildInformation.getMavenVersion())
                .bind(1, buildInformation.getJavaVersion())
                .bind(2, buildInformation.getComputerName())
                .bind(3, buildInformation.getOsName())
                .bind(4, buildInformation.getUsername())
                .bind(5, buildInformation.getOsArch())
                .executeAndReturnGeneratedKeys()
                .first();
        return (Long) insertResult.values().iterator().next();
    }

    private void insertPluginExecutions(Handle handle, BuildInformation buildInformation) {
        for (Project project : buildInformation.getProjects()) {
            long projectId = findOrCreateProject(handle, project);
            for (PluginExecution pluginExecution : project.getPluginExecutions()) {
                insertPluginExecutionFor(handle, buildInformation.getId(), projectId, pluginExecution);
            }
        }
    }

    private void insertPluginExecutionFor(Handle handle, long buildId, long projectId, PluginExecution pluginExecution) {
        long pluginId = findOrCreatePlugin(handle, pluginExecution);
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

    private long findOrCreatePlugin(Handle handle, Artifact artifact) {
        String groupId = artifact.groupId;
        String artifactId = artifact.artifactId;
        String version = artifact.version;

        if (pluginDoesNotExist(handle, groupId, artifactId, version)) {
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

    private void insertProjects(Handle handle, BuildInformation buildInformation) {
        for (Project project : buildInformation.getProjects()) {
            findOrCreateProject(handle, project);
        }
    }

    private void insertBuild(Handle handle, BuildInformation buildInformation, Long machineInfoId) {
        long projectId = findOrCreateProject(handle, buildInformation.getTopLevelProject());

        handle.createStatement("insert into build (id, start_time, goals, top_level_project_id, data, end_time, machine_info_id) values (?,?,?,?,?,?,?)")
                .bind(0, buildInformation.getId())
                .bind(1, buildInformation.getStartTime())
                .bind(2, StringUtils.join(buildInformation.getGoals().iterator(), " "))
                .bind(3, projectId)
                .bind(4, buildInformation.getUserSpecifiedBuildData())
                .bind(5, buildInformation.getEndTime())
                .bind(6, machineInfoId)
                .execute();
    }

    private boolean pluginDoesNotExist(Handle handle, String groupId, String artifactId, String version) {
        return handle.createQuery("select count(1) from plugin where group_id = ? and artifact_id = ? and version = ?")
                .bind(0, groupId)
                .bind(1, artifactId)
                .bind(2, version)
                .mapTo(Integer.class)
                .first() == 0;
    }

    private long findOrCreateProject(Handle handle, Artifact project) {
        if (projectDoesNotExist(handle, project)) {
            insertProject(handle, project.groupId, project.artifactId, project.version);
        }
        return findProject(handle, project.groupId, project.artifactId, project.version);
    }

    private long findProject(Handle handle, String groupId, String artifactId, String version) {
        return handle.createQuery("select id from project where group_id = ? and artifact_id = ? and version = ?")
                .bind(0, groupId)
                .bind(1, artifactId)
                .bind(2, version)
                .mapTo(Long.class)
                .first();
    }

    private void insertProject(Handle handle, String groupId, String artifactId, String version) {
        handle.createStatement("insert into project (group_id, artifact_id, version) values (?,?,?)")
                .bind(0, groupId)
                .bind(1, artifactId)
                .bind(2, version)
                .execute();
    }

    private boolean projectDoesNotExist(Handle handle, Artifact project) {
        return handle.createQuery("select count(1) from project where group_id = ? and artifact_id = ? and version = ?")
                .bind(0, project.groupId)
                .bind(1, project.artifactId)
                .bind(2, project.version)
                .mapTo(Integer.class)
                .first() == 0;
    }

    private void deletePartialBuilds(Handle handle) {
        List<Long> buildIds = handle.createQuery("select id from build where end_time is null and start_time < ?")
                .bind(0, today())
                .mapTo(Long.class)
                .list();
        for (Long buildId : buildIds) {
            deleteBuildDataFor(handle, buildId);
        }
    }

    private void deleteBuildDataFor(Handle handle, Long buildId) {
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

    private void execute(Transaction transaction) {
        DBI dbi = new DBI(h2DatabaseManager.load());
        Handle handle = dbi.open();
        transaction.inTransaction(handle);
        handle.close();
    }

    public void setH2DatabaseManager(H2DatabaseManager h2DatabaseManager) {
        this.h2DatabaseManager = h2DatabaseManager;
    }

    private interface Transaction {
        void inTransaction(Handle handle);
    }
}
