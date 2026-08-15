; Compatibility shims for compiling the legacy SCP:CB 1.3.11 source with the
; current open-source Blitz3D compiler/runtime.
;
; The historical CB build used a modified/mavless Blitz3D runtime that exposed
; several fixed-function bump-environment helpers which are not present in the
; current open-source SoLoud runtime. They are visual-only setup calls; leaving
; them as no-ops preserves room geometry, diffuse/lightmap materials, collision,
; events and gameplay while allowing the modern compiler to build the project.

Function TextureBumpEnvMat(texture%, stage%, row%, value#)
	; Legacy fixed-function bump environment matrix entry.
End Function

Function TextureBumpEnvOffset(texture%, offset#)
	; Legacy fixed-function bump environment offset.
End Function

Function TextureBumpEnvScale(texture%, scale#)
	; Legacy fixed-function bump environment scale.
End Function
