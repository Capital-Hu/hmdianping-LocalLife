@echo off
echo Starting RocketMQ NameServer...

cd /d C:\Users\Hans\Desktop\java\hm-dianping

set JAVA_HOME=D:\java1.8
set ROCKETMQ_HOME=C:\Users\Hans\Desktop\java\hm-dianping\rocketmq-all-5.3.2-bin-release
set NAMESRV_ADDR=localhost:9876

echo Environment variables set:
echo JAVA_HOME=%JAVA_HOME%
echo ROCKETMQ_HOME=%ROCKETMQ_HOME%
echo NAMESRV_ADDR=%NAMESRV_ADDR%

echo.
echo Starting NameServer on port 9876...
java -cp rocketmq-all-5.3.2-bin-release\lib\* org.apache.rocketmq.namesrv.NamesrvStartup

pause
