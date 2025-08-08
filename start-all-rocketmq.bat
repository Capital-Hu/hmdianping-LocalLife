@echo off
echo Starting RocketMQ NameServer and Broker...

cd /d C:\Users\Hans\Desktop\java\hm-dianping

echo.
echo Step 1: Starting NameServer in new window...
start "RocketMQ NameServer" cmd /k "start-rocketmq-nameserver.bat"

echo.
echo Waiting 5 seconds for NameServer to start...
timeout /t 5 /nobreak

echo.
echo Step 2: Starting Broker in new window...
start "RocketMQ Broker" cmd /k "start-rocketmq-broker.bat"

echo.
echo RocketMQ services are starting...
echo - NameServer: localhost:9876
echo - Broker: localhost:10911
echo.
echo Check the opened windows to see if services started successfully.
echo You can now start your Spring Boot application.

pause
