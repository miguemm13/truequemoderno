# SIVC Java Backend Integration Tests

$baseUrl = "http://localhost:8080"
Write-Host "Iniciando Pruebas de Integracion SIVC..." -ForegroundColor Cyan
Write-Host "Base URL: $baseUrl" -ForegroundColor Gray
Write-Host "================================================="

# Clean existing database files to start fresh
$dbPath = "data/usuarios.json"
if (Test-Path $dbPath) {
    Remove-Item $dbPath -Force
    Write-Host "Base de datos usuarios.json limpia para pruebas frescas." -ForegroundColor Yellow
}
$offersPath = "data/ofertas.json"
if (Test-Path $offersPath) {
    Remove-Item $offersPath -Force
    Write-Host "Base de datos ofertas.json limpia para pruebas frescas." -ForegroundColor Yellow
}
$txPath = "data/transacciones.json"
if (Test-Path $txPath) {
    Remove-Item $txPath -Force
    Write-Host "Base de datos transacciones.json limpia para pruebas frescas." -ForegroundColor Yellow
}
$repPath = "data/reputacion.json"
if (Test-Path $repPath) {
    Remove-Item $repPath -Force
    Write-Host "Base de datos reputacion.json limpia para pruebas frescas." -ForegroundColor Yellow
}

# Helper to execute POST requests using curl
function Invoke-PostRequest($endpoint, $payloadMap) {
    $json = $payloadMap | ConvertTo-Json -Compress
    
    # Save payload to temp file
    $tempPayload = "temp_payload.json"
    [System.IO.File]::WriteAllText((Get-Item .).FullName + "/" + $tempPayload, $json, [System.Text.Encoding]::UTF8)
    
    $tempBody = "temp_body.json"
    $url = "$baseUrl$endpoint"
    
    # Run curl to get status code and write body to file
    $status = curl.exe -s -o $tempBody -w "%{http_code}" -X POST -H "Content-Type: application/json" -d "@$tempPayload" $url
    
    $bodyText = ""
    if (Test-Path $tempBody) {
        $bodyText = Get-Content $tempBody -Raw
    }
    
    # Cleanup
    if (Test-Path $tempPayload) { Remove-Item $tempPayload -Force }
    if (Test-Path $tempBody) { Remove-Item $tempBody -Force }
    
    # Parse body
    $bodyObj = $null
    if ($bodyText -ne $null -and $bodyText.Trim() -ne "") {
        try {
            $bodyObj = ConvertFrom-Json $bodyText
        } catch {
            $bodyObj = $bodyText
        }
    }
    
    return [PSCustomObject]@{
        Status = [int]$status
        Body = $bodyObj
    }
}

# Helper to execute GET requests using curl
function Invoke-GetRequest($endpoint) {
    $tempBody = "temp_body.json"
    $url = "$baseUrl$endpoint"
    
    # Run curl to get status code and write body to file
    $status = curl.exe -s -o $tempBody -w "%{http_code}" -X GET $url
    
    $bodyText = ""
    if (Test-Path $tempBody) {
        $bodyText = Get-Content $tempBody -Raw
        Remove-Item $tempBody -Force
    }
    
    # Parse body
    $bodyObj = $null
    if ($bodyText -ne $null -and $bodyText.Trim() -ne "") {
        try {
            $bodyObj = ConvertFrom-Json $bodyText
        } catch {
            $bodyObj = $bodyText
        }
    }
    
    return [PSCustomObject]@{
        Status = [int]$status
        Body = $bodyObj
    }
}

# Helper to print test header
function Test-Header($title) {
    Write-Host "`n[TEST] $title" -ForegroundColor Magenta -NoNewline
}

# Helper to print pass/fail
function Test-Result($passed, $msg = "") {
    if ($passed) {
        Write-Host " -> PASSED" -ForegroundColor Green
    } else {
        Write-Host " -> FAILED ($msg)" -ForegroundColor Red
    }
}

# -------------------------------------------------------------
# HU1: REGISTRO DE USUARIO Y SEGURIDAD
# -------------------------------------------------------------

# TEST 1: Registro Exitoso (Usuario 1: Prestador)
Test-Header "1. Registro exitoso de miembro con saldo 0.00h"
$regPayload = @{
    nombre = "Juan Perez"
    correo = "juan.perez@ucab.edu.ve"
    contrasena = "clave123"
    habilidades = @("Carpinteria", "Plomeria")
    preguntaSeguridad = "Nombre de tu primera mascota"
    respuestaSeguridad = "Firulais"
}
$res = Invoke-PostRequest "/api/miembros/registro" $regPayload
$ok = $res.Status -eq 201 -and $res.Body.id -ne $null -and $res.Body.saldo -eq 0.00 -and $res.Body.limite -eq -2.00
Test-Result $ok "Status: $($res.Status), ID: $($res.Body.id), Saldo: $($res.Body.saldo)"
$juanUserId = $res.Body.id # Juan will publish the offer

