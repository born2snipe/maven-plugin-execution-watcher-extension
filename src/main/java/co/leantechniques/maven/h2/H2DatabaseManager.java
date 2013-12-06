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

import co.leantechniques.maven.flyway.NoOpLogCreator;
import com.googlecode.flyway.core.Flyway;
import com.googlecode.flyway.core.util.logging.LogFactory;
import org.h2.jdbcx.JdbcConnectionPool;

import javax.sql.DataSource;
import java.io.File;

public class H2DatabaseManager {
    private static final Object LOCK = new Object();
    private File dbLocation;
    private boolean showFlyWayLogging;
    private DataSource cachedDataSource;

    public H2DatabaseManager(File dbLocation) {
        this(dbLocation, false);
    }

    public H2DatabaseManager(File dbLocation, boolean showFlyWayLogging) {
        this.dbLocation = dbLocation;
        this.showFlyWayLogging = showFlyWayLogging;
        dbLocation.mkdirs();
    }

    public DataSource load() {
        synchronized (LOCK) {
            if (cachedDataSource == null) {
                cachedDataSource = JdbcConnectionPool.create("jdbc:h2:" + dbLocation.getAbsolutePath() + "/stats;AUTO_SERVER=TRUE", "", "");
                applySqlMigrationScripts();
            }
            return cachedDataSource;
        }
    }

    private void applySqlMigrationScripts() {
        if (!showFlyWayLogging) LogFactory.setLogCreator(new NoOpLogCreator());
        Flyway flyway = new Flyway();
        flyway.setDataSource(cachedDataSource);
        flyway.setInitOnMigrate(true);
        flyway.migrate();
    }

    public void setDirectoryOfRepository(File directoryOfRepository) {
        this.dbLocation = directoryOfRepository;
    }
}
