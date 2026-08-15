; SCP-682 player proof of concept
; Real SCP: Containment Breach source-mod gameplay layer.
; This file intentionally uses the existing CB world, .rmesh rooms, doors, NPCs,
; collision, events, sound and rendering systems. It does not replace them.
;
; Character art/viewmodel is deliberately NOT faked here. The proof of concept
; focuses on gameplay integration until a properly modelled/rigged 682 asset exists.

Const SCP682_KEY_INTERACT% = 18 ; E
Const SCP682_KEY_CHARGE%   = 19 ; R
Const SCP682_KEY_SLAM%     = 16 ; Q
Const SCP682_KEY_ROAR%     = 33 ; F
Const SCP682_KEY_RAGE%     = 46 ; C

Global SCP682_Mode% = True
Global SCP682_Initialized% = False
Global SCP682_LastCollider% = 0

Global SCP682_Biomass# = 100.0
Global SCP682_MaxBiomass# = 100.0
Global SCP682_Rage# = 0.0
Global SCP682_RageTimer# = 0.0
Global SCP682_RegenDelay# = 0.0
Global SCP682_ReconstituteTimer# = 0.0

Global SCP682_AdaptBallistic# = 0.0
Global SCP682_AdaptTrauma# = 0.0

Global SCP682_SlashCooldown# = 0.0
Global SCP682_MaulCooldown# = 0.0
Global SCP682_ChargeCooldown# = 0.0
Global SCP682_SlamCooldown# = 0.0
Global SCP682_RoarCooldown# = 0.0
Global SCP682_ChargeTimer# = 0.0

Global SCP682_Message$ = ""
Global SCP682_MessageTimer# = 0.0


Function InitSCP682Player()
	SCP682_Initialized = True
	SCP682_LastCollider = Collider
	
	SCP682_Biomass = SCP682_MaxBiomass
	SCP682_Rage = 0.0
	SCP682_RageTimer = 0.0
	SCP682_RegenDelay = 0.0
	SCP682_ReconstituteTimer = 0.0
	
	SCP682_AdaptBallistic = 0.0
	SCP682_AdaptTrauma = 0.0
	
	SCP682_SlashCooldown = 0.0
	SCP682_MaulCooldown = 0.0
	SCP682_ChargeCooldown = 0.0
	SCP682_SlamCooldown = 0.0
	SCP682_RoarCooldown = 0.0
	SCP682_ChargeTimer = 0.0
	
	Injuries = 0
	Bloodloss = 0
	Infect = 0
	Stamina = 100
	KillTimer = 0
	DeathTimer = 0
	FallTimer = 0
	Playable = True
	
	SCP682_Message = "SCP-682 PLAYER PROTOTYPE ACTIVE"
	SCP682_MessageTimer = 70*4
End Function


Function SCP682GameplayInputAllowed%()
	If MainMenuOpen Then Return False
	If MenuOpen Then Return False
	If InvOpen Then Return False
	If ConsoleOpen Then Return False
	If Using294 Then Return False
	If EndingTimer < 0 Then Return False
	If SCP682_ReconstituteTimer > 0 Then Return False
	If Not Playable Then Return False
	Return True
End Function


