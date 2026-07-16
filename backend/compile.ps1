# SIVC Java Backend Compilation Script

Write-Host "Compilando backend..." -ForegroundColor Cyan

# Create output bin directory if it doesn't exist
if (-not (Test-Path -Path "bin")) {
    New-Item -ItemType Directory -Path "bin" | Out-Null
}

# Compile starting from Main.java using the Eclipse Compiler for Java (ECJ) since JDK is not installed
java -jar "lib/ecj-3.19.0.jar" -cp "lib/gson-2.10.1.jar" -sourcepath src -d bin -8 src/Main.java

if ($LASTEXITCODE -eq 0) {
    Write-Host "Compilacion exitosa. Archivos compilados en la carpeta 'bin/'." -ForegroundColor Green
} else {
    Write-Host "Error en la compilacion." -ForegroundColor Red
    exit $LASTEXITCODE
}