# Register User 2 (Receptor)
$regPayload2 = @{
    nombre = "Maria Gomez"
    correo = "maria.gomez@ucab.edu.ve"
    contrasena = "maria456"
    habilidades = @("Diseno Grafico")
    preguntaSeguridad = "Nombre de tu calle"
    respuestaSeguridad = "Bolivar"
}
$res2 = Invoke-PostRequest "/api/miembros/registro" $regPayload2
$mariaUserId = $res2.Body.id # Maria will request Juan's services

# TEST 2: Verificacion de Cifrado
Test-Header "2. Verificacion de cifrado en archivo JSON (AES-256 / SHA-256)"
try {
    if (Test-Path $dbPath) {
        $content = Get-Content $dbPath -Raw
        $json = ConvertFrom-Json $content
        $miembroObj = $json[0]
        $nombrePlain = "Juan Perez"
        $contrasenaPlain = "clave123"
        $respuestaPlain = "Firulais"

        $nombreEsCifrado = $miembroObj.nombre -ne $nombrePlain -and $miembroObj.nombre.Length -gt 20
        $respuestaEsCifrada = $miembroObj.respuestaSeguridad -ne $respuestaPlain -and $miembroObj.respuestaSeguridad.Length -gt 20
        $contrasenaEsHash = $miembroObj.contrasena -ne $contrasenaPlain -and $miembroObj.contrasena.Length -eq 64

        $ok = $nombreEsCifrado -and $respuestaEsCifrada -and $contrasenaEsHash
        Test-Result $ok "Nombre cifrado: $nombreEsCifrado, Respuesta cifrada: $respuestaEsCifrada, Contrasena Hash: $contrasenaEsHash"
    } else {
        Test-Result $false "No se encontro el archivo usuarios.json"
    }
} catch {
    Test-Result $false $_.Exception.Message
}

# TEST 3: Captura de errores de validacion
Test-Header "3. Captura de errores de validacion (Registro invalido)"
$invalidPayload = @{
    nombre = "" 
    correo = "juan.perez@ucab.edu.ve"
    contrasena = "clave123"
    habilidades = @() 
    preguntaSeguridad = "Nombre de tu primera mascota"
    respuestaSeguridad = "Firulais"
}
$res = Invoke-PostRequest "/api/miembros/registro" $invalidPayload
$ok = $res.Status -eq 400 -and $res.Body.validationError -ne $null
Test-Result $ok "Status: $($res.Status), Error: $($res.Body.validationError)"

# TEST 4: Duplicados
Test-Header "4. Rechazo de correo duplicado"
$dupPayload = @{
    nombre = "Juan Otro"
    correo = "juan.perez@ucab.edu.ve" 
    contrasena = "clave456"
    habilidades = @("Otros")
    preguntaSeguridad = "Otra pregunta"
    respuestaSeguridad = "Otra respuesta"
}
$res = Invoke-PostRequest "/api/miembros/registro" $dupPayload
$ok = $res.Status -eq 400 -and $res.Body.validationError -like "*correo*"
Test-Result $ok "Status: $($res.Status), Error: $($res.Body.validationError)"

# TEST 5: Login Exitoso
Test-Header "5. Login exitoso con credenciales correctas (Decifra nombre)"
$loginPayload = @{
    correo = "juan.perez@ucab.edu.ve"
    contrasena = "clave123"
}
$res = Invoke-PostRequest "/api/miembros/login" $loginPayload
$ok = $res.Status -eq 200 -and $res.Body.id -ne $null -and $res.Body.nombre -eq "Juan Perez"
Test-Result $ok "Status: $($res.Status), Nombre desencriptado: $($res.Body.nombre)"

# TEST 6: Login Fallido
Test-Header "6. Login fallido con contrasena incorrecta"
$badLoginPayload = @{
    correo = "juan.perez@ucab.edu.ve"
    contrasena = "claveEquivocada"
}
$res = Invoke-PostRequest "/api/miembros/login" $badLoginPayload
$ok = $res.Status -eq 401 -and $res.Body.error -ne $null
Test-Result $ok "Status: $($res.Status), Error: $($res.Body.error)"

# TEST 7: Pregunta Seguridad
Test-Header "7. Obtener pregunta de seguridad por correo"
$recupPregPayload = @{
    correo = "juan.perez@ucab.edu.ve"
}
$res = Invoke-PostRequest "/api/miembros/recuperar/pregunta" $recupPregPayload
$ok = $res.Status -eq 200 -and $res.Body.preguntaSeguridad -eq "Nombre de tu primera mascota"
Test-Result $ok "Status: $($res.Status), Pregunta: $($res.Body.preguntaSeguridad)"

# TEST 8: Respuesta Incorrecta
Test-Header "8. Validacion de respuesta incorrecta en recuperacion"
$badAnswerPayload = @{
    correo = "juan.perez@ucab.edu.ve"
    respuestaSeguridad = "GatoEquivocado"
    nuevaContrasena = "nuevaClave456"
}
$res = Invoke-PostRequest "/api/miembros/recuperar/verificar" $badAnswerPayload
$ok = $res.Status -eq 400 -and $res.Body.validationError -like "*incorrecta*"
Test-Result $ok "Status: $($res.Status), Error: $($res.Body.validationError)"

