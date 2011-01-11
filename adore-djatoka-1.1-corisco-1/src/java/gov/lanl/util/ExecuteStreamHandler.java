package gov.lanl.util;

/*
 * Copyright  2000,2002,2004-2005 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// MODIFIED FROM ANT CVS HEAD:
//    http://cvs.apache.org/viewcvs.cgi/ant/src/main/org/apache/tools/ant/taskdefs/
/**
 * Used by <code>Execute</code> to handle input and output stream of
 * subprocesses.
 *
 * @since Ant 1.2
 */
public interface ExecuteStreamHandler {

    /**
     * Install a handler for the input stream of the subprocess.
     *
     * @param os output stream to write to the standard input stream of the
     *           subprocess
     * @throws IOException on error
     */
    void setProcessInputStream(OutputStream os) throws IOException;

    /**
     * Install a handler for the error stream of the subprocess.
     *
     * @param is input stream to read from the error stream from the subprocess
     * @throws IOException on error
     */
    void setProcessErrorStream(InputStream is) throws IOException;

    /**
     * Install a handler for the output stream of the subprocess.
     *
     * @param is input stream to read from the error stream from the subprocess
     * @throws IOException on error
     */
    void setProcessOutputStream(InputStream is) throws IOException;

    /**
     * Start handling of the streams.
     * @throws IOException on error
     */
    void start() throws IOException;

    /**
     * Stop handling of the streams - will not be restarted.
     */
    void stop();
}
