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
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.File;

public class H2PluginStatsRepository implements PluginStatsRepository {
    private H2DatabaseManager h2DatabaseManager;
    private JdbcTemplate jdbcTemplate;

    public H2PluginStatsRepository(File dbLocation) {
        h2DatabaseManager = new H2DatabaseManager(dbLocation);
    }

    @Override
    public void initialize(EventSpy.Context context) {
        if (h2DatabaseManager.doesDatabaseNotExist())
            h2DatabaseManager.create();

        jdbcTemplate = new JdbcTemplate(h2DatabaseManager.load());
    }

    @Override
    public void save(PluginStats pluginStats) {
        long projectId = findOrCreateProject(pluginStats);
        long pluginId = findOrCreatePlugin(pluginStats);

        if (pluginStats.type == PluginStats.Type.START) {
            jdbcTemplate.update(
                    "insert into plugin_execution (project_id, plugin_id, goal, execution_id, execution_hashcode, start_time, result) values (?,?,?,?,?,?,?)",
                    projectId, pluginId, pluginStats.plugin.goal, pluginStats.executionId, pluginStats.executionHashCode, pluginStats.timestamp, pluginStats.type.name()
            );
        } else {
            jdbcTemplate.update(
                    "update plugin_execution set end_time=?,result=? where execution_hashcode=?",
                    pluginStats.timestamp, pluginStats.type.name(), pluginStats.executionHashCode
            );
        }
    }

    private long findOrCreatePlugin(PluginStats pluginStats) {
        String groupId = pluginStats.plugin.groupId;
        String artifactId = pluginStats.plugin.artifactId;
        String version = pluginStats.plugin.version;

        if (pluginDoesNotExist(groupId, artifactId, version)) {
            jdbcTemplate.update(
                    "insert into plugin (group_id, artifact_id, version) values (?,?,?)",
                    groupId, artifactId, version
            );
        }

        return jdbcTemplate.queryForLong(
                "select id from plugin where group_id = ? and artifact_id = ? and version = ?",
                groupId, artifactId, version
        );
    }

    private boolean pluginDoesNotExist(String groupId, String artifactId, String version) {
        return jdbcTemplate.queryForInt(
                "select count(1) from plugin where group_id = ? and artifact_id = ? and version = ?",
                groupId, artifactId, version
        ) == 0;
    }

    private long findOrCreateProject(PluginStats pluginStats) {
        String groupId = pluginStats.project.groupId;
        String artifactId = pluginStats.project.artifactId;
        String version = pluginStats.project.version;

        if (projectDoesNotExist(groupId, artifactId)) {
            jdbcTemplate.update(
                    "insert into project (group_id, artifact_id, version) values (?,?,?)",
                    groupId, artifactId, version
            );
        }
        return jdbcTemplate.queryForLong(
                "select id from project where group_id = ? and artifact_id = ? and version = ?",
                groupId, artifactId, version);
    }

    private boolean projectDoesNotExist(String groupId, String artifactId) {
        return jdbcTemplate.queryForInt(
                "select count(1) from project where group_id = ? and artifact_id = ?",
                groupId, artifactId
        ) == 0;
    }

    public void setH2DatabaseManager(H2DatabaseManager h2DatabaseManager) {
        this.h2DatabaseManager = h2DatabaseManager;
    }

    public JdbcTemplate getJdbcTemplate() {
        return jdbcTemplate;
    }
}
