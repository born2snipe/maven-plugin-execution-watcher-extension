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
package co.leantechniques.maven.scm;

import org.openide.util.lookup.ServiceProvider;

import java.io.File;

@ServiceProvider(service = ScmRevisionQuery.class)
public class GitRevisionQuery implements ScmRevisionQuery {
    @Override
    public boolean supports(File directory) {
        return new File(directory, ".git").exists();
    }

    @Override
    public CodeRevision queryRevision(File directory) {
        Cmd cmd = new Cmd("git").arg("log").arg("-1").arg("--pretty=oneline");
        String hash = cmd.execute().split("\\s")[0];
        return new CodeRevision("git", hash);
    }
}
