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


import dx.channel.ApkSigns;
import dx.channel.ChannelBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.List;
import java.util.zip.DataFormatException;

public class SignWorker {
    private static final Logger log = LoggerFactory.getLogger(SignWorker.class);

    private static void sign(Path inApk, Path ksPath, String ksPass, String keyAlias, String keyPass, Path outApk) throws Throwable {
        KeyStore.PrivateKeyEntry key = ApkSigns.loadKey(ksPath, ksPass, keyAlias, keyPass);
        ApkSigns.sign(inApk, outApk, key, inApk.getFileName().toString().endsWith("aab"));
    }

    public static int signApk(Path apkUnsigned, Path apkOut, Path ksPath, String ksPass, String keyAlias, String keyPass) {
        Path tmp = null;
        String suffix = apkUnsigned.getFileName().toString().endsWith("aab") ? "aab" : "apk";
        try {
            Path p = apkOut.toAbsolutePath().getParent();
            if (!Files.exists(p)) {
                Files.createDirectories(p);
            }
            tmp = Files.createTempFile(p, "tmpsigner", "." + suffix);
        } catch (IOException e) {
            e.printStackTrace(System.err);
            return 2;
        }

        try {
            log.info("{}", "> 签名中, 请稍等 ...");

            log.info("{}", ">> 清理原有签名, 对齐 ...");
            ApkSigns.zipAlign(apkUnsigned, tmp, false);
            log.info("{}", "<< 完成");

            log.info("{}", ">> 签名 ...");
            sign(tmp, ksPath, ksPass, keyAlias, keyPass, apkOut);
            log.info("{}", "<< 完成");

            log.info("{}", "< 签名结束, 结果： 完成");
            log.info("  输出APK: {}", apkOut);
        } catch (Throwable e) {
            log.info("签名结束, 结果： 失败", e);
            return -1;
        } finally {
            if (tmp != null) {
                try {
                    Files.deleteIfExists(tmp);
                } catch (IOException ignore) {
                }
            }
        }
        return 0;
    }

    public static int signChannelApk(Path input, String inputFileName, Path outDir,
                                     Path channelListFile,
                                     Path ksPath,
                                     String ksPass,
                                     String keyAlias,
                                     String keyPass) throws IOException {

        if (inputFileName == null || inputFileName.trim().length() == 0) {
            inputFileName = input.getFileName().toString();
        }
        if (inputFileName.endsWith(".aab")) {
            throw new RuntimeException("only .apk supported");
        }

        List<String> channelList = ChannelBuilder.readChannelList(channelListFile);
        log.info("读取到{}个渠道", channelList.size());

        KeyStore.PrivateKeyEntry key = ApkSigns.loadKey(ksPath, ksPass, keyAlias, keyPass);
        try (ChannelBuilder cb = new ChannelBuilder(input, key)) {
            log.info("已加载模板: {}", input);
            int dot = inputFileName.lastIndexOf('.');
            String apkName = dot > 0 ? inputFileName.substring(0, dot) : inputFileName;
            if (apkName.startsWith("dx_unsigned_")) {
                apkName = apkName.substring("dx_unsigned_".length());
            }
            for (String channel : channelList) {
                String safeName = String.format("SIGNED_%s-%s.apk", apkName, channel)
                        .replace('/', '_')
                        .replace('\\', '_')
                        .replace(' ', '_');
                Path outPath = outDir.resolve(safeName);
                log.info("正在输出渠道: {}", channel);
                try {
                    cb.build(channel, outPath);
                    log.info("已经生成: {}", outPath);
                }catch (Throwable e) {
                    log.error("多渠道失败", e);
                    return 1;
                }
            }
            log.info("多渠道完成: {}", outDir);
        }

        return 0;
    }

}
