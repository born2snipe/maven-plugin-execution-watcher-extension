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

import co.leantechniques.maven.scm.CodeRevision;
import org.apache.maven.execution.ExecutionEvent;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.MavenProject;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class BuildInformation {
    private final MavenSession session;
    private String userSpecifiedBuildData;
    private ArrayList<ExecutionEvent> eventsProcessed = new ArrayList<ExecutionEvent>();
    private Date endTime;
    private ArrayList<Project> projects = new ArrayList<Project>();
    private CodeRevision codeRevision;

    public BuildInformation(MavenSession session, String userSpecifiedBuildData, CodeRevision codeRevision) {
        this.userSpecifiedBuildData = userSpecifiedBuildData;
        this.session = session;
        this.codeRevision = codeRevision;
        initializeProjects(session);
    }

    public List<String> getGoals() {
        return session.getRequest().getGoals();
    }

    public String getUserSpecifiedBuildData() {
        return userSpecifiedBuildData;
    }

    public List<ExecutionEvent> getMavenEvents() {
        return eventsProcessed;
    }

    public void addMavenEvent(ExecutionEvent event) {
        eventsProcessed.add(event);
        projectOf(event).addPluginExecution(event);
    }

    public long getId() {
        return getStartTime().getTime();
    }

    public Date getStartTime() {
        return session.getRequest().getStartTime();
    }

    public String getJavaVersion() {
        return getSystemProperty("java.version");
    }

    public String getMavenVersion() {
        return getSystemProperty("maven.version");
    }

    public String getComputerName() {
        return getSystemProperty("env.COMPUTERNAME");
    }

    public String getOsName() {
        return getSystemProperty("os.name");
    }

    private String getSystemProperty(String name) {
        return session.getRequest().getSystemProperties().getProperty(name);
    }

    public Artifact getTopLevelProject() {
        MavenProject project = session.getTopLevelProject();
        return new Project(project.getGroupId(), project.getArtifactId(), project.getVersion());
    }


    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public List<Project> getProjects() {
        return projects;
    }

    private Project projectOf(ExecutionEvent event) {
        MavenProject mavenProject = event.getProject();
        Project currentProject = new Project(
                mavenProject.getGroupId(),
                mavenProject.getArtifactId(),
                mavenProject.getVersion()
        );
        return projects.get(projects.indexOf(currentProject));
    }

    private void initializeProjects(MavenSession session) {
        for (MavenProject mavenProject : session.getProjects()) {
            projects.add(new Project(
                    mavenProject.getGroupId(),
                    mavenProject.getArtifactId(),
                    mavenProject.getVersion()
            ));
        }
    }

    public String getUsername() {
        return getSystemProperty("user.name");
    }

    public String getOsArch() {
        return getSystemProperty("os.arch");
    }

    public CodeRevision getCodeRevision() {
        return codeRevision;
    }
}
