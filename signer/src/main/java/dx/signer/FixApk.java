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

import java.io.File;
import java.io.IOException;

public class FixApk {
    public static void main(String... args) throws IOException {
        ApkSigns.zipAlign(new File(args[0]).toPath(), new File(args[1]).toPath(), false);
    }
}
