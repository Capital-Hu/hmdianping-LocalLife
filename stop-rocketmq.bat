@echo off
echo Stopping RocketMQ services...

echo.
echo Stopping RocketMQ Broker processes...
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq java.exe" /fo table ^| findstr "BrokerStartup"') do (
    echo Killing Broker process %%i
    taskkill /pid %%i /f
)

echo.
echo Stopping RocketMQ NameServer processes...
for /f "tokens=2" %%i in ('tasklist /fi "imagename eq java.exe" /fo table ^| findstr "NamesrvStartup"') do (
    echo Killing NameServer process %%i
    taskkill /pid %%i /f
)

echo.
echo RocketMQ services stopped.
pause
