# Apk签名和多渠道打包工具

## 编译方法

```bash
./gradlew fatjar
# 输出文件位于 signer/build/libs/dx-signer.jar
```

## 图形界面

请双击`dx-signer.jar`文件启动，或者使用命令行启动。

```
java -jar dx-signer.jar
```

    您需要Java 8+的运行环境，推荐使用OpenJDK的实现。
    请根据界面提示操作。
    当指定渠道清单时，工具进入多渠道模式， 如果 输出apk/aab 指向一个文件，那么渠道包会保存在同目录下； 如果 输出apk/aab 指向目录，那么渠道包会保存在这个目录下。

## 命令行界面

```bash
java -jar dx-signer.apk sign [--option value]+
```

    您需要Java 8+的运行环境，推荐使用OpenJDK的实现。
    其中第一个参数必须是`sign`用于区分命令行还是图形界面。
    option可以重复出现，后面的值覆盖前面的。


支持的`option`如下

| option       | type   | 必须  | 描述                                                             |
| :----------- | :----- | :---: | ---------------------------------------------------------------- |
| config       | Path   |       | 配置文件,文件格式满足java.util.Properties要求，key值与option一样 |
| in           | Path   |  是   | 输入文件apk、aab                                                 |
| out          | Path   |  是   | 输出文件或文件夹                                                 |
| ks           | Path   |  是   | Keystore位置                                                     |
| ks-pass      | String |       | Keystore密码, 默认android                                        |
| ks-key-alias | String |       | alias，默认第一个                                                |
| key-pass     | String |       | alias密码，默认与ks-pass相同                                     |
| channel-list | Path   |       | 渠道清单，格式见 多渠道                                          |
| in-filename  | String |       | 多渠道模式下，指定输入文件名                                     |

    当指定渠道清单时，工具进入多渠道模式，out参数需要指向一个存在的目录。
    config可以视作option的集合， 其避免命令行过长。

例如：

```bash

# 使用etc/cfg.properties指定的参数进行签名
java -jar dx-signer.apk sign --config etc/cfg.properties

# 使用etc/cfg.properties, 并使用keystore.properties里面的证书信息进行签名
java -jar dx-signer.apk sign --config etc/cfg.properties --config keystore.properties

# 使用etc/cfg.properties指定的参数, 但是修改掉apk的输出路径
java -jar dx-signer.apk sign --config etc/cfg.properties --out path/to/other/location.apk

# 不使用config进行签名
java -jar dx-signer.apk sign --in in.apk --out signed.apk --ks keystore.JKS --ks-pass android

# 多渠道
mkdir -p out-apks
java -jar dx-signer.apk sign --config keystore.properties \
    --in in.apk --channel-list channel.txt \
    --out out-apks/

```


## 多渠道

请准备渠道清单文件`channel.txt`， 格式为每一行一个渠道， 例如：

```
0001_my
0003_baidu
0004_huawei
0005_oppo
0006_vivo
0007_360
0008_xiaomi
0009_yingyongbao
0011_lianxiang
0012_meizu
0013_yingyonghui
0014_ali
# 注释行
0015_test  # 注释内容
```

### 读取渠道信息：UMENG_CHANNEL

输出的Apk中将会包含`UMENG_CHANNEL`的`mata-data`

```xml
<application ... >
    <meta-data
        android:name="UMENG_CHANNEL"
        android:value="XXX" />
</application>
```

您可以读取这个字段。

```java
public static String getChannel(Context ctx) {
    String channel = "";
    try {
        ApplicationInfo appInfo = ctx.getPackageManager().getApplicationInfo(ctx.getPackageName(),
                PackageManager.GET_META_DATA);
        channel = appInfo.metaData.getString("UMENG_CHANNEL");
    } catch (PackageManager.NameNotFoundException ignore) {
    }
    return channel;
}
```

### 读取渠道信息：Walle

输出的Apk也包含Walle风格的渠道信息

您可以在使用[Walle](https://github.com/Meituan-Dianping/walle)的方式进行读取。


```gradle
implementation 'com.meituan.android.walle:library:1.1.7'
```


```java

String channel = WalleChannelReader.getChannel(this.getApplicationContext());

```

## License

```
dx-signer

Copyright 2022 北京顶象技术有限公司

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
