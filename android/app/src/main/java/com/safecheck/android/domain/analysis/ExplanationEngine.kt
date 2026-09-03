package com.safecheck.android.domain.analysis

import com.safecheck.android.ui.theme.RiskBand

object ExplanationEngine {

    enum class Language(val code: String, val label: String) {
        ENGLISH("en", "English"),
        HINDI("hi", "हिंदी"),
    }

    data class ExplanationResult(
        val narrative: String,
        val whatDetected: String,
        val whySuspicious: String,
        val whatToDo: List<String>,
        val whatNotToDo: List<String>,
        val recommendedActions: List<String>,
    )

    fun generate(
        score: Int,
        band: RiskBand,
        entities: ExtractedEntities,
        urlResult: UrlAnalysisResult,
        sender: String? = null,
        language: Language = Language.ENGLISH,
    ): ExplanationResult {
        return if (language == Language.HINDI) {
            generateHindi(score, band, entities, urlResult, sender)
        } else {
            generateEnglish(score, band, entities, urlResult, sender)
        }
    }

    private fun generateEnglish(
        score: Int,
        band: RiskBand,
        entities: ExtractedEntities,
        urlResult: UrlAnalysisResult,
        sender: String?,
    ): ExplanationResult {
        val targetEntity = (entities.banks + entities.government + entities.couriers).firstOrNull() ?: "a known service"
        val senderPrefix = if (!sender.isNullOrBlank()) "Message from sender '$sender': " else ""

        val whatDetected = senderPrefix + when {
            entities.remoteTools.isNotEmpty() ->
                "Request to install remote desktop access software (${entities.remoteTools.joinToString(", ")})."
            entities.hasApkMention ->
                "Instruction to download and install an unauthorized Android application package (.APK)."
            urlResult.isLookalike ->
                "A link pointing to an unverified domain mimicking $targetEntity (${urlResult.primaryDomain})."
            entities.hasOtpAsk || entities.hasPinAsk ->
                "Direct solicitation of your confidential authentication credentials (OTP / UPI PIN)."
            entities.hasPanDemand ->
                "A demand to update or link your PAN / KYC details under threat of account disruption."
            else ->
                "Message or content inspected for social engineering, coercive urgency, and suspicious links."
        }

        val whySuspicious = when {
            entities.remoteTools.isNotEmpty() ->
                "Legitimate customer support representatives will never ask you to install AnyDesk or TeamViewer. Fraudsters use these tools to take over your screen and siphon funds."
            entities.hasApkMention ->
                "Financial institutions and government departments only distribute apps via Google Play Store, never via direct download links in SMS."
            urlResult.isLookalike ->
                "The link is not an official domain of $targetEntity. Real institutions operate on secure, verified web portals."
            entities.hasOtpAsk || entities.hasPinAsk ->
                "Banks and payment providers never ask for your OTP or UPI PIN under any circumstance."
            band == RiskBand.HIGH ->
                "The message uses artificial urgency ('blocked today') combined with financial pressure to compel an impulsive reaction."
            band == RiskBand.MEDIUM ->
                "The source or content contains ambiguous elements that require verification before proceeding."
            else ->
                "No coercive language, suspicious URLs, or credential solicitation patterns were detected."
        }

        val whatToDo = when (band) {
            RiskBand.HIGH -> listOf(
                "Verify your account status independently using the official mobile app.",
                "Call your bank or provider only using the official helpline printed on your card.",
                "If money was lost, immediately dial 1930 Cyber Helpline or report on cybercrime.gov.in.",
            )
            RiskBand.MEDIUM -> listOf(
                "Reach out to the sender via a known trusted phone number before clicking.",
                "Check the status inside the legitimate app rather than following SMS links.",
            )
            else -> listOf(
                "No immediate action needed. Continue practicing safe digital hygiene.",
            )
        }

        val whatNotToDo = when (band) {
            RiskBand.HIGH -> listOf(
                "Do NOT click on any link or scan QR codes in this message.",
                "Do NOT share your OTP, PIN, password, or CVV with anyone.",
                "Do NOT install any APK or screen-sharing app like AnyDesk or TeamViewer.",
                "Do NOT call phone numbers provided inside the message.",
            )
            RiskBand.MEDIUM -> listOf(
                "Do not approve any UPI payment requests or enter your UPI PIN.",
                "Do not forward the message or OTP to unverified contacts.",
            )
            else -> listOf(
                "Never share your OTP or PIN with anyone claiming to be customer service.",
            )
        }

        val narrative = when (band) {
            RiskBand.HIGH ->
                "High risk detected ($score/100). $whatDetected $whySuspicious"
            RiskBand.MEDIUM ->
                "Caution advised ($score/100). $whatDetected $whySuspicious"
            else ->
                "Safe content verified ($score/100). $whySuspicious"
        }

        val recommendedActions = if (band == RiskBand.HIGH) {
            listOf(
                "Do not click the link or open any attachment.",
                "Do not share OTP, UPI PIN, or bank passwords.",
                "Verify with your bank using their official app or a trusted branch number.",
                "Consult your Safety Circle for a second opinion.",
            )
        } else if (band == RiskBand.MEDIUM) {
            listOf(
                "Do not enter credentials or pay any fees.",
                "Verify the sender's identity through official channels.",
                "Ask a trusted contact in your Safety Circle.",
            )
        } else {
            listOf(
                "No action required. Stay alert if any future message asks for money or an OTP.",
            )
        }

        return ExplanationResult(
            narrative = narrative,
            whatDetected = whatDetected,
            whySuspicious = whySuspicious,
            whatToDo = whatToDo,
            whatNotToDo = whatNotToDo,
            recommendedActions = recommendedActions,
        )
    }

