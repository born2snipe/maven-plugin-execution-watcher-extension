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
package org.apache.maven.eventspy;

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecution;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExecutionEventBuilder {
    private ExecutionEvent event;
    private MavenSessionBuilder sessionBuilder;
    private ExecutionEvent.Type type;
    private MockMojoExecution mojoExecution;

    public ExecutionEventBuilder() {
        this(new MavenSessionBuilder());
    }

    public ExecutionEventBuilder(MavenSessionBuilder sessionBuilder) {
        this.sessionBuilder = sessionBuilder;
        mojoExecution = new MockMojoExecution();
        event = mock(ExecutionEvent.class);
    }

    public ExecutionEventBuilder withBuildStarting() {
        expectEventType(ExecutionEvent.Type.SessionStarted);
        return this;
    }

    public ExecutionEventBuilder withBuildFinished() {
        expectEventType(ExecutionEvent.Type.SessionEnded);
        return this;
    }

    public ExecutionEventBuilder expectEventType(ExecutionEvent.Type expectedType) {
        this.type = expectedType;
        return this;
    }

    public ExecutionEventBuilder withGoals(String... goals) {
        sessionBuilder.withGoals(goals);
        return this;
    }

    public MavenSessionBuilder.MavenProjectBuildResult withProject(String groupId, String artifactId, String version) {
        return sessionBuilder.withProject(groupId, artifactId, version);
    }

    public ExecutionEvent toEvent() {
        MavenSession session = sessionBuilder.toSession();
        when(event.getSession()).thenReturn(session);
        when(event.getType()).thenReturn(type);
        when(event.getMojoExecution()).thenReturn(mojoExecution);
        if (haveProjectsBeenAddedTo(session)) {
            when(event.getProject()).thenReturn(session.getProjects().get(0));
        }
        return event;
    }

    public PluginExecutionState withPlugin(String groupId, String artifactId, String version, String goal) {
        return withPlugin(groupId, artifactId, version, goal, groupId + ":" + artifactId + ":" + version + ":" + goal);
    }

    public PluginExecutionState withPlugin(String groupId, String artifactId, String version, String goal, String executionId) {
        mojoExecution.setGroupId(groupId);
        mojoExecution.setArtifactId(artifactId);
        mojoExecution.setVersion(version);
        mojoExecution.setGoal(goal);
        mojoExecution.setExecutionId(executionId);

        return new PluginExecutionState() {
            @Override
            public void starting() {
                expectEventType(ExecutionEvent.Type.MojoStarted);
            }

            @Override
            public void successful() {
                expectEventType(ExecutionEvent.Type.MojoSucceeded);
            }
        };
    }

    private boolean haveProjectsBeenAddedTo(MavenSession session) {
        return session.getProjects() != null && session.getProjects().size() > 0;
    }

    public static interface PluginExecutionState {
        void starting();

        void successful();
    }

    private class MockMojoExecution extends MojoExecution {
        private String groupId, artifactId, version, goal, executionId;

        public MockMojoExecution() {
            super((Plugin) null, null, null);
        }

        public String getGroupId() {
            return groupId;
        }

        public void setGroupId(String groupId) {
            this.groupId = groupId;
        }

        public String getArtifactId() {
            return artifactId;
        }

        public void setArtifactId(String artifactId) {
            this.artifactId = artifactId;
        }

        public String getVersion() {
            return version;
        }

        public void setVersion(String version) {
            this.version = version;
        }

        public String getGoal() {
            return goal;
        }

        public void setGoal(String goal) {
            this.goal = goal;
        }

        public String getExecutionId() {
            return executionId;
        }

        public void setExecutionId(String executionId) {
            this.executionId = executionId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MockMojoExecution that = (MockMojoExecution) o;

            if (artifactId != null ? !artifactId.equals(that.artifactId) : that.artifactId != null) return false;
            if (executionId != null ? !executionId.equals(that.executionId) : that.executionId != null) return false;
            if (goal != null ? !goal.equals(that.goal) : that.goal != null) return false;
            if (groupId != null ? !groupId.equals(that.groupId) : that.groupId != null) return false;
            if (version != null ? !version.equals(that.version) : that.version != null) return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = groupId != null ? groupId.hashCode() : 0;
            result = 31 * result + (artifactId != null ? artifactId.hashCode() : 0);
            result = 31 * result + (version != null ? version.hashCode() : 0);
            result = 31 * result + (goal != null ? goal.hashCode() : 0);
            result = 31 * result + (executionId != null ? executionId.hashCode() : 0);
            return result;
        }
    }
}
