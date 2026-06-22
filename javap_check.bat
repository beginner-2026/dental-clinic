@echo off
cd /d "C:\Users\Геннадий\Documents\OpenCode\1"
chcp 65001 >nul
echo === DentalDatabaseImpl$Schema ===
"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javap.exe" -c -p "composeApp\build\classes\kotlin\desktop\main\com\dental\data\db\composeApp\DentalDatabaseImpl$Schema.class" > schema_bytecode.txt 2>nul
type schema_bytecode.txt | findstr /i "crownOption MedicalHistory"
echo === ToothQueries$GetByPatientIdAndExamTypeQuery ===
"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javap.exe" -c "composeApp\build\classes\kotlin\desktop\main\com\dental\data\db\ToothQueries$GetByPatientIdAndExamTypeQuery.class" > tooth_bytecode.txt 2>nul
type tooth_bytecode.txt | findstr /i "crownOption MedicalHistory"
echo === Scan ALL class files ===
dir /s /b "composeApp\build\classes\*.class" > all_classes.txt
for /f %%f in (all_classes.txt) do (
  "C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javap.exe" -p "%%f" 2>nul | findstr /i "crownOption MedicalHistory" >nul
  if not errorlevel 1 echo FOUND in %%f
)
echo === DONE ===
