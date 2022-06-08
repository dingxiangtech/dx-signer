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
package dx.channel;

import com.meituan.android.walle.ChannelWriter;
import com.meituan.android.walle.SignatureNotFoundException;
import dx.zip.FastZipEntry;
import dx.zip.FastZipIn;
import dx.zip.Source;
import pxb.android.Res_value;
import pxb.android.axml.Axml;
import pxb.android.axml.NodeVisitor;
import pxb.android.axml.R;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

public class ChannelBuilder implements AutoCloseable {
    private static final String Android_NS = "http://schemas.android.com/apk/res/android";
    private final KeyStore.PrivateKeyEntry key;
    private final List<FastZipEntry> entries;
    private final Axml axml;
    private final FastZipIn in;

    public ChannelBuilder(Path template, KeyStore.PrivateKeyEntry key) throws IOException {
        this.key = key;
        this.in = new FastZipIn(template.toFile());
        entries = ApkSigns.cleanup(in.entries(), true);
        Optional<FastZipEntry> a = entries.stream().filter(e -> e.utf8Name().equals("AndroidManifest.xml")).findFirst();
        if (!a.isPresent()) {
            throw new RuntimeException("no AndroidManifest.xml in apk");
        }
        ByteBuffer bb;
        try {
            bb = in.getUncompressed(a.get());
        } catch (DataFormatException e) {
            throw new IOException(e);
        }
        axml = Axml.parse(bb);
        entries.remove(a.get());
        entries.sort(Comparator.comparing(FastZipEntry::utf8Name));
    }

    public static void updateUM(Axml axml, String keyValue) throws IOException {
        updateMeta(axml, "UMENG_CHANNEL", keyValue);
        updateMeta(axml, "CHANNEL", keyValue);
    }

    private static void updateMeta(Axml axml, String keyName, String keyValue) {
        Axml.Node manifest = axml.findFirst("manifest");
        Axml.Node application = manifest.findFirst("application");

        Axml.Node k = application.children
                .stream()
                .filter(n -> {
                            if (n.name.equals("meta-data")) {
                                Axml.Node.Attr attr = n.findFirstAttr(R.attr.name);
                                if (attr.value.type == Res_value.TYPE_STRING) {
                                    return attr.value.raw.equals(keyName);
                                }
                            }
                            return false;
                        }
                ).findFirst().orElseGet(() -> {
                    NodeVisitor n = application.child(null, "meta-data");
                    n.attr(Android_NS, "name",
                            R.attr.name, keyName, Res_value.newStringValue(keyName));
                    return (Axml.Node) n;
                });

        k.replace(Android_NS, "value",
                R.attr.value, keyValue, Res_value.newStringValue(keyValue));
    }

    public static List<String> readChannelList(Path channelList) throws IOException {
        return Files.readAllLines(channelList, StandardCharsets.UTF_8)
                .stream()
                .map(String::trim)
                .filter(s -> !s.startsWith("#"))
                .map(s -> s.split("#")[0])
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public void build(String channel, Path out) throws IOException {
        out = out.toAbsolutePath();
        Path parent = out.getParent();
        Files.createDirectories(parent);
        Path tmp = Files.createTempFile(parent, "tmp", ".apk");
        Files.deleteIfExists(out);
        try {
            try (AxmlExFastZipOut zout = new AxmlExFastZipOut(tmp.toFile());) {
                zout.initByAndroidManifestContent(axml);
                zout.copyPart(in, entries);
                updateUM(axml, channel);
                ByteBuffer bb = ByteBuffer.wrap(axml.toByteArray());
                Source am = Source.newRawEntry("AndroidManifest.xml", bb);
                zout.copyPart(am, am.entries());
                zout.copyEnd();
            }

            ApkSigns.sign(tmp, out, this.key, false);
            try {
                ChannelWriter.put(out.toFile(), channel);
            } catch (SignatureNotFoundException e) {
                throw new IOException(e);
            }
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    @Override
    public void close() throws IOException {
        in.close();
    }
}
