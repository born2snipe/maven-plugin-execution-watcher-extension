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
package co.leantechniques.maven;

import org.apache.maven.eventspy.ExecutionEventBuilder;
import org.apache.maven.execution.ExecutionEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.*;

public class ProjectTest {
    private Project project;
    private ExecutionEventBuilder eventBuilder;

    @Before
    public void setUp() throws Exception {
        project = new Project("group", "artifact", "version");
        eventBuilder = new ExecutionEventBuilder();
    }

    @Test
    public void shouldStoreExecutionsOfDifferentPlugins_DifferentGroupIds() {
        pluginRuns("plugin-group", "plugin-artifact", "plugin-version", "plugin-goal", "execution");
        pluginRuns("other-plugin-group", "plugin-artifact", "plugin-version", "plugin-goal", "execution");

        List<PluginExecution> executions = project.getPluginExecutions();
        assertFullExecution("plugin-group", "plugin-artifact", "plugin-version", "plugin-goal", "execution", executions.get(0));
        assertFullExecution("other-plugin-group", "plugin-artifact", "plugin-version", "plugin-goal", "execution", executions.get(1));
    }

    @Test
    public void shouldStoreExecutionsOfDifferentPlugins_DifferentArtifactIds() {
        pluginRuns("plugin-group", "plugin-artifact", "plugin-version", "plugin-goal", "execution");
        pluginRuns("plugin-group", "other-plugin-artifact", "plugin-version", "plugin-goal", "execution");

        List<PluginExecution> executions = project.getPluginExecutions();
        assertFullExecution("plugin-group", "plugin-artifact", "plugin-version", "plugin-goal", "execution", executions.get(0));
        assertFullExecution("plugin-group", "other-plugin-artifact", "plugin-version", "plugin-goal", "execution", executions.get(1));
    }

    @Test
    public void shouldStoreExecutionsOfDifferentPluginGoals() {
        pluginRuns("plugin-group", "plugin-artifact", "plugin-version", "plugin-goal", "execution");
        pluginRuns("plugin-group", "plugin-artifact", "plugin-version", "plugin-other-goal", "execution");

        List<PluginExecution> executions = project.getPluginExecutions();
        assertFullExecution("plugin-group", "plugin-artifact", "plugin-version", "plugin-goal", "execution", executions.get(0));
        assertFullExecution("plugin-group", "plugin-artifact", "plugin-version", "plugin-other-goal", "execution", executions.get(1));
    }

    @Test
    public void shouldStoreTheEndDateWhenAPluginFinishes() {
        pluginRuns("plugin-group", "plugin-artifact", "plugin-version", "plugin-goal", "execution");

        List<PluginExecution> executions = project.getPluginExecutions();

        assertFullExecution("plugin-group", "plugin-artifact", "plugin-version", "plugin-goal", "execution", executions.get(0));
    }

    @Test
    public void shouldStoreTheExecutionWhenThePluginStarts() {
        project.addPluginExecution(pluginStarting("plugin-group", "plugin-artifact", "plugin-version", "plugin-goal", "execution"));

        List<PluginExecution> executions = project.getPluginExecutions();
        PluginExecution execution = executions.get(0);

        assertExecution("plugin-group", "plugin-artifact", "plugin-version", "plugin-goal", "execution", execution);
        assertNotNull(execution.startTime);
        assertNull(execution.endTime);
    }

    private void assertExecution(String group, String artifact, String version, String goal, String executionId, PluginExecution execution) {
        assertEquals(group, execution.groupId);
        assertEquals(artifact, execution.artifactId);
        assertEquals(version, execution.version);
        assertEquals(goal, execution.goal);
        assertEquals(executionId, execution.executionId);
    }

    private ExecutionEvent pluginStarting(String groupId, String artifactId, String version, String goal, String executionId) {
        eventBuilder.withPlugin(groupId, artifactId, version, goal, executionId).starting();
        return eventBuilder.toEvent();
    }

    private ExecutionEvent pluginSuccessful(String groupId, String artifactId, String version, String goal, String executionId) {
        eventBuilder.withPlugin(groupId, artifactId, version, goal, executionId).successful();
        return eventBuilder.toEvent();
    }

    private void pluginRuns(String groupId, String artifactId, String version, String goal, String executionId) {
        project.addPluginExecution(pluginStarting(groupId, artifactId, version, goal, executionId));
        project.addPluginExecution(pluginSuccessful(groupId, artifactId, version, goal, executionId));
    }

    private void assertFullExecution(String group, String artifact, String version, String goal, String executionId, PluginExecution execution) {
        assertExecution(group, artifact, version, goal, executionId, execution);
        assertNotNull("we should have set the endTime", execution.endTime);
        assertNotNull("we should have set the startTime", execution.startTime);
    }
}
