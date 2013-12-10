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
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.Query;

import javax.sql.DataSource;
import java.util.Date;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

public class H2TestRepository {
    private Handle handle;

    public H2TestRepository(DataSource dataSource) {
        DBI dbi = new DBI(dataSource);
        handle = dbi.open();
    }

    public void insertBuild(MavenSession session) {
        handle.createStatement("insert into build (id) values (:id)")
                .bind("id", getBuildId(session))
                .execute();
    }

    private long getBuildId(MavenSession session) {
        return session.getRequest().getStartTime().getTime();
    }

    public void assertExecution(MavenSession session, String executionId, String goal, PluginStats.Type type) {
        int count = handle.createQuery("select count(1) from plugin_execution where execution_id = ? and goal = ? and result = ? and start_time is not null and build_id = ?")
                .bind(0, executionId)
                .bind(1, goal)
                .bind(2, type.name())
                .bind(3, getBuildId(session))
                .mapTo(Integer.class)
                .first();

        assertEquals("we should have insert an execution", 1, count);
    }

    public void insertProject(String groupId, String artifactId, String version) {
        handle.createStatement("insert into project (group_id, artifact_id, version) values (?,?,?)")
                .bind(0, groupId)
                .bind(1, artifactId)
                .bind(2, version)
                .execute();
    }

    public void assertPlugin(String groupId, String artifactId, String version) {
        int count = handle.createQuery("select count(1) from plugin where group_id = ? and artifact_id = ? and version = ?")
                .bind(0, groupId)
                .bind(1, artifactId)
                .bind(2, version)
                .mapTo(Integer.class)
                .first();
        assertEquals("we should have saved the plugin", 1, count);
    }

    public void assertProject(String groupId, String artifactId, String version) {
        int count = handle.createQuery("select count(1) from project where group_id = ? and artifact_id = ? and version =?")
                .bind(0, groupId)
                .bind(1, artifactId)
                .bind(2, version)
                .mapTo(Integer.class)
                .first();
        assertEquals("we should have saved the project (" + groupId + ":" + artifactId + ":" + version + ")", 1, count);
    }


    public void assertStartOfBuild(MavenSession session, String buildData) {
        String goals = "";
        for (String goal : session.getRequest().getGoals()) {
            goals += goal + " ";
        }

        MavenProject topLevelProject = session.getTopLevelProject();
        long projectId = handle.createQuery("select id from project where group_id=? and artifact_id=? and version=?")
                .bind(0, topLevelProject.getGroupId())
                .bind(1, topLevelProject.getArtifactId())
                .bind(2, topLevelProject.getVersion())
                .mapTo(Long.class)
                .first();

        Date startTime = session.getRequest().getStartTime();

        StringBuilder sql = new StringBuilder("select count(1) from build where id = ? and start_time = ? and passed is null and goals = ? and top_level_project_id = ? and data ");
        if (buildData == null) {
            sql.append("is null");
        } else {
            sql.append(" = ?");
        }

        Query<Map<String, Object>> query = handle.createQuery(sql.toString())
                .bind(0, startTime.getTime())
                .bind(1, startTime)
                .bind(2, goals.trim())
                .bind(3, projectId);

        if (buildData != null) {
            query.bind(4, buildData);
        }

        int count = query.mapTo(Integer.class).first();

        assertTrue("No build was found or did the build finish?", count > 0);
        assertEquals("Apparently we inserted more than one instance of the build in the database", 1, count);
    }

    public void assertEndOfBuild(MavenSession session, boolean passed) {
        Date startTime = session.getRequest().getStartTime();

        int count = handle.createQuery("select count(1) from build where id =? and end_time is not null and passed = ?")
                .bind(0, startTime.getTime())
                .bind(1, passed ? 1 : 0)
                .mapTo(Integer.class)
                .first();

        assertEquals(1, count);
    }

    public void assertNoPluginExecutionsAreStored() {
        int count = handle.createQuery("select count(1) from plugin_execution").mapTo(Integer.class).first();
        assertEquals("we should have not found any plugin executions", 0, count);
    }

    public void assertNoBuildIsStored() {
        int count = handle.createQuery("select count(1) from build").mapTo(Integer.class).first();
        assertEquals("we should have not found any build", 0, count);
    }
}
