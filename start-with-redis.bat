@echo off
echo ==========================================
echo MagicTech Management System Startup
echo ==========================================

REM Check if Redis is installed
where redis-server >nul 2>nul
if %ERRORLEVEL% NEQ 0 (
    echo ERROR: Redis is not installed or not in PATH
    echo.
    echo Please install Redis for Windows from:
    echo https://github.com/microsoftarchive/redis/releases
    echo.
    echo Or use Windows Subsystem for Linux (WSL)
    pause
    exit /b 1
)

REM Check if Redis is running
redis-cli ping >nul 2>nul
if %ERRORLEVEL% EQU 0 (
    echo ^✓ Redis is already running
) else (
    echo ^✗ Redis is not running
    echo Starting Redis server...

    REM Start Redis in background
    start /B redis-server --port 6379

    REM Wait for Redis to start
    timeout /t 2 /nobreak >nul

    REM Verify Redis started
    redis-cli ping >nul 2>nul
    if %ERRORLEVEL% EQU 0 (
        echo ^✓ Redis server started successfully
    ) else (
        echo ^✗ Failed to start Redis server
        echo Please start Redis manually
        pause
        exit /b 1
    )
)

echo.
echo Redis Information:
for /f "tokens=2 delims=:" %%a in ('redis-cli INFO server ^| findstr "redis_version"') do echo   Version:%%a
echo   Port: 6379
echo   Status: Running

echo.
echo ==========================================
echo Starting Application...
echo ==========================================
echo.

REM Start the application
mvn spring-boot:run
