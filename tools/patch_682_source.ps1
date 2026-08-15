$ErrorActionPreference = 'Stop'

$mainPath = Join-Path $PSScriptRoot '..\Main.bb'
$mainPath = [System.IO.Path]::GetFullPath($mainPath)

function Get-MainText {
    return [System.IO.File]::ReadAllText($mainPath)
}

function Replace-Once([string]$needle, [string]$replacement, [string]$description) {
    $text = Get-MainText
    $count = ([regex]::Matches($text, [regex]::Escape($needle))).Count
    if ($count -ne 1) {
        throw "Expected exactly one match for $description, found $count"
    }
    $text = $text.Replace($needle, $replacement)
    [System.IO.File]::WriteAllText($mainPath, $text, [System.Text.Encoding]::Default)
}

function Replace-FileOnce([string]$relativePath, [string]$needle, [string]$replacement, [string]$description) {
    $path = Join-Path $PSScriptRoot ('..\' + $relativePath)
    $path = [System.IO.Path]::GetFullPath($path)
    $text = [System.IO.File]::ReadAllText($path)
    $count = ([regex]::Matches($text, [regex]::Escape($needle))).Count
    if ($count -ne 1) {
        throw "Expected exactly one match for $description in $relativePath, found $count"
    }
    $text = $text.Replace($needle, $replacement)
    [System.IO.File]::WriteAllText($path, $text, [System.Text.Encoding]::Default)
}

# The current open-source Blitz3D runtime does not expose several helpers used by
# CB's historical mavless build. Define the compatibility layer before MapSystem.
$mapAnchor = 'Include "MapSystem.bb"'
$mapBlock = @"
Include "SCP682Compat.bb"
Include "MapSystem.bb"
"@
Replace-Once $mapAnchor $mapBlock 'modern Blitz3D compatibility include'

# Integrate the 682 player layer only after CB has declared Rooms, Doors, NPCs,
# events, UI globals, etc.
$includeAnchor = "Global I_Zone.MapZones = New MapZones"
$includeBlock = @"
Include "SCP682Player.bb"

Global I_Zone.MapZones = New MapZones
"@
Replace-Once $includeAnchor $includeBlock 'SCP-682 include anchor'

# Run the 682 layer after CB has drawn its normal HUD/messages. This keeps the custom
# HUD visible and also converts damage inflicted by NPCs during the same frame.
$updateAnchor = "`t`tUpdateAchievementMsg()"
$updateBlock = @"
`t`tUpdateAchievementMsg()
`t`tUpdateSCP682Player()
"@
Replace-Once $updateAnchor $updateBlock 'per-frame SCP-682 hook'

# The prototype is a post-breach creature run, not D-9341's escort/173 intro.
$text = Get-MainText
$introAnchor = "Function InitNewGame()`r`n`tCatchErrors(""Uncaught (InitNewGame)"")"
$newline = "`r`n"
if (-not $text.Contains($introAnchor)) {
    $introAnchor = "Function InitNewGame()`n`tCatchErrors(""Uncaught (InitNewGame)"")"
    $newline = "`n"
}
if (-not $text.Contains($introAnchor)) {
    throw 'Could not find InitNewGame anchor'
}
$introBlock = $introAnchor + $newline + "`tIf SCP682_Mode Then IntroEnabled = False"
Replace-Once $introAnchor $introBlock 'disable D-9341 intro'

# Give 682 a heavier but faster locomotion baseline while preserving CB collision,
# footsteps, gravity and camera handling.
$moveAnchor = "`tLocal Sprint# = 1.0, Speed# = 0.018, i%, angle#"
$moveBlock = @"
`tLocal Sprint# = 1.0, Speed# = 0.018, i%, angle#
`tIf SCP682_Mode Then Speed = 0.026
"@
Replace-Once $moveAnchor $moveBlock '682 movement speed'

# Inventory is a D-9341 mechanic. Keep the actual item/event system alive for the world,
# but do not let the 682 player open the human inventory UI.
$inventoryAnchor = "`t`tIf KeyHit(KEY_INV) And VomitTimer >= 0 Then"
$inventoryBlock = "`t`tIf KeyHit(KEY_INV) And VomitTimer >= 0 And (Not SCP682_Mode) Then"
Replace-Once $inventoryAnchor $inventoryBlock 'disable player inventory UI'

# Identify the build in the window title without touching save compatibility strings.
$titleAnchor = 'AppTitle "SCP - Containment Breach v"+VersionNumber'
$titleBlock = 'AppTitle "SCP-682 // Containment Breach Prototype - CB "+VersionNumber'
Replace-Once $titleAnchor $titleBlock 'prototype app title'

# QA BUILD: lock map generation to one known seed. This deliberately makes the test
# layout reproducible while leaving the stock source behavior untouched outside CI.
$seedClickAnchor = "`t`t`t`t`ttxt = `"NEW GAME`"`r`n`t`t`t`t`tRandomSeed = `"`""
$seedClickBlock = "`t`t`t`t`ttxt = `"NEW GAME`"`r`n`t`t`t`t`tRandomSeed = `"682TEST`""
if (-not ([System.IO.File]::ReadAllText((Join-Path $PSScriptRoot '..\Menu.bb')).Contains($seedClickAnchor))) {
    $seedClickAnchor = "`t`t`t`t`ttxt = `"NEW GAME`"`n`t`t`t`t`tRandomSeed = `"`""
    $seedClickBlock = "`t`t`t`t`ttxt = `"NEW GAME`"`n`t`t`t`t`tRandomSeed = `"682TEST`""
}
Replace-FileOnce 'Menu.bb' $seedClickAnchor $seedClickBlock 'default fixed QA seed'

$seedInputAnchor = 'RandomSeed = Left(InputBox(x+150*MenuScale, y+55*MenuScale, 200*MenuScale, 30*MenuScale, RandomSeed, 3),15)'
$seedInputBlock = 'RandomSeed = Left(InputBox(x+150*MenuScale, y+55*MenuScale, 200*MenuScale, 30*MenuScale, RandomSeed, 3),15) : RandomSeed = "682TEST"'
Replace-FileOnce 'Menu.bb' $seedInputAnchor $seedInputBlock 'lock visible map-seed input'

$seedStartAnchor = @"
`t`t`t`t`tIf RandomSeed = "" Then
`t`t`t`t`t`tRandomSeed = Abs(MilliSecs())
`t`t`t`t`tEndIf
`t`t`t`t`t
`t`t`t`t`tSeedRnd GenerateSeedNumber(RandomSeed)
"@
$seedStartBlock = @"
`t`t`t`t`t; SCP-682 QA build: force the same map seed on every new test run.
`t`t`t`t`tRandomSeed = "682TEST"
`t`t`t`t`tSeedRnd GenerateSeedNumber(RandomSeed)
"@
Replace-FileOnce 'Menu.bb' $seedStartAnchor $seedStartBlock 'force fixed QA seed at START'

# SCP-682 does not need SCP-106's pocket dimension as a progression detour. Omit the
# out-of-map pocket-dimension room from generated worlds entirely.
$pocketRoomAnchor = @"
`tr = CreateRoom(0, ROOM1, (MapWidth-1) * 8, 0, (MapHeight-1) * 8, "pocketdimension")
`tMapRoomID(ROOM1)=MapRoomID(ROOM1)+1`t
"@
$pocketRoomBlock = @"
`t; SCP-682 QA build: SCP-106's pocket dimension is intentionally not generated.
"@
Replace-FileOnce 'MapSystem.bb' $pocketRoomAnchor $pocketRoomBlock 'omit pocket-dimension room'

# Disable every legacy capture route into the pocket dimension. Keep the stock body
# available under a renamed function for source comparison, but no gameplay path calls it.
$pocketFunctionAnchor = @"
Function MoveToPocketDimension()
`tLocal r.Rooms
"@
$pocketFunctionBlock = @"
Function MoveToPocketDimension()
`t; SCP-682 cannot be progression-gated by SCP-106's pocket dimension.
`tFallTimer = 0
`tBlurTimer = 0
`tPlayable = True
`tReturn
End Function

Function MoveToPocketDimension_Legacy()
`tLocal r.Rooms
"@
Replace-FileOnce 'NPCs.bb' $pocketFunctionAnchor $pocketFunctionBlock 'disable SCP-106 pocket-dimension capture'

Write-Host 'Applied SCP-682 source integration patch (fixed QA seed, no 106 pocket dimension).'
