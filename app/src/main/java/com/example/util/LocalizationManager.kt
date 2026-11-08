package com.example.util

import com.example.data.remote.GeminiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class AppLanguage(val code: String, val displayName: String, val nativeName: String) {
    AFRIKAANS("af", "Afrikaans", "Afrikaans"),
    ARABIC("ar", "Arabic", "العربية"),
    ASSAMESE("as", "Assamese", "অসমীয়া"),
    BENGALI("bn", "Bengali", "বাংলা"),
    CHINESE_SIMPLIFIED("zh-CN", "Chinese (Simplified)", "简体中文"),
    CHINESE_TRADITIONAL("zh-TW", "Chinese (Traditional)", "繁體中文"),
    CZECH("cs", "Czech", "Čeština"),
    DUTCH("nl", "Dutch", "Nederlands"),
    ENGLISH("en", "English", "English (System)"),
    FILIPINO("fil", "Filipino", "Tagalog"),
    FINNISH("fi", "Finnish", "Suomi"),
    FRENCH("fr", "French", "Français"),
    GERMAN("de", "German", "Deutsch"),
    GREEK("el", "Greek", "Ελληνικά"),
    GUJARATI("gu", "Gujarati", "ગુજરાતી"),
    HEBREW("he", "Hebrew", "עברית"),
    HINDI("hi", "Hindi", "हिन्दी"),
    INDONESIAN("id", "Indonesian", "Bahasa Indonesia"),
    ITALIAN("it", "Italian", "Italiano"),
    JAPANESE("ja", "Japanese", "日本語"),
    KANNADA("kn", "Kannada", "ಕನ್ನಡ"),
    KOREAN("ko", "Korean", "한국어"),
    MALAY("ms", "Malay", "Bahasa Melayu"),
    MALAYALAM("ml", "Malayalam", "മലയാളം"),
    MARATHI("mr", "Marathi", "मराठी"),
    NEPALI("ne", "Nepali", "नेपाली"),
    ODIA("or", "Odia", "ଓଡ଼ିଆ"),
    PERSIAN("fa", "Persian", "فارسی"),
    POLISH("pl", "Polish", "Polski"),
    PORTUGUESE("pt", "Portuguese", "Português"),
    PUNJABI("pa", "Punjabi", "ਪੰਜਾਬੀ"),
    ROMANIAN("ro", "Romanian", "Română"),
    RUSSIAN("ru", "Russian", "Русский"),
    SPANISH("es", "Spanish", "Español"),
    SWAHILI("sw", "Swahili", "Kiswahili"),
    SWEDISH("sv", "Swedish", "Svenska"),
    TAMIL("ta", "Tamil", "தமிழ்"),
    TELUGU("te", "Telugu", "తెలుగు"),
    THAI("th", "Thai", "ไทย"),
    TURKISH("tr", "Turkish", "Türkçe"),
    UKRAINIAN("uk", "Ukrainian", "Українська"),
    URDU("ur", "Urdu", "اردو"),
    VIETNAMESE("vi", "Vietnamese", "Tiếng Việt");

    companion object {
        fun sortedAlphabetically(): List<AppLanguage> {
            return values().sortedBy { it.displayName }
        }

        fun fromCode(code: String): AppLanguage {
            return values().find { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }

        fun fromDisplayName(name: String): AppLanguage {
            return values().find { 
                it.displayName.equals(name, ignoreCase = true) || 
                it.nativeName.equals(name, ignoreCase = true) ||
                name.contains(it.displayName, ignoreCase = true) ||
                name.contains(it.nativeName, ignoreCase = true)
            } ?: ENGLISH
        }
    }
}

object LocalizationManager {

    private val translations: Map<String, Map<AppLanguage, String>> = mapOf(
        // Navigation Tabs
        "tab_chats" to mapOf(
            AppLanguage.ENGLISH to "Chats",
            AppLanguage.SPANISH to "Chats",
            AppLanguage.FRENCH to "Discussions",
            AppLanguage.GERMAN to "Chats",
            AppLanguage.HINDI to "चैट्स",
            AppLanguage.BENGALI to "চ্যাট",
            AppLanguage.TAMIL to "அட்டைகள்",
            AppLanguage.TELUGU to "చాట్‌లు",
            AppLanguage.MARATHI to "चॅट्स",
            AppLanguage.GUJARATI to "ચેટ્સ",
            AppLanguage.JAPANESE to "チャット",
            AppLanguage.PORTUGUESE to "Conversas",
            AppLanguage.ARABIC to "المحادثات",
            AppLanguage.RUSSIAN to "Чаты"
        ),
        "tab_status" to mapOf(
            AppLanguage.ENGLISH to "Status",
            AppLanguage.SPANISH to "Estado",
            AppLanguage.FRENCH to "Statut",
            AppLanguage.GERMAN to "Status",
            AppLanguage.HINDI to "स्टेटस",
            AppLanguage.BENGALI to "স্ট্যাটাস",
            AppLanguage.TAMIL to "நிலை",
            AppLanguage.TELUGU to "స్టేటస్",
            AppLanguage.MARATHI to "स्टेटस",
            AppLanguage.GUJARATI to "સ્ટેટસ",
            AppLanguage.JAPANESE to "ステータス",
            AppLanguage.PORTUGUESE to "Status",
            AppLanguage.ARABIC to "الحالة",
            AppLanguage.RUSSIAN to "Статус"
        ),
        "tab_friends" to mapOf(
            AppLanguage.ENGLISH to "Friends",
            AppLanguage.SPANISH to "Amigos",
            AppLanguage.FRENCH to "Amis",
            AppLanguage.GERMAN to "Freunde",
            AppLanguage.HINDI to "मित्र",
            AppLanguage.BENGALI to "বন্ধু",
            AppLanguage.TAMIL to "நண்பர்கள்",
            AppLanguage.TELUGU to "స్నేహితులు",
            AppLanguage.MARATHI to "मित्र",
            AppLanguage.GUJARATI to "મિત્રો",
            AppLanguage.JAPANESE to "友達",
            AppLanguage.PORTUGUESE to "Amigos",
            AppLanguage.ARABIC to "الأصدقاء",
            AppLanguage.RUSSIAN to "Друзья"
        ),
        "tab_calls" to mapOf(
            AppLanguage.ENGLISH to "Calls",
            AppLanguage.SPANISH to "Llamadas",
            AppLanguage.FRENCH to "Appels",
            AppLanguage.GERMAN to "Anrufe",
            AppLanguage.HINDI to "कॉल",
            AppLanguage.BENGALI to "কল",
            AppLanguage.TAMIL to "அழைப்புகள்",
            AppLanguage.TELUGU to "కాల్స్",
            AppLanguage.MARATHI to "कॉल",
            AppLanguage.GUJARATI to "કોલ્સ",
            AppLanguage.JAPANESE to "通話",
            AppLanguage.PORTUGUESE to "Chamadas",
            AppLanguage.ARABIC to "المكالمات",
            AppLanguage.RUSSIAN to "Звонки"
        ),
        "tab_settings" to mapOf(
            AppLanguage.ENGLISH to "Settings",
            AppLanguage.SPANISH to "Ajustes",
            AppLanguage.FRENCH to "Paramètres",
            AppLanguage.GERMAN to "Einstellungen",
            AppLanguage.HINDI to "सेटिंग्स",
            AppLanguage.BENGALI to "সেটিংস",
            AppLanguage.TAMIL to "அமைப்புகள்",
            AppLanguage.TELUGU to "సెట్టింగ్‌లు",
            AppLanguage.MARATHI to "सेटिंग्ज",
            AppLanguage.GUJARATI to "સેટિંગ્સ",
            AppLanguage.JAPANESE to "設定",
            AppLanguage.PORTUGUESE to "Configurações",
            AppLanguage.ARABIC to "الإعدادات",
            AppLanguage.RUSSIAN to "Настройки"
        ),

        // Settings Screen
        "settings_title" to mapOf(
            AppLanguage.ENGLISH to "Settings",
            AppLanguage.SPANISH to "Ajustes",
            AppLanguage.FRENCH to "Paramètres",
            AppLanguage.GERMAN to "Einstellungen",
            AppLanguage.HINDI to "सेटिंग्स",
            AppLanguage.BENGALI to "সেটিংস",
            AppLanguage.TAMIL to "அமைப்புகள்",
            AppLanguage.TELUGU to "సెట్టింగ్‌లు",
            AppLanguage.JAPANESE to "設定",
            AppLanguage.PORTUGUESE to "Configurações",
            AppLanguage.ARABIC to "الإعدادات"
        ),
        "settings_search_placeholder" to mapOf(
            AppLanguage.ENGLISH to "Search settings...",
            AppLanguage.SPANISH to "Buscar ajustes...",
            AppLanguage.FRENCH to "Rechercher des paramètres...",
            AppLanguage.GERMAN to "Einstellungen suchen...",
            AppLanguage.HINDI to "सेटिंग्स खोजें...",
            AppLanguage.BENGALI to "সেটিংস খুঁজুন...",
            AppLanguage.TAMIL to "அமைப்புகளைத் தேடு...",
            AppLanguage.TELUGU to "సెట్టింగ్‌లను శోధించండి...",
            AppLanguage.JAPANESE to "設定を検索...",
            AppLanguage.PORTUGUESE to "Pesquisar configurações..."
        ),
        "item_payments" to mapOf(
            AppLanguage.ENGLISH to "Payments",
            AppLanguage.SPANISH to "Pagos",
            AppLanguage.FRENCH to "Paiements",
            AppLanguage.GERMAN to "Zahlungen",
            AppLanguage.HINDI to "भुगतान",
            AppLanguage.BENGALI to "পেমেন্ট",
            AppLanguage.TAMIL to "செலுத்தல்கள்",
            AppLanguage.TELUGU to "చెల్లింపులు",
            AppLanguage.JAPANESE to "支払い",
            AppLanguage.PORTUGUESE to "Pagamentos"
        ),
        "item_payments_sub" to mapOf(
            AppLanguage.ENGLISH to "History, payment methods, UPI",
            AppLanguage.SPANISH to "Historial, métodos de pago",
            AppLanguage.FRENCH to "Historique, modes de paiement",
            AppLanguage.GERMAN to "Verlauf, Zahlungsmethoden",
            AppLanguage.HINDI to "इतिहास, भुगतान विधियां",
            AppLanguage.BENGALI to "ইতিহাস, পেমেন্ট পদ্ধতি",
            AppLanguage.JAPANESE to "履歴、支払い方法",
            AppLanguage.PORTUGUESE to "Histórico, métodos de pagamento"
        ),
        "item_account" to mapOf(
            AppLanguage.ENGLISH to "Account",
            AppLanguage.SPANISH to "Cuenta",
            AppLanguage.FRENCH to "Compte",
            AppLanguage.GERMAN to "Konto",
            AppLanguage.HINDI to "खाता",
            AppLanguage.BENGALI to "অ্যাকাউন্ট",
            AppLanguage.TAMIL to "கணக்கு",
            AppLanguage.TELUGU to "ఖాతా",
            AppLanguage.JAPANESE to "アカウント",
            AppLanguage.PORTUGUESE to "Conta"
        ),
        "item_account_sub" to mapOf(
            AppLanguage.ENGLISH to "Security notifications, change number, two-step verification",
            AppLanguage.SPANISH to "Notificaciones de seguridad, número, verificación en dos pasos",
            AppLanguage.FRENCH to "Notifications de sécurité, changer de numéro, vérification en deux étapes",
            AppLanguage.GERMAN to "Sicherheitsbenachrichtigungen, Nummer ändern, Verifizierung",
            AppLanguage.HINDI to "सुरक्षा सूचनाएं, नंबर बदलें, दो-चरण सत्यापन",
            AppLanguage.BENGALI to "সিকিউরিটি নোটিফিকেশন, নম্বর পরিবর্তন",
            AppLanguage.JAPANESE to "セキュリティ通知、番号変更、2段階認証",
            AppLanguage.PORTUGUESE to "Notificações de segurança, alterar número, verificação em duas etapas"
        ),
        "item_privacy" to mapOf(
            AppLanguage.ENGLISH to "Privacy",
            AppLanguage.SPANISH to "Privacidad",
            AppLanguage.FRENCH to "Confidentialité",
            AppLanguage.GERMAN to "Datenschutz",
            AppLanguage.HINDI to "गोपनीयता",
            AppLanguage.BENGALI to "গোপনীয়তা",
            AppLanguage.TAMIL to "தனியுரிமை",
            AppLanguage.TELUGU to "గోప్యత",
            AppLanguage.JAPANESE to "プライバシー",
            AppLanguage.PORTUGUESE to "Privacidade"
        ),
        "item_privacy_sub" to mapOf(
            AppLanguage.ENGLISH to "Blocked accounts, last seen, read receipts",
            AppLanguage.SPANISH to "Cuentas bloqueadas, hora de última vez, confirmaciones de lectura",
            AppLanguage.FRENCH to "Comptes bloqués, présence en ligne, confirmations de lecture",
            AppLanguage.GERMAN to "Blockierte Konten, Zuletzt gesehen, Lesebestätigungen",
            AppLanguage.HINDI to "ब्लॉक किए गए खाते, अंतिम बार देखा गया, पठन रसीदें",
            AppLanguage.JAPANESE to "ブロックされたアカウント、最終ログイン、既読確認",
            AppLanguage.PORTUGUESE to "Contas bloqueadas, visto por último, confirmações de leitura"
        ),
        "item_lists" to mapOf(
            AppLanguage.ENGLISH to "Lists",
            AppLanguage.SPANISH to "Listas",
            AppLanguage.FRENCH to "Listes",
            AppLanguage.GERMAN to "Listen",
            AppLanguage.HINDI to "सूचियां",
            AppLanguage.BENGALI to "তালিকা",
            AppLanguage.JAPANESE to "リスト",
            AppLanguage.PORTUGUESE to "Listas"
        ),
        "item_lists_sub" to mapOf(
            AppLanguage.ENGLISH to "Manage people and groups",
            AppLanguage.SPANISH to "Gestionar personas y grupos",
            AppLanguage.FRENCH to "Gérer les personnes et les groupes",
            AppLanguage.GERMAN to "Personen und Gruppen verwalten",
            AppLanguage.HINDI to "लोगों और समूहों का प्रबंधन करें",
            AppLanguage.JAPANESE to "人とグループを管理",
            AppLanguage.PORTUGUESE to "Gerenciar pessoas e grupos"
        ),
        "item_chats" to mapOf(
            AppLanguage.ENGLISH to "Chats",
            AppLanguage.SPANISH to "Chats",
            AppLanguage.FRENCH to "Discussions",
            AppLanguage.GERMAN to "Chats",
            AppLanguage.HINDI to "चैट्स",
            AppLanguage.BENGALI to "চ্যাট",
            AppLanguage.JAPANESE to "チャット",
            AppLanguage.PORTUGUESE to "Conversas"
        ),
        "item_chats_sub" to mapOf(
            AppLanguage.ENGLISH to "Theme, wallpapers, chat history, media visibility",
            AppLanguage.SPANISH to "Tema, fondos de pantalla, historial de chat, visibilidad de archivos",
            AppLanguage.FRENCH to "Thème, fonds d'écran, historique de discussion",
            AppLanguage.GERMAN to "Design, Hintergrundbilder, Chat-Verlauf",
            AppLanguage.HINDI to "थीम, वॉलपेपर, चैट इतिहास",
            AppLanguage.JAPANESE to "テーマ、壁紙、チャット履歴",
            AppLanguage.PORTUGUESE to "Tema, papéis de parede, histórico de chat"
        ),
        "item_appearance" to mapOf(
            AppLanguage.ENGLISH to "Appearance",
            AppLanguage.SPANISH to "Apariencia",
            AppLanguage.FRENCH to "Apparence",
            AppLanguage.GERMAN to "Erscheinungsbild",
            AppLanguage.HINDI to "दिखावट",
            AppLanguage.BENGALI to "উপস্থিতি",
            AppLanguage.JAPANESE to "外観",
            AppLanguage.PORTUGUESE to "Aparência"
        ),
        "item_appearance_sub" to mapOf(
            AppLanguage.ENGLISH to "Chat theme, app icon, accent color theme",
            AppLanguage.SPANISH to "Tema de chat, icono de app, tema de color",
            AppLanguage.FRENCH to "Thème de discussion, icône d'application, couleur d'accentuation",
            AppLanguage.GERMAN to "Chat-Design, App-Icon, Akzentfarben",
            AppLanguage.HINDI to "चैट थीम, ऐप आइकन, रंग पैलेट",
            AppLanguage.JAPANESE to "チャットテーマ、アプリのアイコン、アクセントカラー",
            AppLanguage.PORTUGUESE to "Tema do chat, ícone do app, cores de destaque"
        ),
        "item_broadcasts" to mapOf(
            AppLanguage.ENGLISH to "Broadcasts",
            AppLanguage.SPANISH to "Difusiones",
            AppLanguage.FRENCH to "Diffusions",
            AppLanguage.GERMAN to "Broadcasts",
            AppLanguage.HINDI to "प्रसारण (ब्रॉडकास्ट)",
            AppLanguage.BENGALI to "প্রচার (ব্রডকাস্ট)",
            AppLanguage.JAPANESE to "一斉送信",
            AppLanguage.PORTUGUESE to "Transmissões"
        ),
        "item_broadcasts_sub" to mapOf(
            AppLanguage.ENGLISH to "Manage lists and send broadcasts",
            AppLanguage.SPANISH to "Gestionar listas y enviar difusiones",
            AppLanguage.FRENCH to "Gérer les listes et envoyer des diffusions",
            AppLanguage.GERMAN to "Listen verwalten und Broadcasts senden",
            AppLanguage.HINDI to "सूचियां प्रबंधित करें और प्रसारण भेजें",
            AppLanguage.JAPANESE to "リストを管理して一斉送信",
            AppLanguage.PORTUGUESE to "Gerenciar listas e enviar transmissões"
        ),
        "item_notifications" to mapOf(
            AppLanguage.ENGLISH to "Notifications",
            AppLanguage.SPANISH to "Notificaciones",
            AppLanguage.FRENCH to "Notifications",
            AppLanguage.GERMAN to "Benachrichtigungen",
            AppLanguage.HINDI to "सूचनाएं",
            AppLanguage.BENGALI to "বিজ্ঞপ্তি",
            AppLanguage.TAMIL to "அறிவிப்புகள்",
            AppLanguage.TELUGU to "నోటిఫికేషన్‌లు",
            AppLanguage.JAPANESE to "通知",
            AppLanguage.PORTUGUESE to "Notificações"
        ),
        "item_notifications_sub" to mapOf(
            AppLanguage.ENGLISH to "Message, group & call tones",
            AppLanguage.SPANISH to "Tonos de mensajes, grupos y llamadas",
            AppLanguage.FRENCH to "Tonalités de message, groupe et appel",
            AppLanguage.GERMAN to "Töne für Nachrichten, Gruppen und Anrufe",
            AppLanguage.HINDI to "संदेश, समूह और कॉल टोन",
            AppLanguage.JAPANESE to "メッセージ、グループ、通話の通知音",
            AppLanguage.PORTUGUESE to "Tons de mensagem, grupo e chamada"
        ),
        "item_storage" to mapOf(
            AppLanguage.ENGLISH to "Storage and Data",
            AppLanguage.SPANISH to "Almacenamiento y datos",
            AppLanguage.FRENCH to "Stockage et données",
            AppLanguage.GERMAN to "Speicher und Daten",
            AppLanguage.HINDI to "स्टोरेज और डेटा",
            AppLanguage.BENGALI to "স্টোরেজ ও ডেটা",
            AppLanguage.JAPANESE to "ストレージとデータ",
            AppLanguage.PORTUGUESE to "Armazenamento e dados"
        ),
        "item_storage_sub" to mapOf(
            AppLanguage.ENGLISH to "Network usage, auto-download media",
            AppLanguage.SPANISH to "Uso de red, descarga automática de archivos",
            AppLanguage.FRENCH to "Utilisation du réseau, téléchargement automatique",
            AppLanguage.GERMAN to "Netzwerknutzung, automatischer Medien-Download",
            AppLanguage.HINDI to "नेटवर्क उपयोग, ऑटो-डाउनलोड मीडिया",
            AppLanguage.JAPANESE to "ネットワーク使用量、メディアの自動ダウンロード",
            AppLanguage.PORTUGUESE to "Uso de rede, download automático de mídia"
        ),
        "item_accessibility" to mapOf(
            AppLanguage.ENGLISH to "Accessibility",
            AppLanguage.SPANISH to "Accesibilidad",
            AppLanguage.FRENCH to "Accessibilité",
            AppLanguage.GERMAN to "Barrierefreiheit",
            AppLanguage.HINDI to "पहुंच-योग्यता (एक्सेसिबिलिटी)",
            AppLanguage.BENGALI to "অ্যাক্সেসিবিলিটি",
            AppLanguage.JAPANESE to "アクセシビリティ",
            AppLanguage.PORTUGUESE to "Acessibilidade"
        ),
        "item_accessibility_sub" to mapOf(
            AppLanguage.ENGLISH to "Increase contrast, reduce animation",
            AppLanguage.SPANISH to "Aumentar contraste, reducir animaciones",
            AppLanguage.FRENCH to "Augmenter le contraste, réduire les animations",
            AppLanguage.GERMAN to "Kontrast erhöhen, Animationen reduzieren",
            AppLanguage.HINDI to "कंट्रास्ट बढ़ाएं, एनिमेशन कम करें",
            AppLanguage.JAPANESE to "コントラストを上げる、アニメーションを減らす",
            AppLanguage.PORTUGUESE to "Aumentar contraste, reduzir animações"
        ),
        "item_language" to mapOf(
            AppLanguage.ENGLISH to "App Language",
            AppLanguage.SPANISH to "Idioma de la aplicación",
            AppLanguage.FRENCH to "Langue de l'application",
            AppLanguage.GERMAN to "App-Sprache",
            AppLanguage.HINDI to "ऐप भाषा",
            AppLanguage.BENGALI to "অ্যাপের ভাষা",
            AppLanguage.TAMIL to "செயலி மொழி",
            AppLanguage.TELUGU to "యాప్ భాష",
            AppLanguage.JAPANESE to "アプリの言語",
            AppLanguage.PORTUGUESE to "Idioma do aplicativo"
        ),
        "item_help" to mapOf(
            AppLanguage.ENGLISH to "Help and Feedback",
            AppLanguage.SPANISH to "Ayuda y comentarios",
            AppLanguage.FRENCH to "Aide et commentaires",
            AppLanguage.GERMAN to "Hilfe und Feedback",
            AppLanguage.HINDI to "सहायता और प्रतिक्रिया",
            AppLanguage.BENGALI to "সাহায্য ও প্রতিক্রিয়া",
            AppLanguage.JAPANESE to "ヘルプとフィードバック",
            AppLanguage.PORTUGUESE to "Ajuda e feedback"
        ),
        "item_help_sub" to mapOf(
            AppLanguage.ENGLISH to "Help centre, contact us, privacy policy",
            AppLanguage.SPANISH to "Centro de ayuda, contáctanos, política de privacidad",
            AppLanguage.FRENCH to "Centre d'aide, nous contacter, politique de confidentialité",
            AppLanguage.GERMAN to "Hilfe-Center, Kontakt, Datenschutz",
            AppLanguage.HINDI to "सहायता केंद्र, संपर्क करें, गोपनीयता नीति",
            AppLanguage.JAPANESE to "ヘルプセンター、お問い合わせ、プライバシーポリシー",
            AppLanguage.PORTUGUESE to "Central de ajuda, fale conosco, política de privacidade"
        ),
        "item_invite" to mapOf(
            AppLanguage.ENGLISH to "Invite a Friend",
            AppLanguage.SPANISH to "Invitar a un amigo",
            AppLanguage.FRENCH to "Inviter un ami",
            AppLanguage.GERMAN to "Einen Freund einladen",
            AppLanguage.HINDI to "मित्र को आमंत्रित करें",
            AppLanguage.BENGALI to "বন্ধুকে আমন্ত্রণ জানান",
            AppLanguage.JAPANESE to "友達を招待",
            AppLanguage.PORTUGUESE to "Convidar um amigo"
        ),
        "item_invite_sub" to mapOf(
            AppLanguage.ENGLISH to "Share Linko invite link",
            AppLanguage.SPANISH to "Compartir enlace de invitación a Linko",
            AppLanguage.FRENCH to "Partager le lien d'invitation Linko",
            AppLanguage.GERMAN to "Linko Einladungslink teilen",
            AppLanguage.HINDI to "Linko आमंत्रण लिंक साझा करें",
            AppLanguage.JAPANESE to "Linkoの招待リンクを共有",
            AppLanguage.PORTUGUESE to "Compartilhar link de convite do Linko"
        ),
        "item_accounts_centre" to mapOf(
            AppLanguage.ENGLISH to "Accounts Centre",
            AppLanguage.SPANISH to "Centro de cuentas",
            AppLanguage.FRENCH to "Espace comptes",
            AppLanguage.GERMAN to "Konten-Übersicht",
            AppLanguage.HINDI to "खाता केंद्र",
            AppLanguage.BENGALI to "অ্যাকাউন্টস সেন্টার",
            AppLanguage.JAPANESE to "アカウントセンター",
            AppLanguage.PORTUGUESE to "Central de contas"
        ),
        "sign_out_button" to mapOf(
            AppLanguage.ENGLISH to "Sign Out Account",
            AppLanguage.SPANISH to "Cerrar sesión de la cuenta",
            AppLanguage.FRENCH to "Se déconnecter du compte",
            AppLanguage.GERMAN to "Konto abmelden",
            AppLanguage.HINDI to "खाता साइन आउट करें",
            AppLanguage.BENGALI to "অ্যাকাউন্ট সাইন আউট করুন",
            AppLanguage.JAPANESE to "アカウントからサインアウト",
            AppLanguage.PORTUGUESE to "Sair da conta"
        ),

        // General dialog buttons
        "btn_save" to mapOf(
            AppLanguage.ENGLISH to "Save Changes",
            AppLanguage.SPANISH to "Guardar cambios",
            AppLanguage.FRENCH to "Enregistrer",
            AppLanguage.GERMAN to "Änderungen speichern",
            AppLanguage.HINDI to "बदलाव सहेजें",
            AppLanguage.BENGALI to "পরিবর্তন সংরক্ষণ করুন",
            AppLanguage.JAPANESE to "変更を保存",
            AppLanguage.PORTUGUESE to "Salvar alterações"
        ),
        "btn_cancel" to mapOf(
            AppLanguage.ENGLISH to "Cancel",
            AppLanguage.SPANISH to "Cancelar",
            AppLanguage.FRENCH to "Annuler",
            AppLanguage.GERMAN to "Abbrechen",
            AppLanguage.HINDI to "रद्द करें",
            AppLanguage.BENGALI to "বাতিল",
            AppLanguage.JAPANESE to "キャンセル",
            AppLanguage.PORTUGUESE to "Cancelar"
        ),
        "btn_done" to mapOf(
            AppLanguage.ENGLISH to "Done",
            AppLanguage.SPANISH to "Listo",
            AppLanguage.FRENCH to "Terminé",
            AppLanguage.GERMAN to "Fertig",
            AppLanguage.HINDI to "संपन्न",
            AppLanguage.BENGALI to "সম্পন্ন",
            AppLanguage.JAPANESE to "完了",
            AppLanguage.PORTUGUESE to "Concluído"
        ),
        "btn_close" to mapOf(
            AppLanguage.ENGLISH to "Close",
            AppLanguage.SPANISH to "Cerrar",
            AppLanguage.FRENCH to "Fermer",
            AppLanguage.GERMAN to "Schließen",
            AppLanguage.HINDI to "बंद करें",
            AppLanguage.BENGALI to "বন্ধ করুন",
            AppLanguage.JAPANESE to "閉じる",
            AppLanguage.PORTUGUESE to "Fechar"
        )
    )

    fun getString(key: String, language: AppLanguage): String {
        val found = translations[key]?.get(language)
        if (found != null) return found
        
        // If not in standard dictionary map, provide clean fallback
        return translations[key]?.get(AppLanguage.ENGLISH) ?: key
    }

    // Dynamic AI Translation Engine for Chat Messages using Gemini AI API with fallback
    suspend fun translateChatMessage(
        originalMessage: String,
        targetLanguage: AppLanguage
    ): String = withContext(Dispatchers.IO) {
        if (targetLanguage == AppLanguage.ENGLISH || originalMessage.isBlank()) {
            return@withContext originalMessage
        }

        // Common phrase dictionary for fast translation
        val fastMap = getFastPhraseDictionary(originalMessage, targetLanguage)
        if (fastMap != null) {
            return@withContext fastMap
        }

        // Call Gemini AI for dynamic high-quality translation
        val prompt = "Translate the following chat message into ${targetLanguage.displayName} (${targetLanguage.nativeName}). Return ONLY the translated string without quotes, commentary, or explanation:\n\n$originalMessage"
        
        try {
            val response = GeminiClient.generateResponse(prompt)
            if (response.isNotBlank() && !response.contains("Gemini AI Assistant Error") && !response.contains("Note: Please set a valid Gemini API Key")) {
                return@withContext response.trim().removeSurrounding("\"")
            }
        } catch (_: Exception) {
            // Fallback
        }

        // Fallback dictionary or smart transformation if Gemini API is offline/key missing
        return@withContext getFallbackTranslation(originalMessage, targetLanguage)
    }

    private fun getFastPhraseDictionary(text: String, lang: AppLanguage): String? {
        val normalized = text.trim().lowercase()
        return when (normalized) {
            "hello", "hi", "hey" -> when (lang) {
                AppLanguage.SPANISH -> "¡Hola!"
                AppLanguage.FRENCH -> "Bonjour !"
                AppLanguage.GERMAN -> "Hallo!"
                AppLanguage.HINDI -> "नमस्ते!"
                AppLanguage.BENGALI -> "হ্যালো!"
                AppLanguage.TAMIL -> "வணக்கம்!"
                AppLanguage.TELUGU -> "నమస్కారం!"
                AppLanguage.MARATHI -> "नमस्कार!"
                AppLanguage.GUJARATI -> "નમસ્તે!"
                AppLanguage.JAPANESE -> "こんにちは！"
                AppLanguage.PORTUGUESE -> "Olá!"
                AppLanguage.ARABIC -> "مرحبا!"
                AppLanguage.RUSSIAN -> "Привет!"
                else -> null
            }
            "how are you?", "how are you" -> when (lang) {
                AppLanguage.SPANISH -> "¿Cómo estás?"
                AppLanguage.FRENCH -> "Comment vas-tu ?"
                AppLanguage.GERMAN -> "Wie geht es dir?"
                AppLanguage.HINDI -> "आप कैसे हैं?"
                AppLanguage.BENGALI -> "আপনি কেমন আছেন?"
                AppLanguage.TAMIL -> "எப்படி இருக்கிறீர்கள்?"
                AppLanguage.TELUGU -> "ఎలా ఉన్నారు?"
                AppLanguage.MARATHI -> "तुम्ही कसे आहात?"
                AppLanguage.GUJARATI -> "તમે કેમ છો?"
                AppLanguage.JAPANESE -> "お元気ですか？"
                AppLanguage.PORTUGUESE -> "Como você está?"
                AppLanguage.ARABIC -> "كيف حالك؟"
                AppLanguage.RUSSIAN -> "Как дела?"
                else -> null
            }
            "good morning" -> when (lang) {
                AppLanguage.SPANISH -> "Buenos días"
                AppLanguage.FRENCH -> "Bonjour"
                AppLanguage.GERMAN -> "Guten Morgen"
                AppLanguage.HINDI -> "सुप्रभात"
                AppLanguage.BENGALI -> "শুভ সকাল"
                AppLanguage.JAPANESE -> "おはようございます"
                AppLanguage.PORTUGUESE -> "Bom dia"
                else -> null
            }
            "good night" -> when (lang) {
                AppLanguage.SPANISH -> "Buenas noches"
                AppLanguage.FRENCH -> "Bonne nuit"
                AppLanguage.GERMAN -> "Gute Nacht"
                AppLanguage.HINDI -> "शुभ रात्रि"
                AppLanguage.BENGALI -> "শুভরাত্রি"
                AppLanguage.JAPANESE -> "おやすみなさい"
                AppLanguage.PORTUGUESE -> "Boa noite"
                else -> null
            }
            "thank you", "thanks" -> when (lang) {
                AppLanguage.SPANISH -> "¡Muchas gracias!"
                AppLanguage.FRENCH -> "Merci beaucoup !"
                AppLanguage.GERMAN -> "Vielen Dank!"
                AppLanguage.HINDI -> "धन्यवाद!"
                AppLanguage.BENGALI -> "ধন্যবাদ!"
                AppLanguage.JAPANESE -> "ありがとうございます！"
                AppLanguage.PORTUGUESE -> "Muito obrigado!"
                else -> null
            }
            "yes" -> when (lang) {
                AppLanguage.SPANISH -> "Sí"
                AppLanguage.FRENCH -> "Oui"
                AppLanguage.GERMAN -> "Ja"
                AppLanguage.HINDI -> "हाँ"
                AppLanguage.BENGALI -> "হ্যাঁ"
                AppLanguage.JAPANESE -> "はい"
                AppLanguage.PORTUGUESE -> "Sim"
                else -> null
            }
            "no" -> when (lang) {
                AppLanguage.SPANISH -> "No"
                AppLanguage.FRENCH -> "Non"
                AppLanguage.GERMAN -> "Nein"
                AppLanguage.HINDI -> "नहीं"
                AppLanguage.BENGALI -> "না"
                AppLanguage.JAPANESE -> "いいえ"
                AppLanguage.PORTUGUESE -> "Não"
                else -> null
            }
            else -> null
        }
    }

    private fun getFallbackTranslation(text: String, lang: AppLanguage): String {
        return "[${lang.nativeName}] $text"
    }
}
