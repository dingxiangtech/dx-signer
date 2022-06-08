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

import dx.zip.AxmlFastZipOut;
import pxb.android.Res_value;
import pxb.android.axml.Axml;

import java.io.File;
import java.io.IOException;

class AxmlExFastZipOut extends AxmlFastZipOut {
    public AxmlExFastZipOut(File out) throws IOException {
        super(out);
    }

    public void initByAndroidManifestContent(Axml axml) {
        Axml.Node manifest = axml.findFirst("manifest");
        if (manifest != null) {
            Axml.Node application = axml.findFirst("application");
            if (application != null) {
                // FIXME extractNativeLibs is 0x010104ea in aapt but axml.R.attr.extractNativeLibs is 0x010104d6
                Axml.Node.Attr attr = application.findFirstAttr(0x010104ea);
                if (attr != null) {
                    Res_value v = attr.value;
                    if (v.type == Res_value.TYPE_INT_BOOLEAN) {
                        // extractNativeLibs 是false时， 要求 so 4k对齐
                        super.setNativeLibraryStore4K(v.data == 0);
                    }
                }
            }

            Axml.Node usessdk = manifest.findFirst("uses-sdk");
            if (usessdk != null) {
                //  android:targetSdkVersion(0x01010270)=(type 0x10)0x17
                Axml.Node.Attr attr = usessdk.findFirstAttr(0x01010270);
                if (attr != null) {
                    Res_value v = attr.value;
                    if (v.type >= Res_value.TYPE_FIRST_INT && v.type <= Res_value.TYPE_LAST_INT) {
                        setTargetSdk(v.data);
                    }
                }
            }
        }
    }
}
