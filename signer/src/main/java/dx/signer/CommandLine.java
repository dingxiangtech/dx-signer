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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.impl.SimpleLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class CommandLine {
    public static void main(String... args) throws IOException {
        System.setProperty(SimpleLogger.SHOW_LOG_NAME_KEY, "false");
        System.setProperty(SimpleLogger.SHOW_THREAD_NAME_KEY, "false");

        Logger log = LoggerFactory.getLogger(CommandLine.class);


        Properties p = new Properties();
        try {
            if (args.length < 1 || !args[0].equals("sign")) {
                throw new RuntimeException("参数解析失败");
            }
            for (int i = 1; i < args.length; i += 2) {
                String key = args[i];
                String value = args[i + 1];
                if ("--config".equals(key)) {
                    Properties p2;
                    try {
                        p2 = load(Paths.get(value));
                    } catch (IOException e) {
                        throw new RuntimeException("文件" + value + "解析失败", e);
                    }
                    p.putAll(p2);
                } else {
                    if (!key.startsWith("--")) {
                        throw new RuntimeException("参数解析失败");
                    }
                    if (key.equals("--in")) {
                        p.setProperty("in-filename", "");
                    }
                    p.setProperty(key.substring(2), value);
                }
            }
            for (String k : new String[]{"in", "out", "ks"}) {
                String v = p.getProperty(k, "");
                if (v == null || v.length() == 0) {
                    throw new RuntimeException("请指定参数" + k);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("USAGE: java -jar dx-signer.apk sign [--option value]*");
            System.err.println("  option:");
            System.err.println("    --config 配置文件");
            System.err.println("    --in 输入文件apk、aab");
            System.err.println("    --out 输出文件、文件夹");
            System.err.println("    --ks Keystore位置");
            System.err.println("    --ks-pass Keystore密码");
            System.err.println("    --ks-key-alias");
            System.err.println("    --key-pass");
            System.err.println("    --channel-list 渠道清单");
            System.exit(3);
        }
        Path input = Paths.get(p.getProperty("in"));
        Path ks = Paths.get(p.getProperty("ks"));
        String ksPass = p.getProperty("ks-pass", "");

        String ksKeyAlias = p.getProperty("ks-key-alias", "");
        String keyPass = p.getProperty("key-pass", "");
        if (p.getProperty("channel-list", "").length() > 0) {
            Path out = detectOutDir(p.getProperty("out"));

            int result = SignWorker.signChannelApk(input, p.getProperty("in-filename", ""),
                    out,
                    Paths.get(p.getProperty("channel-list")),
                    ks, ksPass, ksKeyAlias, keyPass);

            if (result != 0) {
                log.error("多渠道失败");
                System.exit(2);
            }
        } else {
            Path out = Paths.get(p.getProperty("out"));
            int result = SignWorker.signApk(input, out, ks,
                    ksPass, ksKeyAlias, keyPass);

            if (result != 0) {
                log.error("签名失败");
                System.exit(2);
            }
        }
    }

    static Properties load(Path configFile) throws IOException {
        Properties p = new Properties();
        try (BufferedReader r = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
            p.load(r);
        }
        return p;
    }

    static Path detectOutDir(String out) {
        Path path = Paths.get(out);
        return (out.endsWith("/") || Files.isDirectory(path)) ? path : path.toAbsolutePath().getParent();
    }
}
