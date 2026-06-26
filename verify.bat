@echo off
echo Checking ToothQueries generated file for crownOption:
C:\Windows\System32\findstr.exe crownOption "C:\OpenCode\DentalClinic\composeApp\build\generated\sqldelight\code\DentalDatabase\commonMain\com\dental\data\db\ToothQueries.kt"
echo ---
echo Checking DentalDatabaseImpl generated file for crownOption:
C:\Windows\System32\findstr.exe crownOption "C:\OpenCode\DentalClinic\composeApp\build\generated\sqldelight\code\DentalDatabase\commonMain\com\dental\data\db\composeApp\DentalDatabaseImpl.kt"
echo ---
echo Checking compiled ToothQueries for crownOption:
"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javap.exe" -c "C:\OpenCode\DentalClinic\composeApp\build\classes\kotlin\desktop\main\com\dental\data\db\ToothQueries$GetByPatientIdAndExamTypeQuery.class" 2>nul | C:\Windows\System32\findstr.exe crownOption
echo ---
echo Checking compiled DentalDatabaseImpl Schema for crownOption:
"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javap.exe" -c "C:\OpenCode\DentalClinic\composeApp\build\classes\kotlin\desktop\main\com\dental\data\db\composeApp\DentalDatabaseImpl$Schema.class" 2>nul | C:\Windows\System32\findstr.exe crownOption
