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

import co.leantechniques.maven.flyway.FlywayDatabaseMigrator;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

import java.io.File;

public abstract class AbstractDatabaseTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    protected File databaseFolder;

    @Before
    public void initializeDatabase() throws Exception {
        FlywayDatabaseMigrator.showFlyWayLogging = true;
        databaseFolder = temporaryFolder.newFolder("test.db.directory");
        System.setProperty(H2DatabaseDirectoryProvider.DB_DIRECTORY_KEY, databaseFolder.getAbsolutePath());
    }

    @After
    public void removeTheDatabaseDirectoryKey() throws Exception {
        System.getProperties().remove(H2DatabaseDirectoryProvider.DB_DIRECTORY_KEY);
    }
}
