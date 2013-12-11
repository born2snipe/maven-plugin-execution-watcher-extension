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

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Cmd {
    private final CommandLine cmd;
    private DefaultExecutor executor = new DefaultExecutor();
    private ExecuteWatchdog watchdog = new ExecuteWatchdog(10000);

    public Cmd(String cmd) {
        this.cmd = new CommandLine(cmd);
        executor.setWatchdog(watchdog);
    }

    public String execute() {
        CaptureOutputHandler captureOutputHandler = new CaptureOutputHandler();
        try {
            executor.setStreamHandler(captureOutputHandler);
            executor.execute(cmd);
        } catch (Exception e) {
            throw new RuntimeException("A problem occurred when trying to run cmd: [" + cmd + "]", e);
        }
        return new String(captureOutputHandler.output.toByteArray());
    }

    public Cmd arg(String arg) {
        cmd.addArgument(arg);
        return this;
    }

    private class CaptureOutputHandler implements ExecuteStreamHandler {
        private final ByteArrayOutputStream output = new ByteArrayOutputStream();

        @Override
        public void setProcessInputStream(OutputStream os) throws IOException {

        }

        @Override
        public void setProcessErrorStream(InputStream is) throws IOException {

        }

        @Override
        public void setProcessOutputStream(InputStream is) throws IOException {
            byte[] buffer = new byte[1024];
            int length = -1;
            while ((length = is.read(buffer)) != -1) {
                output.write(buffer, 0, length);
            }
        }

        @Override
        public void start() throws IOException {

        }

        @Override
        public void stop() {

        }
    }
}
