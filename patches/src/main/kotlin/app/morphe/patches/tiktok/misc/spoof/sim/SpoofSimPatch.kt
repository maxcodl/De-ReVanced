/*
 * Forked from:
 * https://gitlab.com/ReVanced/revanced-patches/-/blob/main/patches/src/main/kotlin/app/revanced/patches/tiktok/misc/spoof/sim/SpoofSimPatch.kt
 */
package app.morphe.patches.tiktok.misc.spoof.sim

import app.morphe.patcher.extensions.ClassDefExtensions.firstMethod
import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.patches.tiktok.misc.extension.sharedExtensionPatch
import app.morphe.patches.tiktok.misc.settings.SettingsStatusLoadFingerprint
import app.morphe.patches.tiktok.misc.settings.settingsPatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction35c
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val SPOOF_SIM_CLASS_DESCRIPTOR = "Lapp/morphe/extension/tiktok/spoof/sim/SpoofSimPatch;"
private const val TELEPHONY_MANAGER_DESCRIPTOR = "Landroid/telephony/TelephonyManager;"

@Suppress("unused")
val spoofSimPatch = bytecodePatch(
    name = "SIM spoof",
    description = "Spoofs SIM card information returned by Android APIs. Supports TikTok 43.6.2.",
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
            "invoke-static {}, Lapp/morphe/extension/tiktok/settings/SettingsStatus;->enableSimSpoof()V",
        )

        val replacements = hashMapOf(
            "getSimCountryIso" to "getCountryIso",
            "getNetworkCountryIso" to "getCountryIso",
            "getSimOperator" to "getOperator",
            "getNetworkOperator" to "getOperator",
            "getSimOperatorName" to "getOperatorName",
            "getNetworkOperatorName" to "getOperatorName",
        )

        classDefs.forEach classDef@{ classDef ->
            val classMethods = classDef.methods
            classMethods.forEach methodLoop@{ method ->
                val instructions = method.implementation?.instructions ?: return@methodLoop
                val patchIndices = ArrayDeque<Pair<Int, String>>()

                instructions.forEachIndexed { index, instruction ->
                    if (instruction.opcode != Opcode.INVOKE_VIRTUAL) return@forEachIndexed

                    val methodRef = (instruction as? Instruction35c)?.reference as? MethodReference ?: return@forEachIndexed
                    if (methodRef.definingClass != TELEPHONY_MANAGER_DESCRIPTOR) return@forEachIndexed

                    replacements[methodRef.name]?.let { replacement ->
                        patchIndices.add(index to replacement)
                    }
                }

                if (patchIndices.isEmpty()) return@methodLoop

                val mutableMethod = classDef.firstMethod(method)
                while (patchIndices.isNotEmpty()) {
                    val (index, replacement) = patchIndices.removeLast()
                    val resultRegister = mutableMethod.getInstruction<OneRegisterInstruction>(index + 1).registerA

                    mutableMethod.addInstructions(
                        index + 2,
                        """
                            invoke-static {v$resultRegister}, $SPOOF_SIM_CLASS_DESCRIPTOR->$replacement(Ljava/lang/String;)Ljava/lang/String;
                            move-result-object v$resultRegister
                        """,
                    )
                }
            }
        }
    }
}
