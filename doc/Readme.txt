
**************************************************************
**** LogCutter - JessMA Open Source, all rights reserved. ****
**************************************************************

一、环境要求
--------------------------------------------------
1) Java 版 本: JDK / JRE 1.6 以上
2) 依赖程序包: dom4j、log4j、ant、juniversalchardet
--------------------------------------------------

二、配置文件
--------------------------------------------------
1) 程序配置文件: conf/config.xml (默认)
	（示例参考：conf/config-template.xml）
2) 日志配置文件: conf/log4j2.xml (默认)
	（示例请参考：conf/log4j2.xml）
--------------------------------------------------

三、安装部署
（注 ：LogCutter 需要配置 ‘JAVA_HOME’ / ‘JRE_HOME’ 和 ‘CLASSPATH’ 系统环境变量）
--------------------------------------------------
1) 配置系统环境变量 ‘JAVA_HOME’（或 ‘JRE_HOME’） 和 ‘CLASSPATH’
2) 在 LogCutter配置文件（默认：conf/config.xml）中配置清理规则
3) 启动 LogCutter
--------------------------------------------------

四、启动方式
--------------------------------------------------
1) Windows
	A) 前台运行: > run.bat [ -f config-file ]
	
	B) 后台运行: > LogCutter.exe	{	
									-install-demand  (安装手动启动服务)
									-install-auto    (安装自动启动服务)
									-uninstall       (删除服务)
									-start           (启动服务)
									-stop            (停止服务)
									-status          (查看服务状态)
								}

	*** 注 *** 
		@ LogCutter.exe 以 Windows 系统服务的方式运行，安装好后也可以通过 Windows 服务管理器进行管理
		@ LogCutter.exe 是 32 位程序，LogCutter_x64.exe 是 64 位程序，根据当前系统平台使用其中之一

	C) 单次运行: > run.bat -1 [ -f config-file ]

2) Linux / Unix
	A) 前台运行: $ run.sh [ -f config-file ]
	B) 后台运行: $ run.sh [ -f config-file ] -d
	C) 单次运行: $ run.sh -1 [ -f config-file ] [ -d ]

	*** 注 ***
		@ 可以把 run.sh 启动命令加入 /etc/rc.d/rc.local 中，设置为开机时自动运行
		@ 可以把 run.sh -1 放入 CronTab 中定期执行，并且不用常驻内存，如：
			## 30 2 * * 2,4,6 root /usr/local/LogCutter/bin/run.sh -1 > /dev/null
--------------------------------------------------