Function SCP682SetMessage(txt$, time# = 210.0)
	SCP682_Message = txt
	SCP682_MessageTimer = time
End Function


Function SCP682IsHumanTarget%(n.NPCs)
	If n = Null Then Return False
	
	Select n\NPCtype
		Case NPCtypeGuard, NPCtypeD, NPCtypeMTF, NPCtypeZombie, NPCtype008, NPCtypeClerk
			Return True
	End Select
	
	Return False
End Function


Function SCP682CanMaulTarget%(n.NPCs)
	If n = Null Then Return False
	
	Select n\NPCtype
		Case NPCtypeGuard, NPCtypeD, NPCtypeMTF, NPCtypeZombie, NPCtype008, NPCtypeClerk
			Return True
		Case NPCtype939, NPCtype966, NPCtype1048a, NPCtype1499
			Return True
	End Select
	
	Return False
End Function


Function SCP682FindTarget.NPCs(range#, cone#)
	Local n.NPCs
	Local best.NPCs = Null
	Local dist#, bestDist# = range + 1.0
	Local angle#
	
	For n.NPCs = Each NPCs
		If n\Collider <> 0 And (Not n\IsDead) Then
			dist = EntityDistance(Collider, n\Collider)
			If dist <= range And dist < bestDist Then
				angle = Abs(DeltaYaw(Collider, n\Collider))
				If angle <= cone Then
					best = n
					bestDist = dist
				EndIf
			EndIf
		EndIf
	Next
	
	Return best
End Function


Function SCP682NeutralizeNPC(n.NPCs)
	If n = Null Then Return
	
	n\IsDead = True
	n\IgnorePlayer = True
	n\Idle = True
	n\CurrSpeed = 0
	n\Speed = 0
	n\Reload = 999999
	n\PathStatus = 0
	n\PathTimer = 999999
	
	If n\SoundChn <> 0 Then
		If n\SoundChn_IsStream Then
			StopStream_Strict(n\SoundChn)
		Else
			If ChannelPlaying(n\SoundChn) Then StopChannel(n\SoundChn)
		EndIf
	EndIf
	
	If n\SoundChn2 <> 0 Then
		If n\SoundChn2_IsStream Then
			StopStream_Strict(n\SoundChn2)
		Else
			If ChannelPlaying(n\SoundChn2) Then StopChannel(n\SoundChn2)
		EndIf
	EndIf
	
	If n\obj <> 0 Then HideEntity n\obj
	If n\obj2 <> 0 Then HideEntity n\obj2
	If n\obj3 <> 0 Then HideEntity n\obj3
	If n\obj4 <> 0 Then HideEntity n\obj4
	If n\Collider <> 0 Then
		HideEntity n\Collider
		PositionEntity n\Collider, 0, 500.0, 0, True
		ResetEntity n\Collider
	EndIf
End Function


Function SCP682StaggerNPC(n.NPCs, force#)
	If n = Null Then Return
	If n\Collider = 0 Then Return
	
	n\Reload = Max(n\Reload, 70*2)
	n\PathStatus = 0
	n\PathTimer = Max(n\PathTimer, 70)
	
	Local pvt% = CreatePivot()
	PositionEntity pvt, EntityX(n\Collider,True), EntityY(n\Collider,True), EntityZ(n\Collider,True), True
	PointEntity pvt, Collider
	RotateEntity n\Collider, 0, EntityYaw(pvt)+180.0, 0, True
	MoveEntity n\Collider, 0, 0, force
	ResetEntity n\Collider
	FreeEntity pvt
End Function


Function SCP682ImpactTarget(n.NPCs, force# = 0.16)
	If n = Null Then Return
	
	If SCP682CanMaulTarget(n) Then
		SCP682NeutralizeNPC(n)
		SCP682_Rage = Min(100.0, SCP682_Rage + 8.0)
		SCP682SetMessage("TARGET TERMINATED")
	Else
		SCP682StaggerNPC(n, force)
		SCP682_Rage = Min(100.0, SCP682_Rage + 4.0)
		SCP682SetMessage("ANOMALOUS TARGET REPELLED")
	EndIf
End Function


Function SCP682BreachNearestDoor%(range#)
	Local d.Doors
	Local best.Doors = Null
	Local dist#, bestDist# = range + 1.0
	
	For d.Doors = Each Doors
		If d\frameobj <> 0 Then
			dist = EntityDistance(Collider, d\frameobj)
			If dist <= range And dist < bestDist Then
				best = d
				bestDist = dist
			EndIf
		EndIf
	Next
	
	If best = Null Then Return False
	
	best\locked = False
	best\KeyCard = 0
	best\fastopen = True
	best\open = True
	best\timerstate = 70*8
	
	If best\LinkedDoor <> Null Then
		best\LinkedDoor\locked = False
		best\LinkedDoor\KeyCard = 0
		best\LinkedDoor\fastopen = True
		best\LinkedDoor\open = True
		best\LinkedDoor\timerstate = 70*8
	EndIf
	
	If OpenDoorFastSFX <> 0 And best\obj <> 0 Then
		best\SoundCHN = PlaySound2(OpenDoorFastSFX, Camera, best\obj, 10.0, 1.0)
	EndIf
	
	CameraShake = Max(CameraShake, 1.5)
	SCP682SetMessage("BULKHEAD FORCED")
	Return True
End Function


Function SCP682Interact()
	Local d.Doors
	Local best.Doors = Null
	Local dist#, bestDist# = 1.45
	
	For d.Doors = Each Doors
		If d\frameobj <> 0 Then
			dist = EntityDistance(Collider, d\frameobj)
			If dist < bestDist Then
				best = d
				bestDist = dist
			EndIf
		EndIf
	Next
	
	If best <> Null Then UseDoor(best)
End Function


Function SCP682Slash()
	If SCP682_SlashCooldown > 0 Then Return
	
	Local target.NPCs = SCP682FindTarget(1.65, 55.0)
	SCP682_SlashCooldown = 18.0
	CameraShake = Max(CameraShake, 0.35)
	
	If target <> Null Then
		SCP682ImpactTarget(target, 0.12)
	Else
		SCP682BreachNearestDoor(1.15)
	EndIf
End Function


Function SCP682Maul()
	If SCP682_MaulCooldown > 0 Then Return
	
	Local target.NPCs = SCP682FindTarget(1.30, 38.0)
	SCP682_MaulCooldown = 38.0
	CameraShake = Max(CameraShake, 0.75)
	
	If target <> Null Then
		SCP682ImpactTarget(target, 0.24)
	Else
		SCP682BreachNearestDoor(1.35)
	EndIf
End Function


Function SCP682StartCharge()
	If SCP682_ChargeCooldown > 0 Then Return
	
	SCP682_ChargeTimer = 70*0.95
	SCP682_ChargeCooldown = 70*3.0
	CameraShake = Max(CameraShake, 0.8)
	SCP682SetMessage("CHARGE")
End Function


Function SCP682UpdateCharge()
	If SCP682_ChargeTimer <= 0 Then Return
	
	Local speed# = 0.050
	If SCP682_RageTimer > 0 Then speed = 0.070
	
	MoveEntity Collider, 0, 0, speed*FPSfactor
	SCP682BreachNearestDoor(1.25)
	
	Local target.NPCs = SCP682FindTarget(1.10, 48.0)
	If target <> Null Then
		SCP682ImpactTarget(target, 0.34)
		SCP682_ChargeTimer = Max(SCP682_ChargeTimer-(22*FPSfactor),0)
	EndIf
	
	CameraShake = Max(CameraShake, 0.45)
	SCP682_ChargeTimer = Max(SCP682_ChargeTimer-FPSfactor,0)
End Function


Function SCP682BodySlam()
	If SCP682_SlamCooldown > 0 Then Return
	
	SCP682_SlamCooldown = 70*2.8
	CameraShake = Max(CameraShake, 2.2)
	
	Local target.NPCs = SCP682FindTarget(2.30, 90.0)
	If target <> Null Then SCP682ImpactTarget(target, 0.42)
	
	Local n.NPCs
	For n.NPCs = Each NPCs
		If n\Collider <> 0 And (Not n\IsDead) Then
			If EntityDistance(Collider,n\Collider) <= 2.4 Then
				If n <> target Then SCP682StaggerNPC(n,0.18)
			EndIf
		EndIf
	Next
	
	SCP682BreachNearestDoor(1.65)
	SCP682_Rage = Min(100.0,SCP682_Rage+6.0)
	SCP682SetMessage("BODY SLAM")
End Function


Function SCP682Roar()
	If SCP682_RoarCooldown > 0 Then Return
	
	SCP682_RoarCooldown = 70*4.0
	CameraShake = Max(CameraShake, 0.9)
	
	Local n.NPCs
	For n.NPCs = Each NPCs
		If n\Collider <> 0 And (Not n\IsDead) Then
			If EntityDistance(Collider,n\Collider) <= 8.0 Then
				If SCP682IsHumanTarget(n) Then
					n\Reload = Max(n\Reload,70*3.0)
					n\PathStatus = 0
					n\PathTimer = 70*2.0
					SCP682StaggerNPC(n,0.08)
				EndIf
			EndIf
		EndIf
	Next
	
	SCP682_Rage = Min(100.0,SCP682_Rage+5.0)
	SCP682SetMessage("HOSTILE VOCALIZATION")
End Function


Function SCP682ActivateRage()
	If SCP682_Rage < 100.0 Then
		SCP682SetMessage("RAGE NOT READY: "+Int(SCP682_Rage)+"%")
		Return
	EndIf
	
	If SCP682_RageTimer > 0 Then Return
	
	SCP682_Rage = 0
	SCP682_RageTimer = 70*8.0
	SCP682SetMessage("ADAPTIVE RAGE")
	CameraShake = Max(CameraShake,1.25)
End Function


Function SCP682ConvertHumanDamage()
	Local rawTrauma# = Max(Injuries,0.0)
	Local rawBlood# = Max(Bloodloss,0.0)
	Local incoming# = (rawTrauma*6.0)+(rawBlood*0.08)
	
	If incoming > 0.05 Then
		Local reduction# = Min(SCP682_AdaptBallistic*0.65,0.72)
		Local actual# = incoming*(1.0-reduction)
		
		SCP682_Biomass = Max(SCP682_Biomass-actual,0.0)
		SCP682_Rage = Min(SCP682_Rage+(actual*1.8),100.0)
		SCP682_AdaptBallistic = Min(SCP682_AdaptBallistic+(incoming*0.0045),1.0)
		SCP682_RegenDelay = Max(SCP682_RegenDelay,70*1.25)
	EndIf
	
	; Human-specific wound and disease systems are converted into biomass damage.
	Injuries = 0
	Bloodloss = 0
	Infect = 0
	HealTimer = 0
End Function


Function SCP682RecoverFromCBKill()
	If KillTimer >= 0 And DeathTimer >= 0 Then Return
	
	Local lethalDamage# = 22.0*(1.0-Min(SCP682_AdaptTrauma*0.60,0.68))
	SCP682_Biomass = Max(SCP682_Biomass-lethalDamage,0.0)
	SCP682_Rage = Min(100.0,SCP682_Rage+35.0)
	SCP682_AdaptTrauma = Min(1.0,SCP682_AdaptTrauma+0.09)
	SCP682_RegenDelay = Max(SCP682_RegenDelay,70*2.0)
	
	KillTimer = 0
	DeathTimer = 0
	FallTimer = 0
	DropSpeed = 0
	HeadDropSpeed = 0
	Playable = True
	
	If Collider <> 0 Then ShowEntity Collider
	If Head <> 0 Then HideEntity Head
	
	CameraShake = Max(CameraShake,2.5)
	SCP682SetMessage("LETHAL TRAUMA ADAPTED")
End Function


Function SCP682StartReconstitution()
	If SCP682_ReconstituteTimer > 0 Then Return
	
	SCP682_ReconstituteTimer = 70*2.5
	Playable = False
	KillTimer = 0
	DeathTimer = 0
	FallTimer = 0
	DropSpeed = 0
	HeadDropSpeed = 0
	SCP682SetMessage("RECONSTITUTING",70*3)
End Function


Function SCP682UpdateReconstitution()
	If SCP682_ReconstituteTimer <= 0 Then Return
	
	KillTimer = 0
	DeathTimer = 0
	FallTimer = 0
	DropSpeed = 0
	HeadDropSpeed = 0
	
	If Collider <> 0 Then ShowEntity Collider
	If Head <> 0 Then HideEntity Head
	
	SCP682_ReconstituteTimer = Max(SCP682_ReconstituteTimer-FPSfactor,0)
	
	If SCP682_ReconstituteTimer <= 0 Then
		SCP682_Biomass = 40.0
		SCP682_Rage = Min(100.0,SCP682_Rage+50.0)
		SCP682_AdaptTrauma = Min(1.0,SCP682_AdaptTrauma+0.12)
		SCP682_RegenDelay = 70
		Playable = True
		SCP682SetMessage("RECONSTITUTION COMPLETE")
	EndIf
End Function


Function SCP682UpdateRegeneration()
	If SCP682_RegenDelay > 0 Then
		SCP682_RegenDelay = Max(SCP682_RegenDelay-FPSfactor,0)
	Else
		Local rate# = 0.045
		If SCP682_RageTimer > 0 Then rate = 0.105
		SCP682_Biomass = Min(SCP682_MaxBiomass,SCP682_Biomass+(rate*FPSfactor))
	EndIf
	
	If SCP682_RageTimer > 0 Then
		SCP682_RageTimer = Max(SCP682_RageTimer-FPSfactor,0)
	Else
		SCP682_Rage = Max(SCP682_Rage-(0.006*FPSfactor),0)
	EndIf
	
	SCP682_AdaptBallistic = Max(SCP682_AdaptBallistic-(0.00008*FPSfactor),0)
	SCP682_AdaptTrauma = Max(SCP682_AdaptTrauma-(0.00004*FPSfactor),0)
End Function


Function SCP682UpdateCooldowns()
	SCP682_SlashCooldown = Max(SCP682_SlashCooldown-FPSfactor,0)
	SCP682_MaulCooldown = Max(SCP682_MaulCooldown-FPSfactor,0)
	SCP682_ChargeCooldown = Max(SCP682_ChargeCooldown-FPSfactor,0)
	SCP682_SlamCooldown = Max(SCP682_SlamCooldown-FPSfactor,0)
	SCP682_RoarCooldown = Max(SCP682_RoarCooldown-FPSfactor,0)
	
	If SCP682_MessageTimer > 0 Then
		SCP682_MessageTimer = Max(SCP682_MessageTimer-FPSfactor2,0)
	EndIf
End Function


Function DrawSCP682Meter(x%,y%,w%,h%,value#,maxValue#,r%,g%,b%)
	Local frac# = 0.0
	If maxValue > 0 Then frac = Max(Min(value/maxValue,1.0),0.0)
	
	Color 8,8,8
	Rect x,y,w,h,True
	Color 90,90,90
	Rect x,y,w,h,False
	Color r,g,b
	Rect x+2,y+2,Int((w-4)*frac),h-4,True
End Function


Function DrawSCP682HUD()
	If MainMenuOpen Then Return
	
	Local x% = 22*MenuScale
	Local y% = 22*MenuScale
	Local w% = 275*MenuScale
	Local h% = 12*MenuScale
	Local adapt# = Max(SCP682_AdaptBallistic,SCP682_AdaptTrauma)*100.0
	
	AASetFont Font1
	Color 235,235,235
	AAText x,y,"SCP-682 // BIOMASS "+Int(SCP682_Biomass)+"%"
	DrawSCP682Meter(x,y+20*MenuScale,w,h,SCP682_Biomass,SCP682_MaxBiomass,150,35,28)
	
	Color 235,235,235
	AAText x,y+43*MenuScale,"RAGE "+Int(SCP682_Rage)+"%"
	DrawSCP682Meter(x,y+62*MenuScale,w,h,SCP682_Rage,100.0,175,55,25)
	
	Color 235,235,235
	AAText x,y+85*MenuScale,"ADAPTATION "+Int(adapt)+"%"
	DrawSCP682Meter(x,y+104*MenuScale,w,h,adapt,100.0,120,120,120)
	
	Color 210,210,210
	AAText 20*MenuScale,GraphicHeight-(58*MenuScale),"LMB SLASH  RMB MAUL  R CHARGE  Q SLAM  F ROAR  C RAGE  E INTERACT"
	
	If SCP682_MessageTimer > 0 And SCP682_Message <> "" Then
		Color 0,0,0
		AAText (GraphicWidth/2)+1,(GraphicHeight*0.72)+1,SCP682_Message,True,False,Min(SCP682_MessageTimer/70.0,1.0)
		Color 230,70,55
		AAText GraphicWidth/2,GraphicHeight*0.72,SCP682_Message,True,False,Min(SCP682_MessageTimer/70.0,1.0)
	EndIf
End Function


Function UpdateSCP682Player()
	If Not SCP682_Mode Then Return
	
	If Collider = 0 Or Camera = 0 Then
		SCP682_Initialized = False
		SCP682_LastCollider = 0
		Return
	EndIf
	
	If (Not SCP682_Initialized) Or SCP682_LastCollider <> Collider Then InitSCP682Player()
	
	; 682 cannot permanently die. This prevents CB's Keter/permadeath path from
	; deleting a save before the reconstitution layer can catch a lethal event.
	If SelectedDifficulty <> Null Then SelectedDifficulty\permaDeath = False
	
	SCP682ConvertHumanDamage()
	SCP682RecoverFromCBKill()
	
	If SCP682_Biomass <= 0 And SCP682_ReconstituteTimer <= 0 Then SCP682StartReconstitution()
	SCP682UpdateReconstitution()
	
	; D-9341 survival mechanics are intentionally suppressed for SCP-682.
	InfiniteStamina = True
	Stamina = 100
	BlinkTimer = Max(BLINKFREQ,70)
	EyeIrritation = 0
	EyeStuck = 0
	Crouch = False
	CrouchState = 0
	Sanity = 0
	RestoreSanity = True
	
	SCP682UpdateCooldowns()
	SCP682UpdateRegeneration()
	
	If SCP682GameplayInputAllowed() Then
		If MouseHit1 Then SCP682Slash()
		If MouseHit2 Then SCP682Maul()
		If KeyHit(SCP682_KEY_CHARGE) Then SCP682StartCharge()
		If KeyHit(SCP682_KEY_SLAM) Then SCP682BodySlam()
		If KeyHit(SCP682_KEY_ROAR) Then SCP682Roar()
		If KeyHit(SCP682_KEY_RAGE) Then SCP682ActivateRage()
		If KeyHit(SCP682_KEY_INTERACT) Then SCP682Interact()
	EndIf
	
	SCP682UpdateCharge()
	DrawSCP682HUD()
End Function
