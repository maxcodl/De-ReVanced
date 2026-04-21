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

        val replacements = hashMapOf(
            "getLatitude" to "getLatitude",
            "getLongitude" to "getLongitude",
        )

        classDefForEach { classDef ->
            classDef.methods.forEach methodLoop@{ method ->
                val instructions = method.implementation?.instructions ?: return@methodLoop
                val instructionList = instructions.toList()
                val patchIndices = ArrayDeque<Pair<Int, String>>()

                instructionList.forEachIndexed { index, instruction ->
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed

                    val methodRef = (instruction as? Instruction35c)?.reference as? MethodReference ?: return@forEachIndexed
                    if (methodRef.definingClass != LOCATION_CLASS_DESCRIPTOR) return@forEachIndexed
                    if (methodRef.returnType != "D") return@forEachIndexed

                    val moveResult = instructionList.getOrNull(index + 1) ?: return@forEachIndexed
                    if (moveResult.opcode != Opcode.MOVE_RESULT_WIDE) return@forEachIndexed

                    replacements[methodRef.name]?.let { replacement ->
                        patchIndices.add(index to replacement)
                    }
                }

                if (patchIndices.isEmpty()) return@methodLoop

                val mutableMethod = mutableClassDefBy(classDef).findMutableMethodOf(method)
                while (patchIndices.isNotEmpty()) {
                    val (index, replacement) = patchIndices.removeLast()
                    val resultRegister = mutableMethod.getInstruction<OneRegisterInstruction>(index + 1).registerA
                    val resultRegisterHigh = resultRegister + 1

                    mutableMethod.addInstructions(
                        index + 2,
                        """
                            invoke-static/range {v$resultRegister .. v$resultRegisterHigh}, $SPOOF_LOCATION_CLASS_DESCRIPTOR->$replacement(D)D
                            move-result-wide v$resultRegister
                        """,
                    )
                }
            }
        }
    }
}
