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

import org.apache.maven.execution.*;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class MavenSessionBuilder {
    private final MavenSession session;
    private ArrayList<MavenProject> projectsInBuild = new ArrayList<MavenProject>();

    public MavenSessionBuilder() {
        this(new Date());
    }

    public MavenSessionBuilder(Date startTime) {
        session = new MavenSession(null, null, new DefaultMavenExecutionRequest(), new DefaultMavenExecutionResult());
        session.getRequest().setStartTime(startTime);
    }

    public MavenSession toSession() {
        if (projectsInBuild.size() > 0) {
            session.setProjects(projectsInBuild);
        }
        return session;
    }

    public MavenSessionBuilder withGoals(String... goals) {
        session.getRequest().setGoals(Arrays.asList(goals));
        return this;
    }

    public MavenSessionBuilder withJavaVersion(String version) {
        setSystemProperty("java.version", version);
        return this;
    }

    public MavenSessionBuilder withComputerName(String name) {
        setSystemProperty("env.COMPUTERNAME", name);
        return this;
    }

    public MavenSessionBuilder withOSName(String name) {
        setSystemProperty("os.name", name);
        return this;
    }

    public MavenSessionBuilder withUsername(String username) {
        setSystemProperty("user.name", username);
        return this;
    }

    private void setSystemProperty(String name, String value) {
        session.getRequest().getSystemProperties().put(name, value);
    }

    public MavenSessionBuilder withMavenVersion(String version) {
        setSystemProperty("maven.version", version);
        return this;
    }

    public MavenSessionBuilder withOSArch(String arch) {
        setSystemProperty("os.arch", arch);
        return this;
    }

    public MavenSessionBuilder withBaseDirectory(File directory) {
        session.getRequest().setBaseDirectory(directory);
        return this;
    }

    public MavenProjectBuildResult withProject(String groupId, String artifactId, String version) {
        final MavenProject project = mavenProject(groupId, artifactId, version);
        projectsInBuild.add(project);
        return new MavenProjectBuildResult() {
            @Override
            public void withSuccess() {
                session.getResult().addBuildSummary(new BuildSuccess(project, 0));
            }

            @Override
            public void withFailure() {
                session.getResult().addBuildSummary(new BuildFailure(project, 0, null));
            }
        };
    }

    private MavenProject mavenProject(String groupId, String artifactId, String version) {
        MavenProject project = new MavenProject();
        project.setGroupId(groupId);
        project.setArtifactId(artifactId);
        project.setVersion(version);
        return project;
    }

    public static interface MavenProjectBuildResult {
        public void withSuccess();

        public void withFailure();
    }
}
