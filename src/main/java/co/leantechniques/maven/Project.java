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

import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.plugin.MojoExecution;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class Project extends Artifact {
    private ArrayList<PluginExecution> pluginExecutions = new ArrayList<PluginExecution>();
    private HashMap<String, PluginExecution> indexedExecutions = new HashMap<String, PluginExecution>();

    public Project(String groupId, String artifactId, String version) {
        super(groupId, artifactId, version);
    }

    public void addPluginExecution(ExecutionEvent event) {
        PluginExecution execution = findOrCreateExecution(event);
        if (isMojoFinished(event)) {
            execution.endTime = new Date();
        }
    }

    private PluginExecution findOrCreateExecution(ExecutionEvent event) {
        String pluginExecutionKey = generateKeyFrom(event);

        if (isExecutionAlreadyStored(pluginExecutionKey)) {
            return indexedExecutions.get(pluginExecutionKey);
        }

        return addExecution(event);
    }

    private boolean isExecutionAlreadyStored(String pluginExecutionKey) {
        return indexedExecutions.containsKey(pluginExecutionKey);
    }

    private PluginExecution addExecution(ExecutionEvent event) {
        MojoExecution mojoExecution = event.getMojoExecution();
        PluginExecution execution = new PluginExecution(
                plugin(event), mojoExecution.getGoal(), mojoExecution.getExecutionId()
        );
        pluginExecutions.add(execution);
        indexedExecutions.put(generateKeyFrom(event), execution);
        return execution;
    }

    private String generateKeyFrom(ExecutionEvent event) {
        MojoExecution mojoExecution = event.getMojoExecution();
        return mojoExecution.getGroupId()
                + mojoExecution.getArtifactId()
                + mojoExecution.getGoal()
                + mojoExecution.getExecutionId();
    }

    private boolean isMojoFinished(ExecutionEvent event) {
        return event.getType() == ExecutionEvent.Type.MojoSucceeded;
    }

    private Artifact plugin(ExecutionEvent event) {
        return new Artifact(
                event.getMojoExecution().getGroupId(),
                event.getMojoExecution().getArtifactId(),
                event.getMojoExecution().getVersion()
        );
    }

    public List<PluginExecution> getPluginExecutions() {
        return pluginExecutions;
    }
}
