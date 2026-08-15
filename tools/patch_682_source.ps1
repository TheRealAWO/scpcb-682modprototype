$ErrorActionPreference = 'Stop'

$mainPath = Join-Path $PSScriptRoot '..\Main.bb'
$mainPath = [System.IO.Path]::GetFullPath($mainPath)
$text = [System.IO.File]::ReadAllText($mainPath)

function Replace-Once([string]$needle, [string]$replacement, [string]$description) {
    $script:text = [System.IO.File]::ReadAllText($mainPath)
    $count = ([regex]::Matches($script:text, [regex]::Escape($needle))).Count
    if ($count -ne 1) {
        throw "Expected exactly one match for $description, found $count"
    }
    $script:text = $script:text.Replace($needle, $replacement)
    [System.IO.File]::WriteAllText($mainPath, $script:text, [System.Text.Encoding]::Default)
}

# Integrate only after CB has declared Rooms, Doors, NPCs, events, UI globals, etc.
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
$introAnchor = "Function InitNewGame()`r`n`tCatchErrors(""Uncaught (InitNewGame)"")"
if (-not $text.Contains($introAnchor)) {
    # tolerate LF checkouts
    $introAnchor = "Function InitNewGame()`n`tCatchErrors(""Uncaught (InitNewGame)"")"
}
$introBlock = $introAnchor + "`r`n`tIf SCP682_Mode Then IntroEnabled = False"
if ($text.Contains($introAnchor)) {
    Replace-Once $introAnchor $introBlock 'disable D-9341 intro'
} else {
    throw 'Could not find InitNewGame anchor'
}

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

Write-Host 'Applied SCP-682 source integration patch.'
