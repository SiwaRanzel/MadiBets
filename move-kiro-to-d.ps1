<#
    move-kiro-to-d.ps1
    Moves Kiro's data (extensions + app data) from C: to D: and links it back
    with directory junctions so Kiro behaves exactly as before.

    HOW TO RUN
    ----------
    1. FULLY QUIT KIRO. Close every window. Confirm no "Kiro.exe" is running
       (check Task Manager). The script also checks this for you.
    2. Open Windows PowerShell (normal, does NOT need admin for junctions).
    3. Run:   powershell -ExecutionPolicy Bypass -File "D:\University\Third year Project\MadiBets\move-kiro-to-d.ps1"

    WHAT IT DOES
    ------------
    - Moves  C:\Users\<you>\.kiro\extensions        -> D:\KiroData\extensions
    - Moves  C:\Users\<you>\AppData\Roaming\Kiro     -> D:\KiroData\Kiro-AppData
    - Creates junctions at the old C: paths pointing to the new D: paths.

    Junctions are transparent: Kiro reads/writes the old paths, Windows
    redirects to D automatically. No launch flags needed.
#>

$ErrorActionPreference = 'Stop'

# ---- Targets -----------------------------------------------------------
$dRoot        = 'D:\KiroData'
$extSource    = Join-Path $env:USERPROFILE '.kiro\extensions'
$extTarget    = Join-Path $dRoot 'extensions'
$appSource    = Join-Path $env:APPDATA 'Kiro'          # C:\Users\<you>\AppData\Roaming\Kiro
$appTarget    = Join-Path $dRoot 'Kiro-AppData'

# ---- Safety: make sure Kiro is not running -----------------------------
$running = Get-Process -Name 'Kiro' -ErrorAction SilentlyContinue
if ($running) {
    Write-Host "Kiro is still running. Close it completely (check Task Manager) and re-run." -ForegroundColor Red
    exit 1
}

# ---- Helper: move a folder then junction it back -----------------------
function Move-AndLink {
    param($Source, $Target)

    if (-not (Test-Path $Source)) {
        Write-Host "Source not found, skipping: $Source" -ForegroundColor Yellow
        return
    }

    # If the source is already a junction/symlink, it was moved before.
    $item = Get-Item $Source -Force
    if ($item.LinkType) {
        Write-Host "Already linked, skipping: $Source -> $($item.Target)" -ForegroundColor Yellow
        return
    }

    if (-not (Test-Path $Target)) {
        New-Item -ItemType Directory -Path (Split-Path $Target) -Force | Out-Null
        Write-Host "Moving $Source -> $Target ..." -ForegroundColor Cyan
        Move-Item -Path $Source -Destination $Target -Force
    } else {
        Write-Host "Target already exists ($Target). Removing empty source and linking." -ForegroundColor Yellow
        Remove-Item $Source -Recurse -Force
    }

    Write-Host "Creating junction $Source -> $Target" -ForegroundColor Green
    New-Item -ItemType Junction -Path $Source -Target $Target | Out-Null
}

Write-Host "=== Moving Kiro data to $dRoot ===" -ForegroundColor White
New-Item -ItemType Directory -Path $dRoot -Force | Out-Null

Move-AndLink -Source $extSource -Target $extTarget
Move-AndLink -Source $appSource -Target $appTarget

Write-Host ""
Write-Host "Done. Verifying links:" -ForegroundColor White
Get-Item $extSource, $appSource -Force | Select-Object FullName, LinkType, Target | Format-List

Write-Host "You can now start Kiro normally. Its data lives on D:." -ForegroundColor Green
