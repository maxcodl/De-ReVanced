package app.morphe.patches.plusmessenger.misc

import app.morphe.patcher.extensions.InstructionExtensions.addInstruction
import app.morphe.patcher.extensions.InstructionExtensions.addInstructions
import app.morphe.patcher.extensions.InstructionExtensions.getInstruction
import app.morphe.patcher.extensions.InstructionExtensions.replaceInstruction
import app.morphe.patcher.patch.PatchException
import app.morphe.patcher.patch.bytecodePatch
import app.morphe.patcher.util.proxy.mutableTypes.MutableMethod
import app.morphe.patches.shared.compat.AppCompatibilities
import app.morphe.util.findMutableMethodOf
import app.morphe.util.getReference
import app.morphe.util.returnEarly
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.ClassDef
import com.android.tools.smali.dexlib2.iface.Method
import com.android.tools.smali.dexlib2.iface.instruction.NarrowLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.formats.Instruction22s
import com.android.tools.smali.dexlib2.iface.reference.FieldReference
import com.android.tools.smali.dexlib2.iface.reference.MethodReference

private const val TELEGRAM_PREFIX = "Lorg/telegram/"
private const val OFFICIAL_TELEGRAM_CERT_SHA256 =
    "49C1522548EBACD46CE322B6FD47F6092BB745D0F88082145CAF35E14DCC38E1"

