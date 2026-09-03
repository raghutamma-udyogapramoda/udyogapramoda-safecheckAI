package com.safecheck.android.domain.analysis.entity

class BankDetector : EntityDetector {
    private val patterns = mapOf(
        "State Bank of India (SBI)" to Regex("(?i)\\b(sbi|state\\s*bank(\\s*of\\s*india)?|onlinesbi|yono)\\b"),
        "HDFC Bank" to Regex("(?i)\\b(hdfc(\\s*bank)?)\\b"),
        "ICICI Bank" to Regex("(?i)\\b(icici(\\s*bank)?|imobile)\\b"),
        "Axis Bank" to Regex("(?i)\\b(axis(\\s*bank)?)\\b"),
        "Punjab National Bank" to Regex("(?i)\\b(pnb|punjab\\s*national\\s*bank)\\b"),
        "Canara Bank" to Regex("(?i)\\b(canara(\\s*bank)?)\\b"),
        "Bank of Baroda" to Regex("(?i)\\b(bob|bank\\s*of\\s*baroda)\\b"),
        "Kotak Mahindra Bank" to Regex("(?i)\\b(kotak(\\s*bank)?|kotak811)\\b"),
        "Union Bank of India" to Regex("(?i)\\b(union\\s*bank(\\s*of\\s*india)?)\\b"),
        "IndusInd Bank" to Regex("(?i)\\b(indusind(\\s*bank)?)\\b"),
        "Yes Bank" to Regex("(?i)\\b(yes\\s*bank)\\b"),
    )

    override fun detect(text: String): List<ExtractedEntity> {
        return patterns.mapNotNull { (bankName, regex) ->
            regex.find(text)?.let {
                ExtractedEntity(
                    category = EntityCategory.BANK,
                    label = bankName,
                    value = it.value,
                    confidence = 0.95,
                    detectorName = "BankDetector",
                )
            }
        }
    }
}

class GovernmentDetector : EntityDetector {
    private val patterns = mapOf(
        "Income Tax Department" to Regex("(?i)\\b(income\\s*tax(\\s*department)?|it\\s*refund|itr|tax\\s*rebate)\\b"),
        "Cyber Crime Portal (1930)" to Regex("(?i)\\b(cyber\\s*crime|cyber\\s*police|1930|ncrp|i4c)\\b"),
        "Police / Law Enforcement" to Regex("(?i)\\b(police|cbi|ed|narcotics|arrest\\s*warrant|court\\s*summons|court\\s*order)\\b"),
        "Traffic Police (e-Challan)" to Regex("(?i)\\b(traffic\\s*police|e[- ]?challan|pending\\s*challan|vehicle\\s*fine)\\b"),
        "EPFO (Provident Fund)" to Regex("(?i)\\b(epfo|provident\\s*fund|pf\\s*claim|uan\\s*activation)\\b"),
        "UIDAI / Aadhaar" to Regex("(?i)\\b(uidai|aadhaar(\\s*update|\\s*link|\\s*card)?)\\b"),
        "Electricity Board" to Regex("(?i)\\b(electricity(\\s*bill|\\s*power)?|bijli\\s*bill|power\\s*disconnection|bses|uppcl|tneb)\\b"),
    )

    override fun detect(text: String): List<ExtractedEntity> {
        return patterns.mapNotNull { (govBody, regex) ->
            regex.find(text)?.let {
                ExtractedEntity(
                    category = EntityCategory.GOVERNMENT,
                    label = govBody,
                    value = it.value,
                    confidence = 0.92,
                    detectorName = "GovernmentDetector",
                )
            }
        }
    }
}

class CourierDetector : EntityDetector {
    private val patterns = mapOf(
        "India Post" to Regex("(?i)\\b(india\\s*post|post\\s*office|dak\\s*vibh?ag)\\b"),
        "Blue Dart" to Regex("(?i)\\b(blue\\s*dart)\\b"),
        "DTDC" to Regex("(?i)\\b(dtdc)\\b"),
        "Delhivery" to Regex("(?i)\\b(delhivery)\\b"),
        "Amazon Delivery" to Regex("(?i)\\b(amazon(\\s*delivery|\\s*order|\\s*package)?)\\b"),
        "FedEx" to Regex("(?i)\\b(fedex)\\b"),
        "DHL" to Regex("(?i)\\b(dhl)\\b"),
    )

    override fun detect(text: String): List<ExtractedEntity> {
        return patterns.mapNotNull { (courier, regex) ->
            regex.find(text)?.let {
                ExtractedEntity(
                    category = EntityCategory.COURIER,
                    label = courier,
                    value = it.value,
                    confidence = 0.90,
                    detectorName = "CourierDetector",
                )
            }
        }
    }
}

class RemoteAccessDetector : EntityDetector {
    private val tools = mapOf(
        "AnyDesk" to Regex("(?i)\\b(anydesk)\\b"),
        "TeamViewer" to Regex("(?i)\\b(teamviewer|quicksupport)\\b"),
        "RustDesk" to Regex("(?i)\\b(rustdesk)\\b"),
        "UltraViewer" to Regex("(?i)\\b(ultraviewer)\\b"),
        "AirDroid / Screen Share" to Regex("(?i)\\b(airdroid|screenshare|screen\\s*sharing)\\b"),
    )

    override fun detect(text: String): List<ExtractedEntity> {
        return tools.mapNotNull { (tool, regex) ->
            regex.find(text)?.let {
                ExtractedEntity(
                    category = EntityCategory.REMOTE_TOOL,
                    label = "Remote Access Tool ($tool)",
                    value = it.value,
                    confidence = 0.98,
                    detectorName = "RemoteAccessDetector",
                )
            }
        }
    }
}

