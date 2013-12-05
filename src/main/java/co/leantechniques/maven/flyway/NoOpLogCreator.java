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

import com.googlecode.flyway.core.util.logging.Log;
import com.googlecode.flyway.core.util.logging.LogCreator;

public class NoOpLogCreator implements LogCreator {
    private final NoOpLog log = new NoOpLog();

    @Override
    public Log createLogger(Class<?> clazz) {
        return log;
    }

    private class NoOpLog implements Log {

        @Override
        public void debug(String message) {

        }

        @Override
        public void info(String message) {

        }

        @Override
        public void warn(String message) {

        }

        @Override
        public void error(String message) {

        }

        @Override
        public void error(String message, Exception e) {

        }
    }
}
