@echo off
echo ============================================================
echo FIXING BUILD ERRORS - ADMIN SYSTEM
echo ============================================================
echo.
echo This script will:
echo 1. Delete build cache
echo 2. Clean project thoroughly
echo 3. Rebuild from scratch
echo.
pause
echo.
echo Step 1: Deleting build directories...
rmdir /s /q .gradle 2>nul
rmdir /s /q build 2>nul
rmdir /s /q app\build 2>nul
echo ✅ Build directories deleted
echo.
echo Step 2: Cleaning project...
call gradlew.bat clean --no-daemon
echo.
echo Step 3: Rebuilding APK...
call gradlew.bat assembleDebug --no-daemon
echo.
echo Step 4: Installing APK...
call gradlew.bat installDebug --no-daemon
echo.
echo ============================================================
echo BUILD COMPLETE!
echo ============================================================
echo.
echo If build successful, test admin access:
echo 1. Open app
echo 2. TAP LOGO 7 TIMES
echo 3. Login: admin@jalanin.com / admin12345
echo 4. OTP: 123456
echo.
pause