# TEST 9: Recuperacion Exitosa
Test-Header "9. Restablecer contrasena con respuesta correcta y loguear con nueva contrasena"
$goodAnswerPayload = @{
    correo = "juan.perez@ucab.edu.ve"
    respuestaSeguridad = "firulais" 
    nuevaContrasena = "nuevaClave456"
}
$resReset = Invoke-PostRequest "/api/miembros/recuperar/verificar" $goodAnswerPayload
$resetOk = $resReset.Status -eq 200 -and $resReset.Body.mensaje -like "*exito*"

$newLoginPayload = @{
    correo = "juan.perez@ucab.edu.ve"
    contrasena = "nuevaClave456"
}
$resLogin = Invoke-PostRequest "/api/miembros/login" $newLoginPayload
$loginOk = $resLogin.Status -eq 200 -and $resLogin.Body.id -ne $null -and $resLogin.Body.nombre -eq "Juan Perez"

$ok = $resetOk -and $loginOk
Test-Result $ok "Status Reset: $($resReset.Status), Status Login: $($resLogin.Status), Login con nueva contrasena exitoso: $loginOk"


# -------------------------------------------------------------
# HU2: GESTION DE SERVICIOS - PUBLICACION DE OFERTAS
# -------------------------------------------------------------

# TEST 10: Publicacion Exitosa de Oferta
Test-Header "10. Publicacion exitosa de oferta (Metadatos: OFR-001, Timestamp, Estado Activa)"
$offerPayload = @{
    idCreador = $juanUserId
    titulo = "Pintura y decoracion"
    descripcion = "Ofrezco pintar habitaciones interioes, incluyo brochas"
    idCategoria = "CAT-001"
    horas = 4.5
}
$res = Invoke-PostRequest "/api/ofertas/publicar" $offerPayload
$ok = $res.Status -eq 201 -and $res.Body.id_oferta -eq "OFR-001" -and $res.Body.id_creador -eq $juanUserId -and $res.Body.horas_estimadas -eq 4.5 -and $res.Body.estado -eq "Activa" -and $res.Body.fecha_publicacion -ne $null
Test-Result $ok "Status: $($res.Status), ID Oferta: $($res.Body.id_oferta)"

# TEST 11: Error por Horas Estimadas Fuera de Rango (Limite Inferior < 0.5)
Test-Header "11. Error al publicar oferta con horas estimadas muy bajas (0.4)"
$lowHoursPayload = @{
    idCreador = $juanUserId
    titulo = "Ajuste rapido tornillos"
    descripcion = "Ajustar tornillos de puerta"
    idCategoria = "CAT-001"
    horas = 0.4
}
$res = Invoke-PostRequest "/api/ofertas/publicar" $lowHoursPayload
$ok = $res.Status -eq 400 -and $res.Body.validationError -like "*rango*"
Test-Result $ok "Status: $($res.Status), Error: $($res.Body.validationError)"

# TEST 12: Error por Horas Estimadas Fuera de Rango (Limite Superior > 20.0)
Test-Header "12. Error al publicar oferta con horas estimadas muy altas (20.5)"
$highHoursPayload = @{
    idCreador = $juanUserId
    titulo = "Remodelacion completa jardin"
    descripcion = "Remodelar jardin entero"
    idCategoria = "CAT-001"
    horas = 20.5
}
$res = Invoke-PostRequest "/api/ofertas/publicar" $highHoursPayload
$ok = $res.Status -eq 400 -and $res.Body.validationError -like "*rango*"
Test-Result $ok "Status: $($res.Status), Error: $($res.Body.validationError)"

# TEST 13: Error por Categoria Invalida
Test-Header "13. Error al publicar oferta con categoria inexistente (CAT-999)"
$badCategoryPayload = @{
    idCreador = $juanUserId
    titulo = "Clases de Alquimia"
    descripcion = "Enseño alquimia basica"
    idCategoria = "CAT-999" 
    horas = 2.0
}
$res = Invoke-PostRequest "/api/ofertas/publicar" $badCategoryPayload
$ok = $res.Status -eq 400 -and $res.Body.validationError -like "*Categoria*"
Test-Result $ok "Status: $($res.Status), Error: $($res.Body.validationError)"

# TEST 14: Verificacion de persistencia de oferta
Test-Header "14. Verificacion de escritura en archivo ofertas.json"
try {
    if (Test-Path $offersPath) {
        $content = Get-Content $offersPath -Raw
        $json = ConvertFrom-Json $content
        $ofertaObj = $json[0]

        $ok = $ofertaObj.id_oferta -eq "OFR-001" -and $ofertaObj.id_creador -eq $juanUserId -and $ofertaObj.horas_estimadas -eq 4.5
        Test-Result $ok "Encontrada oferta OFR-001"
    } else {
        Test-Result $false "No se encontro el archivo ofertas.json"
    }
} catch {
    Test-Result $false $_.Exception.Message
}