@Suppress("unused")
val plusMessengerPatchesPatch = bytecodePatch(
    name = "Plus Messenger patches",
    description = "Ports the Python Plus Messenger patcher to Morphe Manager: premium checks, story privacy, " +
        "forwarding/saving restrictions, screenshots, sponsored content, banned channel access, secret media, " +
        "and download parameter tweaks.",
) {
    compatibleWith(AppCompatibilities.PLUS_MESSENGER)

    execute {
        var appliedChanges = 0

        fun String.isTelegramClass() = startsWith(TELEGRAM_PREFIX)

        fun ClassDef.simpleNameMatches(simpleName: String) =
            type.substringAfterLast('/').removeSuffix(";") == simpleName

        fun Method.matches(
            name: String,
            returnType: String? = null,
            parameterTypes: List<String>? = null,
        ) = this.name == name &&
            (returnType == null || this.returnType == returnType) &&
            (parameterTypes == null || this.parameterTypes.toList() == parameterTypes)

        fun findClass(simpleName: String): ClassDef? {
            var result: ClassDef? = null
            classDefForEach { classDef ->
                if (result == null && classDef.type.isTelegramClass() && classDef.simpleNameMatches(simpleName)) {
                    result = classDef
                }
            }
            return result
        }

        fun patchClassMethods(
            simpleName: String,
            block: MutableMethod.() -> Unit,
            predicate: Method.() -> Boolean,
        ) {
            val classDef = findClass(simpleName)
            if (classDef == null) {
                logger.warning("Plus Messenger patches: $simpleName not found")
                return
            }

            val mutableClass = mutableClassDefBy(classDef)
            classDef.methods.filter { it.predicate() }.forEach { method ->
                mutableClass.findMutableMethodOf(method).block()
                appliedChanges++
            }
        }

        fun MutableMethod.returnTrue() = returnEarly(true)
        fun MutableMethod.returnFalse() = returnEarly(false)
        fun MutableMethod.returnNullObject() = addInstructions(
            0,
            """
                const/4 v0, 0x0
                return-object v0
            """.trimIndent(),
        )

        fun MutableMethod.returnString(value: String) = returnEarly(value)

        fun MutableMethod.replaceBooleanFieldRead(fieldName: String, value: Boolean): Int {
            val replacementValue = if (value) "0x1" else "0x0"
            val indexes = implementation!!.instructions.mapIndexedNotNull { index, instruction ->
                val field = instruction.getReference<FieldReference>() ?: return@mapIndexedNotNull null
                if (field.definingClass.isTelegramClass() && field.name == fieldName) index else null
            }.asReversed()

            indexes.forEach { index ->
                val register = getInstruction<OneRegisterInstruction>(index).registerA
                addInstruction(index + 1, "const/4 v$register, $replacementValue")
            }

            return indexes.size
        }

        fun patchBooleanFieldReads(fieldName: String, value: Boolean) {
            classDefForEach { classDef ->
                if (!classDef.type.isTelegramClass()) return@classDefForEach

                val matches = classDef.methods.filter { method ->
                    method.implementation?.instructions?.any { instruction ->
                        val field = instruction.getReference<FieldReference>()
                        field?.definingClass?.isTelegramClass() == true && field.name == fieldName
                    } == true
                }
                if (matches.isEmpty()) return@classDefForEach

                val mutableClass = mutableClassDefBy(classDef)
                matches.forEach { method ->
                    appliedChanges += mutableClass.findMutableMethodOf(method)
                        .replaceBooleanFieldRead(fieldName, value)
                }
            }
        }

        fun patchWindowFlagConstants() {
            classDefForEach { classDef ->
                if (!classDef.type.isTelegramClass()) return@classDefForEach

                val methods = classDef.methods.filter { method ->
                    method.implementation?.instructions?.any { instruction ->
                        val methodReference = instruction.getReference<MethodReference>()
                        methodReference?.definingClass == "Landroid/view/Window;" &&
                            methodReference.name.contains("Flags")
                    } == true
                }
                if (methods.isEmpty()) return@classDefForEach

                val mutableClass = mutableClassDefBy(classDef)
                methods.forEach { method ->
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    val indexes = mutableMethod.implementation!!.instructions.mapIndexedNotNull { index, instruction ->
                        if (instruction.opcode == Opcode.CONST_16 &&
                            instruction is NarrowLiteralInstruction && instruction.narrowLiteral == 0x2000
                        ) index else null
                    }.asReversed()

                    indexes.forEach { index ->
                        val register = mutableMethod.getInstruction<OneRegisterInstruction>(index).registerA
                        mutableMethod.replaceInstruction(index, "const/16 v$register, 0x0")
                        appliedChanges++
                    }
                }
            }
        }

        fun patchSecretMediaViewerFlags() {
            listOf("SecretMediaViewer", "PhotoViewer").forEach viewerLoop@{ simpleName ->
                val classDef = findClass(simpleName) ?: return@viewerLoop
                val mutableClass = mutableClassDefBy(classDef)
                classDef.methods.forEach methodLoop@{ method ->
                    val instructions = method.implementation?.instructions ?: return@methodLoop
                    val matches = instructions.mapIndexedNotNull { index, instruction ->
                        if (instruction.opcode == Opcode.OR_INT_LIT16 &&
                            instruction is Instruction22s && instruction.narrowLiteral == 0x2000
                        ) index else null
                    }.asReversed()
                    if (matches.isEmpty()) return@methodLoop

                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    matches.forEach { index ->
                        val instruction = mutableMethod.getInstruction<Instruction22s>(index)
                        mutableMethod.replaceInstruction(
                            index,
                            "or-int/lit16 v${instruction.registerA}, v${instruction.registerA}, 0x0",
                        )
                        appliedChanges++
                    }
                }
            }
        }

        fun patchDownloadParams() {
            classDefForEach { classDef ->
                if (!classDef.type.isTelegramClass()) return@classDefForEach

                val methods = classDef.methods.filter { method ->
                    method.matches("updateParams", "V", emptyList()) &&
                        method.implementation?.instructions?.any { instruction ->
                            instruction is NarrowLiteralInstruction &&
                                (instruction.narrowLiteral == 0x20000 || instruction.narrowLiteral == 0x4)
                        } == true
                }
                if (methods.isEmpty()) return@classDefForEach

                val mutableClass = mutableClassDefBy(classDef)
                methods.forEach { method ->
                    val mutableMethod = mutableClass.findMutableMethodOf(method)
                    val indexes = mutableMethod.implementation!!.instructions.mapIndexedNotNull { index, instruction ->
                        if (instruction is NarrowLiteralInstruction &&
                            (instruction.narrowLiteral == 0x20000 || instruction.narrowLiteral == 0x4)
                        ) index else null
                    }.asReversed()

                    indexes.forEach { index ->
                        val instruction = mutableMethod.getInstruction<OneRegisterInstruction>(index)
                        val literal = (instruction as NarrowLiteralInstruction).narrowLiteral
                        when (literal) {
                            0x20000 -> mutableMethod.replaceInstruction(
                                index,
                                "const/high16 v${instruction.registerA}, 0x80000",
                            )
                            0x4 -> mutableMethod.replaceInstruction(
                                index,
                                "const/16 v${instruction.registerA}, 0x8",
                            )
                        }
                        appliedChanges++
                    }
                }
            }
        }

        patchClassMethods("AndroidUtilities", { returnString(OFFICIAL_TELEGRAM_CERT_SHA256) }) {
            matches("getCertificateSHA256Fingerprint", "Ljava/lang/String;", emptyList())
        }

        patchClassMethods("UserConfig", { returnTrue() }) { matches("isPremium", "Z", emptyList()) }
        patchClassMethods("StoriesController", { returnTrue() }) { matches("isPremium", "Z", listOf("J")) }
        patchClassMethods("PremiumPreviewFragment", { returnTrue() }) {
            matches("access\$3000", "Z", listOf("Lorg/telegram/ui/PremiumPreviewFragment;"))
        }
        patchClassMethods("StoriesController", { returnFalse() }) {
            matches(
                "markStoryAsRead",
                "Z",
                listOf(
                    "Lorg/telegram/tgnet/tl/TL_stories\$PeerStories;",
                    "Lorg/telegram/tgnet/tl/TL_stories\$StoryItem;",
                    "Z",
                ),
            ) || matches(
                "markStoryAsRead",
                "Z",
                listOf("J", "Lorg/telegram/tgnet/tl/TL_stories\$StoryItem;"),
            )
        }
        patchClassMethods("MessagesController", { returnFalse() }) {
            matches("isChatNoForwards", "Z", listOf("J")) ||
                matches("isChatNoForwards", "Z", listOf("Lorg/telegram/tgnet/TLRPC\$Chat;"))
        }
        patchClassMethods("MessagesController", { returnTrue() }) {
            name == "checkCanOpenChat" && returnType == "Z"
        }
        patchClassMethods("MessageObject", { returnFalse() }) { matches("isSponsored", "Z", emptyList()) }
        patchClassMethods("MessagesController", { returnTrue() }) { matches("isSponsoredDisabled", "Z", emptyList()) }
        patchClassMethods("MessagesController", { returnEarly() }) {
            matches("checkPromoInfoInternal", "V", listOf("Z"))
        }
        patchClassMethods("MessageObject", { returnFalse() }) {
            matches("isSecretMedia", "Z", emptyList()) ||
                matches("isSecretPhotoOrVideo", "Z", listOf("Lorg/telegram/tgnet/TLRPC\$Message;")) ||
                matches("isSecretMedia", "Z", listOf("Lorg/telegram/tgnet/TLRPC\$Message;"))
        }
        patchClassMethods("MessageObject", { returnEarly(0) }) { matches("getSecretTimeLeft", "I", emptyList()) }
        patchClassMethods("MessagesStorage", { returnNullObject() }) {
            matches("markMessagesAsDeleted", "Ljava/util/ArrayList;", listOf("J", "I", "Z", "Z")) ||
                matches(
                    "markMessagesAsDeleted",
                    "Ljava/util/ArrayList;",
                    listOf("J", "Ljava/util/ArrayList;", "Z", "Z", "I", "I"),
                )
        }

        classDefForEach { classDef ->
            if (!classDef.type.isTelegramClass()) return@classDefForEach
            val methods = classDef.methods.filter {
                it.name == "isPremiumFeatureAvailable" && it.returnType == "Z"
            }
            if (methods.isEmpty()) return@classDefForEach
            val mutableClass = mutableClassDefBy(classDef)
            methods.forEach { method ->
                mutableClass.findMutableMethodOf(method).returnEarly(true)
                appliedChanges++
            }
        }

        patchBooleanFieldReads("isRestrictedMessage", false)
        patchBooleanFieldReads("noforwards", false)
        patchBooleanFieldReads("premiumLocked", false)
        patchBooleanFieldReads("allowScreenCapture", true)
        patchBooleanFieldReads("allowScreenshots", true)
        patchWindowFlagConstants()
        patchSecretMediaViewerFlags()
        patchDownloadParams()

        if (appliedChanges == 0) {
            throw PatchException("No Plus Messenger patch points were found. The target version may be unsupported.")
        }

        logger.info("Plus Messenger patches applied $appliedChanges bytecode change(s)")
    }
}