    private fun generateHindi(
        score: Int,
        band: RiskBand,
        entities: ExtractedEntities,
        urlResult: UrlAnalysisResult,
        sender: String?,
    ): ExplanationResult {
        val targetEntity = (entities.banks + entities.government + entities.couriers).firstOrNull() ?: "संबंधित संस्था"
        val senderPrefix = if (!sender.isNullOrBlank()) "प्रेषक '$sender' से प्राप्त संदेश: " else ""

        val whatDetected = senderPrefix + when {
            entities.remoteTools.isNotEmpty() ->
                "रिमोट स्क्रीन-शेयरिंग ऐप (${entities.remoteTools.joinToString(", ")}) डाउनलोड करने का संदिग्ध अनुरोध।"
            entities.hasApkMention ->
                "अनाधिकृत एंड्रॉइड ऐप (.APK फाइल) डाउनलोड और इंस्टॉल करने का निर्देश।"
            urlResult.isLookalike ->
                "$targetEntity जैसी दिखने वाली नकली वेबसाइट का लिंक (${urlResult.primaryDomain})।"
            entities.hasOtpAsk || entities.hasPinAsk ->
                "आपके गोपनीय ओटीपी (OTP) या यूपीआई पिन (UPI PIN) मांगने का प्रयास।"
            entities.hasPanDemand ->
                "खाता बंद करने की धमकी देकर तुरंत पैन/केवाईसी अपडेट कराने का दबाव।"
            else ->
                "संदेश की सुरक्षा जांच की गई।"
        }

        val whySuspicious = when {
            entities.remoteTools.isNotEmpty() ->
                "कोई भी बैंक या सरकारी अधिकारी कभी भी AnyDesk या TeamViewer डाउनलोड करने को नहीं कहता। जालसाज इसके जरिए आपका फोन नियंत्रित कर पैसे निकाल लेते हैं।"
            entities.hasApkMention ->
                "असली बैंक केवल Google Play Store से ही ऐप डाउनलोड करने की अनुमति देते हैं, कभी भी SMS में लिंक नहीं भेजते।"
            urlResult.isLookalike ->
                "यह वेबसाइट $targetEntity की आधिकारिक वेबसाइट नहीं है। यह जानकारी चुराने के लिए बनाई गई नकली साइट है।"
            entities.hasOtpAsk || entities.hasPinAsk ->
                "बैंक या कोई भी आधिकारिक प्रतिनिधि कभी भी आपका OTP या UPI PIN नहीं मांगता।"
            band == RiskBand.HIGH ->
                "इस संदेश में तुरंत खाता ब्लॉक करने का डर दिखाकर आपसे गलत कदम उठाने का दबाव बनाया जा रहा है।"
            band == RiskBand.MEDIUM ->
                "इस संदेश में कुछ संदिग्ध बातें हैं, आगे बढ़ने से पहले पूरी तरह जांच करें।"
            else ->
                "संदेश में कोई संदिग्ध लिंक, धमकी या गोपनीय जानकारी मांगने का संकेत नहीं मिला।"
        }

        val whatToDo = when (band) {
            RiskBand.HIGH -> listOf(
                "अपने बैंक की आधिकारिक ऐप खोलकर ही खाते की सही स्थिति देखें।",
                "केवल बैंक की पासबुक या कार्ड पर लिखे हेल्पलाइन नंबर पर बात करें।",
                "यदि पैसे कट गए हैं, तो तुरंत 1930 साइबर हेल्पलाइन पर कॉल करें।",
            )
            RiskBand.MEDIUM -> listOf(
                "संदेश में दिए गए लिंक पर जाने के बजाय सीधे आधिकारिक ऐप का उपयोग करें।",
                "अपने किसी भरोसेमंद परिचित (Safety Circle) से सलाह लें।",
            )
            else -> listOf(
                "किसी कार्रवाई की आवश्यकता नहीं है। डिजिटल सुरक्षा नियमों का पालन करते रहें।",
            )
        }

        val whatNotToDo = when (band) {
            RiskBand.HIGH -> listOf(
                "संदेश में दिए गए किसी भी लिंक या QR कोड पर क्लिक न करें।",
                "किसी के साथ भी अपना OTP, UPI PIN, या पासवर्ड साझा न करें।",
                "AnyDesk, TeamViewer या कोई भी APK फाइल फोन में इंस्टॉल न करें।",
                "संदेश में दिए गए मोबाइल नंबर पर कभी कॉल न करें।",
            )
            RiskBand.MEDIUM -> listOf(
                "कोई भी यूपीआई भुगतान या पिन दर्ज न करें।",
                "संदेश को आगे किसी को फॉरवर्ड न करें।",
            )
            else -> listOf(
                "ग्राहक सेवा के नाम पर आने वाले किसी भी कॉल पर अपना OTP कभी न दें।",
            )
        }

        val narrative = when (band) {
            RiskBand.HIGH ->
                "अत्यधिक जोखिम पाया गया ($score/100)। $whatDetected $whySuspicious"
            RiskBand.MEDIUM ->
                "सावधानी बरतें ($score/100)। $whatDetected $whySuspicious"
            else ->
                "संदेश सुरक्षित प्रतीत होता है ($score/100)। $whySuspicious"
        }

        val recommendedActions = if (band == RiskBand.HIGH) {
            listOf(
                "लिंक पर क्लिक न करें और न ही कोई फाइल खोलें।",
                "अपना OTP या UPI PIN किसी को न बताएं।",
                "बैंक की आधिकारिक ऐप से सीधे पुष्टि करें।",
                "अपने Safety Circle संपर्क से दूसरी राय लें।",
            )
        } else if (band == RiskBand.MEDIUM) {
            listOf(
                "कोई भी पासवर्ड या पिन दर्ज न करें।",
                "आधिकारिक माध्यम से ही प्रेषक की पहचान सुनिश्चित करें।",
            )
        } else {
            listOf(
                "कोई कार्रवाई आवश्यक नहीं। भविष्य में पैसे या OTP मांगने पर सतर्क रहें।",
            )
        }

        return ExplanationResult(
            narrative = narrative,
            whatDetected = whatDetected,
            whySuspicious = whySuspicious,
            whatToDo = whatToDo,
            whatNotToDo = whatNotToDo,
            recommendedActions = recommendedActions,
        )
    }