# TEST 15: Obtener muro comunitario (GET)
Test-Header "15. Obtener muro comunitario (GET /api/ofertas/muro)"
$res = Invoke-GetRequest "/api/ofertas/muro"
$ok = $res.Status -eq 200 -and $res.Body.Length -ge 1 -and $res.Body[0].id_oferta -eq "OFR-001"
Test-Result $ok "Status: $($res.Status), Numero de ofertas: $($res.Body.Length)"


# -------------------------------------------------------------
# HU3: SOLICITUD DE SERVICIO Y VALIDACIÓN PREVENTIVA
# -------------------------------------------------------------

# TEST 16: Solicitud con saldo insuficiente preventivo (Deuda > -2.0)
# Maria has 0.0h. OFR-001 costs 4.5h. Projected = 0.0 - 4.5 = -4.5h. (Límite -2.0) -> Recharzar
Test-Header "16. Rechazo de solicitud por saldo preventivo insuficiente (Balance proyectado -4.5h)"
$solPayloadInsuficiente = @{
    idReceptor = $mariaUserId
    idOferta = "OFR-001"
    horas = 4.5
}
$res = Invoke-PostRequest "/api/transacciones/solicitar" $solPayloadInsuficiente
$ok = $res.Status -eq 400 -and ($res.Body.validationError -like "*Saldo preventivo insuficiente*" -or $res.Body.validationError -like "*tope de deuda*")
Test-Result $ok "Status: $($res.Status), Error: $($res.Body.validationError)"

# TEST 17: Solicitud con saldo suficiente preventivo (Deuda <= -2.0)
# Maria has 0.0h. We request a sub-portion or smaller service of 1.5h. Projected = 0.0 - 1.5 = -1.5h. -> Aprobar en estado "Solicitada"
Test-Header "17. Aprobacion de solicitud dentro del limite de deuda (Balance proyectado -1.5h)"
$solPayloadSuficiente = @{
    idReceptor = $mariaUserId
    idOferta = "OFR-001"
    horas = 1.5
}
$res = Invoke-PostRequest "/api/transacciones/solicitar" $solPayloadSuficiente
$ok = $res.Status -eq 201 -and $res.Body.id_transaccion -eq "TX-001" -and $res.Body.id_receptor -eq $mariaUserId -and $res.Body.id_prestador -eq $juanUserId -and $res.Body.horas -eq 1.5 -and $res.Body.estado -eq "Solicitada"
Test-Result $ok "Status: $($res.Status), ID Transaccion: $($res.Body.id_transaccion), Estado: $($res.Body.estado)"

# TEST 18: Verificacion de persistencia de transacciones
Test-Header "18. Verificacion de escritura en archivo transacciones.json"
try {
    if (Test-Path $txPath) {
        $content = Get-Content $txPath -Raw
        $json = ConvertFrom-Json $content
        $txObj = $json[0]

        $ok = $txObj.id_transaccion -eq "TX-001" -and $txObj.id_receptor -eq $mariaUserId -and $txObj.id_prestador -eq $juanUserId -and $txObj.horas -eq 1.5 -and $txObj.estado -eq "Solicitada"
        Test-Result $ok "Encontrada transaccion TX-001 en transacciones.json en estado Solicitada"
    } else {
        Test-Result $false "No se encontro el archivo transacciones.json"
    }
} catch {
    Test-Result $false $_.Exception.Message
}


# -------------------------------------------------------------
# HU4: GESTION DE TRANSACCION (ACEPTAR / RECHAZAR SOLICITUD)
# -------------------------------------------------------------

# TEST 19: Aceptacion de solicitud por Prestador autorizado
Test-Header "19. Aceptacion de solicitud por Prestador autorizado (Mutacion a estado 'Iniciada')"
$gestionAceptarPayload = @{
    idPrestador = $juanUserId
    idTrans = "TX-001"
    decision = "Aceptar"
}
$res = Invoke-PostRequest "/api/transacciones/gestionar" $gestionAceptarPayload
$ok = $res.Status -eq 200 -and $res.Body.id_transaccion -eq "TX-001" -and $res.Body.estado -eq "Iniciada"
Test-Result $ok "Status: $($res.Status), Estado mutado: $($res.Body.estado)"

# TEST 20: Bloqueo de gestion por usuario no autorizado (Tercero)
Test-Header "20. Rechazo de gestion de solicitud por usuario no autorizado (Tercero)"
# Maria requests a second portion of 0.4h -> creates TX-002 in state "Solicitada"
$solPayload2 = @{
    idReceptor = $mariaUserId
    idOferta = "OFR-001"
    horas = 0.4
}
$resRequest2 = Invoke-PostRequest "/api/transacciones/solicitar" $solPayload2

