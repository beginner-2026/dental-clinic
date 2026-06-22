@echo off
cd /d "C:\Users\Геннадий\Documents\OpenCode\1"
"C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot\bin\javap.exe" -c -p "composeApp\build\classes\kotlin\desktop\main\com\dental\data\db\composeApp\DentalDatabaseImpl$Schema.class" > schema_bytecode.txt 2>nul
echo SCHEMA_BYTECODE_WRITTEN
