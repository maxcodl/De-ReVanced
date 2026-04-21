package app.morphe.patches.tiktok.misc.spoof.location

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.tiktok.misc.extension.sharedExtensionPatch
import app.morphe.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.morphe.patches.tiktok.misc.settings.settingsPatch
import app.morphe.util.findMutableMethodOf
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SPOOF_LOCATION_CLASS_DESCRIPTOR = "Lapp/morphe/extension/tiktok/spoof/location/SpoofLocationPatch;"
private const val LOCATION_CLASS_DESCRIPTOR = "Landroid/location/Location;"
private const val LOCALE_CLASS_DESCRIPTOR = "Ljava/util/Locale;"

@Suppress("unused")
val spoofLocationPatch = bytecodePatch(
    name = "Location spoof",
    description = "Spoofs location values returned by Android Location APIs. Supports TikTok 43.6.2.",
    default = true,
) {
    dependsOn(
        sharedExtensionPatch,
        settingsPatch,
    )

    compatibleWith(*AppCompatibilities.tiktok4362())

    execute {
        SettingsStatusLoadFingerprint.method.addInstruction(
            0,
            "invoke-static {}, Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableLocationSpoof()V",
        )

        val locationReplacements = hashMapOf(
            "getLatitude" to "getLatitude",
            "getLongitude" to "getLongitude",
        )
        val localeReplacements = hashMapOf(
            "getCountry" to "getCountryCode",
            "getISO3Country" to "getIso3Country",
        )

        classDefForEach { classDef ->
            classDef.methods.forEach methodLoop@{ method ->
                val instructions = method.implementation?.instructions ?: return@methodLoop
                val instructionList = instructions.toList()
                val patchIndices = ArrayDeque<Pair<Int, String>>()

                instructionList.forEachIndexed { index, instruction ->
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed

                    val methodRef = (instruction as? Instruction35c)?.reference as? MethodReference ?: return@forEachIndexed
                    if (methodRef.definingClass == LOCATION_CLASS_DESCRIPTOR && methodRef.returnType == "D") {
                        val moveResult = instructionList.getOrNull(index + 1) ?: return@forEachIndexed
                        if (moveResult.opcode != Opcode.MOVE_RESULT_WIDE) return@forEachIndexed

                        locationReplacements[methodRef.name]?.let { replacement ->
                            patchIndices.add(index to replacement)
                        }
                        return@forEachIndexed
                    }

                    if (methodRef.definingClass == LOCALE_CLASS_DESCRIPTOR && methodRef.returnType == "Ljava/lang/String;") {
                        val moveResult = instructionList.getOrNull(index + 1) ?: return@forEachIndexed
                        if (moveResult.opcode != Opcode.MOVE_RESULT_OBJECT) return@forEachIndexed

                        localeReplacements[methodRef.name]?.let { replacement ->
                            patchIndices.add(index to replacement)
                        }
                        return@forEachIndexed
                    }
                }

                if (patchIndices.isEmpty()) return@methodLoop

                val mutableMethod = mutableClassDefBy(classDef).findMutableMethodOf(method)
                while (patchIndices.isNotEmpty()) {
                    val (index, replacement) = patchIndices.removeLast()
                    val moveResultOpcode = mutableMethod.getInstruction(index + 1).opcode

                    if (moveResultOpcode == Opcode.MOVE_RESULT_WIDE) {
                        val resultRegister = mutableMethod.getInstruction<OneRegisterInstruction>(index + 1).registerA
                        val resultRegisterHigh = resultRegister + 1

                        mutableMethod.addInstructions(
                            index + 2,
                            """
                                invoke-static/range {v$resultRegister .. v$resultRegisterHigh}, $SPOOF_LOCATION_CLASS_DESCRIPTOR->$replacement(D)D
                                move-result-wide v$resultRegister
                            """,
                        )
                    } else if (moveResultOpcode == Opcode.MOVE_RESULT_OBJECT) {
                        val resultRegister = mutableMethod.getInstruction<OneRegisterInstruction>(index + 1).registerA

                        mutableMethod.addInstructions(
                            index + 2,
                            """
                                invoke-static {v$resultRegister}, $SPOOF_LOCATION_CLASS_DESCRIPTOR->$replacement(Ljava/lang/String;)Ljava/lang/String;
                                move-result-object v$resultRegister
                            """,
                        )
                    } else {
                        // Unsupported move-result opcode; skip this injection point.
                        continue
                    }
                }
            }
        }
    }
}