    fun generateFromEvidence(
        result: com.safecheck.android.domain.model.RiskResult,
        language: Language = Language.HINDI,
    ): ExplanationResult {
        val hasRemote = result.evidence.any { it.label.contains("Remote", ignoreCase = true) }
        val dummyEntities = ExtractedEntities(
            remoteTools = if (hasRemote) listOf("Remote Desktop Tool") else emptyList(),
            hasApkMention = result.evidence.any { it.label.contains("APK", ignoreCase = true) },
            hasOtpAsk = result.evidence.any { it.label.contains("OTP", ignoreCase = true) },
            hasPinAsk = result.evidence.any { it.label.contains("PIN", ignoreCase = true) },
            hasPanDemand = result.evidence.any { it.label.contains("PAN", ignoreCase = true) || it.label.contains("KYC", ignoreCase = true) },
            hasUrgentThreat = result.evidence.any { it.label.contains("Urgency", ignoreCase = true) || it.label.contains("Threat", ignoreCase = true) },
        )
        val dummyUrlResult = UrlAnalysisResult(
            urls = emptyList(),
            totalUrlPts = result.subScores.urlPts,
            evidence = emptyList(),
            primaryDomain = result.evidence.firstOrNull { it.label.contains("Domain", ignoreCase = true) || it.label.contains("URL", ignoreCase = true) }?.observedValue,
            isLookalike = result.evidence.any { it.label.contains("Lookalike", ignoreCase = true) },
        )
        return generate(
            score = result.score,
            band = result.band,
            entities = dummyEntities,
            urlResult = dummyUrlResult,
            language = language,
        )
    }
}