# Maria (who is the receptor, not the prestador Juan) tries to accept/reject TX-002
$unauthPayload = @{
    idPrestador = $mariaUserId # Unauthorized
    idTrans = "TX-002"
    decision = "Rechazar"
}
$resUnauth = Invoke-PostRequest "/api/transacciones/gestionar" $unauthPayload
$ok = $resUnauth.Status -eq 403 -and $resUnauth.Body.error -like "*No autorizado*"
Test-Result $ok "Status: $($resUnauth.Status), Mensaje: $($resUnauth.Body.error)"

# TEST 21: Rechazo de solicitud por Prestador autorizado y liberacion de saldo preventivo
Test-Header "21. Rechazo de solicitud por Prestador autorizado y liberacion de saldo preventivo"
# Juan Perez rejects TX-002
$rejectPayload = @{
    idPrestador = $juanUserId
    idTrans = "TX-002"
    decision = "Rechazar"
}
$resReject = Invoke-PostRequest "/api/transacciones/gestionar" $rejectPayload
$okReject = $resReject.Status -eq 200 -and $resReject.Body.estado -eq "Cancelada por Prestador"

# Verify that Maria can now request another 0.4h service request
# Since TX-002 is Cancelada por Prestador, her committed balance is only 1.5h (TX-001 is Iniciada).
# If TX-002 had remained active, requesting another 0.4h would put her at: 1.5 + 0.4 + 0.4 = 2.3h (exceeding -2.00 limit).
# Since it is cancelled, requesting 0.4h should SUCCEED (Projected = 0.0 - 1.5 - 0.4 = -1.9h >= -2.00)
$solPayload3 = @{
    idReceptor = $mariaUserId
    idOferta = "OFR-001"
    horas = 0.4
}
$resRequest3 = Invoke-PostRequest "/api/transacciones/solicitar" $solPayload3
$okRequest = $resRequest3.Status -eq 201 -and $resRequest3.Body.id_transaccion -eq "TX-003" -and $resRequest3.Body.estado -eq "Solicitada"

$ok = $okReject -and $okRequest
Test-Result $ok "Rechazo status: $($resReject.Status), Nuevo Request status: $($resRequest3.Status)"


# -------------------------------------------------------------
# HU5: TRANSFERENCIA ATÓMICA DE HORAS
# -------------------------------------------------------------

# TEST 22: Confirmacion de entrega por Receptor autorizado (Transferencia Exitosa)
Test-Header "22. Confirmacion de entrega por Receptor autorizado (Transferencia Exitosa, debitos/creditos)"
# Maria Gomez confirms TX-001 (1.5h).
$confirmPayload = @{
    idMiembroSesion = $mariaUserId
    idTrans = "TX-001"
}
$res = Invoke-PostRequest "/api/transacciones/confirmar" $confirmPayload
$okConfirm = $res.Status -eq 200 -and $res.Body.transaccion.estado -eq "Finalizado con exito"

# Check actual balances in usuarios.json
$balancesOk = $false
try {
    if (Test-Path $dbPath) {
        $content = Get-Content $dbPath -Raw
        $json = ConvertFrom-Json $content
        $juanObj = $null
        $mariaObj = $null
        foreach ($m in $json) {
            if ($m.id -eq $juanUserId) { $juanObj = $m }
            if ($m.id -eq $mariaUserId) { $mariaObj = $m }
        }
        # Juan should have +1.5h, Maria should have -1.5h
        $balancesOk = $juanObj.billetera.saldo -eq 1.5 -and $mariaObj.billetera.saldo -eq -1.5
    }
} catch {
    $balancesOk = $false
}

$ok = $okConfirm -and $balancesOk
Test-Result $ok "Status: $($res.Status), Estado: $($res.Body.transaccion.estado), Saldos: Juan($($juanObj.billetera.saldo)h), Maria($($mariaObj.billetera.saldo)h)"

# TEST 23: Bloqueo de confirmacion por usuario no autorizado (Prestador intenta confirmar)
Test-Header "23. Bloqueo de confirmacion por usuario no autorizado (Prestador intenta cerrar)"
# Juan Perez (prestador) tries to confirm delivery. Should return 403.
$unauthConfirmPayload = @{
    idMiembroSesion = $juanUserId
    idTrans = "TX-003" # TX-003 is still in Solicitada or another active state, but Juan is prestador. Let's make sure TX-003 is Iniciada first
}
# Accept TX-003 first so it is Iniciada
$acceptPayload3 = @{
    idPrestador = $juanUserId
    idTrans = "TX-003"
    decision = "Aceptar"
}
$null = Invoke-PostRequest "/api/transacciones/gestionar" $acceptPayload3

# Now Juan tries to confirm
$resUnauth = Invoke-PostRequest "/api/transacciones/confirmar" $unauthConfirmPayload
$ok = $resUnauth.Status -eq 403 -and $resUnauth.Body.error -like "*No autorizado*"
Test-Result $ok "Status: $($resUnauth.Status), Mensaje: $($resUnauth.Body.error)"

