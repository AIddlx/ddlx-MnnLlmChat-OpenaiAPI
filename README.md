# 为MNN安卓端适配兼容openai的API接口

为MNN安卓端适配兼容openai的API接口，
临时存放，后续会从MNN仓库创建分支里面更新API适配



我实现的代码部分完全由AI生成，如果商用需要自己洗代码！
临时比较仓促代码没有整理过，干脆把src文件夹上传过来。
之后的时间我会重新弄个极简的程序，那样会更方便的阅读代码。https://www.bilibili.com/video/BV1HN98Y8EC9 关注我的b站，更新后会第一时间放出来

按照官方的https://github.com/alibaba/MNN/blob/master/apps/Android/MnnLlmChat/README_CN.md
教程下载编译好之后克隆本仓库
git clone https://github.com/AIddlx/ddlx.git
克隆后复制src、build.gradle到 MNN\apps\Android\MnnLlmChat\app ，
兼容性适配：
1.Android studio编译时要修改gradle/wrapper/gradle-wrapper.properties内的
distributionUrl=https\://mirrors.aliyun.com/gradle/distributions/v8.10.2/gradle-8.10.2-bin.zip
2.Java版本 需要17（需要java15的文本块支持），gradle-wrapper.properties要修改distributionUrl=https\://mirrors.aliyun.com/gradle/distributions/v8.10.2/gradle-8.10.2-bin.zip。

已经验证的仓库版本：
git log -1
commit 05c45045f0016293b78d7786772397a2a37b1d9a (HEAD -> master, origin/master, origin/HEAD)
Merge: 9070fc36 c1430c20Author: 王召德 <8401806+wangzhaode@users.noreply.github.com>
Date: Tue Feb 25 20:31:49 2025 +0800 
Merge pull request #3273 from Yogayu/master
 Update local model debug Instructions

官方更新了Version 0.3.0版本的MnnLlmChat，我没有验证该代码是否可以运行

编译好的程序在：https://pan.quark.cn/s/dba96c666fdf 有问题可以关注+私信 不使用时一定要清退程序！！！不然会常驻暴露0.0.0.0:8080（可以设置别的端口）不用的时候也要记得清退程序
自己用于低功耗文本处理的，现在还只支持文本功能，暂时不支持文件（图片，文档）功能哦，

技术演示：【手机给电脑提供大模型API】 https://www.bilibili.com/video/BV1HN98Y8EC9 
