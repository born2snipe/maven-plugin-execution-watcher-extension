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

import co.leantechniques.maven.scm.CodeRevision;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenSession;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import javax.sql.DataSource;
import java.util.Date;
import java.util.Map;
import java.util.Properties;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

public class H2TestRepository {
    private Handle handle;

    public H2TestRepository(DataSource dataSource) {
        DBI dbi = new DBI(dataSource);
        handle = dbi.open();
    }

    private long getBuildId(MavenSession session) {
        return session.getRequest().getStartTime().getTime();
    }

    public void assertExecution(MavenSession session, String executionId, String goal) {
        int count = handle.createQuery("select count(1) from plugin_execution where execution_id = ? and goal = ? and start_time is not null and build_id = ?")
                .bind(0, executionId)
                .bind(1, goal)
                .bind(2, getBuildId(session))
                .mapTo(Integer.class)
                .first();

        assertEquals("we should have insert an execution", 1, count);
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

    public void assertEndOfBuild(MavenSession session) {
        Date startTime = session.getRequest().getStartTime();

        int count = handle.createQuery("select count(1) from build where id =? and end_time is not null")
                .bind(0, startTime.getTime())
                .mapTo(Integer.class)
                .first();

        assertEquals(1, count);
    }

    public void assertMachineInfoStored(MavenSession session) {
        MavenExecutionRequest request = session.getRequest();
        Map<String, Object> machineInfo = handle.createQuery("select * from machine_info mi inner join build b on mi.id = b.machine_info_id and b.id = ?")
                .bind(0, request.getStartTime().getTime())
                .first();
        assertNotNull("we did not find machine info for the build", machineInfo);
        Properties systemProperties = request.getSystemProperties();
        assertEquals(systemProperties.get("maven.version"), machineInfo.get("maven_version"));
        assertEquals(systemProperties.get("java.version"), machineInfo.get("java_version"));
        assertEquals(systemProperties.get("env.COMPUTERNAME"), machineInfo.get("computer_name"));
        assertEquals(systemProperties.get("os.name"), machineInfo.get("os"));
        assertEquals(systemProperties.get("user.name"), machineInfo.get("username"));
        assertEquals(systemProperties.get("os.arch"), machineInfo.get("os_arch"));
    }

    public void assertCodeRevision(MavenSession session, CodeRevision codeRevision) {
        MavenExecutionRequest request = session.getRequest();
        Map<String, Object> build = handle.createQuery("select * from build where id = ?")
                .bind(0, request.getStartTime().getTime())
                .first();
        assertEquals(codeRevision.scm, build.get("scm"));
        assertEquals(codeRevision.revision, build.get("scm_revision"));
    }
}
