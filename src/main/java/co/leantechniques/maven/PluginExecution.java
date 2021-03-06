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

import java.util.Date;

public class PluginExecution extends Artifact {
    public final String goal;
    public final String executionId;
    public final Date startTime = new Date();
    public Date endTime;

    public PluginExecution(Artifact plugin, String goal, String executionId) {
        super(plugin.groupId, plugin.artifactId, plugin.version);
        this.goal = goal;
        this.executionId = executionId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PluginExecution that = (PluginExecution) o;

        if (executionId != null ? !executionId.equals(that.executionId) : that.executionId != null) return false;
        if (goal != null ? !goal.equals(that.goal) : that.goal != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (goal != null ? goal.hashCode() : 0);
        result = 31 * result + (executionId != null ? executionId.hashCode() : 0);
        return result;
    }
}
