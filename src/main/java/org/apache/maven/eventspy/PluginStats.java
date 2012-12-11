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

import java.util.Date;

public class PluginStats {
    public Type type;
    public Project project;
    public Plugin plugin;
    public String executionId;
    public Date timestamp;
    public int executionHashCode;

    public static enum Type {
        START(ExecutionEvent.Type.MojoStarted),
        FAILED(ExecutionEvent.Type.MojoFailed),
        SUCCEED(ExecutionEvent.Type.MojoSucceeded);

        private ExecutionEvent.Type mavenType;

        private Type(ExecutionEvent.Type mavenType) {
            this.mavenType = mavenType;
        }

        public static Type valueOf(ExecutionEvent.Type mavenType) {
            for (Type type : values()) {
                if (type.mavenType.equals(mavenType)) {
                    return type;
                }
            }
            return null;
        }
    }

    public static class Project {
        public String groupId;
        public String artifactId;
        public String version;
    }

    public static class Plugin {
        public String groupId;
        public String artifactId;
        public String version;
        public String goal;
    }
}
