/**
 * dx-signer
 *
 * Copyright 2022 北京顶象技术有限公司
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dx.signer;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.ref.WeakReference;

public class JTextAreaOutputStream extends OutputStream {
    static private WeakReference<JTextArea> mDestination = new WeakReference<>(null);
    PrintStream orgOut;

    static JTextAreaOutputStream hjOut = new JTextAreaOutputStream();
    static JTextAreaOutputStream hjErr = new JTextAreaOutputStream();

    private JTextAreaOutputStream() {
    }

    public synchronized static void hijack(JTextArea destination) {
        if (hjOut.orgOut == null) {
            hjOut.orgOut = System.out;
        }
        if (hjErr.orgOut == null) {
            hjErr.orgOut = System.err;
        }
        mDestination = new WeakReference<>(destination);

        System.setOut(new PrintStream(hjOut, true));
        System.setErr(new PrintStream(hjErr, true));
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
        final String text = new String(buffer, offset, length);
        orgOut.write(buffer, offset, length);
        JTextArea t = mDestination.get();
        if (t != null) {
            SwingUtilities.invokeLater(() -> t.append(text));
        }
    }

    @Override
    public void write(int b) throws IOException {
        write(new byte[]{(byte) b}, 0, 1);
    }
}