# TEST 24: Bloqueo de confirmacion por estado invalido
Test-Header "24. Bloqueo de confirmacion por estado invalido (Transaccion ya finalizada)"
# Maria Gomez attempts to confirm TX-001 again (already finalized). Should return 400.
$resDouble = Invoke-PostRequest "/api/transacciones/confirmar" $confirmPayload
$ok = $resDouble.Status -eq 400 -and $resDouble.Body.validationError -like "*Iniciada*"
Test-Result $ok "Status: $($resDouble.Status), Mensaje: $($resDouble.Body.validationError)"


# -------------------------------------------------------------
# HU6: EVALUACIÓN DE EXPERIENCIA Y REPUTACIÓN
# -------------------------------------------------------------

# TEST 25: Calificacion exitosa del Receptor al Prestador (TX-001)
Test-Header "25. Calificacion exitosa del Receptor al Prestador (TX-001, Juan recibe 5 estrellas)"
$calif1 = @{
    idEvaluador = $mariaUserId
    idTrans = "TX-001"
    estrellas = 5
    comentario = "Excelente carpintero, muy puntual."
}
$res = Invoke-PostRequest "/api/reputacion/calificar" $calif1
$okConfirm = $res.Status -eq 201 -and $res.Body.id_calificacion -eq "CAL-001" -and $res.Body.estrellas -eq 5

# Check Juan's updated reputation in usuarios.json
$repOk = $false
try {
    if (Test-Path $dbPath) {
        $content = Get-Content $dbPath -Raw
        $json = ConvertFrom-Json $content
        foreach ($m in $json) {
            if ($m.id -eq $juanUserId) {
                $repOk = $m.reputacion -eq 5.0 -and $m.totalResenas -eq 1
            }
        }
    }
} catch { $repOk = $false }

$ok = $okConfirm -and $repOk
Test-Result $ok "Status: $($res.Status), ID Calif: $($res.Body.id_calificacion), Reputacion Juan: 5.0 (Resenas: 1)"

# TEST 26: Calificacion exitosa del Prestador al Receptor (TX-001)
Test-Header "26. Calificacion exitosa del Prestador al Receptor (TX-001, Maria recibe 4 estrellas)"
$calif2 = @{
    idEvaluador = $juanUserId
    idTrans = "TX-001"
    estrellas = 4
    comentario = "Excelente trato y coordinacion."
}
$res = Invoke-PostRequest "/api/reputacion/calificar" $calif2
$okConfirm = $res.Status -eq 201 -and $res.Body.id_calificacion -eq "CAL-002"

# Check Maria's updated reputation in usuarios.json
$repOk = $false
try {
    if (Test-Path $dbPath) {
        $content = Get-Content $dbPath -Raw
        $json = ConvertFrom-Json $content
        foreach ($m in $json) {
            if ($m.id -eq $mariaUserId) {
                $repOk = $m.reputacion -eq 4.0 -and $m.totalResenas -eq 1
            }
        }
    }
} catch { $repOk = $false }

$ok = $okConfirm -and $repOk
Test-Result $ok "Status: $($res.Status), ID Calif: $($res.Body.id_calificacion), Reputacion Maria: 4.0 (Resenas: 1)"

# TEST 27: Bloqueo de duplicado de reseña (Inmutabilidad)
Test-Header "27. Bloqueo de duplicado de reseña (Inmutabilidad, una calificacion por transaccion)"
$resDup = Invoke-PostRequest "/api/reputacion/calificar" $calif1
$ok = $resDup.Status -eq 400 -and $resDup.Body.validationError -like "*Ya has enviado*"
Test-Result $ok "Status: $($resDup.Status), Mensaje: $($resDup.Body.validationError)"

# TEST 28: Regla de Reciprocidad Obligatoria (Ocultamiento/Filtro de comentarios)
Test-Header "28. Regla de Reciprocidad Obligatoria (Comentario oculto hasta calificar de vuelta)"
# 1. Confirm delivery of TX-003 so it is finalized
$confirmPayload3 = @{
    idMiembroSesion = $mariaUserId
    idTrans = "TX-003"
}
$null = Invoke-PostRequest "/api/transacciones/confirmar" $confirmPayload3

# Maria rates Juan for TX-003
$calif3 = @{
    idEvaluador = $mariaUserId
    idTrans = "TX-003"
    estrellas = 3
    comentario = "Buen trabajo decorando, pero tardo algo."
}
$res3 = Invoke-PostRequest "/api/reputacion/calificar" $calif3

# 2. Juan queries his reviews before rating Maria back for TX-003
# The review for TX-001 should be fully visible, but the comment for TX-003 must be masked!
$resReviewsBefore = Invoke-GetRequest "/api/reputacion/usuario?idUsuario=$juanUserId&idConsultante=$juanUserId"
$c1 = $null
$c3 = $null
foreach ($c in $resReviewsBefore.Body) {
    if ($c.id_transaccion -eq "TX-001") { $c1 = $c }
    if ($c.id_transaccion -eq "TX-003") { $c3 = $c }
}

$okBefore = $c1.comentario -eq "Excelente carpintero, muy puntual." -and $c3.comentario -like "*Comentario oculto*"

