@echo off
cd /d "C:\OpenCode\DentalClinic"
"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\jar.exe" tf composeApp\build\libs\composeApp-desktop.jar > jar_contents.txt
C:\Windows\System32\findstr.exe crownOption jar_contents.txt
C:\Windows\System32\findstr.exe MedicalHistory jar_contents.txt
echo ---
echo CrownOption in DentalDatabaseImpl:
"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javap.exe" -c -p "C:\OpenCode\DentalClinic\composeApp\build\classes\kotlin\desktop\main\com\dental\data\db\composeApp\DentalDatabaseImpl$Schema.class" 2>nul | C:\Windows\System32\findstr.exe crownOption
echo ---
echo CrownOption in ToothQueries:
"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javap.exe" -c "C:\OpenCode\DentalClinic\composeApp\build\classes\kotlin\desktop\main\com\dental\data\db\ToothQueries$GetByPatientIdAndExamTypeQuery.class" 2>nul | C:\Windows\System32\findstr.exe crownOption
