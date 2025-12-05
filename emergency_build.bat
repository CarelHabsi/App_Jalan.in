@echo off
echo ============================================================
echo EMERGENCY FIX - ADMIN SYSTEM
echo ============================================================
echo.
echo This will perform DEEP CLEAN to fix cache issues:
echo 1. Stop all Gradle daemons
echo 2. Delete ALL cache (.gradle, build, app/build)
echo 3. Clean project
echo 4. Rebuild APK
echo 5. Install to device
echo.
pause
echo.
echo Step 1: Stopping Gradle daemons...
call gradlew.bat --stop
echo ✅ Daemons stopped
echo.
echo Step 2: Deleting ALL cache directories...
rmdir /s /q .gradle
rmdir /s /q build
rmdir /s /q app\build
rmdir /s /q app\.cxx
echo ✅ Cache deleted
echo.
echo Step 3: Cleaning project...
call gradlew.bat clean
echo.
echo Step 4: Building APK (this may take a while)...
call gradlew.bat assembleDebug
echo.
echo Step 5: Installing APK...
call gradlew.bat installDebug
echo.
echo ============================================================
echo BUILD PROCESS COMPLETE!
echo ============================================================
echo.
echo If successful:
echo 1. Open app
echo 2. TAP LOGO 7 TIMES for admin access
echo 3. Login: admin@jalanin.com / admin12345
echo 4. OTP: 123456
echo 5. Enjoy admin dashboard!
echo.
pause