# 3. Juan rates Maria back for TX-003
$calif4 = @{
    idEvaluador = $juanUserId
    idTrans = "TX-003"
    estrellas = 5
    comentario = "Todo correcto, muy atenta."
}
$null = Invoke-PostRequest "/api/reputacion/calificar" $calif4

# 4. Juan queries his reviews again after rating Maria back. Both comments must be visible!
$resReviewsAfter = Invoke-GetRequest "/api/reputacion/usuario?idUsuario=$juanUserId&idConsultante=$juanUserId"
$c1_after = $null
$c3_after = $null
foreach ($c in $resReviewsAfter.Body) {
    if ($c.id_transaccion -eq "TX-001") { $c1_after = $c }
    if ($c.id_transaccion -eq "TX-003") { $c3_after = $c }
}

$okAfter = $c1_after.comentario -eq "Excelente carpintero, muy puntual." -and $c3_after.comentario -eq "Buen trabajo decorando, pero tardo algo."

# 5. Check Juan's final averaged reputation: (5.0 + 3.0) / 2 = 4.0
$repFinalOk = $false
try {
    if (Test-Path $dbPath) {
        $content = Get-Content $dbPath -Raw
        $json = ConvertFrom-Json $content
        foreach ($m in $json) {
            if ($m.id -eq $juanUserId) {
                $repFinalOk = $m.reputacion -eq 4.0 -and $m.totalResenas -eq 2
            }
        }
    }
} catch { $repFinalOk = $false }

$ok = $okBefore -and $okAfter -and $repFinalOk
Test-Result $ok "Reciprocidad Before: $okBefore, Reciprocidad After: $okAfter, Reputacion Final Juan: $($juanObj.billetera.saldo)h, Rep: 4.0 (Resenas: 2)"


# -------------------------------------------------------------
# HU7: SISTEMA DE SUGERENCIAS Y ALGORITMO DE MATCHING (GRAFOS Y CICLOS)
# -------------------------------------------------------------
Test-Header "29. Registro de miembros para pruebas de matching (Ciclos de 2 y 3)"

# Register USR-003: Miguel Moya (offers: Clases de Calculo, demands: Pintar casa)
$regA = @{
    nombre = "Miguel Moya"
    correo = "miguel@barter.com"
    contrasena = "miguel123"
    habilidades = @("Clases de Calculo")
    demandas = @("Pintar casa")
    preguntaSeguridad = "Mascota"
    respuestaSeguridad = "A"
}
$resA = Invoke-PostRequest "/api/miembros/registro" $regA
$idA = $resA.Body.id

# Register USR-004: Mini Miguelito (offers: Arreglar carro, demands: Clases de Calculo)
$regB = @{
    nombre = "Mini Miguelito"
    correo = "mini@barter.com"
    contrasena = "mini123"
    habilidades = @("Arreglar carro")
    demandas = @("Clases de Calculo")
    preguntaSeguridad = "Mascota"
    respuestaSeguridad = "B"
}
$resB = Invoke-PostRequest "/api/miembros/registro" $regB
$idB = $resB.Body.id

# Register USR-005: Eithan Rodan (offers: Pintar casa, demands: Arreglar carro)
$regC = @{
    nombre = "Eithan Rodan"
    correo = "eithan@barter.com"
    contrasena = "eithan123"
    habilidades = @("Pintar casa")
    demandas = @("Arreglar carro")
    preguntaSeguridad = "Mascota"
    respuestaSeguridad = "C"
}
$resC = Invoke-PostRequest "/api/miembros/registro" $regC
$idC = $resC.Body.id

# Register USR-006: Juan Perez2 (offers: Carpinteria, demands: Plomeria)
$regD = @{
    nombre = "Juan Perez2"
    correo = "juan2@barter.com"
    contrasena = "juan123"
    habilidades = @("Carpinteria")
    demandas = @("Plomeria")
    preguntaSeguridad = "Mascota"
    respuestaSeguridad = "D"
}
$resD = Invoke-PostRequest "/api/miembros/registro" $regD
$idD = $resD.Body.id

# Register USR-007: Maria Gomez2 (offers: Plomeria, demands: Carpinteria)
$regE = @{
    nombre = "Maria Gomez2"
    correo = "maria2@barter.com"
    contrasena = "maria123"
    habilidades = @("Plomeria")
    demandas = @("Carpinteria")
    preguntaSeguridad = "Mascota"
    respuestaSeguridad = "E"
}
$resE = Invoke-PostRequest "/api/miembros/registro" $regE
$idE = $resE.Body.id

$registroOk = $idA -ne $null -and $idB -ne $null -and $idC -ne $null -and $idD -ne $null -and $idE -ne $null
Test-Result $registroOk "Registrados miembros matching."


