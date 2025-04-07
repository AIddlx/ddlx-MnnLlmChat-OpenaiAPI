# 为MNN安卓端适配兼容openai的API接口

为MNN安卓端适配兼容openai的API接口，
3.8号从MNN仓库创建分支里面更新API适配了：https://github.com/AIddlx/MNN
按照 https://github.com/AIddlx/MNN/blob/master/apps/Android/MnnLlmChat/README_CN.md  进行安装编译就行

API使用时模型名称设置为mnn-local， baseUrl可在模型对话界面的API Setting 内复制和开关，网络接口自动切到换WiFi、流量、本地
api暂时仅支持文本

编译好的程序在：https://pan.quark.cn/s/371bc3bd34f6 有问题可以关注+私信 不使用时一定要清退程序！！！不然会常驻暴露0.0.0.0:8080（可以设置别的端口）不用的时候也要记得清退程序
自己用于低功耗文本处理的，现在还只支持文本功能，暂时不支持文件（图片，文档）功能哦，

之后的时间我会重新弄个极简的程序，那样会更方便的阅读代码。
技术演示：【手机给电脑提供大模型API】 https://www.bilibili.com/video/BV1HN98Y8EC9 关注我的b站，更新后会第一时间放出来


本仓库的代码就不用再克隆和使用了