class CredentialDetector : EntityDetector {
    private val otpRegex = Regex("(?i)(share|send|enter|give|provide|verify|input)\\s+(?:the\\s+|your\\s+|this\\s+)?(otp|one\\s*time\\s*password|verification\\s*code)\\b")
    private val pinRegex = Regex("(?i)(enter|provide|share|verify|give|input)\\s+(?:the\\s+|your\\s+)?(upi\\s*pin|atm\\s*pin|security\\s*pin|mpin|pin)\\b")
    private val pwdRegex = Regex("(?i)(enter|provide|send|reset|share|give|input)\\s+(?:the\\s+|your\\s+)?(password|netbanking\\s*password|credentials)\\b")
    private val cvvRegex = Regex("(?i)(enter|share|provide|give|input)\\s+(?:the\\s+|your\\s+)?(cvv|card\\s*cvv|security\\s*code)\\b")

    override fun detect(text: String): List<ExtractedEntity> {
        val list = mutableListOf<ExtractedEntity>()
        otpRegex.find(text)?.let {
            list.add(ExtractedEntity(EntityCategory.CREDENTIAL, "OTP Solicitation", it.value, 0.95, "CredentialDetector"))
        }
        pinRegex.find(text)?.let {
            list.add(ExtractedEntity(EntityCategory.CREDENTIAL, "UPI / ATM PIN Solicitation", it.value, 0.98, "CredentialDetector"))
        }
        pwdRegex.find(text)?.let {
            list.add(ExtractedEntity(EntityCategory.CREDENTIAL, "Password Solicitation", it.value, 0.95, "CredentialDetector"))
        }
        cvvRegex.find(text)?.let {
            list.add(ExtractedEntity(EntityCategory.CREDENTIAL, "Card CVV Solicitation", it.value, 0.98, "CredentialDetector"))
        }
        return list
    }
}

class PaymentDetector : EntityDetector {
    private val amountRegex = Regex("(?i)(?:₹|rs\\.?|inr)\\s*([0-9,]+(?:\\.[0-9]{2})?)")
    private val upiVpaRegex = Regex("(?i)\\b[a-zA-Z0-9.\\-_]{2,256}@(okhdfcbank|okaxis|oksbi|okicici|paytm|ybl|ibl|axl|upi)\\b")
    private val ifscRegex = Regex("(?i)\\b[A-Z]{4}0[A-Z0-9]{6}\\b")
    private val paymentApps = Regex("(?i)\\b(google\\s*pay|gpay|phonepe|paytm|bhim(\\s*upi)?|cred)\\b")

    override fun detect(text: String): List<ExtractedEntity> {
        val list = mutableListOf<ExtractedEntity>()
        amountRegex.findAll(text).forEach {
            list.add(ExtractedEntity(EntityCategory.PAYMENT, "Currency Amount", it.value, 0.90, "PaymentDetector"))
        }
        upiVpaRegex.findAll(text).forEach {
            list.add(ExtractedEntity(EntityCategory.PAYMENT, "UPI VPA ID", it.value, 0.95, "PaymentDetector"))
        }
        ifscRegex.findAll(text).forEach {
            list.add(ExtractedEntity(EntityCategory.PAYMENT, "Bank IFSC Code", it.value, 0.90, "PaymentDetector"))
        }
        paymentApps.findAll(text).forEach {
            list.add(ExtractedEntity(EntityCategory.PAYMENT, "Payment App Mention", it.value, 0.85, "PaymentDetector"))
        }
        return list
    }
}

class SocialEngineeringDetector : EntityDetector {
    private val urgencyRegex = Regex("(?i)\\b(blocked|block|suspend(ed)?|deactivat(ed)?|disconnection|disconnect(ed)?|immediate(ly)?|urgent(ly)?|within\\s*24\\s*h(ou)?rs?|expire(d)?|freeze|frozen|action\\s+required)\\b")
    private val panDemandRegex = Regex("(?i)\\b(complete|update|link|verify|submit|upload|re-?kyc|mandatory)\\s+(?:your\\s+)?(pan|pan\\s*card|kyc|aadhaar|identity|documents?)\\b|\\b(kyc\\s*verification|pan\\s*update|kyc\\s*update)\\b")
    private val apkRegex = Regex("(?i)(\\.apk\\b|download\\s+apk|install\\s+app|update\\s+application|install\\s+this\\s+file)\\b")
    private val lotteryRegex = Regex("(?i)(congratulations|kbc\\s*lucky\\s*draw|won\\s*(rs\\.?|₹)?[0-9,]+|lottery\\s*winner|cash\\s*prize)\\b")

    override fun detect(text: String): List<ExtractedEntity> {
        val list = mutableListOf<ExtractedEntity>()
        urgencyRegex.find(text)?.let {
            list.add(ExtractedEntity(EntityCategory.URGENCY_THREAT, "Coercive Urgency Threat", it.value, 0.92, "SocialEngineeringDetector"))
        }
        panDemandRegex.find(text)?.let {
            list.add(ExtractedEntity(EntityCategory.URGENCY_THREAT, "Mandatory KYC/PAN Demand", it.value, 0.90, "SocialEngineeringDetector"))
        }
        apkRegex.find(text)?.let {
            list.add(ExtractedEntity(EntityCategory.URGENCY_THREAT, "Unauthorized APK / App Installation Ask", it.value, 0.95, "SocialEngineeringDetector"))
        }
        lotteryRegex.find(text)?.let {
            list.add(ExtractedEntity(EntityCategory.URGENCY_THREAT, "Lottery / Prize Fraud Bait", it.value, 0.92, "SocialEngineeringDetector"))
        }
        return list
    }
}
