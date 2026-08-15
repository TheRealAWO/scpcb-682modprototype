; Compatibility shims for compiling the legacy SCP:CB 1.3.11 source with the
; current open-source Blitz3D compiler/runtime.
;
; The historical CB build used a modified/mavless Blitz3D runtime that exposed
; several helpers which are not present in the current open-source SoLoud runtime.
; Visual-only texture calls are harmless no-ops here. ErrorLog() is also a
; historical runtime hook; returning an empty string preserves CB's normal
; CatchErrors control flow while the modern runtime reports fatal errors itself.

Function TextureBumpEnvMat(texture%, stage%, row%, value#)
End Function

Function TextureBumpEnvOffset(texture%, offset#)
End Function

Function TextureBumpEnvScale(texture%, scale#)
End Function

Function TextureLodBias(value#)
End Function

Function ErrorLog$()
	Return ""
End Function
