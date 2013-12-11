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

import co.leantechniques.maven.BuildInformation;
import co.leantechniques.maven.BuildInformationRepository;
import co.leantechniques.maven.BuildInformationRepositoryProvider;
import co.leantechniques.maven.scm.CodeRevisionProvider;
import co.leantechniques.maven.scm.ScmRevisionProvider;
import org.apache.maven.execution.BuildFailure;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;

import java.io.File;
import java.util.Date;

@Component(role = EventSpy.class)
public class PluginWatcherEventSpy extends AbstractEventSpy {
    public static final String BUILD_DATA_KEY = "plugin.execution.watcher.build.data";

    private BuildInformationRepositoryProvider buildInformationRepositoryProvider = new BuildInformationRepositoryProvider();
    private BuildInformationRepository buildInformationRepository;
    private BuildInformation currentBuildInformation;
    private CodeRevisionProvider codeRevisionProvider = new ScmRevisionProvider();

    @Override
    public void init(Context context) throws Exception {
        System.out.println("------------------------------------------------------------------------");
        System.out.println(" TRACKING BUILD STATS");
        System.out.println("------------------------------------------------------------------------");
        buildInformationRepository = buildInformationRepositoryProvider.provide();
        buildInformationRepository.initialize(context);
    }

    @Override
    public void onEvent(Object event) throws Exception {

        if (event instanceof ExecutionEvent) {
            ExecutionEvent executionEvent = (ExecutionEvent) event;

            if (shouldInitializeBuildInformation(executionEvent)) {
                File baseDirectory = new File(((ExecutionEvent) event).getSession().getRequest().getBaseDirectory());
                currentBuildInformation = new BuildInformation(
                        executionEvent.getSession(),
                        System.getProperty(BUILD_DATA_KEY),
                        codeRevisionProvider.determineRevisionOf(baseDirectory)
                );
            }

            if (isPluginRelated(executionEvent)) {
                currentBuildInformation.addMavenEvent(executionEvent);
            } else if (isBuildFinished(executionEvent) && isBuildSuccessful(executionEvent)) {
                currentBuildInformation.setEndTime(new Date());
                buildInformationRepository.save(currentBuildInformation);
            }
        }
    }

    private boolean shouldInitializeBuildInformation(ExecutionEvent executionEvent) {
        MavenSession session = executionEvent.getSession();
        return currentBuildInformation == null
                && session.getProjects() != null
                && session.getProjects().size() > 0;
    }

    private boolean isBuildSuccessful(ExecutionEvent executionEvent) {
        boolean passing = true;
        MavenSession session = executionEvent.getSession();
        for (MavenProject project : session.getProjects()) {
            if (session.getResult().getBuildSummary(project) instanceof BuildFailure) {
                passing = false;
                break;
            }
        }
        return passing;
    }

    @Override
    public void close() throws Exception {
        buildInformationRepository.cleanUp();
    }

    private boolean isBuildFinished(ExecutionEvent executionEvent) {
        return executionEvent.getType() == ExecutionEvent.Type.SessionEnded;
    }

    private boolean isPluginRelated(ExecutionEvent event) {
        ExecutionEvent.Type type = event.getType();
        switch (type) {
            case MojoSucceeded:
            case MojoFailed:
            case MojoStarted:
                return true;
        }
        return false;
    }

    protected BuildInformation getCurrentBuildInformation() {
        return currentBuildInformation;
    }

    public void setCodeRevisionProvider(CodeRevisionProvider codeRevisionProvider) {
        this.codeRevisionProvider = codeRevisionProvider;
    }
}