Test-Header "30. Prueba de matching cruzado de 3 elementos (Ciclo A -> B -> C -> A)"
# Query matching for Miguel Moya (idA = USR-003)
$resMatchA = Invoke-GetRequest "/api/matching/sugerencias?idUsuario=$idA"
# Should have suggestions pointing to USR-004 and USR-005
$hasB = $false
$hasC = $false
foreach ($s in $resMatchA.Body) {
    if ($s.habilidadOfrecida -eq "Clases de Calculo" -and $s.habilidadDemandada -eq "Pintar casa") {
        if ($s.idSocioSugeridoCifrado -ne $null) {
            $hasB = $true
        }
    }
}
$match3Ok = $resMatchA.Status -eq 200 -and $resMatchA.Body.Length -eq 2 -and $hasB
Test-Result $match3Ok "Matching de 3 elementos. Sugerencias devueltas: $($resMatchA.Body.Length)"


Test-Header "31. Prueba de matching directo de 2 elementos (Ciclo D -> E -> D)"
# Query matching for Juan Perez2 (idD = USR-006)
$resMatchD = Invoke-GetRequest "/api/matching/sugerencias?idUsuario=$idD"
# Should have 1 suggestion pointing to USR-007 (Maria Gomez2)
$match2Ok = $resMatchD.Status -eq 200 -and $resMatchD.Body.Length -eq 1 -and $resMatchD.Body[0].habilidadOfrecida -eq "Carpinteria" -and $resMatchD.Body[0].habilidadDemandada -eq "Plomeria"
Test-Result $match2Ok "Matching de 2 elementos. Sugerencia devuelta: $($resMatchD.Body[0].habilidadOfrecida) -> $($resMatchD.Body[0].habilidadDemandada)"


Test-Header "32. Prueba de priorizacion de matching por saldo negativo"
# Modify USR-006 (Juan Perez2) balance to be negative (-1.5h) in database file directly, then run query again
try {
    $dbContent = Get-Content $dbPath -Raw
    $members = ConvertFrom-Json $dbContent
    foreach ($m in $members) {
        if ($m.id -eq $idD) {
            $m.billetera.saldo = -1.5
        }
    }
    $newJson = $members | ConvertTo-Json -Depth 5 -Compress
    [System.IO.File]::WriteAllText((Get-Item .).FullName + "/" + $dbPath, $newJson, [System.Text.Encoding]::UTF8)

    # Re-query suggestion score for USR-006
    $resMatchNegative = Invoke-GetRequest "/api/matching/sugerencias?idUsuario=$idD"
    
    # Original score was 15.0 (since reputacion is 0.0). Boost is 1.5 * 5 = 7.5. New score = 22.5.
    $scoreBefore = $resMatchD.Body[0].prioridadPuntaje
    $scoreAfter = $resMatchNegative.Body[0].prioridadPuntaje
    $scoreDiff = $scoreAfter - $scoreBefore

    $priorizacionOk = $scoreAfter -eq 22.5 -and $scoreDiff -eq 7.5
    Test-Result $priorizacionOk "Priorizacion aplicada. Score anterior: $scoreBefore, Score con boost: $scoreAfter"
} catch {
    Test-Result $false $_.Exception.Message
}


Test-Header "33. Prueba de Ping/Echo - Ping Exitoso"
$resPing = Invoke-GetRequest "/api/matching/ping"
$pingOk = $resPing.Status -eq 200 -and $resPing.Body.status -eq "OK"
Test-Result $pingOk "Servicio de Matching responde exitosamente."


Test-Header "34. Prueba de Ping/Echo - Simular Falla (Deberia retornar 503)"
# Activate simulated fault
$resFallaActivar = Invoke-GetRequest "/api/matching/simular-falla?activar=true"
$activarOk = $resFallaActivar.Status -eq 200 -and $resFallaActivar.Body.simuladorFalla -eq $true

# Ping should now fail with 503 Service Unavailable
$resPingFail = Invoke-GetRequest "/api/matching/ping"
$pingFailOk = $resPingFail.Status -eq 503 -and $resPingFail.Body.error -like "*no disponible*"

# Suggestions query should also fail with 503
$resSugerenciasFail = Invoke-GetRequest "/api/matching/sugerencias?idUsuario=$idD"
$sugFailOk = $resSugerenciasFail.Status -eq 503

$tacticOk = $activarOk -and $pingFailOk -and $sugFailOk
Test-Result $tacticOk "El motor de matching entra en estado offline y responde con codigo de error 503."


Test-Header "35. Prueba de Ping/Echo - Restaurar Servicio (Desactivar Falla)"
# Deactivate simulated fault
$resFallaDesactivar = Invoke-GetRequest "/api/matching/simular-falla?activar=false"
$desactivarOk = $resFallaDesactivar.Status -eq 200 -and $resFallaDesactivar.Body.simuladorFalla -eq $false

# Ping should succeed again
$resPingRestored = Invoke-GetRequest "/api/matching/ping"
$pingRestoredOk = $resPingRestored.Status -eq 200 -and $resPingRestored.Body.status -eq "OK"

$restoreOk = $desactivarOk -and $pingRestoredOk
Test-Result $restoreOk "Servicio de Matching restaurado exitosamente."


Write-Host "`n================================================="
Write-Host "Pruebas de Integracion SIVC finalizadas." -ForegroundColor Cyan

