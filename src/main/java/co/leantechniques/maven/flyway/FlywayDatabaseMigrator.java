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
package co.leantechniques.maven.flyway;

import co.leantechniques.maven.h2.DatabaseMigrator;
import com.googlecode.flyway.core.Flyway;
import com.googlecode.flyway.core.util.logging.LogFactory;

import javax.sql.DataSource;

public class FlywayDatabaseMigrator implements DatabaseMigrator {
    public static boolean showFlyWayLogging = false;

    @Override
    public void migrate(DataSource dataSource) {
        if (!showFlyWayLogging) LogFactory.setLogCreator(new NoOpLogCreator());
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        flyway.setInitOnMigrate(true);
        flyway.migrate();
    }
}
