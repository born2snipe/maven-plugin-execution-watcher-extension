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
import org.h2.jdbcx.JdbcConnectionPool;

import javax.sql.DataSource;
import java.io.File;

public class H2DatabaseManager {
    private static final Object LOCK = new Object();
    private JdbcConnectionPool cachedDataSource;
    private DatabaseDirectoryProvider directoryProvider;
    private DatabaseMigrator databaseMigrator;

    public H2DatabaseManager() {
        this(new SystemPropertyDirectoryProvider(), new FlywayDatabaseMigrator());
    }

    public H2DatabaseManager(DatabaseDirectoryProvider directoryProvider, DatabaseMigrator databaseMigrator) {
        this.directoryProvider = directoryProvider;
        this.databaseMigrator = databaseMigrator;
    }

    public DataSource load() {
        synchronized (LOCK) {
            if (cachedDataSource == null) {
                File dbLocation = directoryProvider.provide();
                cachedDataSource = JdbcConnectionPool.create("jdbc:h2:" + dbLocation.getAbsolutePath() + "/stats;AUTO_SERVER=TRUE", "", "");
                databaseMigrator.migrate(cachedDataSource);
            }
            return cachedDataSource;
        }
    }

    public void unload() {
        synchronized (LOCK) {
            if (cachedDataSource != null) {
                cachedDataSource.dispose();
                cachedDataSource = null;
            }
        }
    }
}
