# Plugin For Zhiwei

为了在集成开发环境中快速地使用知微的相关功能，不用频繁的切换应用程序，而开发了这个插件项目

## 功能

- [x] commit message 卡片搜索
- [ ] 使用 JCEF 打开知微相关视图
- [ ] 艾特飞书小伙伴
- [ ] commit message 支持移动「缺陷卡」到「解决验证」状态

## 使用方法
安装本插件和准备一份配置文件(YAML)  
一、假设配置文件路径为 ~/.zhiwei4idea，需要填写用户名和密码  
```yaml
# location at ~/.zhiwei4idea

# username of zhiwei
usename: abc@agilean.cn
# password of zhiwei
password: 123321
```
二、进入插件配置 Preferences->Version Control->Zhiwei4idea 填写对应的配置文件地址和知微域名  

![screenshots preference](screenshots/preference.jpg)

## 功能介绍

### commit message 卡片搜索

知微提交代码规范中，需要在提交信息中包含用户故事或者缺陷等相关卡片的编号  
在 commit dialog 面板上，暂存修改部分，然后在 message 的输入过程中，可以  
输入触发前缀「card::」键入搜索关键字，即可搜索  
该功能移植知微评论区井号搜索功能  

![screenshots commit search card](screenshots/searchCardCode.gif)  

