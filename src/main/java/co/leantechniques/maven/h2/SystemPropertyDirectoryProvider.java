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

import java.io.File;

public class SystemPropertyDirectoryProvider implements DatabaseDirectoryProvider {
    public static final File DEFAULT_LOCATION = new File(System.getProperty("user.home"), ".m2-plugin-execution-watcher");
    public static final String DB_DIRECTORY_KEY = "plugin.execution.watcher.directory";

    @Override
    public File provide() {
        File directory = DEFAULT_LOCATION;
        if (System.getProperty(DB_DIRECTORY_KEY) != null) {
            directory = new File(System.getProperty(DB_DIRECTORY_KEY));
        }
        directory.mkdirs();
        return directory;
    }
}
